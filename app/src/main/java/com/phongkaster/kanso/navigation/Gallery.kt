package com.phongkaster.kanso.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The photo gallery opened by a tool: the user picks a photo here to work on with [tool].
 *
 * The tool is the key's only argument, so it is saved with the back stack and the gallery comes
 * back for the same tool after rotation or process death.
 */
@Serializable
data class Gallery(val tool: Tool) : NavKey
