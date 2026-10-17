package com.andi.proxy;
import java.nio.file.*;
public final class GenerateConfig {
    public static void main(String[] args) throws Exception {
        Profile p=ProfileImporter.parse(new String(Files.readAllBytes(Path.of(args[0])),java.nio.charset.StandardCharsets.UTF_8));
        String config=ConfigBuilder.build(p,args.length>2?args[2]:"127.0.0.1").toString(2);
        Files.write(Path.of(args[1]),config.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        System.out.println("Configuration generated; credentials omitted.");
    }
}
