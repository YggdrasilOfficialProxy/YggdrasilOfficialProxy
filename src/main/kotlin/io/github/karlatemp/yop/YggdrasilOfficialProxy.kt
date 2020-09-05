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
import io.ktor.client.engine.*
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.ConfigurationOptions
import ninja.leaping.configurate.commented.CommentedConfigurationNode
import ninja.leaping.configurate.hocon.HoconConfigurationLoader
import java.io.File
import java.lang.instrument.Instrumentation
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.jar.JarFile
import kotlin.system.exitProcess

object YggdrasilOfficialProxy {
    lateinit var yggdrasilClient: HttpClient
    lateinit var officialClient: HttpClient

    val hasJoin = buildString { // Skip AuthLib Injector
        append("https://sessionserver")
        append(".mojang.com")
        append("/session/minecraft/hasJoined")
    }
    val profilesMinecraft = buildString { // Skip AuthLib Injector
        append("https://api.")
        append("mojang.")
        append("com/profiles/minecraft")
    }

    val output = System.out

    var baseAPI = "https://skin.prinzeugen.net/api/yggdrasil"
    var officialFirst = false
    var host_C = "0.0.0.0"
    var port_C = 32217
    var authlib by AtomicReference<String?>()

    @OptIn(KtorExperimentalAPI::class)
    fun reloadConfiguration() {
        val file = File("YggdrasilOfficialProxy.conf")
        val loader = HoconConfigurationLoader.builder()
                .setFile(file)
                .setDefaultOptions(ConfigurationOptions.defaults()
                        .setShouldCopyDefaults(true)
                )
                .build()
        val conf = kotlin.runCatching {
            if (file.isFile)
                loader.load()
            else null
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
        val edited = conf != null && conf.getNode("edited").boolean
        if (!file.isFile || conf == null || !edited) {
            output.println("# YggdrasilOfficialProxy v2.0.0")
            output.println("# #############################")
            output.println("# Oops.")
            output.println(when {
                conf == null && file.isFile -> "# Your Configuration of YggdrasilOfficialProxy looks like broken. We will fix it."
                edited -> {
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
            return when (getNode("type").getString("direct")) {
                "http" -> {
                    ProxyBuilder.http(Url(getNode("url").string ?: error("Missing url")))
                }
                "socks" -> {
                    ProxyBuilder.socks(getNode("host").string ?: error("Missing host"), getNode("port").int)
                }
                else -> null
            }
        }

        fun openClient(type: String): HttpClient {
            val p = conf.getNode("proxy", type).parseProxy()
            return HttpClient(io.ktor.client.engine.okhttp.OkHttp) {
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
        this.officialFirst = conf.getNode("official-first").boolean
        this.host_C = conf.getNode("server", "host").getString("0.0.0.0")
        this.port_C = conf.getNode("server", "port").getInt(port_C)
        authlib = conf.getNode("authlib-injector").string
        baseAPI = conf.getNode("api").getString("https://skin.prinzeugen.net/api/yggdrasil")
                .let {
                    if (it.endsWith("/"))
                        it.substring(0, it.length - 1)
                    else it
                }
        if (conf != loader.load()) {
            loader.save(conf)
        }
    }

    private fun HoconConfigurationLoader.regenFile() {
        fun CommentedConfigurationNode.setCV(comment: String, value: Any?) {
            setComment(comment)
            setValue(value)
        }
        save(createEmptyNode().apply {
            getNode("edited").setCV(
                    "IMPORTANT: Set this value to `true`!",
                    false
            )
            getNode("api").setCV(
                    "The yggdrasil api root",
                    "https://skin.prinzeugen.net/api/yggdrasil"
            )
            getNode("server").setCV(
                    "The server binding actions.",
                    createEmptyNode().apply {
                        getNode("host").value = "0.0.0.0"
                        getNode("port").value = 32217
                    }
            )
            getNode("official-first").setCV(
                    "When access multiple apis. Use official api first.",
                    false
            )
            getNode("authlib-injector").setCV(
                    "The location of YggdrasilInjector\n" +
                            "@see https://github.com/yushijinhun/authlib-injector/",
                    "./authlib-injector-XXXX.jar"
            )
            getNode("proxy").setCV(
                    "Proxy settings",
                    createEmptyNode().apply {
                        getNode("official").setCV(
                                "The proxy of official connecting",
                                createEmptyNode().apply {
                                    getNode("type").value = "socks"
                                    getNode("host").value = "localhost"
                                    getNode("port").value = 1080
                                }
                        )
                        getNode("yggdrasil").setCV(
                                "The proxy of yggdrasil connecting",
                                createEmptyNode().apply {
                                    getNode("type").value = "direct"
                                }
                        )
                        getNode("just-example-for-http").value =
                                createEmptyNode().apply {
                                    getNode("type").value = "http"
                                    getNode("url").value = "http://localhost/proxy"
                                }

                    }
            )
        })
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
            parentCoroutineContext = Dispatchers.Main
            this.log = WrappedLogger
            WrappedLogger.trace("Verbose enabled.")

            module {
                install(DefaultHeaders)
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
                                if (p1.first.toLowerCase() in filtered) {
                                    return false
                                }
                                filtered.add(p1.first.toLowerCase())
                                return true
                            }
                        })
                        val mapping = query.asSequence().filter {
                            it.second != null
                        }.associate {
                            it.first.toLowerCase() to when (it.second) {
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
                                officialClient.post(profilesMinecraft) {
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
                                yggdrasilClient.post(
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
                            existed.add(elm.asJsonObject["id"].asString.toLowerCase())
                            existed.add(elm.asJsonObject["name"].asString.toLowerCase())
                        }
                        dataSecond.forEach { elm ->
                            val obj = elm.asJsonObject
                            val id = obj["id"].asString.toLowerCase()
                            val name = obj["name"].asString.toLowerCase()
                            if (id !in existed && name !in existed) {
                                existed.add(id)
                                existed.add(name)
                                response.add(elm)
                            }
                        }
                        response.forEach { resp ->
                            resp.asJsonObject.let {
                                it.addProperty("name", it["name"].asString.let { old ->
                                    old + (mapping[old.toLowerCase()] ?: "")
                                })
                            }
                        }
                        this.call.respondText(response.toString(), ContentType("application", "json"), HttpStatusCode.OK)
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
                    getCatching("/sessionserver/session/minecraft/hasJoined") get@{
                        val user = this.call.parameters["username"]
                        val server = this.call.parameters["serverId"]
                        val ip = this.call.parameters["ip"]
                        WrappedLogger.trace("I - hasJoined <- user=$user, server=$server")
                        if ((user ?: server) == null) {
                            WrappedLogger.trace("No server come.")
                            this.call.respond(HttpStatusCode.NoContent)
                            return@get
                        }
                        val future = CompletableDeferred<HttpResponse>()
                        val counter = AtomicInteger()
                        val compiled = AtomicBoolean(false)
                        val allFailed = {
                            if (counter.incrementAndGet() == 2 && !compiled.get()) {
                                // Failed...
                                WrappedLogger.trace("No server compiled......")
                                kotlin.runCatching {
                                    future.complete(NoContextResponse)
                                }.onFailure {
                                    WrappedLogger.error(
                                            msg = "Oops,, Error here",
                                            t = it
                                    )
                                }
                            }
                        }
                        launch(Dispatchers.IO) {
                            runCatching {
                                WrappedLogger.trace("Connecting to official...")
                                val response = officialClient.get<HttpResponse>(
                                        url = URLBuilder().apply {
                                            takeFrom(hasJoin)
                                            parameters.append("username", user!!)
                                            parameters.append("serverId", server!!)
                                            if (ip != null) {
                                                parameters.append("ip", ip)
                                            }
                                        }.build()
                                )
                                if (response.status.value == 200) {
                                    compiled.set(true)
                                    WrappedLogger.trace("Official Responsed...")
                                    future.complete(response)
                                }
                            }.onFailure { WrappedLogger.trace("Official NetWork error", t = it) }
                        }.invokeOnCompletion { allFailed() }
                        launch(Dispatchers.IO) {
                            WrappedLogger.trace("Connecting to Yggdrasil...")
                            runCatching {
                                val response = yggdrasilClient.get<HttpResponse>(
                                        url = URLBuilder().apply {
                                            takeFrom("$baseAPI/sessionserver/session/minecraft/hasJoined")
                                            parameters.append("username", user!!)
                                            parameters.append("serverId", server!!)
                                            if (ip != null) {
                                                parameters.append("ip", ip)
                                            }
                                        }.build().also {
                                            WrappedLogger.trace("Trying to connect $it")
                                        }
                                )
                                if (response.status.value == 200) {
                                    compiled.set(true)
                                    WrappedLogger.trace("Yggdrasil Responsed...")
                                    future.complete(response)
                                }
                            }.onFailure { WrappedLogger.trace("Yggdrasil", t = it) }
                        }.invokeOnCompletion { allFailed() }
                        val resp = future.await()

                        WrappedLogger.trace("You, and Me.... Finished.")
                        this.call.respond(object : OutgoingContent.ReadChannelContent() {
                            override fun readFrom(): ByteReadChannel {
                                return resp.content
                            }

                            override val status: HttpStatusCode?
                                get() = resp.status
                            override val contentType: ContentType?
                                get() = ContentType("application", "json; charset=utf8")
                        })
                    }
                    get {
                        val uri = this.call.request.origin.uri
                        val resp: HttpResponse = yggdrasilClient.get("$baseAPI$uri")
                        this.call.respond(object : OutgoingContent.ReadChannelContent() {
                            override fun readFrom(): ByteReadChannel {
                                return resp.content
                            }

                            override val status: HttpStatusCode?
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
        })
        try {
            server.start(args != null)
        } catch (any: Throwable) {
            any.printStackTrace()
            exitProcess(-548998)
        }
    }


    @JvmStatic
    fun premain(args: String?, instrumentation: Instrumentation) {
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

