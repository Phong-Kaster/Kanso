package com.phongkaster.kanso.navigation

import androidx.annotation.StringRes
import com.phongkaster.kanso.R
import kotlinx.serialization.Serializable

/**
 * The six tools Kanso offers on its Home screen, each with the string resource for its label.
 *
 * It is @Serializable because a tool travels inside navigation keys such as [Gallery], and every
 * key on the back stack must be saveable so it survives rotation and process death.
 */
@Serializable
enum class Tool(@StringRes val labelRes: Int) {
    /** General photo editing. */
    EDIT(R.string.tool_edit),

    /** Cutting the subject out of its background. */
    REMOVE_BACKGROUND(R.string.tool_remove_background),

    /** Automatic quality improvement. */
    ENHANCE(R.string.tool_enhance),

    /** Face and skin retouching. */
    RETOUCH(R.string.tool_retouch),

    /** Artistic filters and styles. */
    ARTISTIC(R.string.tool_artistic),

    /** Taking a new photo with the camera. */
    CAMERA(R.string.tool_camera),
}
