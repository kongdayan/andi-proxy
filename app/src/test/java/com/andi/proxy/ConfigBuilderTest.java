package com.andi.proxy;

import org.junit.Test;
import static org.junit.Assert.*;
import org.json.JSONObject;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigBuilderTest {
    static Profile socks() { Profile p=new Profile(); p.host="127.0.0.1"; p.port=19080; p.username="test-user"; p.password="test-pass"; p.proxyDomains="localhost"; return p; }
    @Test public void passwordAuthenticationAndRuleOrder() throws Exception {
        Profile p=socks(); p.directDomains="localhost\nexample.com";
        JSONObject c=ConfigBuilder.build(p,"127.0.0.1");
        assertEquals("5",c.getJSONArray("outbounds").getJSONObject(0).getString("version"));
        assertEquals("test-pass",c.getJSONArray("outbounds").getJSONObject(0).getString("password"));
        var rules=c.getJSONObject("route").getJSONArray("rules");
        assertEquals("proxy",rules.getJSONObject(2).getString("outbound"));
        assertEquals("direct",rules.getJSONObject(3).getString("outbound"));
        assertEquals("remote-dns",c.getJSONObject("dns").getJSONArray("rules").getJSONObject(0).getString("server"));
    }
    @Test public void importingShadowrocketRetainsTlsChoice() throws Exception {
        Profile p=ProfileImporter.parse("{\"type\":\"Hysteria2\",\"host\":\"192.0.2.1\",\"port\":\"443\",\"password\":\"temporary-test\",\"peer\":\"example.com\",\"allowInsecure\":1,\"obfs\":\"salamander\",\"obfsParam\":\"test-obfs\"}");
        JSONObject out=ConfigBuilder.build(p,"127.0.0.1").getJSONArray("outbounds").getJSONObject(0);
        assertEquals("hysteria2",p.protocol); assertTrue(out.getJSONObject("tls").getBoolean("insecure"));
        assertEquals("example.com",out.getJSONObject("tls").getString("server_name"));
        assertEquals("test-obfs",out.getJSONObject("obfs").getString("password"));
    }
    @Test public void secureByDefaultAndLocalAuthentication() throws Exception {
        Profile p=socks(); p.protocol="hysteria2"; p.localAuth=true; p.localPassword="local-password";
        JSONObject c=ConfigBuilder.build(p,"192.168.43.1");
        assertFalse(c.getJSONArray("outbounds").getJSONObject(0).getJSONObject("tls").getBoolean("insecure"));
        assertEquals("local-password",c.getJSONArray("inbounds").getJSONObject(0).getJSONArray("users").getJSONObject(0).getString("password"));
        assertEquals("192.168.43.1",c.getJSONArray("inbounds").getJSONObject(0).getString("listen"));
    }
    @Test public void rejectsInvalidInput() {
        for(String cidr:new String[]{"example.com/24","192.0.2.1/33","::1/129"}) assertThrows(IllegalArgumentException.class,()->Profile.cidrs(cidr));
        assertThrows(IllegalArgumentException.class,()->Profile.domains("https://example.com/path"));
        Profile p=socks(); p.username=""; assertThrows(IllegalArgumentException.class,p::validate);
    }
    @Test public void rulesAndPreferencesCanBeConfiguredWithoutServer() {
        Profile p=new Profile(); p.subscriptionEnabled=true; p.theme=2; p.hotspotBand=2;
        p.validateRules(); p.validateSettings();
        assertThrows(IllegalArgumentException.class,p::validateServer);
        p.directDomains="https://invalid.example";
        assertThrows(IllegalArgumentException.class,p::validateRules);
        p.directDomains=""; p.localAuth=true; p.localPassword="short";
        assertThrows(IllegalArgumentException.class,p::validateSettings);
    }
    @Test public void produceRealCoreFixtures() throws Exception {
        Path dir=Path.of("build/test-configs"); Files.createDirectories(dir);
        Profile p=socks(); Files.write(dir.resolve("socks.json"),ConfigBuilder.build(p,"127.0.0.1").toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        p.protocol="hysteria2"; p.sni="localhost"; p.insecure=true;
        Files.write(dir.resolve("hy2.json"),ConfigBuilder.build(p,"127.0.0.1").toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
