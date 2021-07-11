/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/08/07 12:40:34
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/YggdrasilOfficialProxy.kt
 */

package io.github.karlatemp.yop

import com.google.gson.JsonArray
import com.google.gson.JsonParser
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.engine.okhttp.*
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
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.pipeline.*
import io.ktor.utils.io.*
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueEventLoopGroup
import io.netty.channel.kqueue.KQueueServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.ServerSocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import java.io.File
import java.lang.instrument.Instrumentation
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.jar.JarFile
import kotlin.reflect.KClass
import kotlin.system.exitProcess
import kotlin.text.toCharArray

object YggdrasilOfficialProxy {
    lateinit var yggdrasilClient: HttpClient
    lateinit var officialClient: HttpClient
    lateinit var msaPEClient: HttpClient

    val pe_join by lazy {
        buildString { // Skip AuthLib Injector
            if (CDN_enable) {
                append(CDN_origin_link)
                append("/sessionserver")
            } else {
                append("https://sessionserver.")
                append("mojang.com")
            }
            append("/session/minecraft/join")
        }
    }
    val hasJoin by lazy {
        buildString { // Skip AuthLib Injector
            if (CDN_enable) {
                append(CDN_origin_link)
                append("/sessionserver")
            } else {
                append("https://sessionserver.")
                append("mojang.com")
            }
            append("/session/minecraft/hasJoined")
        }
    }
    val login_with_xbox by lazy {
        buildString { // Skip AuthLib Injector
            if (CDN_enable) {
                append(CDN_origin_link)
                append("/minecraftservices")
            } else {
                append("https://api.minecraftservices.com")
            }
            append("/authentication/login_with_xbox")
        }
    }
    val profilesMinecraft by lazy {
        buildString { // Skip AuthLib Injector\
            if (CDN_enable) {
                append(CDN_origin_link)
                append("/api")
            } else {
                append("https://api.")
                append("mojang.com")
            }
            append("/profiles/minecraft")
        }
    }
    val pe_profile by lazy {
        buildString { // Skip AuthLib Injector
            if (CDN_enable) {
                append(CDN_origin_link)
                append("/minecraftservices")
            } else {
                append("https://api.minecraftservices.com")
            }
            append("/minecraft/profile")
        }
    }
    val pe_mojang_authserver_refresh by lazy {
        buildString { // Skip AuthLib Injector
            if (CDN_enable) {
                append(CDN_origin_link)
                append("/authserver")
            } else {
                append("https://authserver.mojang.com")
            }
            append("/refresh")
        }
    }
    val pe_mojang_authserver_authenticate by lazy {
        buildString { // Skip AuthLib Injector
            if (CDN_enable) {
                append(CDN_origin_link)
                append("/authserver")
            } else {
                append("https://authserver.mojang.com")
            }
            append("/authenticate")
        }
    }


    val output = System.out

    var baseAPI = "https://skin.prinzeugen.net/api/yggdrasil"
    var officialFirst = false
    var host_C = "0.0.0.0"
    var port_C = 32217
    var authlib by AtomicReference<String?>()
    var CDN_enable = false
    var CDN_origin_link = ""

    var daemon = false
    val threadFactory by lazy {
        val yop = ThreadGroup("Yggdrasl Official Proxy Server")
        val counter = AtomicInteger()
        ThreadFactory { task ->
            Thread(yop, task, "YOP Thread #" + counter.getAndIncrement()).also {
                it.isDaemon = daemon
            }
        }
    }
    val service by lazy { Executors.newScheduledThreadPool(3, threadFactory) }
    val dispatcher by lazy {
        service.asCoroutineDispatcher()
    }


