/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/03 20:42:58
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/string.kt
 */

package io.github.yggdrasilofficialproxy

fun String.substringAfterAndInclude(delimiter: Char, missingDelimiterValue: String = this):String {
    val indexOf = this.indexOf(delimiter)
    if (indexOf == -1) return missingDelimiterValue
    return this.substring(indexOf)
}
