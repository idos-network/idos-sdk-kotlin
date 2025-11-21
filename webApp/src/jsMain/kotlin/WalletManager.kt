import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.idos.IdosClient
import org.w3c.dom.HTMLButtonElement
import signer.ReownSigner
import ui.ProfileView
import viewmodel.ProfileViewModel
import viewmodel.ProfileState as ViewModelProfileState

/**
 * Handles wallet connection and Reown AppKit setup.
 */
object WalletManager {

    fun setupConnectButton() {
        val connectBtn = document.getElementById("connect-btn") as? HTMLButtonElement
        connectBtn?.addEventListener("click", { connect() })
    }

    fun setupDisconnectButton() {
        val disconnectBtn = document.getElementById("disconnect-btn") as? HTMLButtonElement
        disconnectBtn?.addEventListener("click", { disconnect() })
    }

    fun initializeReownAppKit() {
        try {
            val hasInjectedProvider = js("typeof window.ethereum !== 'undefined'") as Boolean
            console.log("Injected provider detected:", hasInjectedProvider)
            if (hasInjectedProvider) {
                console.log("Provider:", js("window.ethereum"))
            }

            val metadata = js("{}")
            metadata.name = Config.APP_NAME
            metadata.description = Config.APP_DESCRIPTION
            metadata.url = window.location.origin
            metadata.icons = arrayOf(Config.APP_ICON_URL)

            val mainnet = ReownNetworks.mainnet
            val ethersAdapter = EthersAdapterModule.EthersAdapter()

            val config = js("{}")
            config.projectId = Config.REOWN_PROJECT_ID
            config.adapters = arrayOf(ethersAdapter)
            config.networks = arrayOf(mainnet)
            config.metadata = metadata
            config.themeMode = "dark"
            config.themeVariables = js("{}")
            config.themeVariables["--w3m-accent"] = Config.THEME_ACCENT

            config.features = js("{}")
            config.features.analytics = false
            config.features.email = false
            config.features.socials = arrayOf<String>()
            config.features.swaps = false
            config.features.onramp = false

            AppState.appKit = ReownAppKit.createAppKit(config)
            console.log("Reown AppKit initialized successfully")
        } catch (e: Exception) {
            console.error("Failed to initialize Reown AppKit:", e)
            console.error("Error message:", e.message)
        }
    }

    fun checkPreviousConnection() {
        AppState.scope.launch {
            try {
                val state = AppState.appKit.getState()
                val isConnected = state?.selectedNetworkId != null

                if (isConnected) {
                    console.log("Found active AppKit session")
                    AppState.appKit.subscribeAccount { account: dynamic ->
                        if (account != null && account.address != null) {
                            val address = account.address as String
                            console.log("Restoring session for address:", address)
                            AppState.scope.launch {
                                handleConnected(address)
                            }
                        }
                    }
                } else {
                    console.log("No active session found")
                    Navigation.showConnectPage()
                }
            } catch (e: Exception) {
                console.error("Failed to check previous connection:", e)
                Navigation.showConnectPage()
            }
        }
    }

    private fun connect() {
        try {
            console.log("Opening wallet connect modal...")

            if (AppState.appKit == null) {
                console.error("AppKit not initialized")
                return
            }

            AppState.appKit.subscribeAccount { account: dynamic ->
                console.log("Account changed:", account)
                if (account != null && account.address != null) {
                    val address = account.address as String
                    AppState.scope.launch {
                        handleConnected(address)
                    }
                }
            }

            val options = js("{}")
            options.view = "Connect"
            AppState.appKit.open(options)
        } catch (e: Exception) {
            console.error("Failed to connect wallet:", e)
        }
    }

    private suspend fun handleConnected(address: String) {
        try {
            console.log("Wallet connected:", address)

            if (AppState.isConnecting) {
                console.log("Connection already in progress, ignoring duplicate event")
                return
            }

            if (AppState.connectedAddress == address && AppState.client != null) {
                console.log("Already connected to this address")
                return
            }

            AppState.isConnecting = true
            AppState.connectedAddress = address

            window.localStorage.setItem("connectedAddress", address)

            val providerState = AppState.appKit.getState()
            console.log("Provider state:", providerState)

            val walletProvider = if (providerState != null && providerState.selectedNetworkId != null) {
                val caipNetworkId = providerState.selectedNetworkId.unsafeCast<String>()
                console.log("CAIP Network ID:", caipNetworkId)
                AppState.appKit.getWalletProvider()
            } else {
                throw Exception("No network selected")
            }

            if (walletProvider == null) {
                throw Exception("No wallet provider found")
            }

            console.log("Got wallet provider:", walletProvider)

            val ethersProvider = EthersLib.BrowserProvider(walletProvider)
            console.log("Created ethers provider:", ethersProvider)

            initializeIdosClient(address, ethersProvider)
            Navigation.showProfilePage()
        } catch (e: Exception) {
            console.error("Failed to handle wallet connection:", e)
            AppState.isConnecting = false
        }
    }

    private fun disconnect() {
        AppState.scope.launch {
            try {
                AppState.appKit?.disconnect()
                window.localStorage.removeItem("connectedAddress")
                AppState.reset()
                ProfileManager.clear()
                Navigation.showConnectPage()
                console.log("Wallet disconnected")
            } catch (e: Exception) {
                console.error("Failed to disconnect wallet:", e.message)
            }
        }
    }

    private fun initializeIdosClient(address: String, provider: dynamic) {
        AppState.scope.launch {
            try {
                val signer = ReownSigner(address, provider)
                AppState.signer = signer

                val client = IdosClient.create(
                    baseUrl = Config.IDOS_BASE_URL,
                    chainId = Config.CHAIN_ID,
                    signer = signer
                )
                AppState.client = client

                // Initialize profile and get configured orchestrator (pass signer for MPC)
                val orchestrator = ProfileManager.initializeProfile(address, client, signer)

                if (orchestrator == null) {
                    // No profile found
                    ProfileView.showError("No idOS profile found for this wallet. Please create a profile first.")
                    AppState.isConnecting = false
                    return@launch
                }

                AppState.orchestrator = orchestrator
                orchestrator.checkStatus()

                val viewModel = ProfileViewModel(client)
                AppState.viewModel = viewModel

                ProfileView.updateConnectedAddress(address)
                loadProfile(viewModel)

                AppState.isConnecting = false
            } catch (e: Exception) {
                console.error("Failed to initialize idOS client:", e.message)
                ProfileView.showError(e.message ?: "Failed to connect to idOS")
                AppState.isConnecting = false
            }
        }
    }

    private fun loadProfile(viewModel: ProfileViewModel) {
        AppState.scope.launch {
            ProfileView.showLoading()
            viewModel.loadProfile()

            viewModel.profileState.collect { state ->
                when (state) {
                    is ViewModelProfileState.Loading -> ProfileView.showLoading()
                    is ViewModelProfileState.Success -> {
                        ProfileView.renderCredentials(state.credentials)
                        ProfileView.renderWallets(state.wallets)
                    }
                    is ViewModelProfileState.Error -> ProfileView.showError(state.message)
                }
            }
        }
    }
}
