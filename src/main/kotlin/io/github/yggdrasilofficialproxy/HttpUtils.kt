/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/06 24:01:59
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/HttpUtils.kt
 */

package io.github.yggdrasilofficialproxy

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.features.json.*
import okhttp3.Dispatcher

object HttpUtils {
    fun YopConfiguration.YggdrasilServerInfo.ProxyInfo.newHttpClient(
        dispatcher : Dispatcher? = null,
    ) : HttpClient {
        val proxyInfo = this
        return HttpClient(OkHttp) {
            install(JsonFeature) {
                serializer = GsonSerializer()
            }
            expectSuccess = false
            when (proxyInfo.type) {
                "http" -> ProxyBuilder.http(
                    proxyInfo.url ?: error("Proxy in [mojang] is missing url")
                )
                "socks" -> ProxyBuilder.socks(
                    proxyInfo.host ?: error("Proxy in [mojang] is missing host"),
                    proxyInfo.port ?: error("Proxy in [mojang] is missing port")
                )
                "direct" -> null
                else -> null
            }.also { proxyInstance ->
                engine {
                    proxy = proxyInstance
                    config {
                        retryOnConnectionFailure(true)
                        dispatcher?.also { dispatcher -> dispatcher(dispatcher) }
                    }
                }
            }
        }
    }
}