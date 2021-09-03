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
    val host: ServerHost = ServerHost(),
    @Comment("Yggdrasil servers using")
    val yggdrasilServers: List<String> = listOf(
        "https://skin.prinzeugen.net/api/yggdrasil",
        "mojang",
    ),
    @Comment("Mojang CDN Yggdrasil server")
    val mojangServerCdn: String = "",
) {

    @ConfigSerializable
    data class ServerHost @JvmOverloads constructor(
        val host: String = "0.0.0.0",
        val port: Int = 32217,
    )

}

