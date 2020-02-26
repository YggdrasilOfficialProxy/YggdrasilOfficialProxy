/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/02/27 03:21:23
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy/HasJoined.java
 */

package cn.mcres.karlatemp.yop.server.sessionserver.session.minecraft;


import cn.mcres.karlatemp.yop.Metadata;
import cn.mcres.karlatemp.yop.server.Connecting;
import cn.mcres.karlatemp.yop.server.HttpHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.*;

import java.net.URI;

public class HasJoined implements HttpHandler {

    @Override
    public void request(ChannelHandlerContext ctx, URI uri,
                        boolean keepAlive, HttpRequest msg) throws Exception {
        if (!Connecting.run(ctx, msg.headers(), HttpMethod.GET, null,
                keepAlive, uri.toString(), Metadata.LoginOfficialFirst, false, x -> x == 200)) {
            Connecting.run(ctx, msg.headers(), HttpMethod.GET, null,
                    keepAlive, uri.toString(), !Metadata.LoginOfficialFirst, true, x -> x == 200);
        }
    }
}
