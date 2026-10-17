package com.andi.proxy;

import android.app.*;
import android.content.*;
import android.net.wifi.WifiManager;
import android.net.wifi.SoftApConfiguration;
import android.os.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.Map;
import java.util.HashMap;
import java.net.NetworkInterface;
import java.net.Inet4Address;
import java.util.Collections;

public final class ProxyService extends Service {
    public static final String START="com.andi.proxy.START", STOP="com.andi.proxy.STOP", TEST="com.andi.proxy.TEST", APPLY="com.andi.proxy.APPLY", VPN_CHANGED="com.andi.proxy.VPN_CHANGED", STOP_SHARE="com.andi.proxy.STOP_SHARE";
    public static volatile String status=L.t("未连接"), detail="", address="", wifiName="", wifiPassword="", exitIp="", rulesSummary="";
    public static volatile boolean active=false, busy=false, failure=false;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private CoreRunner core;
    private Profile profile;
    private WifiManager.LocalOnlyHotspotReservation hotspot;
    private volatile int generation;
    private boolean pendingApply;

    @Override public void onCreate() {
        super.onCreate(); core=new CoreRunner(this);
        NotificationManager nm=getSystemService(NotificationManager.class);
        NotificationChannel channel=nm.getNotificationChannel("proxy");
        if(channel==null) channel=new NotificationChannel("proxy",L.t("热点代理"),NotificationManager.IMPORTANCE_LOW);
        else channel.setName(L.t("热点代理"));
        nm.createNotificationChannel(channel);
    }
    private Notification notification(String text) {
        PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop=PendingIntent.getService(this,1,new Intent(this,ProxyService.class).setAction(STOP),PendingIntent.FLAG_IMMUTABLE);
        PendingIntent test=PendingIntent.getService(this,2,new Intent(this,ProxyService.class).setAction(TEST),PendingIntent.FLAG_IMMUTABLE);
        String subtitle=active?(isSharing()?L.f("热点共享 · %s:2080",address):L.t("本机代理")) :text;
        String body=subtitle+(exitIp.isEmpty()?"":L.f(" · 出口 %s",exitIp));
        return new Notification.Builder(this,"proxy").setSmallIcon(R.drawable.ic_status).setContentTitle("Andi · "+text)
            .setContentText(subtitle).setStyle(new Notification.BigTextStyle().bigText(body))
            .setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
            .addAction(new Notification.Action.Builder(null,L.t("检测出口"),test).build())
            .addAction(new Notification.Action.Builder(null,L.t("停止"),stop).build()).build();
    }
    private void update(String s,String d) { update(s,d,false); }
    private void update(String s,String d,boolean failed) {
        status=s; detail=d; failure=failed;
        if(active||busy) getSystemService(NotificationManager.class).notify(1,notification(s));
        QuickControls.refresh(this);
    }
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        if(intent==null) return START_NOT_STICKY;
        String action=intent.getAction();
        if(STOP.equals(action)) { stopSession(L.t("未连接"),"",false); return START_NOT_STICKY; }
        if(VPN_CHANGED.equals(action)) {
            try {
                Profile next=new ProfileStore(this).load();
                if(active) {
                    profile.vpnEnabled=next.vpnEnabled;
                    if(next.vpnEnabled) startForegroundService(new Intent(this,LocalVpnService.class));
                    else { LocalVpnService.stop(); if(!isSharing()) stopSession(L.t("未连接"),"",false); }
                    QuickControls.refresh(this);
                    return START_NOT_STICKY;
                }
                if(!next.vpnEnabled) { if(!busy) stopSelf(); return START_NOT_STICKY; }
                intent=new Intent(this,ProxyService.class).setAction(START).putExtra("deviceOnly",true); action=START;
            } catch(Exception e) { stopSession(L.t("设置失败"),e.getMessage(),true); return START_NOT_STICKY; }
        }
        if(STOP_SHARE.equals(action)) {
            if(busy) return START_NOT_STICKY;
            try {
                Profile next=new ProfileStore(this).load();
                if(!next.vpnEnabled) { stopSession(L.t("未连接"),"",false); return START_NOT_STICKY; }
                restart(next,false,"127.0.0.1");
            } catch(Exception e) { stopSession(L.t("启动失败"),e.getMessage(),true); }
            return START_NOT_STICKY;
        }
        if(APPLY.equals(action)) {
            if(busy) { pendingApply=true; return START_NOT_STICKY; }
            if(active&&!busy) try { restart(new ProfileStore(this).load(),hotspot!=null,address); }
                catch(Exception e) { update(L.t("应用设置失败"),e.getMessage(),true); }
            else if(!active&&!busy) stopSelf();
            return START_NOT_STICKY;
        }
        if(TEST.equals(action)) {
            if(!active) { stopSelf(); return START_NOT_STICKY; }
            if(active&&!busy) { busy=true; update(L.t("正在检测出口"),detail,false); final Profile current=profile; final String at=address;
                final int token=generation;
                worker.execute(()->{ try { String ip=ProxyProbe.test(at,current); main.post(()->{ if(token!=generation) return; busy=false; if(active) { exitIp=ip; update(L.t("已连接"),connectionInfo(),false); applyPending(); } }); }
                    catch(Exception e) { main.post(()->{ if(token!=generation) return; busy=false; if(active) { update(L.t("出口检测失败"),e.getMessage(),true); applyPending(); } }); } }); }
            return START_NOT_STICKY;
        }
        if(!START.equals(action)||busy) return START_NOT_STICKY;
        if(active) {
            try {
                if(!isSharing()&&!intent.getBooleanExtra("deviceOnly",false)) {
                    Profile next=new ProfileStore(this).load();
                    if(intent.getBooleanExtra("createHotspot",false)) restartHotspot(next);
                    else restart(next,false,DeviceDiagnostics.address(next.iface));
                }
            } catch(Exception e) { update(L.t("启动失败"),e.getMessage(),true); }
            return START_NOT_STICKY;
        }
        busy=true; startForeground(1,notification(L.t("正在启动")));
        final int token=++generation;
        final Intent request=intent;
        update(L.t("正在启动"),L.t("正在检查配置和网络接口…"),false);
        worker.execute(()->{
            try {
                profile=new ProfileStore(this).load(); profile.validate();
                if(request.getBooleanExtra("createHotspot",false)) main.post(()->createHotspot(token));
                else { String at=request.getBooleanExtra("deviceOnly",false)||profile.iface.isEmpty()?"127.0.0.1":DeviceDiagnostics.address(profile.iface); launch(at,token); }
            } catch(Exception e) { main.post(()->fail(e.getMessage(),token)); }
        });
        return START_NOT_STICKY;
    }
    public static boolean isSharing() { return active&&!address.isEmpty()&&!address.equals("127.0.0.1"); }
    private void applyPending() { if(pendingApply&&active&&!busy) { pendingApply=false; startService(new Intent(this,ProxyService.class).setAction(APPLY)); } }
    private void restart(Profile next,boolean keepHotspot,String at) throws Exception {
        next.validate(); final int token=++generation;
        LocalVpnService.stop(); busy=true; active=false; exitIp="";
        if(!keepHotspot) closeHotspot();
        profile=next; update(L.t("正在应用设置"),"",false);
        worker.execute(()->{ try { launch(at,token); } catch(Exception e) { main.post(()->fail(e.getMessage(),token)); } });
    }
    private void restartHotspot(Profile next) throws Exception {
        next.validate(); final int token=++generation;
        LocalVpnService.stop(); busy=true; active=false; profile=next; exitIp="";
        update(L.t("正在开启热点"),"",false); worker.execute(()->{ core.stop(); main.post(()->createHotspot(token)); });
    }
    private void closeHotspot() {
        wifiName=""; wifiPassword="";
        if(hotspot!=null) { var old=hotspot; hotspot=null; old.close(); }
    }
    private Map<String,String> privateAddresses() throws Exception {
        Map<String,String> result=new HashMap<>();
        for(NetworkInterface ni:Collections.list(NetworkInterface.getNetworkInterfaces()))
            if(ni.isUp()) for(var a:Collections.list(ni.getInetAddresses())) if(a instanceof Inet4Address&&a.isSiteLocalAddress()) result.put(ni.getName(),a.getHostAddress());
        return result;
    }
    private void createHotspot(int token) {
        if(token!=generation) return;
        try {
            if(Build.VERSION.SDK_INT>=33) {
                if(checkSelfPermission(android.Manifest.permission.NEARBY_WIFI_DEVICES)!=android.content.pm.PackageManager.PERMISSION_GRANTED) throw new SecurityException(L.t("需要附近设备权限"));
            } else if(checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)!=android.content.pm.PackageManager.PERMISSION_GRANTED) throw new SecurityException(L.t("需要位置权限"));
            Map<String,String> before=privateAddresses();
            WifiManager.LocalOnlyHotspotCallback callback=new WifiManager.LocalOnlyHotspotCallback() {
                @Override public void onStarted(WifiManager.LocalOnlyHotspotReservation r) {
                    if(token!=generation) { r.close(); return; } hotspot=r;
                    if(Build.VERSION.SDK_INT>=30) { wifiName=r.getSoftApConfiguration().getSsid(); wifiPassword=r.getSoftApConfiguration().getPassphrase(); }
                    else { wifiName=r.getWifiConfiguration().SSID; wifiPassword=r.getWifiConfiguration().preSharedKey; }
                    update(L.t("热点已创建"),L.t("正在获取热点地址…"),false);
                    worker.execute(()->{
                        try {
                            for(int attempt=0;attempt<30;attempt++) {
                                Map<String,String> now=privateAddresses();
                                var changed=now.entrySet().stream().filter(e->!e.getValue().equals(before.get(e.getKey())) && e.getKey().matches("(?:wlan|ap|softap|swlan|ap_br_).*|wlan\\d|ap\\d")).collect(java.util.stream.Collectors.toList());
                                if(changed.size()==1) { profile.iface=changed.get(0).getKey(); launch(changed.get(0).getValue(),token); return; }
                                Thread.sleep(200);
                            }
                            main.post(()->fail(L.t("无法确定热点接口。请使用系统热点并选择接口后重试。"),token));
                        } catch(Exception e) { main.post(()->fail(e.getMessage(),token)); }
                    });
                }
                @Override public void onStopped() { if(token==generation) stopSession(L.t("热点已关闭"),L.t("App 热点已被系统关闭，请重新启动。"),false); }
                @Override public void onFailed(int reason) { fail(L.f("热点创建失败（%d）。检查附近设备权限、定位服务或系统热点是否已开启。",reason),token); }
            };
            if(profile.customHotspot||profile.hotspotBand!=0) {
                if(Build.VERSION.SDK_INT<36) throw new IllegalStateException(L.t("自定义 App 热点需要 Android 16；本机可用系统热点设置修改名称、密码和频段"));
                int band=profile.hotspotBand==1?SoftApConfiguration.BAND_2GHZ:profile.hotspotBand==2?SoftApConfiguration.BAND_5GHZ:SoftApConfiguration.BAND_2GHZ|SoftApConfiguration.BAND_5GHZ;
                android.util.SparseIntArray channels=new android.util.SparseIntArray(); channels.put(band,0);
                SoftApConfiguration.Builder builder=new SoftApConfiguration.Builder().setChannels(channels);
                if(profile.customHotspot) {
                    if(Build.VERSION.SDK_INT_FULL<3600001) throw new IllegalStateException(L.t("自定义热点名称与密码需要 Android 16.1；本机 16.0 可选择频段，名称与密码请在系统热点设置修改"));
                    // These methods became public in SDK 36.1; reflection keeps compatibility with the 36.0 build SDK.
                    SoftApConfiguration.Builder.class.getMethod("setWifiSsid",android.net.wifi.WifiSsid.class).invoke(builder,android.net.wifi.WifiSsid.fromBytes(profile.hotspotName.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8)));
                    SoftApConfiguration.Builder.class.getMethod("setPassphrase",String.class,int.class).invoke(builder,profile.hotspotPassword,SoftApConfiguration.SECURITY_TYPE_WPA2_PSK);
                }
                SoftApConfiguration config=builder.build();
                getSystemService(WifiManager.class).startLocalOnlyHotspotWithConfiguration(config,getMainExecutor(),callback);
            } else getSystemService(WifiManager.class).startLocalOnlyHotspot(callback,main);
        } catch(SecurityException e) { fail(L.f("热点权限不足：%s",e.getMessage()),token); }
        catch(LinkageError e) { fail(L.t("当前系统 Wi-Fi 模块尚不支持此配置接口，请选择自动频段或使用系统热点。"),token); }
        catch(Exception e) { fail(L.f("热点创建失败：%s",e.getMessage()),token); }
    }
    private void launch(String at,int token) throws Exception {
        if(token!=generation) return;
        core.start(profile,at,message->main.post(()->{ if(token==generation) stopSession(L.t("代理异常退出"),message,true); }));
        main.post(()->{
            if(token!=generation) { worker.execute(core::stop); return; }
            address=at; rulesSummary=core.ruleInfo; active=true; busy=false; update(L.t("已连接"),connectionInfo(),false);
            if(profile.vpnEnabled) startForegroundService(new Intent(this,LocalVpnService.class));
            applyPending();
        });
    }
    private String connectionInfo() {
        StringBuilder info=new StringBuilder(L.f("HTTP / SOCKS5：%s:%d\n",address,ConfigBuilder.MIXED_PORT));
        if(address.equals("127.0.0.1")) info.append(L.t("当前仅本机可访问。创建 App 热点或选择共享接口后重新启动。")).append("\n");
        else info.append(L.t("客户端 Wi-Fi → 代理 → 手动，填写以上地址和端口。")).append("\n");
        info.append(L.f("默认出口：%s · %s\n",RuleSubscription.policyName(core.effectiveFinal),profile.protocol.equals("hysteria2")?"HY2":"SOCKS5"));
        info.append(core.ruleInfo).append("\n");
        if(profile.localAuth) info.append(L.t("本地代理认证已开启，使用配置页中的用户名和密码。")).append("\n");
        if(profile.insecure&&profile.protocol.equals("hysteria2")) info.append(L.t("此 HY2 配置已跳过服务器证书校验。")).append("\n");
        if(hotspot!=null) {
            String ssid,pass;
            if(Build.VERSION.SDK_INT>=30) { var c=hotspot.getSoftApConfiguration(); ssid=c.getSsid(); pass=c.getPassphrase();
                if(Build.VERSION.SDK_INT>=36) info.append(L.f("配置频段：%s（1 = 2.4 GHz，2 = 5 GHz）\n",c.getChannels()));
            }
            else { var c=hotspot.getWifiConfiguration(); ssid=c.SSID; pass=c.preSharedKey; }
            info.append(L.f("\n热点名称：%s\nWi-Fi 密码：%s\n此热点提供局域网连接；互联网通过本 App 代理访问。",ssid,pass));
        }
        return info.toString();
    }
    private void fail(String message,int token) { if(token==generation) stopSession(L.t("启动失败"),message,true); }
    private void stopSession(String s,String d,boolean failed) {
        ++generation; active=false; busy=false; pendingApply=false; address=""; exitIp="";
        LocalVpnService.stop();
        closeHotspot();
        worker.execute(core::stop); update(s,d,failed); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf();
    }
    @Override public void onDestroy() {
        boolean wasActive=active;
        ++generation; active=false; busy=false;
        LocalVpnService.stop();
        if(wasActive) { status=L.t("已停止"); detail=L.t("后台服务已结束，请重新启动共享。"); failure=false; address=""; }
        closeHotspot();
        worker.execute(core::stop); worker.shutdown(); QuickControls.refresh(this); super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
