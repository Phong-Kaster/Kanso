package com.phongkaster.kanso.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The screen where [tool] works on one photo, named by [photoUri].
 *
 * The photo travels inside the key, as the text form of its content Uri (a Uri itself is not
 * @Serializable). The key is saved with the back stack, so after rotation or process death the
 * same tool reopens on the same photo with nothing else to restore.
 */
@Serializable
data class ToolPhoto(val tool: Tool, val photoUri: String) : NavKey
