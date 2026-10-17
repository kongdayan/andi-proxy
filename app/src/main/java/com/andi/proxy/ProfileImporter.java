package com.andi.proxy;

import org.json.JSONObject;

public final class ProfileImporter {
    public static Profile parse(String text) throws Exception {
        JSONObject j=new JSONObject(text.trim());
        Profile p;
        if(j.has("type")) {
            p=new Profile(); String type=j.getString("type");
            if(type.equalsIgnoreCase("Hysteria2")||type.equalsIgnoreCase("hy2")) p.protocol="hysteria2";
            else if(type.equalsIgnoreCase("SOCKS5")||type.equalsIgnoreCase("socks")) p.protocol="socks";
            else throw new IllegalArgumentException(L.t("仅支持 HY2 和 SOCKS5 配置"));
            p.host=j.optString("host"); p.port=Integer.parseInt(j.optString("port",p.protocol.equals("socks")?"1080":"443"));
            p.password=j.optString("password"); p.username=j.optString("user"); p.sni=j.optString("peer");
            p.insecure=j.optInt("allowInsecure",0)==1 || j.optBoolean("allowInsecure",false);
            p.socksUdp=j.optInt("udp",1)!=0;
            if(j.optString("obfs").equalsIgnoreCase("salamander")) p.obfs=j.optString("obfsParam");
        } else p=Profile.fromJson(j);
        p.validate(); return p;
    }
}
