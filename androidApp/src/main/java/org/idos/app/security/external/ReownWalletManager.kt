package org.idos.app.security.external

import android.app.Application
import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import com.reown.appkit.client.models.request.Request
import com.reown.appkit.presets.AppKitChainsPresets
import com.reown.appkit.utils.EthUtils
import com.reown.util.bytesToHex
import com.reown.util.randomBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.idos.crypto.eip712.TypedData
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Manager for orchestrating Reown AppKit operations.
 * Provides high-level methods for wallet connection and signing.
 */
class ReownWalletManager(
    private val application: Application,
    private val projectId: String
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var isInitialized = false

    /**
     * Initialize Reown SDK (CoreClient and AppKit).
     * Must be called before any other operations.
     */
    fun initialize() {
        if (isInitialized) {
            Timber.d("ReownWalletManager already initialized")
            return
        }

        val appMetaData = Core.Model.AppMetaData(
            name = "idOS SDK",
            description = "",
            url = "https://idos.network",
            icons = listOf("https://idos.network/icon.png"),
            redirect = "idos-app://request",
//            linkMode = true
        )

        CoreClient.initialize(
            application = application,
            projectId = projectId,
            metaData = appMetaData,
        ) {
            Timber.e(it.throwable, "CoreClient initialization failed")
        }

        AppKit.initialize(Modal.Params.Init(core = CoreClient)) { error ->
            Timber.e(error.throwable, "AppKit initialization failed")
        }

        AppKit.setChains(AppKitChainsPresets.ethChains.values.toList())

        // Initialize delegate
        ReownDelegate.initialize()

        isInitialized = true
        Timber.d("ReownWalletManager initialized")
    }

    /**
     * Get the currently connected wallet address.
     */
    fun getConnectedAddress(): String? {
        return AppKit.getAccount()?.address
    }

    /**
     * Send a personal_sign request to the connected wallet.
     *
     * @param message Message to sign (will be hex encoded)
     * @return Signature as hex string
     */
    suspend fun personalSign(message: ByteArray): String {
        val address = getConnectedAddress()
            ?: throw IllegalStateException("Wallet not connected")

        val messageHex = "0x" + message.bytesToHex()

        val requestParams = Request(
            method = "personal_sign",
            params = """["$messageHex", "$address"]"""
        )

        return sendWalletRequest(requestParams, "personal_sign")
    }

    /**
     * Send an eth_signTypedData_v4 request to the connected wallet.
     *
     * @param typedData EIP-712 typed data to sign
     * @return Signature as hex string
     */
    suspend fun signTypedData(typedData: TypedData): String {
        val address = getConnectedAddress()
            ?: throw IllegalStateException("Wallet not connected")

        // Serialize TypedData to JSON string
        val escapedTypedData = typedData.toJsonString()

        val requestParams = Request(
            method = "eth_signTypedData_v4",
            params = """["$address", $escapedTypedData]"""
        )

        return sendWalletRequest(requestParams, "eth_signTypedData_v4")
    }

    /**
     * Disconnect from the current wallet session.
     */
    fun disconnect() {
        try {
            AppKit.disconnect(
                onSuccess = {
                    Timber.d("Disconnected from wallet")
                },
                onError = { error ->
                    Timber.e(error, "Failed to disconnect from wallet")
                }
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to disconnect")
        }
    }

    /**
     * Send a request to the wallet and await response.
     * Common handler for all wallet signing operations.
     *
     * @param requestParams The request to send
     * @param requestName Human-readable name for logging (e.g., "personal_sign")
     * @return Result string from the wallet
     */
    private suspend fun sendWalletRequest(
        requestParams: Request,
        requestName: String
    ): String = suspendCoroutine { continuation ->
        Timber.d("Sending $requestName request")

        var isResumed = false

        // Collect events in coroutine scope
        scope.launch {
            ReownDelegate.appKitEvents
                .filter { it is Modal.Model.SessionRequestResponse || it is Modal.Model.ExpiredRequest }
                .collect { event ->
                    if (isResumed) return@collect

                    when (event) {
                        is Modal.Model.SessionRequestResponse -> {
                            when (val result = event.result) {
                                is Modal.Model.JsonRpcResponse.JsonRpcResult -> {
                                    Timber.d("$requestName request successful")
                                    isResumed = true
                                    continuation.resume(result.result ?: "")
                                }
                                is Modal.Model.JsonRpcResponse.JsonRpcError -> {
                                    Timber.e("$requestName request failed: ${result.message}")
                                    isResumed = true
                                    continuation.resumeWithException(
                                        Exception("$requestName request failed: ${result.message}")
                                    )
                                }
                            }
                        }
                        is Modal.Model.ExpiredRequest -> {
                            Timber.e("$requestName request expired")
                            isResumed = true
                            continuation.resumeWithException(
                                Exception("$requestName request expired")
                            )
                        }
                        else -> {}
                    }
                }
        }

        // Send the request
        AppKit.request(
            request = requestParams,
            onSuccess = { _ ->
                Timber.d("Request sent successfully, awaiting response from delegate")
            },
            onError = { error: Throwable ->
                Timber.e(error, "Failed to send $requestName request")
                if (!isResumed) {
                    isResumed = true
                    continuation.resumeWithException(
                        Exception("Failed to send $requestName request: ${error.message}")
                    )
                }
            }
        )
    }
}
