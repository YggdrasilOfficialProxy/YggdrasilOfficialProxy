/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 03:21:23
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/ConfigSetup.java
 */

package cn.mcres.karlatemp.yop;

import cn.mcres.karlatemp.yop.config.ConfigurationSection;
import cn.mcres.karlatemp.yop.config.YamlConfiguration;
import cn.mcres.karlatemp.yop.server.api.user.profiles.XNames;
import cn.mcres.karlatemp.yop.server.api.users.Minecraft;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ServerSocket;
import java.net.URL;

public class ConfigSetup {
    private static boolean initialized;
    public static ConfigurationSection opts;

    public static void writeTo(InputStream input, OutputStream out) throws IOException {
        byte[] buf = new byte[1024];
        do {
            final int read = input.read(buf);
            if (read == -1) break;
            out.write(buf, 0, read);
        } while (true);
    }

    public synchronized static void init(String path) throws IOException {
        if (initialized) return;
        YamlConfiguration yaml = new YamlConfiguration();
        File file = new File(path);
        if (file.isFile()) {
            yaml.load(file);
        } else {
            //noinspection ResultOfMethodCallIgnored
            file.getParentFile().mkdirs();
            final InputStream stream = ConfigSetup.class.getResourceAsStream("config.yml");
            if (stream == null) {
                System.out.println("[Yggdrasil Official Proxy] [WARNING] Default Configuration Missing.");
            } else {
                try (InputStream target = stream) {
                    try (FileOutputStream os = new FileOutputStream(file)) {
                        writeTo(target, os);
                    }
                }
            }
            throw new IOException("Configuration not found. saved to " + path);
        }
        opts = yaml.section("extension.opts").useSplitter(true);
        Metadata.yggdrasilAsURL = new URL(Metadata.yggdrasil = shorter(yaml.string("yggdrasil", Metadata.yggdrasil)));
        Metadata.LoginOfficialFirst = yaml.booleanV("official-first", false);
        Metadata.READ_TIMED_OUT = yaml.intV("timed-out.reading", Metadata.READ_TIMED_OUT);
        Metadata.CONNECT_TIMED_OUT = yaml.intV("timed-out.connecting", Metadata.CONNECT_TIMED_OUT);
        Metadata.OfficialProxy = parseProxy(yaml.section("proxy.official"));
        Metadata.YggdrasilProxy = parseProxy(yaml.section("proxy.yggdrasil"));
        Metadata.serverPort = parsePort(yaml.string("port"));
        if (Metadata.yggdrasilFix = yaml.booleanV("extension.yggdrasil-fix", true)) {
            new Minecraft().register();
            new XNames().register();
        }
        PrintStream out = System.out;
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (out) {
            out.println("############################");
            out.println("# Yggdrasil Official Proxy #");
            out.println("# Copyright (c) Karlatemp  #");
            out.println("#     2018-2020.           #");
            out.println("# All rights reserved.     #");
            out.println("############################");
            out.println("#            Version: " + Metadata.VERSION);
            out.println("#    Java    Version: " + Metadata.javaVersion);
            out.println("#    Server Port    : " + Metadata.serverPort);
            out.println("# Configuration Path: " + path);
            out.println("# Yggdrasil API ROOT: " + Metadata.yggdrasil);
            out.println("#     First Connect : " + (Metadata.LoginOfficialFirst ? "Official" : "Yggdrasil"));
            out.println("# Proxy of Official : " + Metadata.OfficialProxy);
            out.println("# Proxy of Yggdrasil: " + Metadata.YggdrasilProxy);
            out.println("# Connect Timed out : " + Metadata.READ_TIMED_OUT);
            out.println("# Reading Timed out : " + Metadata.CONNECT_TIMED_OUT);
            out.println("# Yggdrasil-Fix: " + Metadata.yggdrasilFix);
            out.println("############################");
        }
        initialized = true;
    }

    private static int parsePort(String port) {
        try {
            int p = Integer.parseInt(port);
            if (p == 0) {
                try (ServerSocket socket = new ServerSocket(0)) {
                    p = socket.getLocalPort();
                }
            }
            return p;
        } catch (Throwable ignore) {
        }
        final int indexOf = port.indexOf('~');
        if (indexOf != -1) {
            int START = 0;
            int end = 0xFFFF - 2;
            String fir = port.substring(0, indexOf);
            String endf = port.substring(indexOf + 1);
            if (!fir.isEmpty()) {
                START = Integer.parseInt(fir);
            }
            if (!endf.isEmpty()) {
                end = Integer.parseInt(endf);
            }
            end++;
            int chunk = end - START;
            int maxTest = 500;
            int result;
            // START + (int) (chunk * Math.random())
            do {
                result = START + (int) (chunk * Math.random());
                try (ServerSocket ignore = new ServerSocket(result)) {
                    return result;
                } catch (IOException ignore) {
                }
            } while (maxTest-- > 0);
            return result;
        }
        return 4321;
    }

    private static String shorter(String yggdrasil) {
        if (yggdrasil == null) return null;
        do {
            if (yggdrasil.isEmpty()) return yggdrasil;
            int len = yggdrasil.length();
            if (yggdrasil.charAt(len - 1) == '/') {
                yggdrasil = yggdrasil.substring(0, len - 1);
                continue;
            }
            return yggdrasil + '/';
        } while (true);
    }

    public static Proxy parseProxy(ConfigurationSection value) {
        if (value != null) {
            Proxy.Type type;
            switch (value.string("type", "direct")) {
                case "direct":
                case "no":
                case "close":
                    return Proxy.NO_PROXY;
                case "http":
                    type = Proxy.Type.HTTP;
                    break;
                case "socks":
                    type = Proxy.Type.SOCKS;
                    break;
                default:
                    return null;
            }
            return new Proxy(type, new InetSocketAddress(value.string("host"), value.intV("port")));
        }
        return null;
    }
}
