import Foundation
import Combine

/// Protocol defining base ViewModel behavior
/// Matches Android's BaseViewModel abstract class
protocol BaseViewModel: ObservableObject {
    associatedtype State
    associatedtype Event

    var state: State { get set }
    func onEvent(_ event: Event)
}