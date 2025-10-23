package org.idos.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Severity
import co.touchlab.kermit.platformLogWriter

/**
 * Represents a logging destination (sink) for SDK logs.
 *
 * Multiple sinks can be configured to route logs to different destinations
 * like Logcat, Timber, Crashlytics, file, etc.
 */
interface LogSink {
    /**
     * Convert this sink to a Kermit LogWriter.
     */
    fun toLogWriter(): LogWriter
}

/**
 * Platform default log sink (Logcat on Android, OSLog on iOS, console on JVM/JS).
 */
class PlatformLogSink : LogSink {
    override fun toLogWriter(): LogWriter = platformLogWriter()
}

/**
 * Custom callback-based log sink.
 *
 * Useful for integrating with app-specific logging frameworks like Timber.
 *
 * @param tagPrefix Optional prefix to add to all tags (e.g., "idOS-")
 * @param callback The logging callback (level, tag, message)
 */
class CallbackLogSink(
    private val tagPrefix: String? = null,
    private val callback: (level: SdkLogLevel, tag: String, message: String) -> Unit,
) : LogSink {
    override fun toLogWriter(): LogWriter = CallbackLogWriter(tagPrefix, callback)
}

/**
 * Kermit LogWriter that delegates to a callback.
 */
private class CallbackLogWriter(
    private val tagPrefix: String?,
    private val callback: (level: SdkLogLevel, tag: String, message: String) -> Unit,
) : LogWriter() {
    override fun log(
        severity: Severity,
        message: String,
        tag: String,
        throwable: Throwable?,
    ) {
        val level = severity.toSdkLogLevel()
        val prefixedTag =
            if (tagPrefix != null && tag.isNotEmpty()) {
                "$tagPrefix$tag"
            } else if (tagPrefix != null) {
                tagPrefix.removeSuffix("-") // Remove trailing dash if no tag
            } else {
                tag
            }

        val fullMessage =
            buildString {
                append(message)
                throwable?.let {
                    append(" | Exception: ${it.message}")
                }
            }

        callback(level, prefixedTag, fullMessage)
    }
}

/**
 * Convert Kermit Severity to SdkLogLevel.
 */
private fun Severity.toSdkLogLevel(): SdkLogLevel =
    when (this) {
        Severity.Verbose -> SdkLogLevel.VERBOSE
        Severity.Debug -> SdkLogLevel.DEBUG
        Severity.Info -> SdkLogLevel.INFO
        Severity.Warn -> SdkLogLevel.WARN
        Severity.Error -> SdkLogLevel.ERROR
        Severity.Assert -> SdkLogLevel.ERROR
    }
