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
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

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

suspend fun patch(response: HttpResponse, isOfficial: Boolean): ByteArray {
    val dto = suspendCoroutine<GameProfileDTO> { continuation ->
        thread { response.content.toInputStream().bufferedReader().use {
            continuation.resume(adapter.fromJson(it))
    } } }
    val p = GameProfileDTO.PropertyDTO("yop_isOfficial", isOfficial.toString(), "=")
    dto.properties?.add(p) ?: run {
        dto.properties = mutableListOf(p)
    }
    return ByteArrayOutputStream().also { stream ->
        OutputStreamWriter(stream).use {
            val writer = JsonWriter(it)
            writer.isHtmlSafe = false
            writer.serializeNulls = false
            suspendCoroutine { thread { it.resume(adapter.write(writer, dto)) } }
        }
    }.toByteArray().also { data ->
        WrappedLogger.debug { "Fetched: ${String(data)}" }
    }
}
