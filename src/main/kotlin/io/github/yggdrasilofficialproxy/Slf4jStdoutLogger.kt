/*
 * Copyright (c) 2018-2021 Karlatemp. All rights reserved.
 * @author Karlatemp <karlatemp@vip.qq.com> <https://github.com/Karlatemp>
 * @create 2021/09/03 21:33:10
 *
 * YggdrasilOfficialProxy/YggdrasilOfficialProxy.main/Slf4jStdoutLogger.kt
 */

package io.github.yggdrasilofficialproxy

import io.github.yggdrasilofficialproxy.YopConfiguration.LoggerLevel
import org.slf4j.helpers.MarkerIgnoringBase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Slf4jStdoutLogger : MarkerIgnoringBase() {
    var level: LoggerLevel = LoggerLevel.ALL

    private val FORMAT_REGEX = """\{(\d+)\}""".toRegex()
    private inline val NUL_THROWABLE: Throwable? get() = null
    private val D_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss")
    private fun doFormat(format: String?, vararg arguments: Any?): String {
        if (format == null) return "null"
        return format.replace(FORMAT_REGEX) { result ->
            result.groupValues[0].toIntOrNull()?.let { index ->
                if (index >= 0 && index < arguments.size) {
                    return@replace arguments[index].toString()
                }
            }
            result.value
        }
    }

    // TODO: Log Level
    override fun isTraceEnabled(): Boolean = level >= LoggerLevel.TRACE
    override fun isDebugEnabled(): Boolean = level >= LoggerLevel.DEBUG
    override fun isInfoEnabled(): Boolean = level >= LoggerLevel.INFO
    override fun isWarnEnabled(): Boolean = level >= LoggerLevel.WARN
    override fun isErrorEnabled(): Boolean = level >= LoggerLevel.ERROR

    private fun log0(type: LoggerLevel, msg: String?, t: Throwable?) {
        val sout = System.out
        synchronized(sout) {
            sout
                .append(D_FORMATTER.format(Instant.now().atZone(ZoneId.systemDefault())))
                .append(" ").append(type.echoType).append(" ")
                .println(msg)
            t?.printStackTrace(sout)
        }
    }

    override fun debug(msg: String?, t: Throwable?) {
        if (!isDebugEnabled) return
        log0(LoggerLevel.DEBUG, msg, t)
    }

    override fun info(msg: String?, t: Throwable?) {
        if (!isInfoEnabled) return
        log0(LoggerLevel.INFO, msg, t)

    }

    override fun warn(msg: String?, t: Throwable?) {
        if (!isWarnEnabled) return
        log0(LoggerLevel.WARN, msg, t)

    }

    override fun error(msg: String?, t: Throwable?) {
        if (!isErrorEnabled) return
        log0(LoggerLevel.ERROR, msg, t)

    }

    override fun trace(msg: String?, t: Throwable?) {
        if (!isTraceEnabled) return
        log0(LoggerLevel.TRACE, msg, t)
    }

    override fun trace(msg: String?) {
        if (!isTraceEnabled) return
        trace(msg, NUL_THROWABLE)
    }

    override fun trace(format: String?, arg: Any?) {
        if (!isTraceEnabled) return
        trace(doFormat(format, arg))
    }

    override fun trace(format: String?, arg1: Any?, arg2: Any?) {
        if (!isTraceEnabled) return
        trace(doFormat(format, arg1, arg2))
    }

    override fun trace(format: String?, vararg arguments: Any?) {
        if (!isTraceEnabled) return
        trace(doFormat(format, arguments = arguments))
    }

    override fun debug(msg: String?) {
        if (!isDebugEnabled) return
        debug(msg, NUL_THROWABLE)
    }

    override fun debug(format: String?, arg: Any?) {
        if (!isDebugEnabled) return
        debug(doFormat(format, arg))
    }

    override fun debug(format: String?, arg1: Any?, arg2: Any?) {
        if (!isDebugEnabled) return
        debug(doFormat(format, arg1, arg2))
    }

    override fun debug(format: String?, vararg arguments: Any?) {
        if (!isDebugEnabled) return
        debug(doFormat(format, arguments = arguments))
    }


    override fun info(msg: String?) {
        if (!isInfoEnabled) return
        info(msg, NUL_THROWABLE)
    }

    override fun info(format: String?, arg: Any?) {
        if (!isInfoEnabled) return
        info(doFormat(format, arg))
    }

    override fun info(format: String?, arg1: Any?, arg2: Any?) {
        if (!isInfoEnabled) return
        info(doFormat(format, arg1, arg2))
    }

    override fun info(format: String?, vararg arguments: Any?) {
        if (!isInfoEnabled) return
        info(doFormat(format, arguments = arguments))
    }


    override fun warn(msg: String?) {
        if (!isWarnEnabled) return
        warn(msg, NUL_THROWABLE)
    }

    override fun warn(format: String?, arg: Any?) {
        if (!isWarnEnabled) return
        warn(doFormat(format, arg))
    }

    override fun warn(format: String?, vararg arguments: Any?) {
        if (!isWarnEnabled) return
        warn(doFormat(format, arguments = arguments))
    }

    override fun warn(format: String?, arg1: Any?, arg2: Any?) {
        if (!isWarnEnabled) return
        warn(doFormat(format, arg1, arg2))
    }

    override fun error(msg: String?) {
        if (!isErrorEnabled) return
        error(msg, NUL_THROWABLE)
    }

    override fun error(format: String?, arg: Any?) {
        if (!isErrorEnabled) return
        error(doFormat(format, arg))
    }

    override fun error(format: String?, arg1: Any?, arg2: Any?) {
        if (!isErrorEnabled) return
        error(doFormat(format, arg1, arg2))
    }

    override fun error(format: String?, vararg arguments: Any?) {
        if (!isErrorEnabled) return
        error(doFormat(format, arguments = arguments))
    }

    inline fun info(msg: () -> String) {
        if (isInfoEnabled) info(msg())
    }

    inline fun warn(msg: () -> String) {
        if (isWarnEnabled) warn(msg())
    }

    inline fun error(msg: () -> String) {
        if (isErrorEnabled) error(msg())
    }

    inline fun debug(msg: () -> String) {
        if (isDebugEnabled) debug(msg())
    }

    inline fun trace(msg: () -> String) {
        if (isTraceEnabled) trace(msg())
    }
}