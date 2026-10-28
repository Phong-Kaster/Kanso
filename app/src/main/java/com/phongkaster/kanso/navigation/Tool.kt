package com.phongkaster.kanso.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.phongkaster.kanso.R
import kotlinx.serialization.Serializable

/**
 * The five tools Kanso offers, each with the string resource for its label and the drawable
 * resource for its icon. Home offers each as a shortcut and each opens its own tool screen.
 *
 * Taking a photo with the camera is not a tool: it makes a photo instead of working on one, so
 * the floating bar's Camera button opens it and the `Camera` navigation key stands for it.
 *
 * It is @Serializable because a tool travels inside navigation keys such as [Gallery], and every
 * key on the back stack must be saveable so it survives rotation and process death.
 *
 * The icons are Material Symbols Rounded vector drawables in `res/drawable/`. A screen draws one
 * with `Icon(painterResource(tool.iconRes), contentDescription = ...)` so it takes the colour of
 * the text around it instead of the white the file paints with.
 */
@Serializable
enum class Tool(@StringRes val labelRes: Int, @DrawableRes val iconRes: Int) {
    /** General photo editing. */
    EDIT(R.string.tool_edit, R.drawable.ic_edit),

    /** Cutting the subject out of its background. */
    REMOVE_BACKGROUND(R.string.tool_remove_background, R.drawable.ic_background_replace),

    /** Automatic quality improvement. */
    ENHANCE(R.string.tool_enhance, R.drawable.ic_auto_fix_high),

    /** Face and skin beautifying. */
    BEAUTIFY(R.string.tool_beautify, R.drawable.ic_face_retouching_natural),

    /** Artistic filters and styles. */
    ARTISTIC(R.string.tool_artistic, R.drawable.ic_brush),
}
