package org.idos.app.ui.screens.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.component.KoinComponent

abstract class BaseViewModel<State : Any, Event> : ViewModel(), KoinComponent {
    private val _state = MutableStateFlow(initialState())
    val state: StateFlow<State> = _state.asStateFlow()

    protected var currentState: State
        get() = _state.value
        set(value) {
            _state.value = value
        }

    protected inline fun updateState(update: State.() -> State) {
        currentState = currentState.update()
    }

    abstract fun initialState(): State

    abstract fun onEvent(event: Event)
}
