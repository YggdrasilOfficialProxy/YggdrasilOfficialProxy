/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 03:21:23
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/HttpHandler.java
 */

package cn.mcres.karlatemp.yop.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;

import java.net.URI;

public interface HttpHandler {
    void request(ChannelHandlerContext ctx, URI uri,
                 boolean keepAlive, HttpRequest msg) throws Exception;

}
