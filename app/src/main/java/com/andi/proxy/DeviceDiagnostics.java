package com.andi.proxy;

import android.os.Build;
import java.net.NetworkInterface;
import java.net.Inet4Address;
import java.util.Collections;

public final class DeviceDiagnostics {
    public static String address(String iface) throws Exception {
        NetworkInterface ni=NetworkInterface.getByName(iface);
        if (ni==null || !ni.isUp()) throw new IllegalArgumentException(L.f("热点接口不存在或未开启：%s", iface));
        for (var a:Collections.list(ni.getInetAddresses())) if (a instanceof Inet4Address && a.isSiteLocalAddress()) return a.getHostAddress();
        throw new IllegalArgumentException(L.t("热点接口没有私网 IPv4 地址"));
    }
    public static String interfaces() {
        StringBuilder b=new StringBuilder();
        try {
            for (NetworkInterface ni:Collections.list(NetworkInterface.getNetworkInterfaces())) {
                for (var a:Collections.list(ni.getInetAddresses())) if (a instanceof Inet4Address && !a.isLoopbackAddress())
                    b.append(ni.getName()).append("  ").append(a.getHostAddress()).append('\n');
            }
        } catch(Exception e) { b.append(L.t("网络接口读取失败")); }
        return b.toString();
    }
    public static String report(String corePath) {
        StringBuilder b=new StringBuilder(Build.MANUFACTURER+" "+Build.MODEL+"\nAndroid "+Build.VERSION.RELEASE+" / API "+Build.VERSION.SDK_INT+"\n");
        b.append(L.t("ABI：")).append(String.join(", ",Build.SUPPORTED_ABIS)).append("\n\n").append(interfaces());
        try { b.append("\n").append(Shell.run(Shell.quote(corePath)+" version",8).output()); } catch(Exception e) { b.append(L.t("核心执行失败：")).append(e.getMessage()); }
        b.append("\n").append(L.t("本版本无需 root。热点客户端设置 HTTP/SOCKS5 代理后，按规则连接远程出口。系统 Wi-Fi HTTP 代理只覆盖遵守代理设置的应用。")).append("\n");
        return b.toString();
    }
}
