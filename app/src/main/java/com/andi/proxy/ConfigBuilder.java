package com.andi.proxy;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.List;

public final class ConfigBuilder {
    public static final int MIXED_PORT=2080;
    public static final int VPN_PORT=2081;
    private static JSONObject o(String type, String tag) throws Exception { return new JSONObject().put("type",type).put("tag",tag); }
    private static void route(JSONArray rules, String field, List<String> values, String target) throws Exception {
        if (!values.isEmpty()) rules.put(new JSONObject().put(field,new JSONArray(values)).put("action","route").put("outbound",target));
    }
    public static JSONObject build(Profile p, String listenAddress) throws Exception {
        return build(p,listenAddress,null);
    }
    public static JSONObject build(Profile p, String listenAddress,SubscriptionStore.Snapshot subscription) throws Exception {
        p.validate();
        JSONObject remote=o(p.protocol,"proxy").put("server",p.host).put("server_port",p.port).put("connect_timeout","10s");
        if (p.protocol.equals("socks")) {
            remote.put("version","5").put("username",p.username).put("password",p.password);
            if (!p.socksUdp) remote.put("network","tcp");
        } else {
            remote.put("password",p.password).put("tls",new JSONObject().put("enabled",true).put("server_name",p.sni.trim().isEmpty()?p.host:p.sni).put("insecure",p.insecure));
            if (!p.obfs.isEmpty()) remote.put("obfs",new JSONObject().put("type","salamander").put("password",p.obfs));
        }
        JSONObject mixed=o("mixed","local-proxy").put("listen",listenAddress).put("listen_port",MIXED_PORT);
        if (p.localAuth) {
            mixed.put("users",new JSONArray().put(new JSONObject().put("username",p.localUsername).put("password",p.localPassword)));
        }
        JSONArray in=new JSONArray().put(mixed);
        if(p.vpnEnabled) in.put(o("mixed","device-vpn").put("listen","127.0.0.1").put("listen_port",VPN_PORT));
        JSONArray rules=new JSONArray();
        rules.put(new JSONObject().put("action","sniff").put("timeout","500ms"));
        rules.put(new JSONObject().put("port",53).put("action","hijack-dns"));
        // Explicit proxy rules take precedence, including domains resolving to private destinations.
        route(rules,"domain_suffix",Profile.domains(p.proxyDomains),"proxy");
        route(rules,"domain_suffix",Profile.domains(p.directDomains),"direct");
        boolean resolved=!Profile.cidrs(p.directCidrs).isEmpty();
        if(resolved) rules.put(new JSONObject().put("action","resolve").put("strategy","ipv4_only"));
        route(rules,"ip_cidr",Profile.cidrs(p.directCidrs),"direct");
        rules.put(new JSONObject().put("ip_is_private",true).put("action","route").put("outbound","direct"));
        String finalRoute=p.defaultRoute;
        JSONArray sets=new JSONArray();
        JSONArray dnsRules=new JSONArray();
        List<String> proxyDomains=Profile.domains(p.proxyDomains), directDomains=Profile.domains(p.directDomains);
        if (!proxyDomains.isEmpty()) dnsRules.put(new JSONObject().put("domain_suffix",new JSONArray(proxyDomains)).put("server","remote-dns"));
        if (!directDomains.isEmpty()) dnsRules.put(new JSONObject().put("domain_suffix",new JSONArray(directDomains)).put("server","bootstrap"));
        if(subscription!=null) {
            for(RuleSubscription.Entry e:subscription.rules.entries) {
                if((e.field.equals("ip_cidr")||e.field.equals("rule_set"))&&!e.noResolve&&!resolved) {
                    rules.put(new JSONObject().put("action","resolve").put("strategy","ipv4_only")); resolved=true;
                }
                rules.put(e.route());
                if(e.field.startsWith("domain")) {
                    JSONObject d=new JSONObject().put(e.field,new JSONArray().put(e.value));
                    dnsRules.put(e.policy.equals("reject")?d.put("action","reject"):d.put("server",e.policy.equals("direct")?"bootstrap":"remote-dns"));
                }
            }
            for(java.util.Map.Entry<String,String> e:subscription.paths.entrySet()) sets.put(new JSONObject().put("type","local").put("tag",e.getKey()).put("format","binary").put("path",e.getValue()));
            if(subscription.rules.finalRoute!=null) finalRoute=subscription.rules.finalRoute;
        }
        if (p.protocol.equals("socks") && !p.socksUdp) rules.put(new JSONObject().put("network","udp").put("action","reject"));
        if(finalRoute.equals("reject")) rules.put(new JSONObject().put("action","reject"));
        // IP-literal DoH avoids a bootstrap loop; detour is explicit in the modern DNS schema.
        JSONArray servers=new JSONArray().put(o("udp","bootstrap").put("server","223.5.5.5"))
            .put(o("https","remote-dns").put("server","1.1.1.1").put("path","/dns-query").put("detour","proxy")
                .put("tls",new JSONObject().put("enabled",true).put("server_name","cloudflare-dns.com")));
        return new JSONObject().put("log",new JSONObject().put("level","warn").put("timestamp",true))
            .put("inbounds",in).put("outbounds",new JSONArray().put(remote).put(o("direct","direct")))
            .put("dns",new JSONObject().put("servers",servers).put("rules",dnsRules).put("final",finalRoute.equals("direct")?"bootstrap":"remote-dns")
                .put("strategy","ipv4_only").put("reverse_mapping",true))
            .put("route",new JSONObject().put("rules",rules).put("rule_set",sets).put("final",finalRoute.equals("reject")?"direct":finalRoute).put("default_domain_resolver","bootstrap"));
    }
}
