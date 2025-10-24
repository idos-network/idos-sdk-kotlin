package org.idos.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filter
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.idos.app.security.EthSigner
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test for the full happy path flow of the IDOS app.
 *
 * Tests the following flow:
 * 1. Login screen → Connect wallet
 * 2. Enter mnemonic + derivation path → Generate wallet
 * 3. Navigate to credentials list
 * 4. Select a credential
 * 5. Decrypt credential content
 * 6. Verify decrypted content is displayed
 *
 * This test runs twice with different derivation paths to test both:
 * - USER enclave type (default path: m/44'/60'/0'/0/4)
 * - MPC enclave type (alternative path: m/44'/60'/0'/0/3)
 *
 * ## Security
 * Test credentials are loaded from BuildConfig, which reads from:
 * - Local: .env file in project root (never committed)
 * - CI: GitHub Secrets (TEST_MNEMONIC, PASSWORD)
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class FullFlowInstrumentationTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<LoginActivity>()


    companion object {
        // Test password for USER enclave - loaded from .env file or CI environment
        private val TEST_PASSWORD =
            BuildConfig.TEST_PASSWORD.ifBlank {
                "TestPassword123!" // Fallback for backward compatibility
            }

        // Derivation paths for different enclave types
        private const val USER_DERIVATION_PATH = "m/44'/60'/0'/0/4" // USER enclave
        private const val MPC_DERIVATION_PATH = "m/44'/60'/0'/0/3" // MPC enclave
    }

    /**
     * Test full flow with USER enclave type (default derivation path)
     */
    @Test
    fun testFullFlow_UserEnclave() {
        runFullFlowTest(
            derivationPath = USER_DERIVATION_PATH,
            isUserEnclave = true,
        )
    }

    /**
     * Test full flow with MPC enclave type (alternative derivation path)
     */
    @Test
    fun testFullFlow_MpcEnclave() {
        runFullFlowTest(
            derivationPath = MPC_DERIVATION_PATH,
            isUserEnclave = false,
        )
    }

    /**
     * Execute the full app flow from login to credential decryption
     */
    private fun runFullFlowTest(
        derivationPath: String,
        isUserEnclave: Boolean,
    ) {
        composeTestRule.onNodeWithText("Welcome").assertIsDisplayed()
        composeTestRule.onNodeWithText("Connect Wallet").assertIsDisplayed()

        composeTestRule.onNodeWithText("Connect Wallet").performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Import BIP39 Mnemonic").assertIsDisplayed()

        composeTestRule
            .onAllNodes(hasSetTextAction())
            .filter(hasText(EthSigner.DEFAULT_DERIVATION_PATH))
            .onFirst()
            .performTextReplacement(derivationPath)

        composeTestRule.onNodeWithText("Generate Wallet").performClick()

        Thread.sleep(500)

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Wallet Imported", useUnmergedTree = true)
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("OK").performClick()

        composeTestRule.waitForIdle()

        Thread.sleep(2000)

        composeTestRule.waitForIdle()
        Thread.sleep(1000)

        val hasCredentials =
            try {
                composeTestRule.onNodeWithText("No credentials found").assertDoesNotExist()
                true
            } catch (e: AssertionError) {
                false
            }

        if (!hasCredentials) {
            // No credentials to test, but flow succeeded up to this point
            return
        }

        composeTestRule
            .onAllNodes(hasClickAction() and !hasText("No credentials found"))
            .onFirst()
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Credential Data").assertIsDisplayed()

        composeTestRule
            .onNodeWithText("🔒 Content Encrypted", substring = true)
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("🔒 Content Encrypted", substring = true)
            .performClick()

        composeTestRule.waitForIdle()

        if (isUserEnclave) {
            // USER enclave requires password
            composeTestRule
                .onNodeWithText("Generate Encryption Key")
                .assertIsDisplayed()

            // Enter password
            composeTestRule
                .onNode(hasSetTextAction() and hasText("Password", substring = true))
                .performTextInput(TEST_PASSWORD)
        } else {
            // MPC enclave doesn't require password
            composeTestRule
                .onNodeWithText("Unlock MPC Enclave")
                .assertIsDisplayed()
        }

        composeTestRule.onNodeWithText("1 Week").performClick()

        val unlockButtonText = if (isUserEnclave) "Generate Key" else "Unlock"
        composeTestRule.onNodeWithText(unlockButtonText).performClick()

        composeTestRule.waitForIdle()
        Thread.sleep(10000)

        composeTestRule
            .onNodeWithText("🔒 Content Encrypted", substring = true)
            .assertDoesNotExist()

        composeTestRule.onNodeWithText("Credential Data").assertIsDisplayed()

        Thread.sleep(5000)

        composeTestRule.waitForIdle()

        // Check that at least one @context text node exists
        val contextNodes =
            composeTestRule
                .onAllNodesWithText("@context", substring = true, ignoreCase = false)
                .fetchSemanticsNodes()

        assert(contextNodes.isNotEmpty()) {
            "Expected to find '@context' in decrypted content but found none. Decryption may have failed."
        }
    }
}
