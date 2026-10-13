package com.phongkaster.kanso.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The My edits tab: every photo Kanso has saved, newest first. It is one of the three tabs of the
 * floating tab bar (see [KansoTab]), so it only ever sits directly above [Home] on the back stack.
 */
@Serializable
data object MyEdits : NavKey
