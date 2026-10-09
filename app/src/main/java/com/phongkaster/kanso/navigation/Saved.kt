package com.phongkaster.kanso.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The screen shown after a photo is saved: the saved picture and what to do next.
 *
 * [photoUri] is the text of the saved picture's content Uri and [tool] is the tool that saved it,
 * so Edit another can open that tool's gallery again. Both are saved with the back stack.
 */
@Serializable
data class Saved(val tool: Tool, val photoUri: String) : NavKey
