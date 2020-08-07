import java.io.PrintStream

/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/08/07 15:15:50
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.test/TestOverride.kt
 */

fun main() {
    val oldPs = System.out
    System.setOut(object : PrintStream(oldPs) {
        override fun println(x: Any?) {
            super.println("-> $x")
        }
    })
    println("WX")
}