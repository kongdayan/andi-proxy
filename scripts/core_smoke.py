#!/usr/bin/env python3
"""Real TCP/UDP routing and SOCKS5 password tests using the app-generated configuration."""
import contextlib
import copy
import http.server
import json
import pathlib
import socket
import struct
import subprocess
import tempfile
import threading
import time

ROOT = pathlib.Path(__file__).resolve().parents[1]
CORE = ROOT / ".tools/core/sing-box-1.14.2-darwin-arm64/sing-box"
BASE = json.loads((ROOT / "app/build/test-configs/socks.json").read_text())

def receive(sock, count):
    data = bytearray()
    while len(data) < count:
        chunk = sock.recv(count - len(data))
        if not chunk:
            raise RuntimeError("Unexpected EOF")
        data.extend(chunk)
    return bytes(data)

def address(sock):
    kind = receive(sock, 1)[0]
    if kind == 1:
        host = socket.inet_ntop(socket.AF_INET, receive(sock, 4))
    elif kind == 4:
        host = socket.inet_ntop(socket.AF_INET6, receive(sock, 16))
    else:
        host = receive(sock, receive(sock, 1)[0]).decode()
    return host, struct.unpack("!H", receive(sock, 2))[0]

def socks(port, host, destination_port, command=1, auth=None):
    s = socket.create_connection(("127.0.0.1", port), timeout=5)
    s.settimeout(5)
    s.sendall(bytes([5, 1, 2 if auth else 0]))
    assert receive(s, 2) == bytes([5, 2 if auth else 0])
    if auth:
        user, password = [x.encode() for x in auth]
        s.sendall(bytes([1, len(user)]) + user + bytes([len(password)]) + password)
        reply = receive(s, 2)
        if reply != b"\x01\x00":
            s.close()
            raise RuntimeError("Authentication rejected")
    encoded = host.encode()
    s.sendall(bytes([5, command, 0, 3, len(encoded)]) + encoded + struct.pack("!H", destination_port))
    version, reply, reserved = receive(s, 3)
    if version != 5 or reply != 0:
        s.close()
        raise RuntimeError("SOCKS connection rejected")
    target = address(s)
    return s, target

@contextlib.contextmanager
def engine(config, folder, name):
    path = pathlib.Path(folder) / f"{name}.json"
    path.write_text(json.dumps(config))
    subprocess.run([str(CORE), "check", "-c", str(path)], check=True, capture_output=True)
    with open(pathlib.Path(folder) / f"{name}.log", "wb") as log:
        proc = subprocess.Popen([str(CORE), "run", "-c", str(path)], stdout=log, stderr=log)
        try:
            port = config["inbounds"][0]["listen_port"]
            for _ in range(60):
                if proc.poll() is not None:
                    raise RuntimeError(f"{name}: engine exited")
                try:
                    with socket.create_connection(("127.0.0.1", port), timeout=0.1):
                        break
                except OSError:
                    time.sleep(0.05)
            else:
                raise RuntimeError(f"{name}: startup timeout")
            yield
        finally:
            proc.terminate()
            try:
                proc.wait(timeout=5)
            except subprocess.TimeoutExpired:
                proc.kill()
                proc.wait()

class Handler(http.server.BaseHTTPRequestHandler):
    def do_GET(self):
        self.send_response(200)
        self.end_headers()
        self.wfile.write(b"ANDI_ROUTE_OK")
    def log_message(self, *args):
        pass

def fetch(port, host, target, auth=None):
    s, _ = socks(port, host, target, auth=auth)
    with s:
        s.sendall((f"GET / HTTP/1.0\r\nHost: {host}\r\n\r\n").encode())
        data = bytearray()
        while chunk := s.recv(4096):
            data.extend(chunk)
    if b"ANDI_ROUTE_OK" not in data:
        raise RuntimeError("Upstream did not forward the HTTP request")

def rejected(action):
    try:
        action()
    except (RuntimeError, OSError):
        return
    raise AssertionError("Invalid credentials unexpectedly accepted")

def main():
    server = http.server.ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    echo = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    echo.bind(("127.0.0.1", 0))
    def udp_echo():
        while True:
            try:
                data, peer = echo.recvfrom(4096)
                echo.sendto(data, peer)
            except OSError:
                return
    threading.Thread(target=udp_echo, daemon=True).start()
    upstream = {"log": {"level": "error"}, "inbounds": [{"type": "socks", "tag": "upstream", "listen": "127.0.0.1", "listen_port": 19080, "users": [{"username": "test-user", "password": "test-pass"}]}], "outbounds": [{"type": "direct", "tag": "direct"}]}
    client = copy.deepcopy(BASE)
    client["inbounds"][0]["listen_port"] = 19081
    try:
        with tempfile.TemporaryDirectory(prefix="core-test-", dir=ROOT / ".tools") as folder, engine(upstream, folder, "upstream"):
            with engine(client, folder, "client"):
                fetch(19081, "localhost", server.server_port)
                print("PASS SOCKS5 upstream username/password authentication + proxy domain route")
                tcp, relay = socks(19081, "0.0.0.0", 0, command=3)
                with tcp, socket.socket(socket.AF_INET, socket.SOCK_DGRAM) as udp:
                    udp.settimeout(5)
                    packet = b"\x00\x00\x00\x03\x09localhost" + struct.pack("!H", echo.getsockname()[1]) + b"ANDI_UDP_OK"
                    udp.sendto(packet, relay)
                    response, _ = udp.recvfrom(4096)
                    assert response.endswith(b"ANDI_UDP_OK")
                print("PASS SOCKS5 UDP ASSOCIATE echo through authenticated upstream")
            wrong = copy.deepcopy(client)
            wrong["outbounds"][0]["password"] = "incorrect-test-password"
            with engine(wrong, folder, "wrong-password"):
                rejected(lambda: fetch(19081, "localhost", server.server_port))
                fetch(19081, "127.0.0.1", server.server_port)
                print("PASS wrong upstream password rejected; direct route still works")
            local = copy.deepcopy(client)
            local["inbounds"][0]["users"] = [{"username": "hotspot", "password": "local-test-password"}]
            with engine(local, folder, "local-auth"):
                rejected(lambda: fetch(19081, "localhost", server.server_port, ("hotspot", "wrong-password")))
                fetch(19081, "localhost", server.server_port, ("hotspot", "local-test-password"))
                print("PASS local shared proxy authentication accepts correct and rejects wrong password")
            subscription=json.loads((ROOT/"app/build/test-configs/subscription-socks.json").read_text())
            subscription["inbounds"][0]["listen_port"]=19081
            with engine(subscription,folder,"subscription"):
                fetch(19081,"localhost",server.server_port)
                rejected(lambda: fetch(19081,"blocked.invalid",server.server_port))
                subscription["outbounds"][0]["password"]="wrong-test-password"
            with engine(subscription,folder,"subscription-wrong-password"):
                rejected(lambda: fetch(19081,"localhost",server.server_port))
                fetch(19081,"127.0.0.1",server.server_port)
            print("PASS converted subscription: PROXY routes/authenticates; REJECT blocks; DIRECT bypasses bad upstream")
    finally:
        server.shutdown()
        server.server_close()
        echo.close()

if __name__ == "__main__":
    main()