    @OptIn(KtorExperimentalAPI::class)
    fun reloadConfiguration() {
        val file = File("YggdrasilOfficialProxy.conf")
        val loader = HoconConfigurationLoader.builder()
                .file(file)
                .defaultOptions(
                        ConfigurationOptions.defaults()
                                .shouldCopyDefaults(true)
                )
                .build()
        val conf = kotlin.runCatching {
            if (file.isFile)
                loader.load()
            else null
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
        val edited = conf != null && conf.node("edited").boolean
        if (!file.isFile || conf == null || !edited) {
            output.println("# YggdrasilOfficialProxy v2.0.0")
            output.println("# #############################")
            output.println("# Oops.")
            output.println(when {
                conf == null && file.isFile -> "# Your Configuration of YggdrasilOfficialProxy looks like broken. We will fix it."
                !edited -> {
                    output.println(
                            "# You look like started YggdrasilOfficialProxy before"
                    )
                    "# But you forget edit our configuration."
                }
                else -> "# You look like you started YggdrasilOfficialProxy for the first time."
            })
            output.println("#")
            output.println("# Before System starting.")
            output.println("# You should edit our configuration first!")
            if (file.isFile && conf == null) {
                file.renameTo(File("YggdrasilOfficialProxy-" + System.currentTimeMillis() + ".conf.bak"))
            }
            if (conf == null)
                loader.regenFile()
            output.println("# #############################")
            Thread.sleep(5000)
            exitProcess(-4849)
        }
        fun ConfigurationNode.parseProxy(): ProxyConfig? {
            val authUsername = node("username").string
            val authPassword = node("password").string
            if (!authUsername.isNullOrBlank() && !authPassword.isNullOrBlank()) {
                Authenticator.setDefault(
                        object : Authenticator() {
                            override fun getPasswordAuthentication() = PasswordAuthentication(authUsername, authPassword.toCharArray())
                        }
                )
            }
            return when (node("type").getString("direct")) {
                "http" -> {
                    ProxyBuilder.http(Url(node("url").string ?: error("Missing url")))
                }
                "socks" -> {
                    ProxyBuilder.socks(node("host").string ?: error("Missing host"), node("port").int)
                }
                else -> null
            }
        }

        fun openClient(type: String): HttpClient {
            val p = conf.node("proxy", type).parseProxy()
            WrappedLogger.debug {
                "Proxy: $type, $p"
            }
            return HttpClient(OkHttp) {
                expectSuccess = false
                engine {
                    proxy = p
                    config {
                        retryOnConnectionFailure(true)
                    }
                }
            }
        }
        yggdrasilClient = openClient("yggdrasil")
        officialClient = openClient("official")
        msaPEClient = openClient("msape")
        this.officialFirst = conf.node("official-first").boolean
        this.host_C = conf.node("server", "host").getString("0.0.0.0")
        this.port_C = conf.node("server", "port").getInt(port_C)
        authlib = conf.node("authlib-injector").string
        baseAPI = conf.node("api").getString("https://skin.prinzeugen.net/api/yggdrasil")
                .let {
                    if (it.endsWith("/"))
                        it.substring(0, it.length - 1)
                    else it
                }
        conf.node("CDN").apply {
            CDN_enable = node("enable").boolean
            CDN_origin_link = node("origin").getString("")
            if (!(CDN_origin_link.startsWith("http://") || CDN_origin_link.startsWith("https://"))) {
                CDN_origin_link = "http://$CDN_origin_link"
            }
        }

        if (conf != loader.load()) {
            loader.save(conf)
        }
    }

    private fun HoconConfigurationLoader.regenFile() {
        fun CommentedConfigurationNode.setCV(comment: String, value: Any?) {
            comment(comment)
            set(value)
        }
        save(createNode().apply {
            node("edited").setCV(
                    "IMPORTANT: Set this value to `true`!",
                    false
            )
            node("api").setCV(
                    "The yggdrasil api root",
                    "https://skin.prinzeugen.net/api/yggdrasil"
            )
            node("server").setCV(
                    "The server binding actions.",
                    createNode().apply {
                        node("host").set("0.0.0.0")
                        node("port").set(32217)
                    }
            )
            node("official-first").setCV(
                    "When access multiple apis. Use official api first.",
                    false
            )
            node("authlib-injector").setCV(
                    "The location of YggdrasilInjector\n" +
                            "@see https://github.com/yushijinhun/authlib-injector/",
                    "./authlib-injector-XXXX.jar"
            )
            node("proxy").setCV(
                    "Proxy settings",
                    createNode().apply {
                        node("official").setCV(
                                "The proxy of official connecting",
                                createNode().apply {
                                    node("type").set("direct")
                                }
                        )
                        node("just-example-for-socks").setCV(
                                "Example for socks proxy",
                                createNode().apply {
                                    node("type").set("socks")
                                    node("host").set("localhost")
                                    node("port").set(1080)
                                }
                        )
                        node("just-example-for-socks-with-authentication").setCV(
                                "Example for socks proxy with authentication",
                                createNode().apply {
                                    node("type").set("socks")
                                    node("host").set("localhost")
                                    node("port").set(1080)
                                    node("username").set("username")
                                    node("password").set("password")
                                }
                        )
                        node("yggdrasil").setCV(
                                "The proxy of yggdrasil connecting",
                                createNode().apply {
                                    node("type").set("direct")
                                }
                        )
                        node("just-example-for-http").set(createNode().apply {
                            node("type").set("http")
                            node("url").set("http://localhost/proxy")
                        })

                    }
            )
            node("CDN").setCV("CDN settings", createNode().apply {
                node("enable").set(false)
                node("origin").set("CDN origin link")
            })
        })
    }

    private fun getChannelClass(): KClass<out ServerSocketChannel> = when {
        KQueue.isAvailable() -> KQueueServerSocketChannel::class
        Epoll.isAvailable() -> EpollServerSocketChannel::class
        else -> NioServerSocketChannel::class
    }

    @OptIn(KtorExperimentalAPI::class)
    @JvmStatic
    @JvmOverloads
    fun main(args: Array<String>? = null) {
        if (args?.getOrNull(0) == "setup") {
            startSetup()
            return
        }
        reloadConfiguration()
        val server = embeddedServer(Netty, environment = applicationEngineEnvironment {
            parentCoroutineContext = dispatcher
            this.log = WrappedLogger
            WrappedLogger.trace("Verbose enabled.")

            module {
                install(DefaultHeaders)
                install(ContentNegotiation)
                routing {
                    post("/api/profiles/minecraft") {
                        val query = JsonParser.parseString(call.receiveText()).asJsonArray.map { elm ->
                            val value = elm.asString
                            when {
                                value.endsWith("@official") -> value.substring(0, value.length - 9) to true
                                value.endsWith("@yggdrasil") -> value.substring(0, value.length - 10) to false
                                else -> value to null
                            }
                        }.filter(object : (Pair<String, Boolean?>) -> Boolean {
                            val filtered = hashSetOf<String>()
                            override fun invoke(p1: Pair<String, Boolean?>): Boolean {
                                if (p1.first.lowercase(Locale.getDefault()) in filtered) {
                                    return false
                                }
                                filtered.add(p1.first.lowercase(Locale.getDefault()))
                                return true
                            }
                        })
                        val mapping = query.asSequence().filter {
                            it.second != null
                        }.associate {
                            it.first.lowercase(Locale.getDefault()) to when (it.second) {
                                true -> "@official"
                                false -> "@yggdrasil"
                                else -> throw AssertionError()
                            }
                        }
                        val sendToYggdrasil = query.mapNotNull {
                            when (it.second) {
                                true -> null
                                else -> it.first
                            }
                        }
                        val sendToOfficial = query.mapNotNull {
                            when (it.second) {
                                false -> null
                                else -> it.first
                            }
                        }
                        WrappedLogger.debug {
                            "Query profile data:\n" +
                                    "  -> Yggdrasil: $sendToYggdrasil\n" +
                                    "  -> Official : $sendToOfficial"
                        }
                        val contentType = ContentType.parse("application/json; charset=utf-8")
                        val fromOfficial = async {
                            runCatching<String> {
                                if (sendToOfficial.isEmpty()) "[]"
                                else officialClient.post(profilesMinecraft) {
                                    body = ByteArrayContent(JsonArray().also { array ->
                                        sendToOfficial.forEach { array.add(it) }
                                    }.toString().toByteArray(Charsets.UTF_8), contentType = contentType)
                                }
                            }.getOrElse {
                                WrappedLogger.trace(null, t = it)
                                "[]"
                            }.let { JsonParser.parseString(it).asJsonArray }
                        }
                        val fromYggdrasil = async {
                            runCatching<String> {
                                if (sendToYggdrasil.isEmpty()) "[]"
                                else yggdrasilClient.post(
                                        "$baseAPI/api/profiles/minecraft"
                                ) {
                                    body = ByteArrayContent(JsonArray().also { array ->
                                        sendToYggdrasil.forEach { array.add(it) }
                                    }.toString().toByteArray(Charsets.UTF_8), contentType = contentType)
                                }
                            }.getOrElse {
                                WrappedLogger.trace(null, t = it)
                                "[]"
                            }.let { JsonParser.parseString(it).asJsonArray }
                        }
                        val officialResponse = fromOfficial.await()
                        val yggdrasilResponse = fromYggdrasil.await()
                        val dataFirst: JsonArray
                        val dataSecond: JsonArray
                        if (officialFirst) {
                            dataFirst = officialResponse
                            dataSecond = yggdrasilResponse
                        } else {
                            dataFirst = yggdrasilResponse
                            dataSecond = officialResponse
                        }
                        WrappedLogger.debug {
                            "Query response: (officialFirst=$officialFirst)\n" +
                                    "  <- Yggdrasil: $yggdrasilResponse\n" +
                                    "  <- Official : $officialResponse"
                        }
                        val existed = hashSetOf<String>()
                        val response = JsonArray()
                        dataFirst.forEach { elm ->
                            response.add(elm)
                            existed.add(elm.asJsonObject["id"].asString.lowercase(Locale.getDefault()))
                            existed.add(elm.asJsonObject["name"].asString.lowercase(Locale.getDefault()))
                        }
                        dataSecond.forEach { elm ->
                            val obj = elm.asJsonObject
                            val id = obj["id"].asString.lowercase(Locale.getDefault())
                            val name = obj["name"].asString.lowercase(Locale.getDefault())
                            if (id !in existed && name !in existed) {
                                existed.add(id)
                                existed.add(name)
                                response.add(elm)
                            }
                        }
                        response.forEach { resp ->
                            resp.asJsonObject.let {
                                it.addProperty("name", it["name"].asString.let { old ->
                                    old + (mapping[old.lowercase(Locale.getDefault())] ?: "")
                                })
                            }
                        }
                        this.call.respondText(response.toString(), ContentType("application", "json"), HttpStatusCode.OK)
                    }

                    post("/minecraftservices/authentication/login_with_xbox") {
                        val query = JsonParser.parseString(call.receiveText()).asJsonObject
                        val identityToken = query.get("identityToken").toString()
                        WrappedLogger.trace("I - login_with_xbox <- identityToken=$identityToken")
                        if (identityToken == "") {
                            WrappedLogger.trace("No identityToken")
                            this.call.respond(HttpStatusCode.NoContent)
                            return@post
                        }
                        val xboxDeferred = async(Dispatchers.IO) {
                            runCatching {
                                WrappedLogger.trace("Connecting to MSAPE...")
                                val response = msaPEClient.post<HttpResponse>(
                                    url = URLBuilder().apply {
                                        takeFrom(login_with_xbox)
                                    }.build()) {
                                        contentType(ContentType.Application.Json)
                                        body = "{\"identityToken\":$identityToken}"
                                    }
                                WrappedLogger.trace("Body: {\"identityToken\":$identityToken}")
                                if (response.status.value == 200) {
                                    WrappedLogger.trace("MSAPE Responsed...")
                                    val content = response.receive<String>()
                                    WrappedLogger.trace("MSAPE Content: $content")
                                    return@async content
                                } else {
                                    WrappedLogger.trace("MSAPE Error: $response")
                                }
                            }.onFailure { WrappedLogger.trace("MSAPE Network error", t = it) }
                            return@async null
                        }
                        val xboxResponse = xboxDeferred.await()
                        WrappedLogger.trace("You, and Me.... Finished.")
                        this.call.respondText(xboxResponse.toString(), ContentType("application", "json"), HttpStatusCode.OK)
                    }

                    post("/sessionserver/session/minecraft/join") {
                        val query = call.receiveText().toString()
                        WrappedLogger.trace("I - pe_join <- body=$query")
                        val xboxDeferred = async(Dispatchers.IO) {
                            runCatching {
                                WrappedLogger.trace("Connecting to PEJOIN...")
                                val response = msaPEClient.post<HttpResponse>(
                                        url = URLBuilder().apply {
                                            takeFrom(pe_join)
                                        }.build()) {
                                    contentType(ContentType.Application.Json)
                                    body = query
                                }
                                if (response.status.value == 204) {
                                    WrappedLogger.trace("PEJOIN Logged in")
                                    return@async null
                                } else {
                                    WrappedLogger.trace("PEJOIN Error: $response")
                                    val content = response.receive<String>()
                                    WrappedLogger.trace("PEJOIN Content: $content")
                                    return@async content
                                }
                            }.onFailure { WrappedLogger.trace("PEJOIN Network error", t = it) }
                            return@async null
                        }
                        val xboxResponse = xboxDeferred.await()
                        WrappedLogger.trace("You, and Me.... Finished.")
                        this.call.respondText(xboxResponse.toString(), ContentType("application", "json"), HttpStatusCode.OK)
                    }

                    post("/authserver/refresh") {
                        val query = call.receiveText().toString()
                        WrappedLogger.trace("I - PE_MOJANG_AUTH_REFRESH <- body=$query")
                        val deferred= async(Dispatchers.IO) {
                            runCatching {
                                WrappedLogger.trace("Connecting to PE_MOJANG_AUTH_REFRESH...")
                                val response = msaPEClient.post<HttpResponse>(
                                        url = URLBuilder().apply {
                                            takeFrom(pe_mojang_authserver_refresh)
                                        }.build()) {
                                    contentType(ContentType.Application.Json)
                                    body = query
                                }
                                if (response.status.value == 200) {
                                    WrappedLogger.trace("PE_MOJANG_AUTH_REFRESH Responsed...")
                                    val content = response.receive<String>()
                                    WrappedLogger.trace("PE_MOJANG_AUTH_REFRESH Content: $content")
                                    return@async content
                                } else {
                                    WrappedLogger.trace("PE_MOJANG_AUTH_REFRESH Error: $response")
                                }
                            }.onFailure { WrappedLogger.trace("PE_MOJANG_AUTH_REFRESH Network error", t = it) }
                            return@async null
                        }
                        val response = deferred.await()
                        WrappedLogger.trace("You, and Me.... Finished.")
                        this.call.respondText(response.toString(), ContentType("application", "json"), HttpStatusCode.OK)
                    }

                    post("/authserver/authenticate") {
                        val query = call.receiveText().toString()
                        WrappedLogger.trace("I - PE_MOJANG_AUTH_AUTH <- body=$query")
                        val deferred= async(Dispatchers.IO) {
                            runCatching {
                                WrappedLogger.trace("Connecting to PE_MOJANG_AUTH_AUTH...")
                                val response = msaPEClient.post<HttpResponse>(
                                        url = URLBuilder().apply {
                                            takeFrom(pe_mojang_authserver_authenticate)
                                        }.build()) {
                                    contentType(ContentType.Application.Json)
                                    body = query
                                }
                                if (response.status.value == 200) {
                                    WrappedLogger.trace("PE_MOJANG_AUTH_AUTH Responsed...")
                                    val content = response.receive<String>()
                                    WrappedLogger.trace("PE_MOJANG_AUTH_AUTH Content: $content")
                                    return@async content
                                } else {
                                    WrappedLogger.trace("PE_MOJANG_AUTH_AUTH Error: $response")
                                }
                            }.onFailure { WrappedLogger.trace("PE_MOJANG_AUTH_AUTH Network error", t = it) }
                            return@async null
                        }
                        val response = deferred.await()
                        WrappedLogger.trace("You, and Me.... Finished.")
                        this.call.respondText(response.toString(), ContentType("application", "json"), HttpStatusCode.OK)
                    }

                    get("/minecraftservices/minecraft/profile") {
                        val headers = call.request.headers
                        val auth = headers["authorization"]
                        WrappedLogger.trace("I - pe_profile <- authorization=$auth")
                        if (auth == null) {
                            WrappedLogger.trace("No auth")
                            this.call.respond(HttpStatusCode.NoContent)
                            return@get
                        }
                        val pe_profile_request = async(Dispatchers.IO) {
                            runCatching {
                                WrappedLogger.trace("Connecting to PE_PROFILE...")
                                val response = msaPEClient.get<HttpResponse>(
                                        url = URLBuilder().apply {
                                            takeFrom(pe_profile)
                                        }.build()) {
                                    headers {
                                        append("authorization", auth)
                                    }
                                }
                                WrappedLogger.trace("Body: ${call.receiveText()}")
                                if (response.status.value == 200) {
                                    WrappedLogger.trace("PE_PROFILE Responsed...")
                                    val content = response.receive<String>()
                                    WrappedLogger.trace("PE_PROFILE Content: $content")
                                    return@async content
                                } else {
                                    WrappedLogger.trace("PE_PROFILE Error: $response")
                                }
                            }.onFailure { WrappedLogger.trace("PE_PROFILE Network error", t = it) }
                            return@async null
                        }
                        val profile = pe_profile_request.await()
                        WrappedLogger.trace("PE Profile Obtained")
                        this.call.respondText(profile.toString(), ContentType("application", "json"), HttpStatusCode.OK)
                    }

                    @ContextDsl
                    fun getCatching(path: String, body: PipelineInterceptor<Unit, ApplicationCall>): Route = get(path) {
                        runCatching {
                            body(it)
                        }.onFailure { exception ->
                            if (WrappedLogger.traceEnabled) {
                                WrappedLogger.trace(null, t = exception)
                            } else {
                                WrappedLogger.warn("A exception in reading network.")
                            }
                        }
                    }

                    @ContextDsl
                    fun getCatching(body: PipelineInterceptor<Unit, ApplicationCall>): Route = get {
                        runCatching {
                            body(it)
                        }.onFailure { exception ->
                            if (WrappedLogger.traceEnabled) {
                                WrappedLogger.trace(null, t = exception)
                            } else {
                                WrappedLogger.warn("A exception in reading network.")
                            }
                        }
                    }

                    getCatching("/sessionserver/session/minecraft/hasJoined") get@{
                        val user = this.call.parameters["username"]
                        val server = this.call.parameters["serverId"]
                        val ip = this.call.parameters["ip"]
                        WrappedLogger.trace("I - hasJoined <- user=$user, server=$server")
                        if (user == null || server == null) {
                            WrappedLogger.trace("No server come.")
                            this.call.respond(HttpStatusCode.NoContent)
                            return@get
                        }
                        val officialDeferred = async(Dispatchers.IO) {
                            runCatching {
                                WrappedLogger.trace("Connecting to official...")
                                val response = officialClient.get<HttpResponse>(
                                        url = URLBuilder().apply {
                                            takeFrom(hasJoin)
                                            parameters.append("username", user)
                                            parameters.append("serverId", server)
                                            if (ip != null) {
                                                parameters.append("ip", ip)
                                            }
                                        }.build()
                                )
                                if (response.status.value == 200) {
                                    WrappedLogger.trace("Official Responsed...")
                                    return@async response
                                }
                            }.onFailure { WrappedLogger.trace("Official Network error", t = it) }
                            return@async null
                        }
                        val yggdrasilDeferred = async(Dispatchers.IO) {
                            WrappedLogger.trace("Connecting to Yggdrasil...")
                            runCatching {
                                val response = yggdrasilClient.get<HttpResponse>(
                                        url = URLBuilder().apply {
                                            takeFrom("$baseAPI/sessionserver/session/minecraft/hasJoined")
                                            parameters.append("username", user)
                                            parameters.append("serverId", server)
                                            if (ip != null) {
                                                parameters.append("ip", ip)
                                            }
                                        }.build().also {
                                            WrappedLogger.trace("Trying to connect $it")
                                        }
                                )
                                if (response.status.value == 200) {
                                    WrappedLogger.trace("Yggdrasil Responsed...")
                                    return@async response
                                }
                            }.onFailure { WrappedLogger.trace("Yggdrasil", t = it) }
                            return@async null
                        }
                        val officialResponse = officialDeferred.await()
                        val yggdrasilResponse = yggdrasilDeferred.await()
                        val resp = if (officialResponse ?: yggdrasilResponse == null || (officialResponse != null && yggdrasilResponse != null)) {
                            // Failed...
                            WrappedLogger.trace("No server compiled......")
                            NoContextResponse
                        } else {
                            officialResponse ?: yggdrasilResponse!!
                        }
                        val resp0 = patch(resp, resp === officialResponse)

                        WrappedLogger.trace("You, and Me.... Finished.")
                        this.call.respond(object : OutgoingContent.ByteArrayContent() {
                            override fun bytes(): ByteArray {
                                return resp0
                            }

                            override val status: HttpStatusCode
                                get() = resp.status
                            override val contentType: ContentType
                                get() = ContentType("application", "json; charset=utf8")
                        })
                    }
                    getCatching {
                        val uri = this.call.request.origin.uri
                        WrappedLogger.trace("DRX: $uri")
                        val resp: HttpResponse = yggdrasilClient.get("$baseAPI$uri")
                        this.call.respond(object : OutgoingContent.ReadChannelContent() {
                            override fun readFrom(): ByteReadChannel {
                                return resp.content
                            }

                            override val status: HttpStatusCode
                                get() = resp.status
                            override val contentType: ContentType?
                                get() = resp.contentType()
                        })
                    }
                }
            }
            connector {
                this.host = host_C
                this.port = port_C
                output.println("[YggdrasilOfficialProxy] Server running on $host_C:$port_C")
            }
        }) {
            this.shareWorkGroup = true
            this.configureBootstrap = {
                fun g() = when {
                    KQueue.isAvailable() -> KQueueEventLoopGroup(3, threadFactory)
                    Epoll.isAvailable() -> EpollEventLoopGroup(3, threadFactory)
                    else -> NioEventLoopGroup(3, threadFactory)
                }
                this.group(g(), g())
                        .channel(getChannelClass().java)
            }
        }
        try {
            server.start(args != null)
        } catch (any: Throwable) {
            any.printStackTrace()
            exitProcess(-548998)
        }
    }


    @JvmStatic
    fun premain(args: String?, instrumentation: Instrumentation) {
        daemon = true
        main()
        val path = authlib ?: error("Error: Authlib not found. Please fix it.")
        val jar = JarFile(path)
        instrumentation.appendToSystemClassLoaderSearch(jar)
        val `class` = jar.manifest.mainAttributes.getValue("Premain-Class")
        val bootstrap = Class.forName(`class`)
        bootstrap.getMethod("premain", java.lang.String::class.java,
                Instrumentation::class.java
        ).invoke(null, "http://localhost:$port_C", instrumentation)
    }
}

