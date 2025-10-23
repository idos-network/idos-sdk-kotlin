import Foundation
import Combine

/// Base class for all ViewModels
/// Matches Android's BaseViewModel abstract class with State/Event pattern
class BaseViewModel<State, Event>: ObservableObject {
    @Published var state: State

    init(initialState: State) {
        self.state = initialState
    }

    /// Handle events - subclasses must override
    func onEvent(_ event: Event) {
        fatalError("onEvent(_:) must be overridden by subclass")
    }

    /// Update state - helper for atomic state updates
    func updateState(_ transform: (inout State) -> Void) {
        transform(&state)
    }
}