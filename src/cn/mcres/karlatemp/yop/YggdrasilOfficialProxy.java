/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 03:21:23
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/YggdrasilOfficialProxy.java
 */

package cn.mcres.karlatemp.yop;

import cn.mcres.karlatemp.mxlib.network.PipelineUtils;
import cn.mcres.karlatemp.yop.server.ContextHandler;
import cn.mcres.karlatemp.yop.server.PatternContextHandler;
import cn.mcres.karlatemp.yop.server.sessionserver.session.minecraft.HasJoined;
import cn.mcres.karlatemp.yop.server.Connecting;
import cn.mcres.karlatemp.yop.server.HttpHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

public class YggdrasilOfficialProxy implements HttpHandler {
    public static void startup(String path, boolean formMain) throws IOException, ExecutionException, InterruptedException {
        if (path == null || path.isEmpty()) {
            path = "./YggdrasilOfficialProxy.yml";
        }
        ConfigSetup.init(path);
        PatternContextHandler.setNoneCallback(new YggdrasilOfficialProxy());
        PatternContextHandler.register("^/sessionserver/session/minecraft/hasJoined(\\?.+|)$", new HasJoined());
        CompletableFuture<Void> v = new CompletableFuture<>();
        ThreadGroup group = new ThreadGroup("YggdrasilOfficialProxy");
        final AtomicInteger counter = new AtomicInteger();
        new ServerBootstrap()
                .channel(PipelineUtils.getServerChannel())
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<Channel>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new HttpResponseEncoder())
                                .addLast(new HttpRequestDecoder())
                                .addLast(new PatternContextHandler().setup(null, ch))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                        ctx.channel().close();
                                        cause.printStackTrace();
                                    }
                                })
                                .addLast(new HttpServerCodec());
                    }
                })
                .group(PipelineUtils.newEventLoopGroup(10, r -> new Thread(group, r, "YggdrasilOfficialProxy-Server#" + counter.getAndIncrement())))
                .bind(Metadata.serverPort)
                .addListener(future -> {
                    if (!future.isSuccess()) {
                        v.completeExceptionally(future.cause());
                    } else {
                        v.complete(null);
                    }
                });
        v.get();
        System.out.println("# Server started.");
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException, IOException {
        String p = null;
        if (args.length > 0) p = args[0];
        startup(p, true);
    }

    public static void agentmain(String opt, Instrumentation i) throws InterruptedException, ExecutionException, IOException {
        startup(opt, false);
    }

    public static void premain(String opt, Instrumentation i) throws InterruptedException, ExecutionException, IOException {
        startup(opt, false);
    }

    @Override
    public void request(ChannelHandlerContext ctx, URI uri, boolean keepAlive, HttpRequest msg) throws Exception {
        Connecting.run(ctx, msg.headers(), msg.method(), ContextHandler.getContent(msg),
                keepAlive, uri.toString(), false, true);
    }
}
