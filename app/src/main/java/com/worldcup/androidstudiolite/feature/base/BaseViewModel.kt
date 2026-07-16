package com.worldcup.androidstudiolite.feature.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<S, E>(initState: S) : ViewModel() {

    private val _state = MutableStateFlow(initState)
    val state = _state.asStateFlow()

    private val _effect = MutableSharedFlow<E>(extraBufferCapacity = 8)
    val effect: Flow<E> = _effect.asSharedFlow()

    private val _snackBar = MutableStateFlow<String?>(null)
    val snackBar = _snackBar.asStateFlow()

    protected fun <T> tryToExecute(
        callee: suspend () -> T,
        onSuccess: ((T) -> Unit)? = null,
        onError: (Throwable) -> Unit = {},
        inScope: CoroutineScope = viewModelScope,
    ): Job = inScope.launch {
        try {
            val result = callee()
            onSuccess?.invoke(result)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            e.message?.let { showSnackBar(it) }
            onError(e)
        }
    }

    protected fun <T> tryToCollect(
        callee: suspend () -> Flow<T>,
        onNewValue: (T) -> Unit,
        onError: (Throwable) -> Unit = {},
        onCompletion: (() -> Unit)? = null,
        inScope: CoroutineScope = viewModelScope,
    ): Job = inScope.launch {
        try {
            callee().collect { onNewValue(it) }
            onCompletion?.invoke()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            e.message?.let { showSnackBar(it) }
            onError(e)
        }
    }

    protected fun updateState(updater: (S) -> S) {
        _state.update(updater)
    }

    protected fun currentState(): S = _state.value

    protected fun sendNewEffect(newEffect: E) {
        viewModelScope.launch { _effect.emit(newEffect) }
    }

    private var snackBarJob: Job? = null

    protected fun showSnackBar(message: String) {
        snackBarJob?.cancel()
        snackBarJob = viewModelScope.launch {
            _snackBar.value = message
            delay(SNACK_BAR_CLEARING_TIMEOUT)
            _snackBar.value = null
        }
    }

    companion object {
        const val SNACK_BAR_CLEARING_TIMEOUT = 5000L
    }
}
