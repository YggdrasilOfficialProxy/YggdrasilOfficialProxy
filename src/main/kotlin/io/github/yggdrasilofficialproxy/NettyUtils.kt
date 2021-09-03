/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/03 20:24:31
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/NettyUtils.kt
 */

package io.github.yggdrasilofficialproxy

import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueEventLoopGroup
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.ServerSocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel

object NettyUtils {
    @JvmStatic
    val NettyServerSocketClass: Class<out ServerSocketChannel>
        get() = when {
            KQueue.isAvailable() -> KQueueServerSocketChannel::class.java
            Epoll.isAvailable() -> EpollServerSocketChannel::class.java
            else -> NioServerSocketChannel::class.java
        }

    @JvmStatic
    fun newNettyEventLoopGroup(): EventLoopGroup = when {
        KQueue.isAvailable() -> KQueueEventLoopGroup()
        Epoll.isAvailable() -> EpollEventLoopGroup()
        else -> NioEventLoopGroup()
    }
}
