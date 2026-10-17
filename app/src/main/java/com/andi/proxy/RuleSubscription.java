package com.andi.proxy;

import java.net.URI;
import java.util.*;
import org.json.*;

/** Routing-only Shadowrocket parser. Unsupported routing entries fail the update. */
public final class RuleSubscription {
    public static final String EXAMPLE="https://johnshall.github.io/Shadowrocket-ADBlock-Rules-Forever/sr_cnip.conf";
    public static final class Entry {
        public final String field,value,policy;
        public final boolean noResolve;
        Entry(String f,String v,String p,boolean n) { field=f; value=v; policy=p; noResolve=n; }
        public JSONObject route() throws Exception {
            JSONObject j=new JSONObject();
            if(field.equals("ip_is_private")) j.put(field,true);
            else j.put(field,new JSONArray().put(value));
            return policy.equals("reject")?j.put("action","reject"):j.put("action","route").put("outbound",policy);
        }
    }
    public final List<Entry> entries=new ArrayList<>();
    public final Set<String> countries=new LinkedHashSet<>(), ignoredSections=new LinkedHashSet<>();
    public String finalRoute;
    public int count;
    public static void validateUrl(String url) {
        try {
            URI u=new URI(url);
            if(!"https".equalsIgnoreCase(u.getScheme()) || u.getHost()==null || u.getUserInfo()!=null || u.getFragment()!=null) throw new Exception();
        } catch(Exception e) { throw new IllegalArgumentException(L.t("订阅地址需要有效 HTTPS 链接，不含账号或片段")); }
    }
    public static RuleSubscription parse(String text) {
        RuleSubscription s=new RuleSubscription();
        String section="", normalized=text.replace("\uFEFF","");
        boolean hasSections=normalized.matches("(?s).*\\[[^\\r\\n]+].*");
        int lineNo=0;
        for(String raw:normalized.split("\\r?\\n")) {
            lineNo++;
            String line=raw.split("#",2)[0].trim();
            if(line.isEmpty() || line.startsWith(";") || line.startsWith("//")) continue;
            if(line.startsWith("[")&&line.endsWith("]")) { section=line.substring(1,line.length()-1).trim().toLowerCase(Locale.ROOT); if(!section.equals("rule")) s.ignoredSections.add(section); continue; }
            if(hasSections&&!section.equals("rule")) continue;
            try {
                if(s.finalRoute!=null) throw new IllegalArgumentException(L.t("FINAL 后不能再添加规则"));
                String[] parts=line.split(",",-1);
                String type=parts[0].trim().toUpperCase(Locale.ROOT);
                boolean last=type.equals("FINAL")||type.equals("MATCH");
                if(parts.length<(last?2:3)||parts.length>(last?2:4)) throw new IllegalArgumentException(L.t("字段数量错误"));
                String action=parts[last?1:2].trim().toUpperCase(Locale.ROOT);
                String policy;
                switch(action) { case "DIRECT":policy="direct";break; case "PROXY":policy="proxy";break; case "REJECT":case "REJECT-DROP":policy="reject";break; default:throw new IllegalArgumentException(L.f("不支持的策略：%s；使用 DIRECT / PROXY / REJECT", action)); }
                if(parts.length==4&&!parts[3].trim().equalsIgnoreCase("no-resolve")) throw new IllegalArgumentException(L.t("不支持的规则选项"));
                boolean nr=parts.length==4;
                if(last) { s.finalRoute=policy; s.count++; continue; }
                String v=parts[1].trim(), field;
                switch(type) {
                    case "DOMAIN":case "DOMAIN-SUFFIX":
                        List<String> ds=Profile.domains(v); if(ds.size()!=1) throw new IllegalArgumentException(L.t("域名无效")); v=ds.get(0); field=type.equals("DOMAIN")?"domain":"domain_suffix"; break;
                    case "DOMAIN-KEYWORD":
                        if(v.isEmpty() || v.matches(".*[\\s/].*")) throw new IllegalArgumentException(L.t("关键词无效")); v=v.toLowerCase(Locale.ROOT); field="domain_keyword"; break;
                    case "IP-CIDR":case "IP-CIDR6":
                        List<String> ips=Profile.cidrs(v); if(ips.size()!=1) throw new IllegalArgumentException(L.t("CIDR 无效")); field="ip_cidr"; break;
                    case "GEOIP":
                        if(v.equalsIgnoreCase("LAN")) { field="ip_is_private"; v="true"; }
                        else { if(!v.matches("[A-Za-z]{2}")) throw new IllegalArgumentException(L.t("GEOIP 需要两位国家代码或 LAN")); v="geoip-"+v.toLowerCase(Locale.ROOT); s.countries.add(v); field="rule_set"; } break;
                    default:throw new IllegalArgumentException(L.f("不支持的规则类型：%s", type));
                }
                if(nr&&!field.equals("ip_cidr")&&!field.equals("rule_set")&&!field.equals("ip_is_private")) throw new IllegalArgumentException(L.t("no-resolve 仅用于 IP 规则"));
                s.entries.add(new Entry(field,v,policy,nr)); s.count++;
                if(s.count>50000) throw new IllegalArgumentException(L.t("最多 50000 条规则"));
            } catch(Exception e) { throw new IllegalArgumentException(L.f("订阅第 %d 行：%s", lineNo, e.getMessage())); }
        }
        if(s.count==0) throw new IllegalArgumentException(L.t("订阅没有可用的 [Rule] 分流规则"));
        return s;
    }
    public String summary() {
        StringBuilder text = new StringBuilder(L.f("%1$d 条规则 · %2$d 个地区库", count, countries.size()));
        if (finalRoute != null) text.append(L.f(" · 最终出口：%s", policyName(finalRoute)));
        if (!ignoredSections.isEmpty()) text.append("\n").append(L.t("仅导入分流；忽略 General、URL Rewrite、MITM 等设置。"));
        return text.toString();
    }

    public static String policyName(String policy) {
        if ("proxy".equals(policy)) return L.t("代理");
        if ("direct".equals(policy)) return L.t("直连");
        return L.t("拒绝");
    }
}
