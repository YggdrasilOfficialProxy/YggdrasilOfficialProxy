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
data class YopConfiguration @JvmOverloads constructor(
    @Comment("Proxy server host")
    val host : ServerHost = ServerHost(),
    @Comment("Yggdrasil servers using")
    val yggdrasilServers : List<YggdrasilServerInfo> = listOf(
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
) {

    @ConfigSerializable
    data class YggdrasilServerInfo @JvmOverloads constructor(
        @Comment("<Optional> When you use @ selector please fill it")
        val name : String = "",
        @Comment("<Required>")
        val api : String = "",
        @Comment("Proxy info")
        val proxy : ProxyInfo = ProxyInfo()
    ) {
        @ConfigSerializable
        data class ProxyInfo @JvmOverloads constructor(
            @Comment("<Required> (direct, http, socks)")
            val type : String = "direct",
            @Comment("<Optional> Please fill in the [url] when you use http proxy")
            val url : String? = null,
            @Comment("<Optional> Please fill in the [host] when you use socks proxy")
            val host : String? = null,
            @Comment("<Optional> Please fill in the [port] when you use socks proxy")
            val port : Int? = null,
            @Comment("<Optional> Please fill in the [username] when this proxy needs to be authenticated")
            val username : String? = null,
            @Comment("<Optional> Please fill in the [password] when this proxy needs to be authenticated")
            val password : String? = null
        )
    }

    @ConfigSerializable
    data class ServerHost @JvmOverloads constructor(
        val host : String = "0.0.0.0",
        val port : Int = 32217,
    )

}

