#!/usr/bin/env python3
"""Bounded macOS hotspot association test; always restore the original Wi-Fi."""
import ipaddress,pathlib,re,subprocess,time,xml.etree.ElementTree as E
ROOT=pathlib.Path(__file__).resolve().parents[1]
ADB=str(ROOT/'.tools/android-sdk/platform-tools/adb')
def run(args,timeout=20):return subprocess.run(args,capture_output=True,text=True,timeout=timeout)
run([ADB,'shell','uiautomator','dump','/sdcard/andi-ui.xml'])
x=run([ADB,'shell','cat','/sdcard/andi-ui.xml']).stdout
texts='\n'.join(n.get('text','') for n in E.fromstring(x).iter('node'))
ssid=re.search(r'热点名称：([^\n]+)',texts).group(1)
match=re.search(r'(?:Wi-Fi 密码|密码)：([^\n]+)',texts)
if not match or '•' in match.group(1):raise SystemExit('Open connection guide and show Wi-Fi password first')
password=match.group(1)
address=re.search(r'(?:HTTP / SOCKS5|服务器)：([0-9.]+)',texts).group(1)
upstream=run([ADB,'shell','cmd','wifi','status']).stdout
original=re.search(r'SSID: "([^"\n]+)"',upstream).group(1)
original_ip=run(['ipconfig','getifaddr','en0']).stdout.strip()
preferred=run(['networksetup','-listpreferredwirelessnetworks','en0']).stdout
was_known=ssid in [line.strip() for line in preferred.splitlines()]
try:
    result=run(['networksetup','-setairportnetwork','en0',ssid,password],25)
    deadline=time.monotonic()+15
    ip=''
    while time.monotonic()<deadline:
        ip=run(['ipconfig','getifaddr','en0']).stdout.strip()
        if ip.startswith('.'.join(address.split('.')[:3])+'.'):break
        time.sleep(0.5)
    if not ip.startswith('.'.join(address.split('.')[:3])+'.'):
        raise RuntimeError('Hotspot association not verified: no DHCP address on hotspot subnet')
    else:
        print('PASS Wi-Fi association + hotspot DHCP:',ip)
        for flag in ('--proxy','--socks5-hostname'):
            endpoint=('http://' if flag=='--proxy' else '')+address+':2080'
            r=run(['curl','-fLsS','--connect-timeout','4','--max-time','10',flag,endpoint,'https://api.ipify.org'],12)
            if r.returncode: raise RuntimeError('Client '+flag+' failed: curl exit '+str(r.returncode))
            exit_ip=str(ipaddress.ip_address(r.stdout.strip()))
            print('PASS actual hotspot client',flag,'HTTPS exit:',exit_ip)
finally:
    restore=run(['networksetup','-setairportnetwork','en0',original],25)
    if not was_known:run(['networksetup','-removepreferredwirelessnetwork','en0',ssid])
    deadline=time.monotonic()+15
    while time.monotonic()<deadline:
        current=run(['ipconfig','getifaddr','en0']).stdout.strip()
        if current.startswith('.'.join(original_ip.split('.')[:3])+'.'):break
        time.sleep(0.5)
    print('Restored original Wi-Fi subnet:',current)
    if not current.startswith('.'.join(original_ip.split('.')[:3])+'.'):
        raise RuntimeError('Original Wi-Fi subnet restoration not verified')
