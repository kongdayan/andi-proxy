package com.andi.proxy;

import org.json.JSONObject;
import java.net.IDN;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public final class Profile {
    public String protocol = "socks", host = "", username = "", password = "", sni = "", obfs = "";
    public int port = 1080;
    public String mode = "explicit", iface = "", directDomains = "", proxyDomains = "", directCidrs = "";
    public String defaultRoute = "proxy", localUsername = "hotspot", localPassword = "";
    public boolean socksUdp = true, insecure = false, localAuth = false;
    public String subscriptionUrl = RuleSubscription.EXAMPLE, hotspotName = "Andi Share", hotspotPassword = "", bypassApps = "";
    public boolean subscriptionEnabled = false, subscriptionAutoUpdate = true, customHotspot = false, vpnEnabled = false;
    public int hotspotBand = 0, theme = 0;

    public void validate() {
        validateServer(); validateRules(); validateSettings();
    }

    public void validateServer() {
        if (!protocol.equals("socks") && !protocol.equals("hysteria2")) throw new IllegalArgumentException(L.t("请选择 SOCKS5 或 HY2"));
        if (host.trim().isEmpty() || host.contains("://") || host.matches(".*[\\s/].*")) throw new IllegalArgumentException(L.t("服务器填写域名或 IP，不带协议前缀"));
        if (port < 1 || port > 65535) throw new IllegalArgumentException(L.t("端口必须在 1–65535 之间"));
        if (password.isEmpty()) throw new IllegalArgumentException(L.t("请填写服务器认证密码"));
        if (protocol.equals("socks")) {
            if (username.isEmpty()) throw new IllegalArgumentException(L.t("SOCKS5 需要填写认证用户名"));
            if (username.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 255 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 255)
                throw new IllegalArgumentException(L.t("SOCKS5 用户名和密码各最多 255 字节"));
        }
    }

    public void validateRules() {
        if (!defaultRoute.equals("direct") && !defaultRoute.equals("proxy")) throw new IllegalArgumentException(L.t("无效默认出口"));
        domains(directDomains); domains(proxyDomains); cidrs(directCidrs);
        if (subscriptionEnabled) RuleSubscription.validateUrl(subscriptionUrl);
    }

    public void validateSettings() {
        if (!mode.equals("explicit")) throw new IllegalArgumentException(L.t("本版本使用无需 root 的显式代理模式"));
        if (!iface.isEmpty() && !iface.matches("[a-zA-Z0-9_.-]{1,15}")) throw new IllegalArgumentException(L.t("热点接口名称无效"));
        if (localAuth && (localUsername.isEmpty() || localPassword.length()<8)) throw new IllegalArgumentException(L.t("本地代理用户名不能为空，密码至少 8 位"));
        if (localAuth && (localUsername.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>255 || localPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>255))
            throw new IllegalArgumentException(L.t("本地 SOCKS5 认证用户名和密码各最多 255 字节"));
        if (customHotspot) {
            if (hotspotName.trim().isEmpty() || hotspotName.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>32)
                throw new IllegalArgumentException(L.t("热点名称需要 1–32 字节"));
            if (!hotspotPassword.matches("[\\x20-\\x7e]{8,63}")) throw new IllegalArgumentException(L.t("Wi-Fi 密码需要 8–63 位英文字母、数字或符号"));
        }
        if (hotspotBand<0 || hotspotBand>2 || theme<0 || theme>2) throw new IllegalArgumentException(L.t("无效显示或频段设置"));
        for (String name:bypassApps.split("\\s+")) if (!name.isEmpty() && !name.matches("[a-zA-Z0-9_]+(?:\\.[a-zA-Z0-9_]+)+")) throw new IllegalArgumentException(L.t("无效应用包名"));
    }

    public static List<String> domains(String value) {
        List<String> out = new ArrayList<>();
        for (String raw : value.split("[\\s,，]+")) {
            if (raw.trim().isEmpty()) continue;
            String d = raw.toLowerCase(java.util.Locale.ROOT).replaceFirst("^\\*\\.", "").replaceFirst("^\\.", "");
            try { d = IDN.toASCII(d); } catch (IllegalArgumentException e) { throw new IllegalArgumentException(L.f("域名规则无效：%s", raw)); }
            if (!d.matches("[a-z0-9](?:[a-z0-9.-]*[a-z0-9])?") || d.contains("..")) throw new IllegalArgumentException(L.f("域名规则只填域名：%s", raw));
            out.add(d);
        }
        return out;
    }

    public static List<String> cidrs(String value) {
        List<String> out = new ArrayList<>();
        for (String s : value.split("[\\s,，]+")) {
            if (s.trim().isEmpty()) continue;
            try {
                String[] parts = s.split("/", -1);
                if (parts.length != 2 || !parts[0].matches("[0-9a-fA-F:.]+")) throw new IllegalArgumentException();
                InetAddress ip = InetAddress.getByName(parts[0]);
                int bits = Integer.parseInt(parts[1]);
                if (bits < 0 || bits > ip.getAddress().length * 8) throw new IllegalArgumentException();
            } catch (Exception e) { throw new IllegalArgumentException(L.f("IP 规则需要 CIDR，例 192.168.0.0/16：%s", s)); }
            out.add(s);
        }
        return out;
    }

    public JSONObject toJson() throws Exception {
        return new JSONObject().put("protocol", protocol).put("host", host).put("port", port).put("username", username)
            .put("password", password).put("sni", sni).put("obfs", obfs).put("mode", mode).put("iface", iface)
            .put("directDomains", directDomains).put("proxyDomains", proxyDomains).put("directCidrs", directCidrs)
            .put("defaultRoute", defaultRoute).put("socksUdp", socksUdp).put("insecure",insecure).put("localAuth",localAuth)
            .put("localUsername", localUsername).put("localPassword", localPassword)
            .put("subscriptionUrl",subscriptionUrl).put("subscriptionEnabled",subscriptionEnabled).put("subscriptionAutoUpdate",subscriptionAutoUpdate)
            .put("customHotspot",customHotspot).put("hotspotName",hotspotName).put("hotspotPassword",hotspotPassword).put("hotspotBand",hotspotBand)
            .put("vpnEnabled",vpnEnabled).put("bypassApps",bypassApps).put("theme",theme);
    }

    public static Profile fromJson(JSONObject j) {
        Profile p = new Profile();
        p.protocol=j.optString("protocol","socks"); p.host=j.optString("host"); p.port=j.optInt("port",1080);
        p.username=j.optString("username"); p.password=j.optString("password"); p.sni=j.optString("sni"); p.obfs=j.optString("obfs");
        p.mode="explicit"; p.iface=j.optString("iface"); p.directDomains=j.optString("directDomains");
        p.proxyDomains=j.optString("proxyDomains"); p.directCidrs=j.optString("directCidrs"); p.defaultRoute=j.optString("defaultRoute","proxy");
        p.socksUdp=j.optBoolean("socksUdp",true); p.insecure=j.optBoolean("insecure",false); p.localAuth=j.optBoolean("localAuth",false);
        p.localUsername=j.optString("localUsername","hotspot"); p.localPassword=j.optString("localPassword");
        p.subscriptionUrl=j.optString("subscriptionUrl",RuleSubscription.EXAMPLE); p.subscriptionEnabled=j.optBoolean("subscriptionEnabled",false); p.subscriptionAutoUpdate=j.optBoolean("subscriptionAutoUpdate",true);
        p.customHotspot=j.optBoolean("customHotspot",false); p.hotspotName=j.optString("hotspotName","Andi Share"); p.hotspotPassword=j.optString("hotspotPassword"); p.hotspotBand=j.optInt("hotspotBand",0);
        p.vpnEnabled=j.optBoolean("vpnEnabled",false); p.bypassApps=j.optString("bypassApps"); p.theme=j.optInt("theme",0);
        return p;
    }
}
