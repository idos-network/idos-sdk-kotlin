package org.idos.app.security.external

import com.reown.android.Core
import com.reown.android.CoreClient
import com.reown.appkit.client.AppKit
import com.reown.appkit.client.Modal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Delegate for handling Core and AppKit callbacks.
 * Exposes event streams for observing wallet connection and request responses.
 */
object ReownDelegate : AppKit.ModalDelegate, CoreClient.CoreDelegate {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _appKitEvents: MutableSharedFlow<Modal.Model?> = MutableSharedFlow()
    val appKitEvents: SharedFlow<Modal.Model?> = _appKitEvents.asSharedFlow()

    private val _coreEvents: MutableSharedFlow<Core.Model> = MutableSharedFlow()
    val coreEvents: SharedFlow<Core.Model> = _coreEvents.asSharedFlow()

    private val _connectionState: MutableSharedFlow<Modal.Model.ConnectionState> = MutableSharedFlow(replay = 1)
    val connectionState: SharedFlow<Modal.Model.ConnectionState> = _connectionState.asSharedFlow()

    var selectedSessionTopic: String? = null
        private set

    fun initialize() {
        AppKit.setDelegate(this)
        CoreClient.setDelegate(this)
    }

    // AppKit.ModalDelegate implementations
    override fun onConnectionStateChange(state: Modal.Model.ConnectionState) {
        Timber.d("Connection state changed: $state")
        scope.launch {
            _connectionState.emit(state)
        }
    }

    override fun onSessionApproved(approvedSession: Modal.Model.ApprovedSession) {
        selectedSessionTopic = (approvedSession as Modal.Model.ApprovedSession.WalletConnectSession).topic
        Timber.d("Session approved - Topic: $selectedSessionTopic")
        scope.launch {
            _appKitEvents.emit(approvedSession)
        }
    }

    override fun onSessionRejected(rejectedSession: Modal.Model.RejectedSession) {
        Timber.d("Session rejected: ${rejectedSession.reason}")
        scope.launch {
            _appKitEvents.emit(rejectedSession)
        }
    }

    override fun onSessionUpdate(updatedSession: Modal.Model.UpdatedSession) {
        Timber.d("Session updated $updatedSession")
        scope.launch {
            _appKitEvents.emit(updatedSession)
        }
    }

    @Deprecated(
        "Use onSessionEvent(Modal.Model.Event) instead. Using both will result in duplicate events.",
        replaceWith = ReplaceWith("onEvent(event)")
    )
    override fun onSessionEvent(sessionEvent: Modal.Model.SessionEvent) {
    }

    override fun onSessionEvent(sessionEvent: Modal.Model.Event) {
        Timber.d("Session event $sessionEvent")
        scope.launch {
            _appKitEvents.emit(sessionEvent)
        }
    }

    override fun onSessionDelete(deletedSession: Modal.Model.DeletedSession) {
        Timber.d("Session deleted $deletedSession")
        selectedSessionTopic = null
        scope.launch {
            _appKitEvents.emit(deletedSession)
        }
    }

    override fun onSessionExtend(session: Modal.Model.Session) {
        Timber.d("Session extended $session")
        scope.launch {
            _appKitEvents.emit(session)
        }
    }

    override fun onSessionRequestResponse(response: Modal.Model.SessionRequestResponse) {
        Timber.d("Session request response received $response")
        scope.launch {
            _appKitEvents.emit(response)
        }
    }

    override fun onSessionAuthenticateResponse(sessionAuthenticateResponse: Modal.Model.SessionAuthenticateResponse) {
        Timber.d("Session authenticate response received $sessionAuthenticateResponse")
        if (sessionAuthenticateResponse is Modal.Model.SessionAuthenticateResponse.Result) {
            selectedSessionTopic = sessionAuthenticateResponse.session?.topic
        }
        scope.launch {
            _appKitEvents.emit(sessionAuthenticateResponse)
        }
    }

    override fun onProposalExpired(proposal: Modal.Model.ExpiredProposal) {
        Timber.d("Proposal expired $proposal")
        scope.launch {
            _appKitEvents.emit(proposal)
        }
    }

    override fun onRequestExpired(request: Modal.Model.ExpiredRequest) {
        Timber.d("Request expired $request")
        scope.launch {
            _appKitEvents.emit(request)
        }
    }

    override fun onError(error: Modal.Model.Error) {
        Timber.e(error.throwable, "AppKit error")
        scope.launch {
            _appKitEvents.emit(error)
        }
    }

    // CoreClient.CoreDelegate implementations
    @Deprecated("onPairingDelete callback has been deprecated. It will be removed soon. Pairing will disconnect automatically internally.")
    override fun onPairingDelete(deletedPairing: Core.Model.DeletedPairing) {
        // Deprecated - pairings are automatically deleted
    }

    @Deprecated("onPairingExpired callback has been deprecated. It will be removed soon. Pairing will disconnect automatically internally.")
    override fun onPairingExpired(expiredPairing: Core.Model.ExpiredPairing) {
        // Deprecated - pairings are automatically expired
    }

    override fun onPairingState(pairingState: Core.Model.PairingState) {
        Timber.d("Pairing state: ${pairingState.isPairingState}")
    }
}
