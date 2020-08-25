/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/08/26 03:36:19
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/OpenLibrary.kt
 */

package io.github.karlatemp.yop

import java.lang.instrument.Instrumentation

object OpenLibrary {
    internal var ins: Instrumentation? = null

    @JvmStatic
    fun agentmain(opt: String, ins: Instrumentation) {
        this.ins = ins
    }
}