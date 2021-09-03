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
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import okhttp3.Dispatcher
import java.io.ByteArrayOutputStream
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.URL
import java.util.*
import java.util.concurrent.Executors
import kotlin.text.toCharArray

typealias KtorHttpRequest = PipelineContext<Unit, ApplicationCall>


class YopProxyServer(
    val config: YopConfiguration,
    val isDaemon: Boolean,
) {
    var indexContentType: ContentType? = null
    lateinit var indexPageView: ByteArray // yggdrasil index page view

    val resolvedYggdrasilServers = kotlin.run {
        val dispatcher = if (isDaemon) {
            Dispatcher(
                Executors.newScheduledThreadPool(
                    5,
                    ThreadUtils.newThreadFactory("Ktor Dispatcher", true)
                )
            )
        } else null
        var rsp = config.yggdrasilServers.map { YggdrasilServer(it, dispatcher) }
        if (config.mojangServerCdn.isNotBlank()) {
            rsp = rsp.map { server ->
                server.letIf(server.api == "mojang") {
                    it.copy(api = config.mojangServerCdn)
                }
            }
        }
        if (rsp.none { it.api == "mojang" }) {
            Slf4jStdoutLogger.warn("No mojang yggdrasil remote target. Minecraft Online Auth will be disabled")
        }
        rsp.forEach { ys ->
            Slf4jStdoutLogger.debug { "Server: ${ys.api} ${ys.serverIndex}" }
        }
        Authenticator.setDefault(object : Authenticator()
        {
            override fun getPasswordAuthentication() : PasswordAuthentication {
                if (requestorType == RequestorType.PROXY) {
                    val protocol = requestingProtocol.lowercase(Locale.getDefault())
                    Slf4jStdoutLogger.debug { "Proxy protocol: $protocol" }
                    val server = if (protocol.contains("http")) {
                        rsp.firstOrNull {
                            val proxyInfo = it.proxyInfo
                            proxyInfo.type == "http" && URL(proxyInfo.url) == requestingURL && proxyInfo.username != null && proxyInfo.password != null
                        }
                    } else if (protocol.contains("socks")) {
                        rsp.firstOrNull {
                            val proxyInfo = it.proxyInfo
                            proxyInfo.type == "socks" && proxyInfo.host == requestingHost && proxyInfo.port == requestingPort && proxyInfo.username != null && proxyInfo.password != null
                        }
                    } else null
                    server?.also { return PasswordAuthentication(it.proxyInfo.username, it.proxyInfo.password?.toCharArray()) }
                }
                return super.getPasswordAuthentication()
            }
        })
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

        putProperty("yop_isOfficial", server.api == "mojang")
        putProperty("yop_server", server.api)

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
                    server.client.get(uri)
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
            "Fetched available response: " + requests.joinToString { it.second.api }
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
        if (isDaemon) {
            this.parentCoroutineContext = Executors.newScheduledThreadPool(
                5, ThreadUtils.newThreadFactory("Yggdrasil Ktor Server", true)
            ).asCoroutineDispatcher()
        }

        val yopConf = this@YopProxyServer.config
        connector {
            this.host = yopConf.host.host
            this.port = yopConf.host.port
        }
        module {
            install(DefaultHeaders)
            install(StatusPages)
            {
                status(HttpStatusCode.NotFound) {
                    Slf4jStdoutLogger.warn { "====================================================================================" }
                    Slf4jStdoutLogger.warn { """
                        Oh, there seems to be a link that was incorrectly intercepted by authlib-injector,
                        This may cause some functions in the game or plugin to be unavailable, including but not limited to skin/head image,
                        Sorry, we have no way to help you redirect the link because we cannot get the host that authlib-injector has already destroyed.
                        But the good news is that the problem has a solution. This problem has been fixed by my feature in authlib-injector. 
                        PLEASE DO NOT OPEN ISSUE OR PR UNDER THIS PROJECT.
                        You can refer to these two link https://github.com/yushijinhun/authlib-injector/pull/63, https://github.com/YggdrasilOfficialProxy/YggdrasilOfficialProxy/pull/25. Find the link [${context.request.uri}] For the package name of the class and add it to the -Dauthlibinjector.ignoredPackages startup parameter of authlib-injector. 
                    """.trimIndent() }
                    Slf4jStdoutLogger.warn { "====================================================================================" }
                }
            }


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
            }
        }
    }

    fun startProxyServer(wait: Boolean) {
        val server = embeddedServer(Netty, environment = setupYopEnvironment()) {
            this.shareWorkGroup = true
            this.configureBootstrap = {
                this.group(
                    newNettyEventLoopGroup(isDaemon),
                    newNettyEventLoopGroup(isDaemon),
                ).channel(NettyServerSocketClass)
            }
        }
        server.start(wait)
    }
}
