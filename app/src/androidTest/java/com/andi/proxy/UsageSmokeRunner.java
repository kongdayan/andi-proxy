package com.andi.proxy;

import android.app.*;
import android.appwidget.*;
import android.content.*;
import android.os.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.function.BooleanSupplier;

/** Developer-only: exercise independent modes and live settings with the encrypted device profile. */
public final class UsageSmokeRunner extends Instrumentation {
    private Bundle args;
    @Override public void onCreate(Bundle b) { super.onCreate(b); args=b; start(); }
    private void until(BooleanSupplier condition) throws Exception {
        long end=System.currentTimeMillis()+60000;
        while(!condition.getAsBoolean()&&System.currentTimeMillis()<end) Thread.sleep(200);
        if(!condition.getAsBoolean()) throw new IllegalStateException("Timed out: "+ProxyService.status);
    }
    @Override public void onStart() {
        Bundle result=new Bundle(); Context c=getTargetContext(); Profile original=null;
        try {
            original=new ProfileStore(c).load();
            if("widget".equals(args.getString("case"))) {
                int id=Integer.parseInt(args.getString("id")); Bundle options=new Bundle();
                int w=Integer.parseInt(args.getString("width")),h=Integer.parseInt(args.getString("height"));
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,w); options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,w);
                options.putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,h); options.putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,h);
                AppWidgetManager m=AppWidgetManager.getInstance(c); m.updateAppWidgetOptions(id,options); QuickControls.refresh(c);
                result.putString("result","PASS widget options "+w+"x"+h); finish(Activity.RESULT_OK,result); return;
            }
            Profile blank=Profile.fromJson(original.toJson()); blank.host=""; blank.username=""; blank.password="";
            blank.validateRules(); blank.validateSettings(); new ProfileStore(c).save(blank);
            SubscriptionStore.Snapshot cache=new SubscriptionStore(c).update(blank);
            if(cache.rules.count==0) throw new IllegalStateException("No rules");
            result.putString("offline_settings","PASS no server, no sharing, live subscription "+cache.rules.count);
            Profile p=Profile.fromJson(original.toJson()); p.vpnEnabled=true; p.iface="";
            new ProfileStore(c).save(p);
            c.startForegroundService(new Intent(c,ProxyService.class).setAction(ProxyService.START).putExtra("deviceOnly",true));
            until(()->ProxyService.active&&LocalVpnService.active&&!ProxyService.busy);
            if(ProxyService.isSharing()||!ProxyService.wifiName.isEmpty()) throw new IllegalStateException("Device-only unexpectedly shares");
            result.putString("device_only","PASS own VPN without hotspot; exit "+ProxyProbe.test("127.0.0.1",p));
            p.directDomains=p.directDomains+"\nusage-smoke.invalid"; new ProfileStore(c).save(p);
            c.startService(new Intent(c,ProxyService.class).setAction(ProxyService.APPLY));
            until(()->{
                try { return ProxyService.active&&!ProxyService.busy&&LocalVpnService.active&&new String(Files.readAllBytes(c.getFilesDir().toPath().resolve("active-config.json")),StandardCharsets.UTF_8).contains("usage-smoke.invalid"); }
                catch(Exception ignored) { return false; }
            });
            result.putString("live_apply","PASS settings applied to active core without sharing");
            c.startForegroundService(new Intent(c,ProxyService.class).setAction(ProxyService.START).putExtra("createHotspot",true));
            until(()->ProxyService.isSharing()&&LocalVpnService.active&&!ProxyService.busy);
            if(ProxyService.wifiName.isEmpty()||ProxyService.wifiPassword.isEmpty()) throw new IllegalStateException("Missing hotspot information");
            result.putString("combined","PASS add hotspot while device VPN active");
            c.startService(new Intent(c,ProxyService.class).setAction(ProxyService.STOP_SHARE));
            until(()->ProxyService.active&&!ProxyService.busy&&!ProxyService.isSharing()&&LocalVpnService.active);
            if(!ProxyService.wifiName.isEmpty()) throw new IllegalStateException("Hotspot not cleared");
            result.putString("stop_share","PASS stop sharing preserves phone VPN");
            p.vpnEnabled=false; new ProfileStore(c).save(p);
            c.startService(new Intent(c,ProxyService.class).setAction(ProxyService.VPN_CHANGED));
            until(()->!ProxyService.active&&!ProxyService.busy&&!LocalVpnService.active);
            result.putString("stop_phone","PASS stop phone shuts down idle core");
            result.putString("result","PASS independent configuration and control");
        } catch(Exception e) { result.putString("result","FAIL "+e.getClass().getSimpleName()+": "+e.getMessage()); }
        finally {
            if(original!=null) try { new ProfileStore(c).save(original); } catch(Exception e) { result.putString("restore","FAIL profile restore"); }
        }
        finish(result.getString("result","").startsWith("PASS")?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }
}
