/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 03:21:23
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/ProxyTest.java
 */

package cn.mcres.karlatemp.yop.server.bilibili;

import cn.mcres.karlatemp.yop.server.Connecting;
import cn.mcres.karlatemp.yop.server.HttpHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpRequest;

import java.net.Proxy;
import java.net.URI;
import java.net.URL;

public class ProxyTest implements HttpHandler {
    @Override
    public void request(ChannelHandlerContext ctx, URI uri, boolean keepAlive, HttpRequest msg) throws Exception {
        // /bilibili/
        Connecting.run(ctx,
                msg.headers(),
                msg.method(),
                ((DefaultFullHttpRequest) msg).content(),
                keepAlive,
                new URL("https://www.bilibili.com" + uri.toASCIIString().substring(9)), Proxy.NO_PROXY, true);
    }
}
