/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/08/07 16:11:01
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/atomic.kt
 */

package io.github.karlatemp.yop

import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty

operator fun <V> AtomicReference<V>.setValue(
        ignored: Any?, property: KProperty<*>, value: V) {
    set(value)
}

operator fun <V> AtomicReference<V>.getValue(
        vx: Any?, property: KProperty<*>): V {
    return get()
}
