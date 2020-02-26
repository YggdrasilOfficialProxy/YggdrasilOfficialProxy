/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 03:21:23
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/PatternContextHandler.java
 */

package cn.mcres.karlatemp.yop.server;

import cn.mcres.karlatemp.mxlib.tools.Toolkit;
import io.netty.channel.Channel;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class PatternContextHandler extends ContextHandler {
    public static final List<Map.Entry<Pattern, HttpHandler>> handlers = new ArrayList<>();
    private static HttpHandler noneCallback;

    public static void setNoneCallback(HttpHandler notFound) {
        noneCallback = notFound;
    }

    public static void register(
            @NotNull @Language("RegExp") String regex,
            @NotNull HttpHandler handler) {
        register(Pattern.compile(regex), handler);
    }

    public static void register(@NotNull Pattern regex,
                                @NotNull HttpHandler handler) {
        handlers.add(Toolkit.entry(regex, handler));
    }

    @Override
    public ContextHandler setup(Map<String, HttpHandler> handlers, Channel channel) {
        super.setup(handlers, channel);
        return this;
    }

    @Override
    public HttpHandler getHandler(URI uri) {
        return getHandler0(uri);
    }

    public static HttpHandler getHandler0(URI uri) {
        final String string = uri.toString();
        for (Map.Entry<Pattern, HttpHandler> entry : handlers) {
            Pattern p = entry.getKey();
            final HttpHandler handler = entry.getValue();
            if (p == null || handler == null) {
                continue;
            }
            if (p.matcher(string).find()) {
                return handler;
            }
        }
        return noneCallback;
    }
}
