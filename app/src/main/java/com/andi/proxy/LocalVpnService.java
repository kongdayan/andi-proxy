package com.andi.proxy;

import android.app.*;
import android.content.*;
import android.net.VpnService;
import android.os.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import hev.htproxy.TProxyService;

/** Device traffic uses the same routing core through a loopback-only SOCKS listener. */
public final class LocalVpnService extends VpnService {
    public static volatile boolean active;
    public static volatile String state = L.t("本机 VPN 未启动");
    private static volatile LocalVpnService instance;
    private boolean nativeStarted, established;
    private ParcelFileDescriptor tunnel;
    private File config;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable health=new Runnable() { public void run() {
        if(active&&!TProxyService.TProxyIsRunning()) { state=L.t("本机隧道异常退出"); established=false; shutdown(); stopSelf(); }
        else if(active) handler.postDelayed(this,2000);
    }};
    @Override public void onCreate() { super.onCreate(); instance=this; }
    public static void stop() {
        LocalVpnService service=instance;
        if(service!=null) {
            Runnable stop=()->{ service.shutdown(); service.stopSelf(); };
            if(Looper.myLooper()==Looper.getMainLooper()) stop.run(); else service.handler.post(stop);
        }
    }
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        if(intent==null||!ProxyService.active) { stopSelf(); return START_NOT_STICKY; }
        if(active) return START_NOT_STICKY;
        try {
            Profile p=new ProfileStore(this).load();
            if(!p.vpnEnabled) { stopSelf(); return START_NOT_STICKY; }
            if(prepare(this)!=null) throw new SecurityException(L.t("请在 App 中允许 VPN 连接"));
            NotificationManager nm=getSystemService(NotificationManager.class);
            NotificationChannel channel=nm.getNotificationChannel("vpn");
            if(channel==null) channel=new NotificationChannel("vpn",L.t("本机代理"),NotificationManager.IMPORTANCE_LOW);
            else channel.setName(L.t("本机代理"));
            nm.createNotificationChannel(channel);
            PendingIntent open=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE);
            startForeground(2,new Notification.Builder(this,"vpn").setSmallIcon(R.drawable.ic_status).setContentTitle(L.t("Andi · 本机分流"))
                .setContentText(L.t("应用白名单直连，其余应用按规则路由")).setContentIntent(open).setOngoing(true).build());
            Builder b=new Builder().setSession("Andi Proxy").setMtu(1500).addAddress("198.18.0.1",30).addAddress("fd00:a::1",126)
                .addRoute("0.0.0.0",0).addRoute("::",0).addDnsServer("198.18.0.2").setConfigureIntent(open);
            b.addDisallowedApplication(getPackageName()); // Core sockets and tunnel transport must bypass their own VPN.
            int bypass=0;
            for(String name:p.bypassApps.split("\\s+")) if(!name.isEmpty()&&!name.equals(getPackageName())) {
                try { b.addDisallowedApplication(name); bypass++; } catch(android.content.pm.PackageManager.NameNotFoundException ignored) {}
            }
            if(Build.VERSION.SDK_INT>=29) b.setMetered(false);
            tunnel=b.establish(); if(tunnel==null) throw new IllegalStateException(L.t("VPN 权限被撤销或其他 VPN 已接管"));
            config=new File(getFilesDir(),"vpn-tunnel.yml");
            String yaml="tunnel:\n  mtu: 1500\n  ipv4: 198.18.0.1\n  ipv6: 'fd00:a::1'\nsocks5:\n  address: 127.0.0.1\n  port: 2081\n  udp: 'udp'\nmisc:\n  task-stack-size: 86016\n  connect-timeout: 10000\n  log-level: warn\n";
            Files.write(config.toPath(),yaml.getBytes(StandardCharsets.UTF_8));
            if(!TProxyService.TProxyStartService(config.getAbsolutePath(),tunnel.getFd())) throw new IllegalStateException(L.t("本机隧道启动失败"));
            nativeStarted=true; established=true;
            active=true; state=L.f("本机 VPN 已连接 · %d 个白名单应用直连", bypass); handler.postDelayed(health,2000); QuickControls.refresh(this);
        } catch(Exception|LinkageError e) { state=L.f("本机 VPN 失败：%s", e.getMessage()); shutdown(); stopSelf(); }
        return START_NOT_STICKY;
    }
    @Override public void onRevoke() { state=L.t("VPN 已被系统或其他应用接管"); established=false; shutdown(); stopSelf(); }
    private void shutdown() {
        handler.removeCallbacks(health);
        active=false;
        if(nativeStarted) { nativeStarted=false; TProxyService.TProxyStopService(); if(established) state=L.t("本机 VPN 已停止"); }
        established=false;
        if(tunnel!=null) { try { tunnel.close(); } catch(Exception ignored) {} tunnel=null; }
        if(config!=null) { config.delete(); config=null; } stopForeground(STOP_FOREGROUND_REMOVE); QuickControls.refresh(this);
    }
    @Override public void onDestroy() { shutdown(); if(instance==this) instance=null; super.onDestroy(); }
}
