/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 03:21:23
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/XNames.java
 */

package cn.mcres.karlatemp.yop.server.api.user.profiles;

import cn.mcres.karlatemp.yop.Metadata;
import cn.mcres.karlatemp.yop.server.PatternContextHandler;
import cn.mcres.karlatemp.yop.server.HttpHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XNames implements HttpHandler {
    private static final Pattern pattern = Pattern.compile("^/api/user/profiles/(.+)/names$");

    @Override
    public void request(ChannelHandlerContext ctx, URI uri, boolean keepAlive, HttpRequest msg) throws Exception {
        final Matcher matcher = pattern.matcher(uri.getPath());
        matcher.find();
        URL url = new URL(Metadata.yggdrasilAsURL, "sessionserver/session/minecraft/profile/" + matcher.group(1) + "?unsigned=true");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection(Metadata.YggdrasilProxy);
        connection.setRequestProperty("User-Agent", "YggdrasilOfficialProxy/" + Metadata.VERSION + " Java/" + Metadata.javaVersion);
        connection.connect();
        JsonObject object;
        if (connection.getResponseCode() != 200) {
            ctx.writeAndFlush(new DefaultFullHttpResponse(
                    HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT,
                    Unpooled.EMPTY_BUFFER,
                    new DefaultHttpHeaders()
                            .add("Content-Length", 0)
                            .add("Connection", keepAlive ? "keep-alive" : "close"),
                    new DefaultHttpHeaders()
            ));
            if (!keepAlive) ctx.channel().close();
            return;
        }
        try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
            object = JsonParser.parseReader(reader).getAsJsonObject();
        }
        JsonArray result = new JsonArray();
        JsonObject name = new JsonObject();
        result.add(name);
        name.add("name", object.get("name"));
        byte[] data = result.toString().getBytes(StandardCharsets.UTF_8);
        ctx.writeAndFlush(new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                Unpooled.wrappedBuffer(data),
                new DefaultHttpHeaders()
                        .add("Content-Length", data.length)
                        .add("Content-Type", "application/json; charset=utf-8")
                        .add("Connection", keepAlive ? "keep-alive" : "close"),
                new DefaultHttpHeaders()
        ));
        if (!keepAlive) ctx.channel().close();
    }

    public void register() {
        PatternContextHandler.register(pattern, this);
    }
}
