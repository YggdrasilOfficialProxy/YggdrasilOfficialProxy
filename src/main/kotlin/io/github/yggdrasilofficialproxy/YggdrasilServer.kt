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
import io.ktor.client.features.json.*
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
            val api = serverInfo.api
            val proxyInfo = serverInfo.proxy
            if (serverInfo.name == "mojang") return mojangInstance(serverInfo, dispatcher)
            return YggdrasilServer(
                name = serverInfo.name,
                api = URI.create(api).host ?: api,
                hasJoined = "$api/sessionserver/session/minecraft/hasJoined",
                profilesMinecraft = "$api/api/profiles/minecraft",
                serverIndex = api,
                client = HttpClient(OkHttp)
                {
                    expectSuccess = false

                    when (proxyInfo.type) {
                        "http" -> ProxyBuilder.http(proxyInfo.url ?: error("Proxy in [$api] is missing url"))
                        "socks" -> ProxyBuilder.socks(proxyInfo.host
                            ?: error("Proxy in [$api] is missing host"), proxyInfo.port
                            ?: error("Proxy in [$api] is missing port"))
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

        private fun mojangInstance(serverInfo : YopConfiguration.YggdrasilServerInfo, dispatcher : Dispatcher?) : YggdrasilServer
        {
            val api = serverInfo.api
            val notBlank = api.isNotBlank()
            val proxyInfo = serverInfo.proxy
            return YggdrasilServer(
                name = "mojang",
                api = "",
                serverIndex = "", // none
                hasJoined = "${if (notBlank) api else "https://sessionserver.mojang.com"}/session/minecraft/hasJoined",
                profilesMinecraft = "${if (notBlank) "$api/api" else "https://api.mojang.com"}/profiles/minecraft",
                client = HttpClient(OkHttp)
                {
                    install(JsonFeature)
                    {
                        serializer = GsonSerializer()
                    }
                    expectSuccess = false
                    when (proxyInfo.type) {
                        "http" -> ProxyBuilder.http(proxyInfo.url ?: error("Proxy in [mojang] is missing url"))
                        "socks" -> ProxyBuilder.socks(proxyInfo.host
                            ?: error("Proxy in [mojang] is missing host"), proxyInfo.port
                            ?: error("Proxy in [mojang] is missing port"))
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
}
