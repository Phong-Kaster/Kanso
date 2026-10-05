package com.photo.editor.domain.datastructure

import androidx.compose.runtime.Stable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@Stable
class Memento<T> {
    private var states: Array<Any?> = Array(16) { null }

    @Volatile
    private var maxIndex = 0

    @Volatile
    private var currentIndex = -1

    private val _currentStateFlow = MutableStateFlow<T?>(null)
    val currentStateFlow = _currentStateFlow.asStateFlow()

    private val _canUndoFlow = MutableStateFlow(false)
    val canUndoFlow = _canUndoFlow.asStateFlow()

    private val _canRedoFlow = MutableStateFlow(false)
    val canRedoFlow = _canRedoFlow.asStateFlow()

    val currentState: T?
        get() = _currentStateFlow.value

    val canUndo: Boolean
        get() = _canUndoFlow.value

    val canRedo: Boolean
        get() = _canRedoFlow.value

    @Synchronized
    fun saveState(state: T) {
        currentIndex++

        // Expand states array if needed
        if (currentIndex >= states.size) {
            val expanded = Array<Any?>(states.size * 2) { null }
            System.arraycopy(states, 0, expanded, 0, states.size)
            states = expanded
        }

        maxIndex = currentIndex
        states[currentIndex] = state
        updateFlows()
    }

    @Synchronized
    fun undo(): T? {
        if (canUndo) {
            currentIndex--
            updateFlows()
        }
        return currentState
    }

    @Synchronized
    fun redo(): T? {
        if (canRedo) {
            currentIndex++
            updateFlows()
        }
        return currentState
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    private fun updateFlows() {
        _currentStateFlow.value = if (currentIndex < 0) null else states.getOrNull(currentIndex) as? T
        _canUndoFlow.value = currentIndex > 0
        _canRedoFlow.value = currentIndex < maxIndex
    }

    fun reset() {
        currentIndex = -1
        updateFlows()
    }
}
