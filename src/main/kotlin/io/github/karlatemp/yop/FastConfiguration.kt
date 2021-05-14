/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/08/26 01:44:28
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/FastConfiguration.kt
 */

package io.github.karlatemp.yop

import org.fusesource.jansi.Ansi
import org.jline.builtins.Completers
import org.jline.reader.Candidate
import org.jline.reader.Completer
import org.jline.reader.LineReaderBuilder
import org.jline.terminal.TerminalBuilder
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.ConfigurationOptions
import org.spongepowered.configurate.hocon.HoconConfigurationLoader
import java.io.File
import java.net.ServerSocket
import java.net.URL
import java.net.URLClassLoader
import java.util.*
import java.util.jar.JarFile
import java.util.regex.Pattern
import java.util.zip.ZipFile
import kotlin.math.max
import kotlin.math.min
import kotlin.system.exitProcess

fun main() {
    startSetup()
}

var JANSI_MAJOR_VERSION = 0
var JANSI_MINOR_VERSION = 0
fun isAtLeast(major: Int, minor: Int): Boolean {
    return JANSI_MAJOR_VERSION >= major && JANSI_MINOR_VERSION >= minor
}


private fun scanJAnsi(): File? {
    runCatching {
        Class.forName("org.fusesource.jansi.Ansi") // Ansi loaded
        return null
    }
    // First. scan from ./cache (Paper Server)
    val cache = File("cache")
    cache.walk().filter { it.isFile && it.extension == "jar" }
            .forEach { file ->
                runCatching {
                    ZipFile(file).use { zip ->
                        if (zip.getEntry("org/fusesource/jansi/Ansi.class") != null) {
                            return file
                        }
                    }
                }
            }
    // Then. scan in current direction
    File(".").walk().maxDepth(1).filter { it.isFile && it.extension == "jar" }
            .forEach { file ->
                runCatching {
                    ZipFile(file).use { zip ->
                        if (zip.getEntry("org/fusesource/jansi/Ansi.class") != null) {
                            return file
                        }
                    }
                }
            }
    // Fill scan!
    File(".").walk().filter { it.isFile && it.extension == "jar" }
            .forEach { file ->
                runCatching {
                    ZipFile(file).use { zip ->
                        if (zip.getEntry("org/fusesource/jansi/Ansi.class") != null) {
                            return file
                        }
                    }
                }
            }
    return null
}

