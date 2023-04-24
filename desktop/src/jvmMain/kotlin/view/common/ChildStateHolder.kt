package view.common

import kotlinx.coroutines.flow.StateFlow

interface ChildStateHolder<T> {
    val state: StateFlow<T>
    fun setup()
    fun dispose()
}