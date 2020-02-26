/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 03:21:23
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/Metadata.java
 */

package cn.mcres.karlatemp.yop;

import java.net.Proxy;
import java.net.URL;

public class Metadata {
    public static final String NAME = "Yggdrasil Official Proxy";
    public static final String VERSION = System.getProperty("YggdrasilOfficialProxy.version", "1.0.0");
    public static String yggdrasil = System.getProperty("YggdrasilOfficialProxy.yggdrasil", System.getenv("YGGDRASIL"));
    public static URL yggdrasilAsURL;

    public static Proxy OfficialProxy = Proxy.NO_PROXY;
    public static Proxy YggdrasilProxy = Proxy.NO_PROXY;
    public static boolean yggdrasilFix;
    public static int READ_TIMED_OUT = 1000 * 60;
    public static int CONNECT_TIMED_OUT = 1000 * 60;
    public static boolean LoginOfficialFirst = false;
    public static final String javaVersion = System.getProperty("java.version");
    public static int serverPort;
    // public static final Map<String, HttpHandler> handlers = new ConcurrentHashMap<>();
}
