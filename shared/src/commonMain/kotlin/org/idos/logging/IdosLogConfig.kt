package org.idos.logging

/**
 * Configuration for idOS SDK logging.
 *
 * Controls both HTTP client logging and internal SDK logging.
 * Supports both DSL-style and fluent builder APIs for configuring multiple log sinks.
 *
 * ## Example Usage:
 *
 * ### Default (platform logging with INFO level)
 * ```kotlin
 * val client = IdosClient.create(
 *     baseUrl = "...",
 *     chainId = "...",
 *     signer = signer
 * )
 * ```
 *
 * ### DSL-style builder (recommended)
 * ```kotlin
 * val logConfig = IdosLogConfig.build {
 *     httpLogLevel = HttpLogLevel.HEADERS
 *     sdkLogLevel = SdkLogLevel.DEBUG
 *
 *     // Platform default (Logcat/OSLog/console)
 *     platformSink()
 * }
 * ```
 *
 * ### Multiple sinks with DSL (Timber + Crashlytics on Android)
 * ```kotlin
 * val logConfig = IdosLogConfig.build {
 *     httpLogLevel = HttpLogLevel.HEADERS
 *     sdkLogLevel = SdkLogLevel.DEBUG
 *
 *     // Timber for all logs
 *     callbackSink(tagPrefix = "idOS-") { level, tag, message ->
 *         Timber.tag(tag).log(level.toAndroidPriority(), message)
 *     }
 *
 *     // Crashlytics for errors only
 *     callbackSink { level, tag, message ->
 *         if (level == SdkLogLevel.ERROR) {
 *             FirebaseCrashlytics.getInstance().log("[$tag] $message")
 *         }
 *     }
 * }
 * ```
 *
 * ### Fluent builder API (alternative)
 * ```kotlin
 * val logConfig = IdosLogConfig.builder()
 *     .httpLogLevel(HttpLogLevel.HEADERS)
 *     .sdkLogLevel(SdkLogLevel.DEBUG)
 *     .addPlatformSink()
 *     .addCallbackSink(tagPrefix = "idOS-") { level, tag, message ->
 *         Timber.tag(tag).log(level.toAndroidPriority(), message)
 *     }
 *     .build()
 * ```
 *
 * @param httpLogLevel Log level for HTTP requests/responses
 * @param sdkLogLevel Log level for SDK operations
 * @param logSinks List of log sinks (destinations) for SDK logs
 */
@ConsistentCopyVisibility
data class IdosLogConfig internal constructor(
    val httpLogLevel: HttpLogLevel,
    val sdkLogLevel: SdkLogLevel,
    val logSinks: List<LogSink>,
) {
    companion object {
        /**
         * Create a new builder for IdosLogConfig.
         */
        fun builder(): Builder = Builder()

        /**
         * Build IdosLogConfig using DSL-style syntax.
         *
         * Example:
         * ```kotlin
         * val logConfig = IdosLogConfig.build {
         *     httpLogLevel = HttpLogLevel.HEADERS
         *     sdkLogLevel = SdkLogLevel.DEBUG
         *
         *     callbackSink(tagPrefix = "idOS-") { level, tag, message ->
         *         Timber.tag(tag).log(level.toAndroidPriority(), message)
         *     }
         *
         *     callbackSink { level, tag, message ->
         *         if (level == SdkLogLevel.ERROR) {
         *             Crashlytics.log("[$tag] $message")
         *         }
         *     }
         * }
         * ```
         */
        inline fun build(block: Builder.() -> Unit): IdosLogConfig = Builder().apply(block).build()
    }

    /**
     * Builder for IdosLogConfig with fluent API and DSL support.
     */
    class Builder {
        /**
         * HTTP log level (default: NONE).
         */
        var httpLogLevel: HttpLogLevel = HttpLogLevel.NONE

        /**
         * SDK log level (default: INFO).
         */
        var sdkLogLevel: SdkLogLevel = SdkLogLevel.INFO

        private val logSinks = mutableListOf<LogSink>()

        /**
         * Add platform default log sink (Logcat/OSLog/console).
         */
        fun platformSink() {
            logSinks.add(PlatformLogSink())
        }

        /**
         * Add a callback-based log sink.
         *
         * @param tagPrefix Optional prefix to add to all tags (e.g., "idOS-")
         * @param callback Logging callback
         */
        fun callbackSink(
            tagPrefix: String? = null,
            callback: (level: SdkLogLevel, tag: String, message: String) -> Unit,
        ) {
            logSinks.add(CallbackLogSink(tagPrefix, callback))
        }

        /**
         * Add a custom log sink.
         */
        fun sink(sink: LogSink) {
            logSinks.add(sink)
        }

        // Fluent API methods (for backward compatibility)

        /**
         * Set HTTP log level (fluent API).
         */
        fun httpLogLevel(level: HttpLogLevel) = apply { this.httpLogLevel = level }

        /**
         * Set SDK log level (fluent API).
         */
        fun sdkLogLevel(level: SdkLogLevel) = apply { this.sdkLogLevel = level }

        /**
         * Add platform default log sink (fluent API).
         */
        fun addPlatformSink() = apply { platformSink() }

        /**
         * Add a callback-based log sink (fluent API).
         */
        fun addCallbackSink(
            tagPrefix: String? = null,
            callback: (level: SdkLogLevel, tag: String, message: String) -> Unit,
        ) = apply { callbackSink(tagPrefix, callback) }

        /**
         * Add a custom log sink (fluent API).
         */
        fun addSink(sink: LogSink) = apply { sink(sink) }

        /**
         * Build the IdosLogConfig.
         *
         * If no sinks were added, platform default is used.
         */
        fun build(): IdosLogConfig {
            val finalSinks =
                if (logSinks.isEmpty()) {
                    listOf(PlatformLogSink()) // Default to platform logging
                } else {
                    logSinks.toList()
                }
            return IdosLogConfig(httpLogLevel, sdkLogLevel, finalSinks)
        }
    }
}

/**
 * HTTP client logging levels.
 *
 * Controls how much information is logged for HTTP requests and responses.
 */
enum class HttpLogLevel {
    /** No HTTP logging */
    NONE,

    /** Log request/response lines only (method, URL, status) */
    INFO,

    /** Log request/response lines and headers */
    HEADERS,

    /** Log request/response lines and body content */
    BODY,

    /** Log everything (headers + body) */
    ALL,
}

/**
 * SDK internal logging levels.
 *
 * Controls logging for SDK operations like credential management, encryption, etc.
 */
enum class SdkLogLevel {
    /** Detailed trace information */
    VERBOSE,

    /** Debugging information */
    DEBUG,

    /** General information */
    INFO,

    /** Warnings */
    WARN,

    /** Errors */
    ERROR,

    /** No logging */
    NONE,
}
