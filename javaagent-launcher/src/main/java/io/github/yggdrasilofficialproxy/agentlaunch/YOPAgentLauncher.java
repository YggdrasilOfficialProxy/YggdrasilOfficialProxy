/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/04 24:20:54
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.javaagent-launcher.main/YOPAgentLauncher.java
 */

package io.github.yggdrasilofficialproxy.agentlaunch;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.zip.ZipInputStream;

public class YOPAgentLauncher {
    public static void premain(String opt, Instrumentation instrumentation) throws Throwable {
        InputStream resource = YOPAgentLauncher.class.getResourceAsStream("/yop-artifact/yggdrasil-official-proxy.jar");
        if (resource == null) throw new IllegalStateException("No yop artifact");
        InMemoryUrlHandler handler = new InMemoryUrlHandler();
        try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(resource))) {
            handler.rs = InMemoryJarLoader.load(zip);
        }
        URL sysRoot = new URL(null, "vmvs-yop-inm:///", handler);
        URLClassLoader uc = new URLClassLoader(new URL[]{sysRoot}, ClassLoader.getSystemClassLoader().getParent());

        uc.loadClass("io.github.yggdrasilofficialproxy.YopBootstrap")
                .getMethod("main", String[].class)
                .invoke(null, (Object) new String[]{"daemon"});
    }
}
