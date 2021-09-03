/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/04 24:20:54
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.javaagent-launcher.main/InMemoryUrlHandler.java
 */

package io.github.yggdrasilofficialproxy.agentlaunch;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Map;

public class InMemoryUrlHandler extends URLStreamHandler {
    Map<String, byte[]> rs;

    @Override
    protected URLConnection openConnection(URL u) throws IOException {
        byte[] d = rs.get(InMemoryJarLoader.prevPath(u.getPath()));
        if (d == null) throw new FileNotFoundException(u.getPath());
        return new URLConnection(u) {
            @Override
            public int getContentLength() {
                return d.length;
            }

            @Override
            public String getHeaderField(String name) {
                if (name.equalsIgnoreCase("Content-Length")) {
                    return String.valueOf(d.length);
                }
                return super.getHeaderField(name);
            }

            @Override
            public long getContentLengthLong() {
                return d.length;
            }

            @Override
            public InputStream getInputStream() throws IOException {
                return new ByteArrayInputStream(d);
            }

            @Override
            public void connect() throws IOException {
            }
        };
    }
}
