/*
 * Copyright (c) 2018-2020 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2020/08/28 21:15:40
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/WrappedLogger.kt
 */

package io.github.karlatemp.yop

import org.slf4j.helpers.MarkerIgnoringBase
import java.nio.charset.Charset

private fun String.charset(): Charset {
    return Charset.forName(System.getProperty(this))
}

object WrappedLogger : MarkerIgnoringBase() {
    override fun getName(): String = "YggdrasilOfficialProxy"
    private val output get() = System.out
//            = PrintWriter(
//            OutputStreamWriter(FileOutputStream(FileDescriptor.out),
//                    kotlin.runCatching {
//                        "sun.stdout.encoding".charset()
//                    }.recoverCatching {
//                        "sun.stderr.encoding".charset()
//                    }.recoverCatching {
//                        "file.encoding".charset()
//                    }.getOrElse { Charsets.ISO_8859_1 }
//            )
//    )

    override fun trace(msg: String?, t: Throwable?) {
        if (traceEnabled) {
            output.println("[YggdrasilOfficialProxy] [TRACE] $msg")
            t?.printStackTrace(output)
            output.flush()
        }
    }

    override fun info(msg: String?, t: Throwable?) {
        output.println("[YggdrasilOfficialProxy] [ INFO] $msg")
        t?.printStackTrace(output)
        output.flush()
    }

    override fun warn(msg: String?, t: Throwable?) {
        output.println("[YggdrasilOfficialProxy] [ WARN] $msg")
        t?.printStackTrace(output)
        output.flush()
    }

    override fun error(msg: String?, t: Throwable?) {
        output.println("[YggdrasilOfficialProxy] [ERROR] $msg")
        t?.printStackTrace(output)
        output.flush()
    }

    @JvmField
    val traceEnabled =
            System.getProperty("yggdrasilofficialproxy.verbose") !== null
                    || System.getenv("YGGDRASIL_OFFICIAL_PROXY_VERBOSE") !== null

    fun debug(msg: () -> String) {
        if (traceEnabled) trace(msg())
    }

    override fun isTraceEnabled(): Boolean = traceEnabled
    override fun trace(msg: String?) {
        trace(msg, null)
    }

    override fun trace(format: String, arg: Any?) {
        trace(String.format(format, arg))
    }

    override fun trace(format: String, arg1: Any?, arg2: Any?) {
        trace(String.format(format, arg1, arg2))
    }

    override fun trace(format: String, vararg arguments: Any?) {
        trace(msg = String.format(format, *arguments))
    }

    override fun isDebugEnabled(): Boolean = isTraceEnabled

    override fun debug(msg: String) {
        trace(msg)
    }

    override fun debug(format: String, arg: Any?) {
        trace(format, arg)
    }

    override fun debug(format: String, arg1: Any?, arg2: Any?) {
        trace(format, arg1, arg2)
    }

    override fun debug(format: String, vararg arguments: Any?) {
        trace(format, *arguments)
    }

    override fun debug(msg: String?, t: Throwable?) {
        trace(msg, t)
    }

    override fun isInfoEnabled(): Boolean = true

    override fun info(msg: String?) {
        info(msg, null)
    }

    override fun info(format: String, arg: Any?) {
        info(String.format(format, arg))
    }

    override fun info(format: String, arg1: Any?, arg2: Any?) {
        info(String.format(format, arg1, arg2))
    }

    override fun info(format: String, vararg arguments: Any?) {
        info(msg = String.format(format, *arguments))
    }


    override fun isWarnEnabled(): Boolean = true

    override fun warn(msg: String?) {
        warn(msg, null)
    }

    override fun warn(format: String, arg: Any?) {
        warn(String.format(format, arg))
    }

    override fun warn(format: String, vararg arguments: Any?) {
        warn(msg = String.format(format, *arguments))
    }

    override fun warn(format: String, arg1: Any?, arg2: Any?) {
        warn(String.format(format, arg1, arg2))
    }


    override fun isErrorEnabled(): Boolean = true

    override fun error(msg: String?) {
        error(msg, null)
    }

    override fun error(format: String, arg: Any?) {
        error(String.format(format, arg))
    }

    override fun error(format: String, vararg arguments: Any?) {
        error(msg = String.format(format, *arguments))
    }

    override fun error(format: String, arg1: Any?, arg2: Any?) {
        error(String.format(format, arg1, arg2))
    }

}