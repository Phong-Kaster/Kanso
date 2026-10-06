package com.phongkaster.kanso.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// Navigation 3 concepts used in this file:
// - A NavKey is a small value that names one screen (a "destination"), plus any arguments it
//   needs. The app keeps a list of these keys, the back stack; the last key is the screen shown.
// - Keys must be @Serializable so the back stack can be written into the saved state and read
//   back after rotation or after Android kills and restores the process.

/** The Home screen: the list of tools and the Settings entry. It is the first and bottom key. */
@Serializable
data object Home : NavKey

/**
 * The stand-in screen for a tool that is not built yet. It names the [tool] it stands for.
 */
@Serializable
data class Placeholder(val tool: Tool) : NavKey

/**
 * The photo gallery opened by a tool: the user picks a photo here to work on with [tool].
 *
 * The tool is the key's only argument, so it is saved with the back stack and the gallery comes
 * back for the same tool after rotation or process death.
 */
@Serializable
data class Gallery(val tool: Tool) : NavKey

/**
 * The screen where [tool] works on one photo, named by [photoUri].
 *
 * The photo travels inside the key, as the text form of its content Uri (a Uri itself is not
 * @Serializable). The key is saved with the back stack, so after rotation or process death the
 * same tool reopens on the same photo with nothing else to restore.
 */
@Serializable
data class ToolPhoto(val tool: Tool, val photoUri: String) : NavKey

/** The Settings screen. */
@Serializable
data object Settings : NavKey
