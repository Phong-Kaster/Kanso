package com.phongkaster.kanso.ui.tool.component

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith

/**
 * The slide that swaps a two-level tool row between its top row and a group's row, shared by the
 * Beautify panel and the Edit screen so both move the same way. Call it from the
 * `transitionSpec` of an `AnimatedContent` whose state is "which group is open, or none".
 *
 * Going [inward] (into a group) the new row comes from the right and the old one leaves to the
 * left; going back out it is the other way round, so the person sees which way they went. Both
 * rows fade as they slide. The durations and easing are Compose's defaults.
 */
fun <S> AnimatedContentTransitionScope<S>.groupRowTransition(inward: Boolean): ContentTransform =
    (slideInHorizontally(initialOffsetX = { if (inward) it else -it }) + fadeIn()) togetherWith
        (slideOutHorizontally(targetOffsetX = { if (inward) -it else it }) + fadeOut())
