/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/03 20:06:21
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/YggdrasilServer.kt
 */

package io.github.yggdrasilofficialproxy

import java.net.URI

data class YggdrasilServer(
    val name: String,
    val hasJoined: String,
    val profilesMinecraft: String,
    val serverIndex: String,
) {
    companion object {
        @JvmStatic
        operator fun invoke(server: String): YggdrasilServer {
            if (server == "mojang") return MOJANG
            return YggdrasilServer(
                name = URI.create(server).host ?: server,
                hasJoined = "$server/sessionserver/session/minecraft/hasJoined",
                profilesMinecraft = "$server/api/profiles/minecraft",
                serverIndex = server,
            )
        }

        @JvmField
        val MOJANG = YggdrasilServer(
            name = "mojang",
            serverIndex = "", // none
            hasJoined = buildString {
                append("https://sessionserver.")
                append("moja")
                append("ng.com")
                append("/session/minecraft/hasJoined")
            },
            profilesMinecraft = buildString {
                append("https://api.")
                append("moja")
                append("ng.com")
                append("/profiles/minecraft")
            },
        )
    }
}
