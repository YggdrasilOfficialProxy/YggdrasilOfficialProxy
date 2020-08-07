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
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.client.HttpClient
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.ProxyConfig
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpStatement
import io.ktor.features.DefaultHeaders
import io.ktor.features.origin
import io.ktor.http.*
import io.ktor.http.content.OutgoingContent
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.KtorExperimentalAPI
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.jvm.javaio.toInputStream
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
    var authlib by AtomicReference<kotlin.String?>()

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
                engine { proxy = p }
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
        reloadConfiguration()
        val server = embeddedServer(Netty, environment = applicationEngineEnvironment {
            parentCoroutineContext = Dispatchers.Main

            module {
                install(DefaultHeaders)
                routing {
                    post("/api/profiles/minecraft") {
                        val text = call.receiveText()
                        val fromOfficial = async(Dispatchers.IO) {
                            officialClient.post<HttpStatement>(
                                    profilesMinecraft
                            ) {
                                body = text
                            }.execute()
                        }
                        val fromYggdrasil = async(Dispatchers.IO) {
                            yggdrasilClient.post<HttpResponse>(
                                    "$baseAPI/api/profiles/minecraft"
                            ) {
                                body = text
                            }
                        }
                        var r1 = fromOfficial.await()
                        var r2 = fromYggdrasil.await()
                        val response = JsonArray()
                        if (!officialFirst) {
                            val r3 = r1
                            r1 = r2
                            r2 = r3
                        }
                        if (r1.status.value == 200) {
                            response.addAll(r1.content.toInputStream().bufferedReader().use {
                                JsonParser.parseReader(it).asJsonArray
                            })
                        }
                        if (r2.status.value == 200) {
                            val resp = r2.content.toInputStream().bufferedReader().use {
                                JsonParser.parseReader(it).asJsonArray
                            }
                            if (response.size() == 0) {
                                response.addAll(resp)
                            } else {
                                val knowns = response.asSequence()
                                        .map { it.asJsonObject.getAsJsonPrimitive("id").asString }
                                        .toMutableSet()
                                resp.forEach { elm ->
                                    if (elm.asJsonObject["id"].asString !in knowns) {
                                        knowns.add(elm.asJsonObject["id"].asString)
                                        response.add(elm)
                                    }
                                }
                            }
                        }
                        this.call.respondText(response.toString(), ContentType("application", "json; charset=utf8"), HttpStatusCode.OK)
                    }
                    get("/sessionserver/session/minecraft/hasJoined") {
                        val user = this.call.parameters["username"]
                        val server = this.call.parameters["serverId"]
                        val ip = this.call.parameters["ip"]
                        if ((user ?: server) == null) {
                            this.call.respond(HttpStatusCode.NoContent)
                            return@get
                        }
                        val future = CompletableDeferred<HttpResponse>()
                        val counter = AtomicInteger()
                        val allFailed = {
                            if (counter.incrementAndGet() == 2) {
                                // Failed...
                                future.complete(NoContextResponse)
                            }
                        }
                        launch(Dispatchers.IO) {
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
                                future.complete(response)
                            }
                        }.invokeOnCompletion { allFailed() }
                        launch(Dispatchers.IO) {
                            val response = yggdrasilClient.get<HttpResponse>(
                                    url = URLBuilder().apply {
                                        takeFrom("$baseAPI/sessionserver/session/minecraft/hasJoined")
                                        parameters.append("username", user!!)
                                        parameters.append("serverId", server!!)
                                        if (ip != null) {
                                            parameters.append("ip", ip)
                                        }
                                    }.build()
                            )
                            if (response.status.value == 200) {
                                future.complete(response)
                            }
                        }.invokeOnCompletion { allFailed() }
                        val resp = future.await()
                        if (resp.status.value == HttpStatusCode.NoContent.value) {
                            this.call.respond(HttpStatusCode.NoContent.value)
                        } else {
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

