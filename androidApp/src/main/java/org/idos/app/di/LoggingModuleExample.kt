package org.idos.app.di

import android.content.pm.ApplicationInfo
import org.idos.app.logging.TimberLogAdapter
import org.idos.logging.HttpLogLevel
import org.idos.logging.IdosLogConfig
import org.idos.logging.SdkLogLevel
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * Example logging configurations for different scenarios.
 *
 * The main app uses `loggingModule` defined in AppModule.kt.
 * These examples show how to override for testing or different builds.
 */

/**
 * Logging config for unit/integration tests.
 * Logs everything to console for debugging tests.
 */
val testLoggingModule =
    module {
        single {
            IdosLogConfig.build {
                httpLogLevel = HttpLogLevel.ALL
                sdkLogLevel = SdkLogLevel.VERBOSE

                // Log to console in tests
                platformSink()
            }
        }
    }

/**
 * Logging config for production with Crashlytics.
 * Only logs errors to Crashlytics, minimal HTTP logging.
 */
val productionLoggingModule =
    module {
        single {
            IdosLogConfig.build {
                httpLogLevel = HttpLogLevel.NONE
                sdkLogLevel = SdkLogLevel.INFO

                // Local logging via Timber
                callbackSink(tagPrefix = "idOS-", callback = TimberLogAdapter::log)

                // Send errors to Crashlytics (uncomment when Firebase is added)
                // callbackSink { level, tag, message ->
                //     if (level == SdkLogLevel.ERROR) {
                //         FirebaseCrashlytics.getInstance().log("[$tag] $message")
                //     }
                // }
            }
        }
    }

/**
 * Logging config for QA/staging builds.
 * Detailed logging to both Timber and file for bug reports.
 */
val stagingLoggingModule =
    module {
        single {
            IdosLogConfig.build {
                httpLogLevel = HttpLogLevel.HEADERS
                sdkLogLevel = SdkLogLevel.DEBUG

                // Timber for logcat
                callbackSink(tagPrefix = "idOS-", callback = TimberLogAdapter::log)

                // File logging for bug reports (example)
                // callbackSink { level, tag, message ->
                //     FileLogger.write("${System.currentTimeMillis()} [$level] [$tag] $message")
                // }
            }
        }
    }

/**
 * Logging config that's completely silent (for benchmarking).
 */
val noLoggingModule =
    module {
        single {
            IdosLogConfig.build {
                httpLogLevel = HttpLogLevel.NONE
                sdkLogLevel = SdkLogLevel.NONE
                // No sinks = no logging
            }
        }
    }

/**
 * Example: How to use different configs in tests.
 *
 * ```kotlin
 * class MyTest {
 *     @Before
 *     fun setup() {
 *         startKoin {
 *             modules(
 *                 testLoggingModule,  // Use test logging
 *                 networkModule,
 *                 // ... other modules
 *             )
 *         }
 *     }
 * }
 * ```
 */

/**
 * Example: How to dynamically switch based on build variant.
 *
 * In App.kt:
 * ```kotlin
 * val loggingModule = when (BuildConfig.BUILD_TYPE) {
 *     "debug" -> loggingModule
 *     "staging" -> stagingLoggingModule
 *     "release" -> productionLoggingModule
 *     else -> loggingModule
 * }
 *
 * startKoin {
 *     modules(
 *         loggingModule,
 *         networkModule,
 *         // ... other modules
 *     )
 * }
 * ```
 */
