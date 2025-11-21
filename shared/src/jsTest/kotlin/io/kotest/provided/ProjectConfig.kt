package io.kotest.provided

import io.kotest.core.config.AbstractProjectConfig

/**
 * Global test configuration for JS tests.
 * Simplified for minimal test setup.
 *
 * IMPORTANT: This file MUST be in the io.kotest.provided package
 * with the class name ProjectConfig for Kotest auto-discovery to work.
 */
class ProjectConfig : AbstractProjectConfig() {
    override suspend fun beforeProject() {
        println("HELLO - Starting tests")
    }

    override suspend fun afterProject() {
        println("GOODBYE - Tests finished")
    }
}
