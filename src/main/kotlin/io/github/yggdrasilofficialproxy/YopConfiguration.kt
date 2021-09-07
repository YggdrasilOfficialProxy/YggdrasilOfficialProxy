/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/03 20:15:03
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/YopConfiguration.kt
 */

package io.github.yggdrasilofficialproxy

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment

@ConfigSerializable
data class YopConfiguration(
    @field:Comment("Proxy server host")
    @JvmField val host : ServerHost = ServerHost(),

    @field:Comment("Yggdrasil servers using")
    @JvmField val yggdrasilServers : List<YggdrasilServerInfo> = listOf(
        YggdrasilServerInfo(
            name = "mojang",
            proxy = YggdrasilServerInfo.ProxyInfo(
                type = "direct",
                url = "http://localhost:8888"
            )
        ),
        YggdrasilServerInfo(
            name = "prinzeugen",
            api = "https://skin.prinzeugen.net/api/yggdrasil",
            proxy = YggdrasilServerInfo.ProxyInfo(
                type = "direct",
                host = "localhost",
                port = 1080,
                username = "username",
                password = "password"
            )
        )
    ),

    @field:Comment("Logger level, options: NONE, ERROR, WARN, INFO, DEBUG, TRACE, ALL")
    @JvmField val loggerLevel : LoggerLevel = LoggerLevel.INFO,
) {

    @ConfigSerializable
    data class YggdrasilServerInfo(
        @field:Comment("<Optional> When you use @ selector please fill it")
        @JvmField val name : String = "",

        @field:Comment("<Required>")
        @JvmField val api : String = "",

        @field:Comment("Proxy info")
        @JvmField val proxy : ProxyInfo = ProxyInfo()
    ) {
        @ConfigSerializable
        data class ProxyInfo(
            @field:Comment("<Required> (direct, http, socks)")
            @JvmField val type : String = "direct",

            @field:Comment("<Optional> Please fill in the [url] when you use http proxy")
            @JvmField val url : String? = null,

            @field:Comment("<Optional> Please fill in the [host] when you use socks proxy")
            @JvmField val host : String? = null,

            @field:Comment("<Optional> Please fill in the [port] when you use socks proxy")
            @JvmField val port : Int? = null,

            @field:Comment("<Optional> Please fill in the [username] when this proxy needs to be authenticated")
            @JvmField val username : String? = null,

            @field:Comment("<Optional> Please fill in the [password] when this proxy needs to be authenticated")
            @JvmField val password : String? = null,
        )
    }

    @ConfigSerializable
    data class ServerHost(
        @JvmField val host : String = "0.0.0.0",
        @JvmField val port : Int = 32217,
    )

    enum class LoggerLevel {
        NONE,
        ERROR,
        WARN,
        INFO,
        DEBUG,
        TRACE,
        ALL,
        ;

        val echoType : String by lazy {
            val maxWidth = values().maxOf { it.name.length }
            name + " ".repeat(maxWidth - name.length)
        }
    }
}

