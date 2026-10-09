package com.phongkaster.kanso.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.material3.MotionScheme
import androidx.compose.ui.unit.IntOffset
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.scene.Scene

// How Kanso moves from one screen to the next.
//
// NavDisplay (called in KansoNavHost.kt) draws the screen of the last key on the back stack. When
// the back stack changes it does not simply swap the old screen for the new one: it animates from
// one to the other, and it asks the app how, through three parameters. The three functions in
// this file build what Kanso passes to them:
// - kansoPushTransition for `transitionSpec`: a screen opens (a key is added);
// - kansoPopTransition for `popTransitionSpec`: a screen closes (the top key is removed);
// - kansoPredictivePopTransition for `predictivePopTransitionSpec`: the person is in the middle
//   of the back gesture, before they have let go.
// Each function's KDoc explains its part, the first one the ideas the three share.

/**
 * Returns the `transitionSpec` Kanso gives NavDisplay: how the screens move on a push, when a key
 * is added to the back stack and a new screen opens. The new screen slides in from the right edge,
 * across one full screen width, while it fades in; the screen it covers slides a quarter of the
 * width to the left and fades out, so the eye follows the new screen while the old one steps back.
 *
 * What a transition spec is: NavDisplay shows its screens through Compose's `AnimatedContent`, the
 * container that animates from one piece of content to the next. Each time the back stack
 * changes, NavDisplay calls a spec lambda to ask how to make the change, and the lambda returns a
 * `ContentTransform`: an enter transition for the screen that arrives, joined by `togetherWith`
 * to an exit transition for the screen that leaves. Transitions add up with `+`, so a slide plus a
 * fade runs both at once. The lambda runs with an `AnimatedContentTransitionScope<Scene<NavKey>>`
 * as its receiver (its `this`), which says which scene is leaving and which is arriving; a Scene
 * is what NavDisplay draws for the top of the back stack, in Kanso always one single screen.
 * Kanso's lambdas never need to look at it, because every screen moves the same way.
 *
 * Why springs: the slides and the fades take their timing from [motionScheme], which in the app is
 * the motion scheme of Kanso's theme, `MotionScheme.expressive()` (see KansoTheme). Its
 * animations are springs rather than timed curves: a spring pulls the screen towards its place
 * with a given stiffness and damping, so the move starts quickly, slows down as it arrives and,
 * for the spatial springs, overshoots by a hair before it rests. That is the Material 3 Expressive
 * feel, and because every Material component reads the same scheme, a screen change moves like
 * the buttons and the toolbar inside the screens. `defaultSpatialSpec` is the spring for things
 * that move (the slides); `defaultEffectsSpec` is the spring for things that only change how they
 * look (the fades), and it never overshoots, so a fade never goes past fully visible. Both settle
 * in under a second. That matters beyond looks: a transition that never settled would keep every
 * test that waits for the app to be idle waiting for ever.
 *
 * Why the motion scheme is a parameter: the theme's scheme is read with `MaterialTheme.motionScheme`,
 * which only works while Compose is drawing, inside a composable function. The spec lambda is not
 * a composable function: NavDisplay calls it later, from inside the animation. So the caller (the
 * navigation display in KansoNavHost.kt) reads the scheme while it draws and passes it in; this
 * function takes the two springs out of it straight away, and the lambda it returns keeps only
 * those springs. That also lets a test hand it any scheme. Calling it again on every redraw is
 * cheap: it reads two springs and builds one small lambda.
 */
fun kansoPushTransition(
    motionScheme: MotionScheme,
): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform {
    // Read the springs now, while the caller is drawing; the lambda below only captures them.
    // Slides animate an IntOffset (a position in whole pixels), fades a Float (the alpha).
    val slide: FiniteAnimationSpec<IntOffset> = motionScheme.defaultSpatialSpec()
    val fade: FiniteAnimationSpec<Float> = motionScheme.defaultEffectsSpec()
    return {
        // The arriving screen starts one full width to the right (off screen) and slides to 0.
        val enter = slideInHorizontally(animationSpec = slide, initialOffsetX = { fullWidth -> fullWidth }) +
            fadeIn(animationSpec = fade)
        // The leaving screen moves a quarter of its width to the left as it fades away.
        val exit = slideOutHorizontally(animationSpec = slide, targetOffsetX = { fullWidth -> -fullWidth / 4 }) +
            fadeOut(animationSpec = fade)
        enter togetherWith exit
    }
}

