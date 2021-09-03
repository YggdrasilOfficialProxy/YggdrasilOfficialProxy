/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/04 24:20:53
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/ThreadUtils.kt
 */

package io.github.yggdrasilofficialproxy

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

object ThreadUtils {
    fun newThreadFactory(name: String, isDaemon: Boolean): ThreadFactory = object : ThreadFactory {
        val counter = AtomicInteger()
        override fun newThread(r: Runnable): Thread {
            return Thread(
                r, "$name #${counter.getAndIncrement()}"
            ).also { it.isDaemon = isDaemon }
        }
    }
}