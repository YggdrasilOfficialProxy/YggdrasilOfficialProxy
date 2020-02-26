/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 03:21:23
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/Connecting.java
 */

package cn.mcres.karlatemp.yop.server;

import cn.mcres.karlatemp.yop.Metadata;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntPredicate;

public class Connecting {
    public static boolean run(
            ChannelHandlerContext context,
            HttpHeaders headers,
            HttpMethod method,
            ByteBuf content,
            boolean keepAlive,
            String uri,
            boolean official,
            boolean step2) throws IOException {
        return run(context, headers, method, content, keepAlive, uri, official, step2, x -> x < 400);
    }

    public static boolean run(
            ChannelHandlerContext context,
            HttpHeaders headers,
            HttpMethod method,
            ByteBuf content,
            boolean keepAlive,
            String uri,
            boolean official,
            boolean step2,
            IntPredicate codeOk) throws IOException {
        URL url;
        Proxy proxy;
        if (official) {
            int x = uri.indexOf('/', 1);
            String subDomain = uri.substring(1, x);
            String path = uri.substring(x + 1);
            url = new URL(getDomain(subDomain), path);
            proxy = Metadata.OfficialProxy;
        } else {
            url = new URL(Metadata.yggdrasilAsURL, uri.substring(1));
            proxy = Metadata.YggdrasilProxy;
        }
        return run(context, headers, method, content, keepAlive, url, proxy, step2, codeOk);
    }

    private static final ConcurrentHashMap<String, URL> domains = new ConcurrentHashMap<>();

    private static URL getDomain(String subDomain) {
        return domains.computeIfAbsent(subDomain.toLowerCase(), k -> {
            try {
                return new URL("https://" + k + ".mojang.com");
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public static boolean run(
            ChannelHandlerContext context,
            HttpHeaders headers,
            HttpMethod method,
            ByteBuf content,
            boolean keepAlive,
            URL url,
            Proxy proxy,
            boolean step2) throws IOException {
        return run(context, headers, method, content, keepAlive, url, proxy, step2, x -> x < 400);
    }

    public static boolean run(
            ChannelHandlerContext context,
            HttpHeaders headers,
            HttpMethod method,
            ByteBuf content,
            boolean keepAlive,
            URL url,
            Proxy proxy,
            boolean step2, IntPredicate codeOk) throws IOException {
        try {
            HttpURLConnection connection = (HttpURLConnection) url.openConnection(proxy);
            {
                Set<String> visited = new HashSet<>();
                for (Map.Entry<String, String> entry : headers.entries()) {
                    String key = entry.getKey();
                    String low = key.toLowerCase();
                    if (low.endsWith("-override")) continue;
                    if (visited.contains(low)) {
                        connection.addRequestProperty(key, entry.getValue());
                    } else {
                        visited.add(low);
                        connection.setRequestProperty(key, entry.getValue());
                    }
                }
                connection.setRequestProperty("User-Agent", "YggdrasilOfficialProxy/" + Metadata.VERSION + " Java/" + Metadata.javaVersion);
                visited.clear();
                for (Map.Entry<String, String> entry : headers.entries()) {
                    String key = entry.getKey();
                    String low = key.toLowerCase();
                    if (!low.endsWith("-override")) continue;
                    key = key.substring(0, key.length() - 9);
                    if (visited.contains(low)) {
                        connection.addRequestProperty(key, entry.getValue());
                    } else {
                        visited.add(low);
                        connection.setRequestProperty(key, entry.getValue());
                    }
                }
            }
            connection.setRequestMethod(method.name());
            if (content != null) {
                connection.setRequestProperty("Content-Length", Integer.toString(content.readableBytes()));
                connection.setDoOutput(true);
            }
            connection.setReadTimeout(Metadata.READ_TIMED_OUT);
            connection.setConnectTimeout(Metadata.CONNECT_TIMED_OUT);
            connection.connect();
            if (content != null) {
                OutputStream os = connection.getOutputStream();
                byte[] buf = new byte[1024];
                while (content.isReadable()) {
                    int size = Math.min(1024, content.readableBytes());
                    content.readBytes(buf, 0, size);
                    os.write(buf, 0, size);
                }
                os.flush();
                os.close();
            }
            int code = connection.getResponseCode();
            boolean ok = codeOk.test(code);
            if (!step2) {
                if (!ok) {
                    connection.disconnect();
                    return false;
                }
            }
            String message = connection.getResponseMessage();
            DefaultHttpHeaders responseHeaders = new DefaultHttpHeaders();
            for (Map.Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
                String key = header.getKey();
                if (key != null) {
                    responseHeaders.add(key, header.getValue());
                }
            }
            responseHeaders.set("Connection", Collections.singleton(keepAlive ? "keep-alive" : "close"));
            responseHeaders.set("Transfer-Encoding", "chunked");
            responseHeaders.remove("Content-Length");
            DefaultHttpResponse response = new DefaultHttpResponse(
                    HttpVersion.HTTP_1_1, new HttpResponseStatus(code, message), responseHeaders
            );
            context.writeAndFlush(response);
            byte[] buffer = new byte[1024];
            InputStream stream = connection.getResponseCode() >= 400 ? connection.getErrorStream() : connection.getInputStream();
            do {
                int len = stream.read(buffer);
                if (len == -1) break;
                context.writeAndFlush(
                        new DefaultHttpContent(Unpooled.copiedBuffer(buffer, 0, len))
                );
            } while (true);
            context.writeAndFlush(new DefaultLastHttpContent(Unpooled.EMPTY_BUFFER));
            // if (close) context.channel().close();
            return ok;
        } catch (SocketTimeoutException ignore) {
            return false;
        }
    }
}
