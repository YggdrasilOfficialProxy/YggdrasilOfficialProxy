/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 03:21:23
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/YggdrasilOfficialProxy.java
 */

package cn.mcres.karlatemp.yop;

import cn.mcres.karlatemp.yop.server.ContextHandler;
import cn.mcres.karlatemp.yop.server.PatternContextHandler;
import cn.mcres.karlatemp.yop.server.sessionserver.session.minecraft.HasJoined;
import cn.mcres.karlatemp.yop.server.Connecting;
import cn.mcres.karlatemp.yop.server.HttpHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.handler.codec.http.*;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.MissingResourceException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;

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
                    protected void initChannel(Channel ch) {
                        ch.pipeline()
                                .addLast(new HttpResponseEncoder())
                                .addLast(new HttpRequestDecoder())
                                .addLast(new PatternContextHandler().setup(null, ch))
                                .addLast(new ChannelInboundHandlerAdapter() {
                                    @Override
                                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                        ctx.channel().close();
                                        cause.printStackTrace();
                                    }
                                })
                                .addLast(new HttpServerCodec());
                    }
                })
                .group(PipelineUtils.newEventLoopGroup(10, r -> {
                    Thread t = new Thread(group, r, "YggdrasilOfficialProxy-Server#" + counter.getAndIncrement());
                    t.setDaemon(!formMain);
                    return t;
                }))
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

    public static void agentmain(String opt, Instrumentation i) throws Exception {
        startup(opt, false);
        launch("agentmain", "Agent-Class", i);
    }

    private static void launch(String name, String mani, Instrumentation i) throws Exception {
        String pt = ConfigSetup.opts.string("injector.path");
        System.out.println("# Injector path: " + pt);
        String ag = ConfigSetup.opts.string("injector.arg").replace("{proxy}", "http://localhost:" + Metadata.serverPort);
        System.out.println("# Injector argument: " + ag);
        if (pt != null) {
            File f = new File(pt);
            JarFile file = new JarFile(f);
            final String value = file.getManifest().getMainAttributes().getValue(mani);
            if (value == null) throw new MissingResourceException("No " + mani + " in " + pt, "", mani);
            URLClassLoader loader = new URLClassLoader(new URL[]{f.toURI().toURL()});
            final Class<?> target = loader.loadClass(value);
            Method met = null;
            try {
                met = target.getMethod(name, String.class, Instrumentation.class);
            } catch (NoSuchMethodException ignore) {
            }
            if (met == null) met = target.getMethod(name, String.class);
            if (met.getParameterCount() == 1) {
                met.invoke(null, ag);
            } else {
                met.invoke(null, ag, i);
            }
        }
    }

    public static void premain(String opt, Instrumentation i) throws Exception {
        startup(opt, false);
        launch("premain", "Premain-Class", i);
    }

    @Override
    public void request(ChannelHandlerContext ctx, URI uri, boolean keepAlive, HttpRequest msg) throws Exception {
        Connecting.run(ctx, msg.headers(), msg.method(), ContextHandler.getContent(msg),
                keepAlive, uri.toString(), false, true);
    }
}
