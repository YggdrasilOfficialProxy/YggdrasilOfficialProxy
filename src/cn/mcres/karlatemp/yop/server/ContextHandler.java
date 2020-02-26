/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 03:21:23
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/ContextHandler.java
 */

package cn.mcres.karlatemp.yop.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.*;
import io.netty.util.AttributeKey;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

public class ContextHandler extends ChannelInboundHandlerAdapter {
    private Map<String, HttpHandler> handlers;

    static class PostPool {
        HttpRequest request;
        HttpHandler select;
        Deque<ByteBuf> buffers;
        boolean keepalive;
        URI uri;
    }

    public static final AttributeKey<PostPool> DATA = AttributeKey.newInstance("YOP-POST");

    public static ByteBuf getContent(HttpRequest msg) {
        if (msg instanceof PostRequest) return ((PostRequest) msg).content;
        if (msg instanceof HttpContent) return ((HttpContent) msg).content();
        return null;
    }

    public ContextHandler setup(Map<String, HttpHandler> handlers, Channel channel) {
        this.handlers = handlers;
        channel.attr(DATA).set(new PostPool());
        return this;
    }

    public static boolean isKeepAlive(HttpRequest request) {
        if (request.headers().contains("Connection")) {
            return request.headers().get("Connection").equalsIgnoreCase("Keep-Alive");
        }
        return true;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            HttpRequest request = (HttpRequest) msg;
            if (request.protocolVersion() != HttpVersion.HTTP_1_1) {
                ByteBuf body = Unpooled.wrappedBuffer(("<html>" +
                        "<head>" +
                        "<title>Sorry, But Proxy Server not Allow this HTTP Version.</title>" +
                        "</head>" +
                        "<body>Sorry, but this version not allowed. Please use Http version 1.1</body>" +
                        "</html>").getBytes(StandardCharsets.UTF_8));
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.METHOD_NOT_ALLOWED,
                        body,
                        new DefaultHttpHeaders()
                                .add("Content-Type", "text/html; charset=utf8")
                                .add("Content-Length", Long.toString(body.readableBytes())),
                        new DefaultHttpHeaders()
                );
                ctx.writeAndFlush(response);
                ctx.channel().close();
                return;
            }
            boolean keep = isKeepAlive(request);
            URI uri;
            try {
                uri = URI.create(request.uri());
                if (uri.getPort() != -1 || uri.getScheme() != null || uri.getHost() != null) throw new IOException();
            } catch (Throwable throwable) {
                DefaultHttpHeaders headers = new DefaultHttpHeaders();
                headers.set("Content-Length", "0");
                headers.set("Connection", keep ? "keep-alive" : "close");
                ctx.writeAndFlush(new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
                        HttpResponseStatus.BAD_REQUEST,
                        Unpooled.EMPTY_BUFFER,
                        headers,
                        new DefaultHttpHeaders()));
                if (!keep) {
                    ctx.channel().close();
                }
                return;
            }
            HttpHandler handler = getHandler(uri);
            if (handler == null) {
                ByteBuf body = Unpooled.wrappedBuffer(("<html>" +
                        "<head>" +
                        "<title>Sorry, But this context not found.</title>" +
                        "</head>" +
                        "<body>Sorry, But this context not found.</body>" +
                        "</html>").getBytes(StandardCharsets.UTF_8));
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(
                        HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND,
                        body,
                        new DefaultHttpHeaders()
                                .add("Content-Type", "text/html; charset=utf8")
                                .add("Content-Length", Long.toString(body.readableBytes())),
                        new DefaultHttpHeaders()
                );
                ctx.writeAndFlush(response);
                ctx.channel().close();
                return;
            }
            if (request.method() == HttpMethod.POST) {
                final PostPool data = ctx.channel().attr(DATA).get();
                data.select = handler;
                data.buffers = new ConcurrentLinkedDeque<>();
                data.request = request;
                data.keepalive = keep;
                data.uri = uri;
            } else {
                handler.request(ctx, uri, keep, request);
            }
        }
        if (msg instanceof HttpContent) {
            final PostPool pool = ctx.channel().attr(DATA).get();
            if (pool == null || pool.select == null) return;
            ByteBuf buf = ((HttpContent) msg).content();
            if (buf != null) {
                final ByteBuf copy = buf.copy();
                if (copy != null)
                    pool.buffers.add(copy);
            }
            if (msg instanceof LastHttpContent) {
                HttpHandler handler = pool.select;
                pool.select = null;
                Deque<ByteBuf> buffers = pool.buffers;
                pool.buffers = null;
                ByteBuf allInOne = Unpooled.wrappedBuffer(buffers.toArray(new ByteBuf[0]));
                handler.request(ctx, pool.uri, pool.keepalive, new PostRequest(pool.request, allInOne));
            }
        }
    }

    public HttpHandler getHandler(URI uri) {
        String path = uri.getPath();
        do {
            HttpHandler handler = handlers.get(path);
            if (handler != null) return handler;
            int length = path.length();
            if (length == 0 || length == 1) return null;
            if (path.charAt(length - 1) == '/') {
                path = path.substring(0, length - 1);
                continue;
            }
            int index = path.lastIndexOf('/');
            path = path.substring(0, index + 1);
        } while (true);
    }

    static class PostRequest implements HttpRequest {
        final HttpRequest req;
        final ByteBuf content;

        @Override
        @Deprecated
        public HttpMethod getMethod() {
            return req.getMethod();
        }

        @Override
        public HttpMethod method() {
            return req.method();
        }

        @Override
        public HttpRequest setMethod(HttpMethod method) {
            return req.setMethod(method);
        }

        @Override
        @Deprecated
        public String getUri() {
            return req.getUri();
        }

        @Override
        public String uri() {
            return req.uri();
        }

        @Override
        public HttpRequest setUri(String uri) {
            return req.setUri(uri);
        }

        @Override
        public HttpRequest setProtocolVersion(HttpVersion version) {
            return req.setProtocolVersion(version);
        }

        @Override
        @Deprecated
        public HttpVersion getProtocolVersion() {
            return req.getProtocolVersion();
        }

        @Override
        public HttpVersion protocolVersion() {
            return req.protocolVersion();
        }

        @Override
        public HttpHeaders headers() {
            return req.headers();
        }

        @Override
        @Deprecated
        public DecoderResult getDecoderResult() {
            return req.getDecoderResult();
        }

        @Override
        public DecoderResult decoderResult() {
            return req.decoderResult();
        }

        @Override
        public void setDecoderResult(DecoderResult result) {
            req.setDecoderResult(result);
        }

        PostRequest(HttpRequest request, ByteBuf allInOne) {
            this.req = request;
            this.content = allInOne;
        }
    }
}
