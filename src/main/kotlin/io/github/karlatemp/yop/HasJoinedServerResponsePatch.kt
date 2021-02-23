/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/02/20 16:17:21
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/HasJoinedServerResponsePatch.kt
 */

package io.github.karlatemp.yop

import com.google.gson.Gson
import com.google.gson.stream.JsonWriter
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter

data class GameProfileDTO(
        @JvmField var id: String,
        @JvmField var name: String,
        @JvmField var properties: MutableList<PropertyDTO>? = null
) {
    data class PropertyDTO(
            @JvmField var name: String,
            @JvmField var value: String,
            @JvmField var signature: String? = null,
    )
}

val adapter = Gson().getAdapter(GameProfileDTO::class.java)

fun patch(response: HttpResponse, isOfficial: Boolean): ByteArray {
    val dto = response.content.toInputStream().bufferedReader().use {
        adapter.fromJson(it)
    }
    val p = GameProfileDTO.PropertyDTO("yop-isOfficial", isOfficial.toString(), "=")
    dto.properties?.add(p) ?: run {
        dto.properties = mutableListOf(p)
    }
    return ByteArrayOutputStream().also { stream ->
        OutputStreamWriter(stream).use {
            val writer = JsonWriter(it)
            writer.isHtmlSafe = false
            writer.serializeNulls = false
            adapter.write(writer, dto)
        }
    }.toByteArray().also { data ->
        WrappedLogger.debug { "Fetched: ${String(data)}" }
    }
}
