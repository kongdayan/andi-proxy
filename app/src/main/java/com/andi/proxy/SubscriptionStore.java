package com.andi.proxy;

import android.content.Context;
import android.util.AtomicFile;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/** Immutable snapshots plus an atomic pointer; a failed refresh retains the previous rules. */
public final class SubscriptionStore {
    public static final class Snapshot {
        public final RuleSubscription rules;
        public final Map<String,String> paths=new LinkedHashMap<>();
        public long updated;
        public String warning="";
        Snapshot(RuleSubscription r) { rules=r; }
    }
    private final Context context;
    public SubscriptionStore(Context c) { context=c.getApplicationContext(); }
    private File directory(String url) throws Exception { return new File(context.getFilesDir(),"subscriptions/"+sha(url.getBytes(StandardCharsets.UTF_8))); }
    private static String sha(byte[] data) throws Exception {
        StringBuilder b=new StringBuilder(); for(byte v:MessageDigest.getInstance("SHA-256").digest(data)) b.append(String.format(Locale.ROOT,"%02x",v&255)); return b.toString();
    }
    public Snapshot cached(String url) throws Exception { synchronized(SubscriptionStore.class) { return cachedLocked(url); } }
    private Snapshot cachedLocked(String url) throws Exception {
        RuleSubscription.validateUrl(url);
        File root=directory(url); AtomicFile pointer=new AtomicFile(new File(root,"current.json"));
        if(!pointer.getBaseFile().exists()) return null;
        JSONObject meta=new JSONObject(new String(pointer.readFully(),StandardCharsets.UTF_8));
        File dir=new File(root,meta.getString("snapshot"));
        if(!dir.getCanonicalFile().getParentFile().equals(root.getCanonicalFile())) throw new IOException(L.t("订阅缓存路径错误"));
        byte[] body=Files.readAllBytes(new File(dir,"rules.conf").toPath());
        if(!sha(body).equals(meta.getString("sha256"))) throw new IOException(L.t("订阅缓存校验失败"));
        Snapshot result=new Snapshot(RuleSubscription.parse(new String(body,StandardCharsets.UTF_8))); result.updated=meta.getLong("updated");
        for(String tag:result.rules.countries) { File f=new File(dir,tag+".srs"); if(!f.isFile()) throw new IOException(L.f("缺少地区规则库：%s", tag)); result.paths.put(tag,f.getAbsolutePath()); }
        return result;
    }
    public Snapshot forStart(Profile p) throws Exception {
        if(!p.subscriptionEnabled) return null;
        Snapshot old=cached(p.subscriptionUrl);
        if(old==null || (p.subscriptionAutoUpdate && System.currentTimeMillis()-old.updated>24*60*60*1000L)) {
            try { return update(p); }
            catch(Exception e) { if(old==null) throw e; old.warning=L.f("订阅更新失败，继续使用上次成功的缓存：%s", e.getMessage()); }
        }
        return old;
    }
    public Snapshot update(Profile p) throws Exception { synchronized(SubscriptionStore.class) { return updateLocked(p); } }
    private Snapshot updateLocked(Profile p) throws Exception {
        RuleSubscription.validateUrl(p.subscriptionUrl);
        byte[] body=download(p.subscriptionUrl,4*1024*1024);
        RuleSubscription parsed=RuleSubscription.parse(new String(body,StandardCharsets.UTF_8));
        File root=directory(p.subscriptionUrl); root.mkdirs();
        File dir=new File(root,"snapshot-"+UUID.randomUUID()); if(!dir.mkdir()) throw new IOException(L.t("无法创建订阅缓存"));
        File check=new File(dir,"check.json");
        try {
            Files.write(new File(dir,"rules.conf").toPath(),body);
            Snapshot result=new Snapshot(parsed); result.updated=System.currentTimeMillis();
            for(String tag:parsed.countries) {
                File country=new File(dir,tag+".srs");
                Files.write(country.toPath(),download("https://raw.githubusercontent.com/SagerNet/sing-geoip/rule-set/"+tag+".srs",8*1024*1024)); result.paths.put(tag,country.getAbsolutePath());
            }
            // Rule validation must work before a server is configured, and while the proxy is stopped.
            Profile checkProfile=Profile.fromJson(p.toJson());
            checkProfile.protocol="socks"; checkProfile.host="127.0.0.1"; checkProfile.port=1080;
            checkProfile.username="rules-check"; checkProfile.password="rules-check";
            checkProfile.customHotspot=false; checkProfile.localAuth=false; checkProfile.iface="";
            Files.write(check.toPath(),ConfigBuilder.build(checkProfile,"127.0.0.1",result).toString().getBytes(StandardCharsets.UTF_8));
            android.system.Os.chmod(check.getAbsolutePath(),0600);
            Shell.Result validation=Shell.run(Shell.quote(CoreRunner.binary(context))+" check -c "+Shell.quote(check.getAbsolutePath()),15);
            if(!validation.ok()) throw new IOException(L.t("规则核心校验失败，保留原缓存"));
            check.delete();
            AtomicFile pointer=new AtomicFile(new File(root,"current.json")); FileOutputStream out=null;
            try {
                out=pointer.startWrite(); out.write(new JSONObject().put("snapshot",dir.getName()).put("sha256",sha(body)).put("updated",result.updated).toString().getBytes(StandardCharsets.UTF_8)); pointer.finishWrite(out);
            } catch(Exception e) { if(out!=null) pointer.failWrite(out); throw e; }
            // Keep the two newest snapshots so an active core's referenced files remain available.
            File[] dirs=root.listFiles(f->f.isDirectory()&&f.getName().startsWith("snapshot-"));
            if(dirs!=null) { Arrays.sort(dirs,Comparator.comparingLong(File::lastModified).reversed()); for(int i=2;i<dirs.length;i++) delete(dirs[i]); }
            return result;
        } catch(Exception e) { delete(dir); throw e; }
        finally { check.delete(); }
    }
    static byte[] download(String source,int limit) throws Exception {
        URL url=new URL(source);
        for(int redirects=0;redirects<5;redirects++) {
            if(!url.getProtocol().equals("https")) throw new IOException(L.t("拒绝非 HTTPS 订阅跳转"));
            HttpURLConnection conn=(HttpURLConnection)url.openConnection();
            conn.setConnectTimeout(12000); conn.setReadTimeout(18000); conn.setInstanceFollowRedirects(false); conn.setRequestProperty("User-Agent","AndiProxy/"+BuildConfig.VERSION_NAME);
            try {
                int code=conn.getResponseCode();
                if(code>=300&&code<400) { String location=conn.getHeaderField("Location"); if(location==null) throw new IOException(L.t("订阅重定向无地址")); url=new URL(url,location); continue; }
                if(code!=200) throw new IOException(L.f("下载失败 HTTP %d", code));
                if(conn.getContentLengthLong()>limit) throw new IOException(L.t("订阅超过大小限制"));
                try(InputStream in=conn.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
                    byte[] b=new byte[8192]; int n;
                    while((n=in.read(b))!=-1) { if(out.size()+n>limit) throw new IOException(L.t("订阅超过大小限制")); out.write(b,0,n); }
                    return out.toByteArray();
                }
            } finally { conn.disconnect(); }
        }
        throw new IOException(L.t("订阅跳转次数过多"));
    }
    private static void delete(File f) { File[] files=f.listFiles(); if(files!=null) for(File c:files) delete(c); f.delete(); }
    public String describe(String url) {
        try { Snapshot s=cached(url); return s==null?L.t("尚未下载，启用后首次启动会更新。"):s.rules.summary()+"\n"+L.f("最近更新：%s", java.text.DateFormat.getDateTimeInstance().format(new Date(s.updated))); }
        catch(Exception e) { return L.f("缓存不可用：%s", e.getMessage()); }
    }
}
