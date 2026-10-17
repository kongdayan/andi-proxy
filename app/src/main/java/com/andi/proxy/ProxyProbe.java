package com.andi.proxy;

import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public final class ProxyProbe {
    private static int read(InputStream in) throws Exception { int b=in.read(); if(b<0) throw new IllegalStateException(L.t("代理连接提前关闭")); return b; }
    private static void skip(InputStream in,int n) throws Exception { for(int i=0;i<n;i++) read(in); }
    public static String test(String address,Profile p) throws Exception {
        try(Socket raw=new Socket()) {
            raw.connect(new InetSocketAddress(address,ConfigBuilder.MIXED_PORT),5000); raw.setSoTimeout(15000);
            InputStream in=raw.getInputStream(); OutputStream out=raw.getOutputStream();
            out.write(new byte[]{5,1,(byte)(p.localAuth?2:0)}); out.flush();
            if(read(in)!=5 || read(in)!=(p.localAuth?2:0)) throw new IllegalStateException(L.t("本地代理认证协商失败"));
            if(p.localAuth) {
                byte[] u=p.localUsername.getBytes(StandardCharsets.UTF_8), pw=p.localPassword.getBytes(StandardCharsets.UTF_8);
                out.write(1); out.write(u.length); out.write(u); out.write(pw.length); out.write(pw); out.flush();
                if(read(in)!=1 || read(in)!=0) throw new IllegalStateException(L.t("本地代理用户名或密码错误"));
            }
            String host="api.ipify.org";
            byte[] h=host.getBytes(StandardCharsets.US_ASCII);
            out.write(new byte[]{5,1,0,3,(byte)h.length}); out.write(h); out.write(new byte[]{1,(byte)187}); out.flush();
            if(read(in)!=5 || read(in)!=0) throw new IllegalStateException(L.t("远程代理连接失败，请检查认证、TLS 和网络"));
            read(in); int type=read(in); skip(in,type==1?4:type==4?16:read(in)); skip(in,2);
            try(SSLSocket tls=(SSLSocket)((SSLSocketFactory)SSLSocketFactory.getDefault()).createSocket(raw,host,443,true)) {
                var params=tls.getSSLParameters(); params.setEndpointIdentificationAlgorithm("HTTPS"); tls.setSSLParameters(params); tls.setSoTimeout(15000); tls.startHandshake();
                tls.getOutputStream().write(("GET / HTTP/1.1\r\nHost: "+host+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
                java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream(); byte[] buffer=new byte[1024]; int n;
                while(bytes.size()<8192 && (n=tls.getInputStream().read(buffer,0,Math.min(buffer.length,8192-bytes.size())))!=-1) bytes.write(buffer,0,n);
                byte[] response=bytes.toByteArray();
                String text=new String(response,StandardCharsets.UTF_8); int sep=text.indexOf("\r\n\r\n");
                if(!text.startsWith("HTTP/1.1 200") && !text.startsWith("HTTP/1.0 200")) throw new IllegalStateException(L.t("出口检测服务返回异常"));
                String body=sep>=0?text.substring(sep+4).trim():"";
                java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?:\\d{1,3}\\.){3}\\d{1,3}").matcher(body);
                if(!m.find()) throw new IllegalStateException(L.t("出口检测未返回 IPv4"));
                return m.group();
            }
        }
    }
}