fun startSetup() {
    fun File.load() {
        val inst = OpenLibrary.ins
        if (inst != null) {
            inst.appendToSystemClassLoaderSearch(JarFile(this))
            return
        }
        val sys = ClassLoader.getSystemClassLoader()
        runCatching {
            var c: Class<*>? = sys.javaClass
            while (true) {
                val w = c ?: break
                runCatching {
                    w.getDeclaredMethod("appendToClassPathForInstrumentation", java.lang.String::class.java)
                            .apply { isAccessible = true }
                            .invoke(sys, this@load.path)
                    return
                }
                c = w.superclass
            }
        }
        runCatching {
            URLClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java)
                    .apply { isAccessible = true }
                    .invoke(sys, toURI().toURL())
        }
    }
    scanJAnsi()?.takeIf {
        ZipFile(it).use { zip ->
            val entry = zip.getEntry("org/fusesource/jansi/jansi.properties")
                    ?: return@takeIf false
            val prop = zip.getInputStream(entry).use { ver ->
                Properties().also { prop -> ver.use { prop.load(it) } }
            }
            val v = prop["version"]?.toString() ?: return@takeIf false
            val m = Pattern.compile("([0-9]+)\\.([0-9]+)([.-]\\S+)?").matcher(v)
            if (m.matches()) {
                JANSI_MAJOR_VERSION = m.group(1).toInt()
                JANSI_MINOR_VERSION = m.group(2).toInt()
            }
        }
        true
    }?.load()
    runCatching {
        Class.forName("org.fusesource.jansi.Ansi") // Ansi loaded
    }.onFailure { _ ->
        // download
        val file = File("cache/jansi-1.18.jar")
        file.parentFile.mkdirs()
        if (!file.isFile) {
            URL("https://repo1.maven.org/maven2/org/fusesource/jansi/jansi/1.18/jansi-1.18.jar")
                    .openStream().use { inp ->
                        file.outputStream().use { inp.copyTo(it) }
                    }
        }
        file.load()
        runCatching {
            Class.forName("org.fusesource.jansi.Ansi")
        }.onFailure {
            throw RuntimeException(
                    "Error: Please use proxy version or run in server direction.",
                    it.takeIf { it !is ClassNotFoundException }
            )
        }
    }
    // Init terminal
    val terminal = TerminalBuilder.builder()
            .dumb(System.getProperty("yop.terminal") !== null ||
                    System.getProperty("java.class.path")
                            .contains("idea_rt.jar")
            )
            .build()
    val allOptions = ArrayList<String>()
    fun setupOptions(vararg opts: String) {
        allOptions.clear()
        allOptions.addAll(listOf(*opts))
    }

    var delegateCompleter: Completer? = null
    val lineReader = LineReaderBuilder.builder()
            .terminal(terminal)
            .completer { reader, line, candidates ->
                val d = delegateCompleter
                if (d != null) {
                    d.complete(reader, line, candidates)
                    return@completer
                }
                allOptions.forEach { candidates.add(Candidate(it)) }
            }
            .build()

    fun println(any: Any?) {
        lineReader.printAbove(any.toString())
    }
    println("==== [   Shell Setup   ] ====")
    println("First. let us confirm your start script shell.")
    File(".").walk().maxDepth(1)
            .filter {
                it.isFile
            }
            .filter {
                val ext = it.extension
                if (ext == "sh" || ext == "bat" || ext == "cmd") {
                    true
                } else {
                    val first = it.bufferedReader().use { it.readLine() }
                    first == "#!/bin/bash" || first == "#!/bin/sh" || first == "#!/bin/zsh"
                }
            }
            .forEach { allOptions.add(it.path) }
    if (allOptions.isEmpty()) {
        lineReader.printAbove("Oops, We can't find anything of starting shell.")
        delegateCompleter = Completers.FileNameCompleter()
    } else {
        lineReader.printAbove("We found your start script. Use <Tab> to select them.")
    }
    val target = lineReader.readLine("> ").trim()
    delegateCompleter = null
    allOptions.clear()
    lineReader.printAbove("Ok, you selected $target")
    val lines = File(target).readLines()
    // Scan target = -javaagent:*=*
    val regex = """-javaagent:.*?\.jar=http(s|)://[A-Za-z/.\-_0-9]+""".toRegex()
    run lineLoop@{
        lines.forEachIndexed { index, line ->
            val result = regex.find(line)
            if (result != null) {
                println()
                val pre = line.substring(0, result.range.first)
                val next = line.substring(result.range.last + 1)
                val javaagent = result.value
                println(Ansi.ansi().reset()
                        .a(pre)
                        .fgBrightYellow()
                        .a(javaagent)
                        .reset()
                        .a(next)
                )
                setupOptions("yes", "no")
                if (lineReader.readLine("Confirm? [Yes/No]> ").trim().lowercase(Locale.getDefault()) != "no") {
                    allOptions.clear()
                    println("Scanning YggdrasilOfficialProxy location...")
                    File(".").walk().maxDepth(1).filter { it.isFile && it.extension == "jar" }
                            .forEach { allOptions.add(it.path) }
                    println("Now, confirm YggdrasilOfficialProxy's location.")
                    println("Use <Tab> to see we found.")
                    val yop = lineReader.readLine("> ").trim()
                    println("Ok, YggdrasilOfficialProxy.jar is $yop")
                    val pathInArg = javaagent.substring(javaagent.indexOf('=') + 1)
                    val authlibInjector = javaagent.substring(11, javaagent.indexOf('='))
                    println("Authlib-Injector: $authlibInjector")
                    println("Please confirm your Yggdrasil API Root")
                    val api = lineReader.readLine("> ", null, pathInArg).trim()
                    println("YggdrasilRoot: $api")
                    fun printDiff() {
                        // Print diff
                        println("")
                        for (ii in max(0, index - 3) until min(lines.size, index + 3)) {
                            if (ii == index) {
                                println(Ansi.ansi().fgRed()
                                        .a("- ")
                                        .reset().a(pre)
                                        .fgRed().a(javaagent)
                                        .reset().a(next)
                                )
                                println(Ansi.ansi().fgGreen()
                                        .a("+ ")
                                        .reset().a(pre)
                                        .fgGreen().a("-javaagent:").a(yop)
                                        .reset().a(next)
                                )
                            } else {
                                println("  " + lines[ii])
                            }
                        }
                        println("")
                    }
                    printDiff()
                    setupOptions("yes", "no")
                    if (lineReader.readLine("Confirm? [Yes/No]> ").trim().lowercase(Locale.getDefault()) == "no") {
                        println("Cancelled....")
                        exitProcess(1)
                    }
                    allOptions.clear()
                    // Configuration generator
                    println("")
                    println("==== [  Configuration  ] ====")
                    println("")
                    val host = lineReader.readLine("The host of YOP bind> ", null, "127.0.0.1")
                    val port = lineReader.readLine("The port of YOP bind> ", null, "0").let {
                        val p = it.toInt()
                        if (p == 0) {
                            ServerSocket(0).use { it.localPort }
                        } else p
                    }
                    println("YggdrasilOfficialProxy will bind to $host:$port")
                    println("")
                    //             Extended settings
                    println("==== [Extended settings] ====")
                    println("")
                    setupOptions("true", "false")
                    val officialFirst = lineReader.readLine("Priority use data from the official authentication server> ", null, "true").trim().toBoolean()
                    allOptions.clear()
                    val proxyOfficial = HashMap<String, Any>()
                    val proxyYggdrasil = HashMap<String, Any>()
                    setupOptions("direct", "http", "socks")
                    when (lineReader.readLine("Please select the proxy type of 3rd YggdrasilAccessing(Tab)> ").trim().also {
                        allOptions.clear()
                    }) {
                        "direct" -> {
                            proxyYggdrasil["type"] = "direct"
                        }
                        "socks" -> {
                            proxyYggdrasil["type"] = "socks"
                            proxyYggdrasil["host"] = lineReader.readLine("Please input socks host> ")
                            proxyYggdrasil["port"] = lineReader.readLine("Please input socks port> ").toInt()
                        }
                        "http" -> {
                            proxyYggdrasil["type"] = "http"
                            proxyYggdrasil["url"] = lineReader.readLine("Please input http proxy url> ")
                        }
                        else -> {
                            println("Unknown proxy type")
                            proxyYggdrasil["type"] = "direct"
                        }
                    }

                    setupOptions("direct", "http", "socks")
                    when (lineReader.readLine("Please select the proxy type of OfficialAccessing(Tab)> ").trim().also {
                        allOptions.clear()
                    }) {
                        "direct" -> {
                            proxyOfficial["type"] = "direct"
                        }
                        "socks" -> {
                            proxyOfficial["type"] = "socks"
                            proxyOfficial["host"] = lineReader.readLine("Please input socks host> ")
                            proxyOfficial["port"] = lineReader.readLine("Please input socks port> ").toInt()
                        }
                        "http" -> {
                            proxyOfficial["type"] = "http"
                            proxyOfficial["url"] = lineReader.readLine("Please input http proxy url> ")
                        }
                        else -> {
                            println("Unknown proxy type")
                            proxyOfficial["type"] = "direct"
                        }
                    }

                    println("")
                    println("==== [ LATEST  CONFIRM ] ====")
                    println("")

                    println("Yggdrasil API ROOT: $api")
                    println("Authlib   Injector: $authlibInjector")
                    println("diff: $target")
                    printDiff()
                    println("")
                    println("Priority use data from the official authentication server: $officialFirst")
                    println("Proxy - Yggdrasil")
                    proxyYggdrasil.forEach { (key, value) ->
                        println("    $key = $value")
                    }
                    println("")
                    println("Proxy - Official")
                    proxyOfficial.forEach { (key, value) ->
                        println("    $key = $value")
                    }
                    setupOptions("no", "yes")
                    if (lineReader.readLine("CONFIRM? ").trim().lowercase(Locale.getDefault()) != "yes") {
                        println("Cancelled....")
                        exitProcess(5)
                    }
                    val yopConf = File("YggdrasilOfficialProxy.conf")
                    val hoconLoader = HoconConfigurationLoader.builder()
                            .file(yopConf)
                            .defaultOptions(
                                ConfigurationOptions.defaults()
                                    .shouldCopyDefaults(true))
                            .build()
                    hoconLoader.save(hoconLoader.createNode().also { root ->
                        root.node("edited").set(true)
                        root.node("api").set(api)
                        root.node("authlib-injector").set(authlibInjector)
                        root.node("official-first").set(officialFirst)
                        root.node("server", "host").set(host)
                        root.node("server", "port").set(port)
                        fun saveProxy(node: ConfigurationNode, m: Map<String, Any>) {
                            m.forEach { (t, u) ->
                                node.node(t).set(u)
                            }
                        }
                        saveProxy(root.node("proxy", "official"), proxyOfficial)
                        saveProxy(root.node("proxy", "yggdrasil"), proxyYggdrasil)
                    })
                    val copied = ArrayList(lines)
                    copied[index] = "$pre-javaagent:$yop$next"
                    File(target).bufferedWriter().use { writer ->
                        var writed = false
                        copied.forEach { line ->
                            if (writed) {
                                writer.append('\n')
                            }
                            writed = true
                            writer.append(line)
                        }
                    }
                }
                return@lineLoop
            }
        }
    }
    exitProcess(0)
}





















