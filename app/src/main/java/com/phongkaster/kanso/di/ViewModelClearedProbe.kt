package com.phongkaster.kanso.di

import androidx.lifecycle.ViewModel
import java.util.concurrent.CopyOnWriteArraySet
import kotlin.reflect.KClass

/**
 * Remembers which kinds of ViewModel have been cleared, so a test can check that a screen's
 * ViewModel is thrown away when the screen leaves the back stack, and kept while it stays.
 *
 * Koin provides one shared instance (a singleton). It only stores a handful of class
 * references, so it costs nothing noticeable in the shipped app.
 */
class ViewModelClearedProbe {

    /** The classes of every ViewModel cleared so far; safe to touch from any thread. */
    private val clearedTypes = CopyOnWriteArraySet<KClass<out ViewModel>>()

    /** Records that [viewModel] has just been cleared. Call it from the ViewModel's onCleared. */
    fun recordCleared(viewModel: ViewModel) {
        clearedTypes += viewModel::class
    }

    /** Tells whether a ViewModel of class [type] has been cleared at least once. */
    fun wasCleared(type: KClass<out ViewModel>): Boolean = type in clearedTypes
}
