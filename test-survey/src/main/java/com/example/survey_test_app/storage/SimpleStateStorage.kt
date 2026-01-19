package com.example.survey_test_app.storage

import kaist.iclab.tracker.storage.core.StateStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SimpleStateStorage<T>(
    initialState: T
) : StateStorage<T> {
    private val _state = MutableStateFlow(initialState)
    override val stateFlow: StateFlow<T> = _state.asStateFlow()

    override fun get(): T {
        return stateFlow.value
    }

    override fun set(value: T) {
        _state.update { value }
    }
}