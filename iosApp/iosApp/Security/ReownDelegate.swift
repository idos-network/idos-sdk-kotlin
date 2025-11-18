//
// ReownDelegate.swift
// iosApp
//
// Delegate for handling Reown AppKit events.
// Exposes event streams for observing wallet connection and request responses.
// Based on AppKitLab example from reown-swift
//

import Foundation
import Combine
import os.log
import ReownAppKit

/// Manager for observing Reown/WalletConnect events
/// iOS AppKit uses Combine publishers for event handling
class ReownDelegate {
    static let shared = ReownDelegate()

    private let logger = Logger(subsystem: "org.idos.app", category: "ReownDelegate")
    private var cancellables = Set<AnyCancellable>()

    // Event publishers for reactive handling
    private let sessionApprovedSubject = PassthroughSubject<String, Never>() // Session topic
    var sessionApproved: AnyPublisher<String, Never> {
        sessionApprovedSubject.eraseToAnyPublisher()
    }

    private let requestResponseSubject = PassthroughSubject<String, Never>() // Response result
    var requestResponse: AnyPublisher<String, Never> {
        requestResponseSubject.eraseToAnyPublisher()
    }

    private let requestErrorSubject = PassthroughSubject<Error, Never>()
    var requestError: AnyPublisher<Error, Never> {
        requestErrorSubject.eraseToAnyPublisher()
    }

    private(set) var selectedSessionTopic: String?

    private init() {
        // Private to enforce singleton
    }

    func initialize() {
        // Subscribe to AppKit publishers
        // Session state publisher for connection changes
        AppKit.instance.sessionSettlePublisher
            .sink { [weak self] session in
                self?.logger.debug("Session settled: \(session.topic)")
                self?.handleSessionApproved(topic: session.topic)
            }
            .store(in: &cancellables)

        // Session delete publisher
        AppKit.instance.sessionDeletePublisher
            .sink { [weak self] _ in
                self?.logger.debug("Session deleted")
                self?.selectedSessionTopic = nil
            }
            .store(in: &cancellables)

        logger.debug("ReownDelegate initialized with AppKit publishers")
    }

    func handleSessionApproved(topic: String) {
        selectedSessionTopic = topic
        logger.debug("Session approved - Topic: \(topic)")
        sessionApprovedSubject.send(topic)
    }

    func handleRequestResponse(result: String) {
        logger.debug("Request response received")
        requestResponseSubject.send(result)
    }

    func handleRequestError(_ error: Error) {
        logger.error("Request error: \(error.localizedDescription)")
        requestErrorSubject.send(error)
    }
}
