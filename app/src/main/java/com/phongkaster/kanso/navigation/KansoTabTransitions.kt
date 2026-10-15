package com.phongkaster.kanso.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MotionScheme
import androidx.navigation3.scene.Scene
import androidx.navigation3.ui.NavDisplay

/**
 * The metadata key that marks a navigation entry as a tab's. [kansoTabEntryMetadata] puts it on
 * every entry it is given to, and the tab fade looks for it on both screens of a move.
 */
private const val TAB_ENTRY_MARKER = "com.phongkaster.kanso.navigation.KansoTabEntry"

/**
 * Returns the metadata the navigation host gives the entries of the three tabs (Home, My edits
 * and Settings): moving from one tab to another fades, while every other move keeps the slides
 * of KansoTransitions.kt.
 *
 * What entry metadata is: when the navigation host lists its screens (`entry<Home>(metadata =
 * ...) { ... }`), each entry can carry a small map of extra facts. NavDisplay reads two of them,
 * made by `NavDisplay.transitionSpec { ... }` and `NavDisplay.popTransitionSpec { ... }`: a spec
 * of the entry's own, which NavDisplay asks before its own spec. On a push it asks the arriving
 * screen's entry, on a pop the leaving screen's. If that spec returns null, NavDisplay plays its
 * own spec, the slide.
 *
 * Why tab changes are pushes: going from My edits to Settings rewrites the back stack from
 * `[Home, MyEdits]` to `[Home, Settings]`. NavDisplay calls a change a pop only when the new list
 * is the start of the old one, so this is a push, and without this metadata Settings would slide
 * in as if it were a step deeper. Tabs sit side by side, not one above the other, so a slide
 * would tell the wrong story.
 *
 * The fade: the spec returned here gives a cross-fade (the new tab fades in while the old one
 * fades out, both in place, on the motion scheme's default effects spring, which never
 * overshoots), but only when BOTH screens of the move are tabs. A tab is recognised by a marker
 * this same map puts on its entry: the spec sees the two scenes (what NavDisplay draws for the
 * top of the back stack, here one screen each) and their entries, not the back stack's keys, so
 * the marker on the entry is how it knows a tab from any other screen. It is the same set of
 * screens `isTabKey` names, as long as the host gives this metadata exactly to the tab entries.
 * Any other pair (Home to a gallery, the camera back to Settings) returns null, so those moves
 * keep NavDisplay's slide. Back from Settings or My edits to Home, a pop between two tabs, fades
 * too; the predictive back gesture keeps its own spec.
 *
 * Like the specs in KansoTransitions.kt it takes the [motionScheme] as a parameter and reads its
 * spring at once, because the spec lambda is not a composable function and cannot read the theme
 * itself. The lambda is pure, since NavDisplay may call it more than once per move.
 */
fun kansoTabEntryMetadata(motionScheme: MotionScheme): Map<String, Any> {
    // Read the spring now, while the caller is drawing; the lambda below only captures it.
    val fade: FiniteAnimationSpec<Float> = motionScheme.defaultEffectsSpec()
    val tabFade: AnimatedContentTransitionScope<Scene<*>>.() -> ContentTransform? = {
        if (isTabScene(initialState) && isTabScene(targetState)) {
            fadeIn(animationSpec = fade) togetherWith fadeOut(animationSpec = fade)
        } else {
            // Not a move between two tabs: let NavDisplay play its own (sliding) spec.
            null
        }
    }
    return NavDisplay.transitionSpec(tabFade) +
        NavDisplay.popTransitionSpec(tabFade) +
        mapOf(TAB_ENTRY_MARKER to true)
}

/**
 * Tells whether [scene] shows a tab: whether the entry on top of it carries the marker that
 * [kansoTabEntryMetadata] puts on the tab entries.
 */
private fun isTabScene(scene: Scene<*>): Boolean =
    scene.entries.lastOrNull()?.metadata?.get(TAB_ENTRY_MARKER) == true
