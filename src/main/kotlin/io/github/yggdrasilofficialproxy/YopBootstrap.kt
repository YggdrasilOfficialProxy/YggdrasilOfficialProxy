/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/03 21:18:07
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/YopBootstrap.kt
 */

@file:JvmName("YopBootstrap")

package io.github.yggdrasilofficialproxy

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import java.io.File

const val BOOTSTRAP_MESSAGE = """
###########################################################################################
# Yggdrasil Official Proxy v3
###########################################################################################
# Before using this application, you should know:
#
#  1. YOP just for support player login with mojang and custom yggdrasil account servers.
#  2. If you encounter data errors (via uuid duplicated, username duplicated), don't 
#     contact YOP and call your yggdrasil server provider.
#  3. The core application (YOP) don't have any ways to avoid data confusion
#  4. Setup 2FA verify for admin accounts in server to avoiding account login
#     security errors.
#
###########################################################################################
# Using this application means you were approved
#
#  1. At your own risk, possible UUID duplication issues, possible username duplication
#     issues, or more.
#  2. Low technical support if not running in stand-alone server mode.
#  3. Be friendly to everyone.
#
###########################################################################################
"""

fun main(args : Array<String>) {
    bootstrap(args)
}

fun bootstrap(args : Array<String>) : Int {
    println(BOOTSTRAP_MESSAGE.trim())

    val isDaemon = args.getOrNull(0) == "daemon"

    if (isDaemon) {
        Slf4jStdoutLogger.warn { "Running in daemon mode may cause lower stability." }
    }

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

    val yopConfiguration = configNode.get(YopConfiguration::class.java, YopConfiguration())

    if (configNode != oldConf) {
        configLoader.save(configNode)
    }
    Slf4jStdoutLogger.level = yopConfiguration.loggerLevel

    val server = YopProxyServer(
        yopConfiguration,
        isDaemon,
    )
    val ygg = server.resolvedYggdrasilServers.firstOrNull { it.name != "mojang" }
        ?: error("No custom yggdrasil found.")

    runBlocking(Dispatchers.IO) {
        Slf4jStdoutLogger.debug { "Fetching index: ${ygg.serverIndex}" }
        val rsp : HttpResponse = ygg.client.get(ygg.serverIndex)
        server.indexContentType = rsp.contentType()
        server.indexPageView = rsp.content.toInputStream().use { it.readBytes() }
    }
    server.startProxyServer(!isDaemon)

    return yopConfiguration.host.port
}
