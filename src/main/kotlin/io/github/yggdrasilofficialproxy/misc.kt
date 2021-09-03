/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/03 22:13:08
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/misc.kt
 */

package io.github.yggdrasilofficialproxy

import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

inline fun <T> T.letIf(expr: Boolean, action: (T) -> T): T {
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    return if (expr) action(this) else this
}
