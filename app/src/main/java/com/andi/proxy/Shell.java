package com.andi.proxy;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public final class Shell {
    public record Result(int code, String output) { public boolean ok() { return code==0; } }
    public static String quote(String s) { return "'"+s.replace("'","'\\''")+"'"; }
    public static Result run(String command, int seconds) throws Exception {
        Process p=new ProcessBuilder("sh","-c",command).redirectErrorStream(true).start();
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        Thread reader=new Thread(()->{ try { byte[] buffer=new byte[4096]; int n; while((n=p.getInputStream().read(buffer))!=-1) out.write(buffer,0,n); } catch(Exception ignored) {} });
        reader.start();
        if (!p.waitFor(seconds,TimeUnit.SECONDS)) { p.destroyForcibly(); reader.join(1000); throw new IllegalStateException(L.t("命令超时，请检查 root 授权")); }
        reader.join(1000);
        return new Result(p.exitValue(),new String(out.toByteArray(),StandardCharsets.UTF_8));
    }
}
