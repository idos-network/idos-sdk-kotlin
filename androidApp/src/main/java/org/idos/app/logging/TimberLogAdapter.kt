package org.idos.app.logging

import android.util.Log
import org.idos.logging.SdkLogLevel
import timber.log.Timber

/**
 * Adapter that bridges idOS SDK logging to Timber.
 *
 * Maps SDK log levels to appropriate Timber/Android log levels.
 * Tag prefixing is handled by the SDK's CallbackLogSink.
 *
 * Usage with builder:
 * ```kotlin
 * val logConfig = IdosLogConfig.builder()
 *     .httpLogLevel(HttpLogLevel.HEADERS)
 *     .sdkLogLevel(SdkLogLevel.DEBUG)
 *     .addCallbackSink(tagPrefix = "idOS-", callback = TimberLogAdapter::log)
 *     .build()
 * ```
 *
 * Multiple sinks example:
 * ```kotlin
 * val logConfig = IdosLogConfig.builder()
 *     .sdkLogLevel(SdkLogLevel.DEBUG)
 *     // Timber for all logs
 *     .addCallbackSink(tagPrefix = "idOS-", callback = TimberLogAdapter::log)
 *     // Crashlytics for errors only
 *     .addCallbackSink { level, tag, message ->
 *         if (level == SdkLogLevel.ERROR) {
 *             FirebaseCrashlytics.getInstance().log("[$tag] $message")
 *         }
 *     }
 *     .build()
 * ```
 */
object TimberLogAdapter {
    /**
     * Log a message through Timber.
     *
     * @param level SDK log level
     * @param tag Tag (already prefixed by CallbackLogSink if configured)
     * @param message Log message
     */
    fun log(
        level: SdkLogLevel,
        tag: String,
        message: String,
    ) {
        when (level) {
            SdkLogLevel.VERBOSE -> Timber.tag(tag).v(message)
            SdkLogLevel.DEBUG -> Timber.tag(tag).d(message)
            SdkLogLevel.INFO -> Timber.tag(tag).i(message)
            SdkLogLevel.WARN -> Timber.tag(tag).w(message)
            SdkLogLevel.ERROR -> Timber.tag(tag).e(message)
            SdkLogLevel.NONE -> { /* No logging */ }
        }
    }

    /**
     * Alternative implementation using Android Log directly (bypassing Timber).
     * Useful if you want SDK logs separate from Timber's processing.
     */
    fun logDirect(
        level: SdkLogLevel,
        tag: String,
        message: String,
    ) {
        when (level) {
            SdkLogLevel.VERBOSE -> Log.v(tag, message)
            SdkLogLevel.DEBUG -> Log.d(tag, message)
            SdkLogLevel.INFO -> Log.i(tag, message)
            SdkLogLevel.WARN -> Log.w(tag, message)
            SdkLogLevel.ERROR -> Log.e(tag, message)
            SdkLogLevel.NONE -> { /* No logging */ }
        }
    }
}

