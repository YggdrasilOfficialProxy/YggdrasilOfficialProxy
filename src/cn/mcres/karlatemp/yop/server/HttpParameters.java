/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 03:21:23
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/HttpParameters.java
 */

package cn.mcres.karlatemp.yop.server;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpParameters {
    public static <T extends Map<String, String>> T parse(T map, String query) {
        if (query == null) return map;
        if (map == null) return null;
        int start = 0;
        do {
            int next = query.indexOf('&', start);
            String parsing;
            if (next == -1) {
                parsing = query.substring(start);
            } else {
                parsing = query.substring(start, next);
                start = next + 1;
            }
            int set = parsing.indexOf('=');
            if (set != -1) {
                String key = parsing.substring(0, set);
                String val = parsing.substring(set + 1);
                try {
                    map.put(URLDecoder.decode(key, StandardCharsets.UTF_8),
                            URLDecoder.decode(val, StandardCharsets.UTF_8));
                } catch (Exception ignore) {
                }
            }
            if (next == -1) break;
        } while (true);
        return map;
    }
}
