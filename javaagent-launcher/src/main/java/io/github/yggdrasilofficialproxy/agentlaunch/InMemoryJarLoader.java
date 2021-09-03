/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/04 24:20:54
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.javaagent-launcher.main/InMemoryJarLoader.java
 */

package io.github.yggdrasilofficialproxy.agentlaunch;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class InMemoryJarLoader {
    static String prevPath(String path) {
        while (!path.isEmpty() && path.charAt(0) == '/') {
            path = path.substring(1);
        }
        return path;
    }

    public static Map<String, byte[]> load(ZipInputStream stream) throws IOException {
        Map<String, byte[]> res = new HashMap<>();
        ByteArrayOutputStream baos = new ByteArrayOutputStream(20480);
        byte[] buf = new byte[20480];
        while (true) {
            ZipEntry entry = stream.getNextEntry();
            if (entry == null) break;
            if (entry.isDirectory()) continue;
            String path = prevPath(entry.getName());

            while (true) {
                int ld = stream.read(buf);
                if (ld == -1) break;
                baos.write(buf, 0, ld);
            }
            res.put(path, baos.toByteArray());
            baos.reset();
        }
        return res;
    }
}
