package com.andi.proxy;
import org.junit.Test;
import static org.junit.Assert.*;
import org.json.*;
public final class SubscriptionTest {
    @Test public void preservesRoutingOrderAndCountryMeaning() throws Exception {
        RuleSubscription r=RuleSubscription.parse("[General]\ndns-server=system\n[Rule]\nDOMAIN-SUFFIX,Foreign.Example,Proxy # note\nDOMAIN,exact.example,DIRECT\nDOMAIN-KEYWORD,tracking,REJECT\nIP-CIDR,2001:db8::/32,PROXY,no-resolve\nGEOIP,CN,DIRECT\nFINAL,proxy\n[MITM]\nhostname=example.com\n");
        assertEquals(6,r.count); assertEquals("foreign.example",r.entries.get(0).value); assertEquals("reject",r.entries.get(2).policy); assertTrue(r.entries.get(3).noResolve);
        assertTrue(r.countries.contains("geoip-cn")); assertEquals("proxy",r.finalRoute); assertTrue(r.ignoredSections.contains("mitm"));
        SubscriptionStore.Snapshot s=new SubscriptionStore.Snapshot(r); s.paths.put("geoip-cn","/tmp/geoip-cn.srs");
        Profile p=ConfigBuilderTest.socks(); p.defaultRoute="direct"; p.vpnEnabled=true;
        JSONObject c=ConfigBuilder.build(p,"127.0.0.1",s); assertEquals("proxy",c.getJSONObject("route").getString("final"));
        JSONArray rules=c.getJSONObject("route").getJSONArray("rules");
        assertEquals("reject",rules.getJSONObject(6).getString("action"));
        assertEquals("resolve",rules.getJSONObject(8).getString("action"));
        assertEquals("geoip-cn",rules.getJSONObject(9).getJSONArray("rule_set").getString(0));
        assertEquals(2081,c.getJSONArray("inbounds").getJSONObject(1).getInt("listen_port"));
        assertEquals("local",c.getJSONObject("route").getJSONArray("rule_set").getJSONObject(0).getString("type"));
    }
    @Test public void rejectsUnsupportedRoutingInsteadOfDroppingIt() {
        for(String line:new String[]{"RULE-SET,x,PROXY","DOMAIN,x,MyGroup","IP-CIDR,example.com/24,DIRECT","FINAL,DIRECT\nDOMAIN,x,PROXY","DOMAIN,x,DIRECT,unsupported","[General]\nipv6=false"})
            assertThrows(line,IllegalArgumentException.class,()->RuleSubscription.parse(line));
    }
    @Test public void rejectsUnsafeUrlAndRetainsNewPreferences() throws Exception {
        for(String url:new String[]{"http://example.com/rules","https://user:pass@example.com/rules","not-a-url"}) assertThrows(IllegalArgumentException.class,()->RuleSubscription.validateUrl(url));
        Profile p=ConfigBuilderTest.socks(); p.subscriptionEnabled=true; p.customHotspot=true; p.hotspotPassword="test-wifi-password"; p.hotspotBand=2; p.theme=2; p.bypassApps="com.example.browser";
        Profile copy=Profile.fromJson(p.toJson()); copy.validate(); assertTrue(copy.subscriptionEnabled); assertEquals(2,copy.theme); assertEquals(p.bypassApps,copy.bypassApps);
    }
    @Test public void finalRejectIsAnActualReject() throws Exception {
        SubscriptionStore.Snapshot s=new SubscriptionStore.Snapshot(RuleSubscription.parse("DOMAIN-SUFFIX,ads.example,REJECT\nFINAL,REJECT"));
        JSONObject c=ConfigBuilder.build(ConfigBuilderTest.socks(),"127.0.0.1",s); JSONArray rs=c.getJSONObject("route").getJSONArray("rules"); assertEquals("reject",rs.getJSONObject(rs.length()-1).getString("action"));
    }
    @Test public void produceSubscriptionCoreFixture() throws Exception {
        RuleSubscription r=RuleSubscription.parse("DOMAIN-SUFFIX,localhost,PROXY\nDOMAIN,blocked.invalid,REJECT\nIP-CIDR,2001:db8::/32,DIRECT,no-resolve\nFINAL,DIRECT");
        Profile p=ConfigBuilderTest.socks(); p.proxyDomains="";
        java.nio.file.Files.write(java.nio.file.Path.of("build/test-configs/subscription-socks.json"),ConfigBuilder.build(p,"127.0.0.1",new SubscriptionStore.Snapshot(r)).toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

}
