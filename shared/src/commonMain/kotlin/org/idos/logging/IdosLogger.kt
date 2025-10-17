package org.idos.logging

import co.touchlab.kermit.LogWriter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.platformLogWriter
import io.ktor.client.plugins.logging.LogLevel

/**
 * Internal SDK logger.
 *
 * Wraps Kermit for cross-platform logging with optional consumer control.
 * Uses platform defaults (Logcat/OSLog/console) unless a custom logger is provided.
 */
@PublishedApi
internal object IdosLogger {
    private var logger: Logger = createDefaultLogger()
    private var minLevel: SdkLogLevel = SdkLogLevel.INFO

    /**
     * Configure the logger with consumer settings.
     *
     * Should be called once during SDK initialization.
     */
    fun configure(config: IdosLogConfig) {
        minLevel = config.sdkLogLevel

        // Convert log sinks to Kermit LogWriters
        val logWriters = config.logSinks.map { it.toLogWriter() }

        logger =
            Logger(
                config =
                    StaticConfig(
                        logWriterList = logWriters,
                        minSeverity = config.sdkLogLevel.toKermitSeverity(),
                    ),
                tag = "idOS-SDK",
            )
    }

    /**
     * Create a tagged logger for a specific component.
     */
    fun withTag(tag: String): Logger = logger.withTag(tag)

    /**
     * Log verbose message.
     */
    fun v(
        tag: String = "",
        throwable: Throwable? = null,
        message: () -> String,
    ) {
        if (minLevel <= SdkLogLevel.VERBOSE) {
            logger.v(throwable = throwable, tag = tag, message = message)
        }
    }

    /**
     * Log debug message.
     */
    fun d(
        tag: String = "",
        throwable: Throwable? = null,
        message: () -> String,
    ) {
        if (minLevel <= SdkLogLevel.DEBUG) {
            logger.d(throwable = throwable, tag = tag, message = message)
        }
    }

    /**
     * Log info message.
     */
    fun i(
        tag: String = "",
        throwable: Throwable? = null,
        message: () -> String,
    ) {
        if (minLevel <= SdkLogLevel.INFO) {
            logger.i(throwable = throwable, tag = tag, message = message)
        }
    }

    /**
     * Log warning message.
     */
    fun w(
        tag: String = "",
        throwable: Throwable? = null,
        message: () -> String,
    ) {
        if (minLevel <= SdkLogLevel.WARN) {
            logger.w(throwable = throwable, tag = tag, message = message)
        }
    }

    /**
     * Log error message.
     */
    fun e(
        tag: String = "",
        throwable: Throwable? = null,
        message: () -> String,
    ) {
        if (minLevel <= SdkLogLevel.ERROR) {
            logger.e(throwable = throwable, tag = tag, message = message)
        }
    }

    private fun createDefaultLogger(): Logger =
        Logger(
            config =
                StaticConfig(
                    logWriterList = listOf(platformLogWriter()),
                    minSeverity = Severity.Info,
                ),
            tag = "idOS-SDK",
        )
}

/**
 * Convert SdkLogLevel to Kermit Severity.
 */
internal fun SdkLogLevel.toKermitSeverity(): Severity =
    when (this) {
        SdkLogLevel.VERBOSE -> Severity.Verbose
        SdkLogLevel.DEBUG -> Severity.Debug
        SdkLogLevel.INFO -> Severity.Info
        SdkLogLevel.WARN -> Severity.Warn
        SdkLogLevel.ERROR -> Severity.Error
        SdkLogLevel.NONE -> Severity.Assert // Highest level, effectively disables logging
    }

/**
 * Convert HttpLogLevel to Ktor LogLevel.
 */
internal fun HttpLogLevel.toKtorLevel(): LogLevel =
    when (this) {
        HttpLogLevel.NONE -> LogLevel.NONE
        HttpLogLevel.INFO -> LogLevel.INFO
        HttpLogLevel.HEADERS -> LogLevel.HEADERS
        HttpLogLevel.BODY -> LogLevel.BODY
        HttpLogLevel.ALL -> LogLevel.ALL
    }
