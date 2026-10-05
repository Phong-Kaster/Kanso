package com.photo.editor.ui.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.photo.editor.common.util.tryWithoutCatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class BaseViewModel : ViewModel() {
    protected val TAG = javaClass.simpleName

    inline fun launchInDefault(crossinline block: suspend () -> Unit): Job {
        return viewModelScope.launch(Dispatchers.Default) { block() }
    }

    inline fun launchInMain(crossinline block: suspend () -> Unit): Job {
        return viewModelScope.launch(Dispatchers.Main) { block() }
    }

    inline fun launchInIO(crossinline block: suspend () -> Unit): Job {
        return viewModelScope.launch(Dispatchers.IO) { block() }
    }

    inline fun launchInDefaultTryCatch(crossinline block: suspend () -> Unit): Job {
        return viewModelScope.launch(Dispatchers.Default) {
            tryWithoutCatch {
                block()
            }
        }
    }

    inline fun launchInMainTryCatch(crossinline block: suspend () -> Unit): Job {
        return viewModelScope.launch(Dispatchers.Main) {
            tryWithoutCatch {
                block()
            }
        }
    }

    inline fun launchInIOTryCatch(crossinline block: suspend () -> Unit): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            tryWithoutCatch {
                block()
            }
        }
    }

    suspend fun <T> CoroutineScope.withMain(block: suspend () -> T): T {
        return withContext(Dispatchers.Main) { block() }
    }

    suspend fun <T> CoroutineScope.withDefault(block: suspend () -> T): T {
        return withContext(Dispatchers.Default) { block() }
    }

    suspend fun <T> CoroutineScope.withIO(block: suspend () -> T): T {
        return withContext(Dispatchers.IO) { block() }
    }
}