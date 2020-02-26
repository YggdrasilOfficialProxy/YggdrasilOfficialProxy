/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 03:21:23
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/Minecraft.java
 */

package cn.mcres.karlatemp.yop.server.api.users;

import cn.mcres.karlatemp.yop.Metadata;
import cn.mcres.karlatemp.yop.server.PatternContextHandler;
import cn.mcres.karlatemp.yop.server.HttpHandler;
import cn.mcres.karlatemp.yop.server.URLDecoder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Minecraft implements HttpHandler {
    public static final Pattern matcher =
            Pattern.compile("^/api/users/profiles/minecraft/(.+)(\\?.+|)$");

    @Override
    public void request(ChannelHandlerContext ctx, URI uri, boolean keepAlive, HttpRequest msg) throws Exception {
        final Matcher matcher = Minecraft.matcher.matcher(uri.getPath());
        matcher.find();
        final String username = URLDecoder.decode(matcher.group(1), StandardCharsets.UTF_8);
        URL url = new URL(Metadata.yggdrasilAsURL, "api/profiles/minecraft");
        final HttpURLConnection connection = (HttpURLConnection) url.openConnection(Metadata.YggdrasilProxy);
        connection.setRequestMethod("POST");
        final JsonArray array = new JsonArray();
        array.add(username);
        final String string = array.toString();
        byte[] data = string.getBytes(StandardCharsets.UTF_8);
        connection.setRequestProperty("User-Agent", "YggdrasilOfficialProxy/" + Metadata.VERSION + " Java/" + Metadata.javaVersion);
        connection.setRequestProperty("Content-Length", Long.toString(data.length & 0xFFFFFFFFL));
        connection.setDoOutput(true);
        connection.setConnectTimeout(Metadata.CONNECT_TIMED_OUT);
        connection.setReadTimeout(Metadata.READ_TIMED_OUT);
        connection.connect();
        final OutputStream stream = connection.getOutputStream();
        stream.write(data);
        stream.flush();
        stream.close();
        JsonArray array0;
        try (InputStreamReader reader = new InputStreamReader(
                connection.getInputStream(), StandardCharsets.UTF_8
        )) {
            array0 = JsonParser.parseReader(reader).getAsJsonArray();
        }
        switch (array0.size()) {
            case 0:
                ctx.writeAndFlush(new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.NO_CONTENT,
                        Unpooled.EMPTY_BUFFER,
                        new DefaultHttpHeaders().add("Content-Length", 0)
                                .add("Connection", keepAlive ? "keep-alive" : "close"), new DefaultHttpHeaders()
                ));
                break;
            case 1: {
                byte[] data0 = array0.get(0).toString().getBytes(StandardCharsets.UTF_8);
                ctx.writeAndFlush(new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
                        Unpooled.wrappedBuffer(data0),
                        new DefaultHttpHeaders().add("Content-Length", data0.length)
                                .add("Connection", keepAlive ? "keep-alive" : "close")
                                .add("Content-Type", "application/json; charset=utf-8"), new DefaultHttpHeaders()
                ));
                break;
            }
            default:
                ctx.writeAndFlush(new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST,
                        Unpooled.EMPTY_BUFFER,
                        new DefaultHttpHeaders().add("Content-Length", 0)
                                .add("Connection", keepAlive ? "keep-alive" : "close"), new DefaultHttpHeaders()
                ));
                break;
        }
        if (!keepAlive) ctx.channel().close();
    }

    public void register() {
        PatternContextHandler.register(
                matcher, this);
    }
}