/**
 * Returns the `popTransitionSpec` Kanso gives NavDisplay: how the screens move on a pop, when the
 * top key is removed (Back, or a screen that closes itself) and the screen underneath comes back.
 * It is the push played backwards: the leaving screen slides out to the right, a full width, and
 * fades; the screen underneath comes back from a quarter of the width on the left and fades in.
 * During a pop NavDisplay draws the leaving screen above the returning one, so it looks as if the
 * top card were pulled off a pile.
 *
 * Push or pop: NavDisplay chooses between this spec and [kansoPushTransition] by comparing the
 * back stack before and after the change. It is a pop only when the bottom key is the same, the
 * new back stack is shorter, and it is exactly the start of the old one: `[Home, Settings]`
 * becoming `[Home]`, or Saved's Home button turning `[Home, Saved]` into `[Home]`. Every other
 * change is a push, including a change that removes keys and adds others: saving a photo turns
 * `[Home, Gallery, ToolPhoto]` into `[Home, Saved]`, so the Saved screen slides in like a new
 * screen, which is what a person expects of a step forward.
 *
 * Like [kansoPushTransition], it takes the motion scheme as a parameter and reads its two springs
 * at once, because the spec lambda is not a composable function and cannot read the theme itself.
 */
fun kansoPopTransition(
    motionScheme: MotionScheme,
): AnimatedContentTransitionScope<Scene<NavKey>>.() -> ContentTransform {
    // The same two springs as the push, read once here and captured by the lambda.
    val slide: FiniteAnimationSpec<IntOffset> = motionScheme.defaultSpatialSpec()
    val fade: FiniteAnimationSpec<Float> = motionScheme.defaultEffectsSpec()
    return {
        // The returning screen starts a quarter of its width to the left, where the push left it.
        val enter = slideInHorizontally(animationSpec = slide, initialOffsetX = { fullWidth -> -fullWidth / 4 }) +
            fadeIn(animationSpec = fade)
        // The leaving screen slides out of the right edge, the way it came in.
        val exit = slideOutHorizontally(animationSpec = slide, targetOffsetX = { fullWidth -> fullWidth }) +
            fadeOut(animationSpec = fade)
        enter togetherWith exit
    }
}

/**
 * Returns the `predictivePopTransitionSpec` Kanso gives NavDisplay: how the screens move while a
 * person is making the back gesture, before they have let go.
 *
 * What predictive back is: with gesture navigation, a person goes back by swiping in from the left
 * or the right edge of the screen. With predictive back the app shows where Back leads while the
 * finger is still moving: the top screen follows the finger and the screen underneath appears
 * behind it. Letting go once the gesture has gone far enough completes the Back (the top key is
 * popped, exactly as with the Back button); sliding back towards the edge and letting go cancels
 * it, and the top screen returns to its place. So a person can peek at the previous screen and
 * change their mind. Android shows this inside apps from Android 14 on, for apps that switch it on
 * with `android:enableOnBackInvokedCallback="true"` in their manifest, as Kanso does (the owner's
 * phone runs Android 14). With three-button navigation there is no gesture: the Back button simply
 * plays [kansoPopTransition].
 *
 * During the gesture NavDisplay does not play this transition against the clock: it seeks it,
 * holding it at the point that matches how far the gesture has progressed, and moves it as the
 * finger moves. Kanso uses its pop transition here, so the leaving screen slides to the right with
 * the finger while the screen underneath comes in from the left, and letting go simply carries on
 * the same movement to its end (or back to its start, when the gesture is cancelled).
 *
 * The lambda also receives the swipe edge, a number that says which edge of the screen the gesture
 * started from. Kanso moves its screens the same way from either edge, so it ignores it (the `_`).
 * Like the other two, this function takes the motion scheme as a parameter, because the spec
 * lambda is not a composable function.
 */
fun kansoPredictivePopTransition(
    motionScheme: MotionScheme,
): AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform {
    val pop = kansoPopTransition(motionScheme)
    // `pop(this)` runs the pop lambda with this lambda's own receiver, the same transition scope.
    return { _ -> pop(this) }
}
