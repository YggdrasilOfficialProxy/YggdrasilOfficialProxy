/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/03 20:06:21
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/YggdrasilServer.kt
 */

package io.github.yggdrasilofficialproxy

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import okhttp3.Dispatcher
import java.net.URI

data class YggdrasilServer(
    val api: String,
    val hasJoined: String,
    val profilesMinecraft: String,
    val serverIndex: String,
    val client : HttpClient,
    val proxyInfo : YopConfiguration.YggdrasilServerInfo.ProxyInfo,
) {
    companion object {
        @JvmStatic
        operator fun invoke(serverInfo: YopConfiguration.YggdrasilServerInfo, dispatcher : Dispatcher?): YggdrasilServer {
            val api = serverInfo.api
            val proxyInfo = serverInfo.proxy
            if (api == "mojang") return mojangInstance(proxyInfo, dispatcher)
            return YggdrasilServer(
                api = URI.create(api).host ?: api,
                hasJoined = "$api/sessionserver/session/minecraft/hasJoined",
                profilesMinecraft = "$api/api/profiles/minecraft",
                serverIndex = api,
                client = HttpClient(OkHttp)
                {
                    expectSuccess = false

                    when (proxyInfo.type)
                    {
                        "http" -> ProxyBuilder.http(proxyInfo.url ?: error("Proxy in [$api] is missing url"))
                        "socks" -> ProxyBuilder.socks(proxyInfo.host ?: error("Proxy in [$api] is missing host"), proxyInfo.port ?: error("Proxy in [$api] is missing port"))
                        "direct" -> null
                        else -> null
                    }.also {
                        engine {
                            proxy = it
                            config {
                                retryOnConnectionFailure(true)
                                dispatcher?.also { dispatcher -> dispatcher(dispatcher) }
                            }
                        }
                    }
                },
                proxyInfo = proxyInfo
            )
        }

        fun mojangInstance(proxyInfo : YopConfiguration.YggdrasilServerInfo.ProxyInfo, dispatcher : Dispatcher?) = YggdrasilServer(
            api = "mojang",
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
            client = HttpClient(OkHttp)
            {
                expectSuccess = false
                when (proxyInfo.type)
                {
                    "http" -> ProxyBuilder.http(proxyInfo.url ?: error("Proxy in [mojang] is missing url"))
                    "socks" -> ProxyBuilder.socks(proxyInfo.host ?: error("Proxy in [mojang] is missing host"), proxyInfo.port ?: error("Proxy in [mojang] is missing port"))
                    "direct" -> null
                    else -> null
                }.also {
                    engine {
                        proxy = it
                        config {
                            retryOnConnectionFailure(true)
                            dispatcher?.also { dispatcher -> dispatcher(dispatcher) }
                        }
                    }
                }
            },
            proxyInfo = proxyInfo
        )
    }
}
