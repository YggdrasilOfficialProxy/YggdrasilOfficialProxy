/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/03 20:20:36
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/YggdrasilOfficialProxy.kt
 */

package io.github.yggdrasilofficialproxy

import com.google.gson.stream.JsonWriter
import io.github.yggdrasilofficialproxy.NettyUtils.NettyServerSocketClass
import io.github.yggdrasilofficialproxy.NettyUtils.newNettyEventLoopGroup
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runInterruptible
import java.io.ByteArrayOutputStream

typealias KtorHttpRequest = PipelineContext<Unit, ApplicationCall>


class YopProxyServer(
    val config: YopConfiguration,
    val http: HttpClient,
) {
    var indexContentType: ContentType? = null
    lateinit var indexPageView: ByteArray // yggdrasil index page view

    val resolvedYggdrasilServers = kotlin.run {
        var rsp = config.yggdrasilServers.map { YggdrasilServer(it) }
        if (config.mojangServerCdn.isNotBlank()) {
            rsp = rsp.map { server ->
                server.letIf(server.name == "mojang") {
                    YggdrasilServer(config.mojangServerCdn).copy(name = "mojang")
                }
            }
        }
        if (rsp.none { it.name == "mojang" }) {
            Slf4jStdoutLogger.warn("No mojang yggdrasil remote target. Minecraft Online Auth will be disabled")
        }
        rsp.forEach { ys ->
            Slf4jStdoutLogger.debug { "Server: ${ys.name} ${ys.serverIndex}" }
        }
        rsp
    }

    private fun logException(exception: Throwable) {
        Slf4jStdoutLogger.error(null, exception)
    }

    private suspend fun patchHasJoinedResponse(
        response: HttpResponse,
        server: YggdrasilServer,
    ): ByteArray {
        val dto = runInterruptible(Dispatchers.IO) {
            response.content.toInputStream().bufferedReader().use {
                MojangGameProfileDTO.ADAPTER.fromJson(it)
            }
        }

        fun putProperty(key: String, value: Any) {
            val prop = MojangGameProfileDTO.PropertyDTO(
                name = key,
                value = value.toString(),
                signature = "=",
            )
            dto.properties?.add(prop) ?: kotlin.run {
                dto.properties = mutableListOf(prop)
            }
        }

        putProperty("yop_isOfficial", server.name == "mojang")
        putProperty("yop_server", server.name)

        return runInterruptible(Dispatchers.IO) {
            ByteArrayOutputStream().also { baos ->
                JsonWriter(baos.writer()).use { jwriter ->
                    jwriter.isHtmlSafe = false
                    jwriter.serializeNulls = false
                    jwriter.setIndent("  ")
                    MojangGameProfileDTO.ADAPTER.write(jwriter, dto)
                }
            }.toByteArray()
        }
    }

    private suspend fun processHasJoined(request: KtorHttpRequest, scope: CoroutineScope) {
        Slf4jStdoutLogger.debug { "Processing ${request.call.request.origin.uri}" }
        if (resolvedYggdrasilServers.isEmpty()) {
            throw IllegalStateException("No available yggdrasil servers.")
        }
        val requests = resolvedYggdrasilServers.map { server ->
            scope.async(Dispatchers.IO) {
                val uri = server.hasJoined + request.call.request.origin.uri.substringAfterAndInclude('?', "")
                Slf4jStdoutLogger.debug { "Requesting $uri" }
                val resp: HttpResponse = try {
                    http.get(uri)
                } catch (e: Throwable) {
                    if (Slf4jStdoutLogger.isWarnEnabled) {
                        Slf4jStdoutLogger.warn("Error in requesting $uri", e)
                    }
                    return@async null
                }
                resp to server
            }
        }.mapNotNull { it.await() }.filter { (it, _) -> it.status.value == 200 }

        Slf4jStdoutLogger.debug {
            "Fetched available response: " + requests.joinToString { it.second.name }
        }

        if (requests.size != 1) {
            request.call.respond(HttpStatusCode.NoContent)
            return
        }
        val (httpResponse, yggdrasilServer) = requests[0]
        val rsp = patchHasJoinedResponse(httpResponse, yggdrasilServer)

        Slf4jStdoutLogger.debug {
            "hasJoinServer response: " + String(rsp)
        }

        request.call.respond(object : OutgoingContent.ByteArrayContent() {
            override fun bytes(): ByteArray = rsp

            override val status: HttpStatusCode
                get() = httpResponse.status
            override val contentType: ContentType
                get() = ContentType("application", "json; charset=utf8")
        })
    }

    private fun setupYopEnvironment() = applicationEngineEnvironment {
        this.log = Slf4jStdoutLogger

        val yopConf = this@YopProxyServer.config
        connector {
            this.host = yopConf.host.host
            this.port = yopConf.host.port
        }
        module {
            install(DefaultHeaders)

            intercept(ApplicationCallPipeline.Call) {
                Slf4jStdoutLogger.debug {
                    "[" + call.request.httpMethod.value + "] " + call.request.uri
                }
                proceed()
            }

            routing {
                @ContextDsl
                fun getCatching(path: String, body: PipelineInterceptor<Unit, ApplicationCall>): Route = get(path) {
                    runCatching {
                        body(it)
                    }.onFailure { exception ->
                        logException(exception)
                        kotlin.runCatching {
                            call.respond(HttpStatusCode.InternalServerError)
                        }
                    }
                }

                getCatching("/") {
                    call.respond(object : OutgoingContent.ByteArrayContent() {
                        override fun bytes(): ByteArray = indexPageView
                        override val contentType: ContentType? get() = indexContentType
                    })
                }

                getCatching("/sessionserver/session/minecraft/hasJoined") {
                    processHasJoined(this, this)
                }

                // TODO: /api/profiles/minecraft

                route("/*") {
                    handle {
                        // TODO: Request Forward
                    }
                }
            }
        }
    }

    fun startProxyServer(wait: Boolean) {
        val server = embeddedServer(Netty, environment = setupYopEnvironment()) {
            this.shareWorkGroup = true
            this.configureBootstrap = {
                this.group(
                    newNettyEventLoopGroup(),
                    newNettyEventLoopGroup(),
                ).channel(NettyServerSocketClass)
            }
        }
        server.start(wait)
    }
}
