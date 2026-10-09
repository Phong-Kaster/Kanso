package com.phongkaster.kanso.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The camera screen. The shot the person keeps opens [tool], so Home passes the Edit tool and a
 * gallery passes its own. The tool is the key's only argument so it is saved with the back stack.
 */
@Serializable
data class Camera(val tool: Tool) : NavKey
