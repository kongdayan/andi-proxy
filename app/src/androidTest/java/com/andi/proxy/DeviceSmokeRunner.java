package com.andi.proxy;

import android.app.*;
import android.content.*;
import android.os.Bundle;
import java.io.File;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;

/** Developer-only runner in a separate test APK. No test credential is embedded in either APK. */
public final class DeviceSmokeRunner extends Instrumentation {
    private Bundle args;
    @Override public void onCreate(Bundle arguments) { super.onCreate(arguments); args=arguments; start(); }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            Context c=getTargetContext();
            File input=new File(c.getFilesDir(),"import-test.json");
            Profile p="true".equals(args.getString("existing"))?new ProfileStore(c).load():ProfileImporter.parse(new String(Files.readAllBytes(input.toPath()),StandardCharsets.UTF_8));
            if("true".equals(args.getString("cleanup"))) {
                p.bypassApps=""; new ProfileStore(c).save(p); result.putString("result","PASS: temporary test whitelist cleared"); finish(Activity.RESULT_OK,result); return;
            }
            if("true".equals(args.getString("subscription"))) {
                p.subscriptionEnabled=true; p.subscriptionUrl=RuleSubscription.EXAMPLE;
                SubscriptionStore store=new SubscriptionStore(c); SubscriptionStore.Snapshot snapshot=store.update(p);
                if(snapshot.rules.count!=392||!snapshot.rules.countries.contains("geoip-cn")) throw new IllegalStateException("Unexpected live subscription contents");
                String direct=p.directDomains; p.directDomains="https://invalid.example";
                try { store.update(p); throw new IllegalStateException("Invalid rules refresh should fail"); } catch(IllegalArgumentException expected) { /* Keep old cache. */ } finally { p.directDomains=direct; }
                if(store.cached(p.subscriptionUrl).updated!=snapshot.updated) throw new IllegalStateException("Failed update overwrote cache");
                result.putString("subscription","PASS: 392 live rules, CN data, failed update retained cache");
            }
            p.iface=""; new ProfileStore(c).save(p); input.delete();
            Intent activity=new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivitySync(activity); waitForIdleSync();
            boolean hotspot="true".equals(args.getString("hotspot"));
            c.startForegroundService(new Intent(c,ProxyService.class).setAction(ProxyService.START).putExtra("createHotspot",hotspot));
            long deadline=System.currentTimeMillis()+45000;
            while(!ProxyService.active&&System.currentTimeMillis()<deadline) {
                if(ProxyService.failure) throw new IllegalStateException(ProxyService.detail);
                Thread.sleep(250);
            }
            if(!ProxyService.active) throw new IllegalStateException("Service startup timed out: "+ProxyService.status);
            String ip=ProxyProbe.test(ProxyService.address,p);
            result.putString("result","PASS: Android core, foreground service, authenticated HY2 exit verified");
            result.putString("exit_ipv4",ip);
            result.putString("listen",ProxyService.address+":2080");
            result.putString("app_hotspot",Boolean.toString(hotspot));
            finish(Activity.RESULT_OK,result);
        } catch(Exception e) {
            result.putString("result","FAIL: "+e.getClass().getSimpleName()+": "+e.getMessage());
            finish(Activity.RESULT_CANCELED,result);
        }
    }
}
