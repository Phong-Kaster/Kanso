package com.phongkaster.kanso.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * The Result screen: the picture Done just rendered, with Share, Save and Go home. It sits above
 * the tool screen, so system Back returns to the editing session with everything intact.
 *
 * The key carries nothing. A navigation key is saved as text and cannot hold a bitmap, so the
 * rendered picture travels in `RenderedPhotoStore`, a small object that lives as long as the
 * process. If the process was killed, the store is empty and Result says there is nothing to show.
 */
@Serializable
data object Result : NavKey
