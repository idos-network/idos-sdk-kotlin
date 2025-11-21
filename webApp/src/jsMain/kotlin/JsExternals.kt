/**
 * External declarations for JavaScript libraries.
 */

/**
 * External declarations for Reown AppKit JavaScript library.
 */
@JsModule("@reown/appkit")
@JsNonModule
external object ReownAppKit {
    fun createAppKit(config: dynamic): dynamic
}

/**
 * External declarations for Reown AppKit networks.
 */
@JsModule("@reown/appkit/networks")
@JsNonModule
external object ReownNetworks {
    val mainnet: dynamic
    val sepolia: dynamic
    val polygon: dynamic
    val arbitrum: dynamic
}

/**
 * External declaration for Ethers adapter.
 */
@JsModule("@reown/appkit-adapter-ethers")
@JsNonModule
external object EthersAdapterModule {
    @JsName("EthersAdapter")
    class EthersAdapter
}

/**
 * External declarations for ethers.js library.
 */
@JsModule("ethers")
@JsNonModule
external object EthersLib {
    class BrowserProvider(provider: dynamic)
}
