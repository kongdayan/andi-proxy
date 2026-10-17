package com.andi.proxy;

import android.content.Context;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public final class CoreRunner {
    private Process process;
    private final Context context;
    private File config;
    public String ruleInfo="", effectiveFinal="proxy";
    public CoreRunner(Context c) { context=c; }
    public static String binary(Context c) { return c.getApplicationInfo().nativeLibraryDir+"/libsingbox.so"; }
    public synchronized void start(Profile p,String address,Consumer<String> onExit) throws Exception {
        stop();
        config=new File(context.getFilesDir(),"active-config.json");
        SubscriptionStore.Snapshot subscription=new SubscriptionStore(context).forStart(p);
        effectiveFinal=subscription!=null&&subscription.rules.finalRoute!=null?subscription.rules.finalRoute:p.defaultRoute;
        ruleInfo=subscription==null?L.t("手动分流规则"):subscription.rules.summary()+(subscription.warning.isEmpty()?"":"\n"+subscription.warning);
        Files.write(config.toPath(),ConfigBuilder.build(p,address,subscription).toString().getBytes(StandardCharsets.UTF_8));
        android.system.Os.chmod(config.getAbsolutePath(),0600);
        Shell.Result check=Shell.run(Shell.quote(binary(context))+" check -c "+Shell.quote(config.getAbsolutePath()),12);
        if(!check.ok()) { config.delete(); throw new IllegalStateException(L.f("配置校验失败：%s", redact(check.output(),p))); }
        process=new ProcessBuilder(binary(context),"run","-c",config.getAbsolutePath()).redirectErrorStream(true).start();
        Process launched=process;
        StringBuilder tail=new StringBuilder();
        Thread reader=new Thread(()->{
            try(BufferedReader r=new BufferedReader(new InputStreamReader(launched.getInputStream()))) {
                String line; while((line=r.readLine())!=null) synchronized(tail) { tail.append(redact(line,p)).append('\n'); if(tail.length()>3000) tail.delete(0,tail.length()-3000); }
                int code=launched.waitFor();
                synchronized(this) { if(process!=launched) return; process=null; }
                synchronized(tail) { onExit.accept(L.f("核心退出（%d）：%s", code, tail)); }
            } catch(Exception ignored) {}
        },"proxy-core-output"); reader.start();
        for(int i=0;i<40;i++) {
            if(!launched.isAlive()) { synchronized(tail) { throw new IllegalStateException(L.f("核心启动失败：%s", tail)); } }
            try(Socket s=new Socket()) { s.connect(new InetSocketAddress(address,ConfigBuilder.MIXED_PORT),150); return; }
            catch(Exception e) { Thread.sleep(100); }
        }
        stop(); throw new IllegalStateException(L.t("代理监听端口启动超时"));
    }
    public synchronized void stop() {
        Process old=process; process=null;
        if(old!=null) { old.destroy(); try { if(!old.waitFor(3,TimeUnit.SECONDS)) old.destroyForcibly(); } catch(InterruptedException e) { Thread.currentThread().interrupt(); } }
        if(config!=null) config.delete();
    }
    private static String redact(String s,Profile p) {
        for(String secret:new String[]{p.password,p.obfs,p.localPassword}) if(!secret.isEmpty()) s=s.replace(secret,L.t("[已隐藏]"));
        return s;
    }
}
