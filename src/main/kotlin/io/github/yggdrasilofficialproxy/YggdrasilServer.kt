/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/03 20:06:21
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/YggdrasilServer.kt
 */

package io.github.yggdrasilofficialproxy

import io.github.yggdrasilofficialproxy.HttpUtils.newHttpClient
import io.ktor.client.*
import okhttp3.Dispatcher
import java.net.URI

data class YggdrasilServer(
    val name : String,
    val api : String,
    val hasJoined : String,
    val profilesMinecraft : String,
    val serverIndex : String,
    val client : HttpClient,
    val proxyInfo : YopConfiguration.YggdrasilServerInfo.ProxyInfo,
) {
    companion object {
        @JvmStatic
        operator fun invoke(serverInfo : YopConfiguration.YggdrasilServerInfo, dispatcher : Dispatcher?) : YggdrasilServer {
            if (serverInfo.name == "mojang")
                return mojangInstance(serverInfo, dispatcher)

            val api = serverInfo.api
            val proxyInfo = serverInfo.proxy

            return YggdrasilServer(
                name = serverInfo.name,
                api = URI.create(api).host ?: api,
                hasJoined = "$api/sessionserver/session/minecraft/hasJoined",
                profilesMinecraft = "$api/api/profiles/minecraft",
                serverIndex = api,
                client = proxyInfo.newHttpClient(dispatcher),
                proxyInfo = proxyInfo
            )
        }

        private fun mojangInstance(serverInfo : YopConfiguration.YggdrasilServerInfo, dispatcher : Dispatcher?) : YggdrasilServer {
            val api = serverInfo.api
            val notBlank = api.isNotBlank()
            val proxyInfo = serverInfo.proxy
            return YggdrasilServer(
                name = "mojang",
                api = "",
                serverIndex = "", // none
                hasJoined = "${if (notBlank) api else "https://sessionserver.mojang.com"}/session/minecraft/hasJoined",
                profilesMinecraft = "${if (notBlank) "$api/api" else "https://api.mojang.com"}/profiles/minecraft",
                client = proxyInfo.newHttpClient(dispatcher),
                proxyInfo = proxyInfo
            )
        }
    }
}
