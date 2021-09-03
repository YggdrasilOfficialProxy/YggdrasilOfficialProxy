/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/03 21:18:07
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/YopBootstrap.kt
 */

@file:JvmName("YopBootstrap")

package io.github.yggdrasilofficialproxy

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import java.io.File


fun main() {
    val configLoader = HoconConfigurationLoader.builder()
        .prettyPrinting(true)
        .emitComments(true)
        .defaultOptions { options ->
            options
                .header(
                    """
Yggdrasil Official Proxy Configuration
""".trimIndent()
                )
                .shouldCopyDefaults(true)
        }
        .file(File("yop.conf"))
        .build()
    val configNode = configLoader.load()
    val oldConf = configLoader.load()

    val yopConfiguration = configNode.get(YopConfiguration::class.java) ?: kotlin.run {
        val cf = YopConfiguration()
        configNode.set(YopConfiguration::class.java, cf)
        cf
    }

    if (configNode != oldConf) {
        configLoader.save(configNode)
    }

    val server = YopProxyServer(
        yopConfiguration,
        HttpClient(OkHttp) {
            // TODO: HttpClient Proxy
            this.expectSuccess = false
            this.engine {
                config {
                    retryOnConnectionFailure(true)
                }
            }
        }
    )
    val ygg = server.resolvedYggdrasilServers.firstOrNull { it.name != "mojang" }
        ?: error("No custom yggdrasil found.")

    runBlocking(Dispatchers.IO) {
        val rsp: HttpResponse = server.http.get(ygg.serverIndex)
        server.indexContentType = rsp.contentType()
        server.indexPageView = rsp.content.toInputStream().use { it.readBytes() }
    }
    server.startProxyServer(true)
}
