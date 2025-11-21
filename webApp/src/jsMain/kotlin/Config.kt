/**
 * Application configuration.
 *
 * Sensitive values (like REOWN_PROJECT_ID) are loaded from environment variables
 * at build time via webpack. See webApp/.env.example for required variables.
 */
object Config {

    // ==========================================================================
    // idOS API
    // ==========================================================================

    /** idOS Kwil API base URL */
    const val IDOS_BASE_URL = "https://nodes.playground.idos.network"

    /** Kwil chain ID */
    const val CHAIN_ID = "kwil-testnet"

    // ==========================================================================
    // MPC (Multi-Party Computation)
    // ==========================================================================

    /** Partisia blockchain reader node URL */
    const val MPC_PARTISIA_URL = "https://partisia-reader-node.playground.idos.network:8080"

    /** MPC secret sharing contract address */
    const val MPC_CONTRACT_ADDRESS = "0223996d84146dbf310dd52a0e1d103e91bb8402b3"

    /** Total number of MPC nodes */
    const val MPC_TOTAL_NODES = 6

    /** Threshold for secret reconstruction */
    const val MPC_THRESHOLD = 4

    /** Number of potentially malicious nodes tolerated */
    const val MPC_MALICIOUS_NODES = 2

    // ==========================================================================
    // Reown (WalletConnect)
    // ==========================================================================

    /** Reown Cloud project ID - loaded from .env at build time */
    const val REOWN_PROJECT_ID: String = "dd0c266d74fcc668992f89e3d15e0d01"

    /** App name shown in wallet */
    const val APP_NAME = "idOS"

    /** App description shown in wallet */
    const val APP_DESCRIPTION = "idOS Identity Management"

    /** App icon URL */
    const val APP_ICON_URL = "https://app.idos.network/logo.png"

    /** Theme accent color */
    const val THEME_ACCENT = "#00D796"
}
