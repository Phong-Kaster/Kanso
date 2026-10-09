package com.phongkaster.kanso.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
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
 * How long, in milliseconds, the top screen's straight-line slide lasts during the back gesture
 * ([kansoPredictivePopTransition]): the only animation in this file that is not a spring.
 *
 * Why one second: during the back gesture NavDisplay turns the gesture's progress into a moment of
 * the transition's running time, and that running time is the length of the transition's longest
 * animation. The other animations of that transition are the screen underneath's slide and fade,
 * on springs that settle in less than 0.8 s at a phone's width (worked out from their stiffness
 * and damping and the width, not timed on a phone). A one-second slide is therefore the longest,
 * so the running time is exactly this slide's length: progress 0.2 is 0.2 s into it, which puts
 * the top screen a fifth of its width along, and progress 1 puts it at the right edge. The number
 * is chosen, not measured: any length a little longer than every spring of the transition would
 * do. If one of those springs ever took longer, the running time would become that spring's
 * length and the slide would reach the edge before the gesture's end.
 *
 * It is `internal`, not private, so that PredictiveBackGestureTest reads the same number instead of
 * keeping a copy that could go stale.
 */
internal const val PREDICTIVE_SLIDE_MILLIS = 1_000

/**
 * Returns the `predictivePopTransitionSpec` Kanso gives NavDisplay: how the screens move while a
 * person is making the back gesture, before they have let go. The top screen slides towards the
 * right edge along a straight line that the gesture's progress follows, instead of on a spring,
 * and does not fade; the screen underneath appears behind it, coming back from a quarter of the
 * width on the left as it fades in, on the same two springs [kansoPopTransition] uses to bring it
 * back.
 *
 * What predictive back is: with gesture navigation, a person goes back by swiping in from the left
 * or the right edge of the screen. With predictive back the app shows where Back leads while the
 * finger is still moving: the top screen moves aside and the screen underneath appears behind it.
 * Letting go once the gesture has gone far enough completes the Back (the top key is popped,
 * exactly as with the Back button); sliding back towards the edge and letting go cancels it, and
 * the top screen returns to its place. So a person can peek at the previous screen and change
 * their mind. Android shows this inside apps from Android 14 on; on Android 14, the owner's phone,
 * an app has to switch it on with `android:enableOnBackInvokedCallback="true"` in its manifest, as
 * Kanso does (later Android versions may turn it on for an app by default, and the flag then
 * only opts out). Android 13 already hands Back to an app through the same callback that flag
 * switches on, but it calls it only once Back is finished and sends no progress while the finger
 * moves, so on Android 13 there is no preview: the pop simply plays [kansoPopTransition]. With
 * three-button navigation there is no gesture either: the Back button plays [kansoPopTransition]
 * too, on its springs.
 *
 * How NavDisplay plays it: during the gesture it does not run this transition against the clock;
 * it seeks it. The gesture's progress is a number Android sends that grows from 0 towards 1 as the
 * finger moves in from the edge. NavDisplay hands it to the transition as the fraction of its
 * running time to show (`seekTo`), so progress 0.5 shows the screens as they would be halfway
 * through that time, and it does this again each time the finger moves. The running time is the
 * length of the transition's longest animation. So a seek maps progress to time, not to distance:
 * how far a screen has moved at a given progress depends on how its animation moves over time.
 * NavDisplay also decides which screen is drawn on top (it replaces any drawing order a spec asks
 * for): during the gesture the leaving screen is on top, so it covers the screen underneath until
 * it has moved aside.
 *
 * Why the top screen's slide is not a spring: here, and only for this one slide, Kanso departs on
 * purpose from the motion scheme's springs. A spring that starts at rest has no speed at the first
 * instant, then speeds up quickly (the one below is at its fastest about 0.05 s in) and covers
 * most of its way early. If the top screen slid on the expressive scheme's default spatial spring
 * (damping 0.8, stiffness 380), as in [kansoPopTransition], that full-width slide would be the
 * longest animation, the running time
 * would be roughly 0.8 s, and a seek would put the top screen about half its width along at a
 * tenth of the swipe and about nine tenths along at a fifth (worked out from the spring, not timed
 * on a phone): it would run far ahead of the finger and be gone before the person had decided
 * anything. So the slide is a tween instead: an animation that runs for a fixed time,
 * [PREDICTIVE_SLIDE_MILLIS], along an easing curve, the curve that says how far along the
 * animation is at each moment. With [LinearEasing] that curve is a straight line, a constant
 * speed, and because this slide is the longest animation, the top screen has moved the same
 * fraction of its width as the gesture's progress: at progress 0.2 it is a fifth of its width to
 * the right. So it is drawn at the finger's pace, but it is only ever pushed to the right of its
 * place (it comes back left when the finger slides back): the swipe edge is ignored, so in a swipe
 * from the right edge the finger goes left while the screen still goes right.
 *
 * Why the top screen does not fade: [kansoPopTransition] fades the leaving screen out on the
 * effects spring, which is about nine tenths done after a tenth of a second. Seeked over a running
 * time of one second, that fade would be about nine tenths done at a tenth of the swipe, and the
 * top screen would vanish while the person was still deciding. So here it has no fade: it stays
 * opaque while it slides, and the person sees less and less of it as it leaves by the right edge.
 * The screen underneath keeps the two springs: its slide and its fade-in are nearly finished by a
 * fifth of the swipe, but it is drawn behind the top screen and only shows where the top screen
 * has moved aside.
 *
 * Letting go: NavDisplay stops seeking. If Android judges that the gesture went far enough, it is
 * committed: the top key is really removed from the back stack, as the Back button removes it,
 * and NavDisplay calls `animateTo` on the transition it was seeking, giving it no animation of its
 * own. The rest of the move then continues from where the finger left it, on the animations the
 * seek was set up with: the top screen finishes its straight-line slide off the right edge while
 * the screen underneath finishes its springs. The cost of the straight line is that the finish is
 * not quick after a short swipe: the slide goes on at its constant speed, so letting go at a tenth
 * of the swipe leaves about nine tenths of a second of slide (the test pins a release at 0.3, which
 * leaves about 0.7 s). If the person slid back before letting go, the
 * gesture is cancelled: nothing is removed, and NavDisplay seeks the move back to its start over
 * a time in proportion to how far it had gone (the fraction reached times the running time), so
 * the top screen slides back over the one underneath.
 *
 * What a machine checks, and what it does not: the unit test PredictiveBackGestureTest drives the
 * gesture by sending its events into the activity's back dispatcher, which on a phone gets them
 * from the system by another route (the platform's back callback, which that test does not
 * drive), and checks that the top screen sits at the gesture's progress times its width, that a
 * cancel brings it back and leaves only it, and that letting go pops the back stack and carries the
 * top screen out of the right edge. PredictiveBackOpacityTest checks, by pixels, that the top
 * screen stays opaque and drawn over the one underneath. Both send the swipe from the left edge
 * only. What no machine says is how the preview feels with a real swipe, which needs Android 14 or
 * later: a person checks it on the phone, with Settings > Display > Navigation bar > Swipe gestures
 * turned on, from both edges, and judges whether the top screen keeps pace with the finger.
 *
 * The lambda also receives the swipe edge, a number that says which edge of the screen the gesture
 * started from. Kanso moves its screens the same way from either edge, so it ignores it (the `_`).
 * Like the other two, this function takes the motion scheme as a parameter and reads its two
 * springs at once, because the spec lambda is not a composable function and cannot read the theme
 * itself. It builds the straight-line slide at once too, so every call of the lambda shares it.
 */
fun kansoPredictivePopTransition(
    motionScheme: MotionScheme,
): AnimatedContentTransitionScope<Scene<NavKey>>.(Int) -> ContentTransform {
    // The same two springs as the pop, read once here and captured by the lambda. Only the screen
    // underneath moves on them.
    val slide: FiniteAnimationSpec<IntOffset> = motionScheme.defaultSpatialSpec()
    val fade: FiniteAnimationSpec<Float> = motionScheme.defaultEffectsSpec()
    // The top screen's slide: a fixed one-second run at a constant speed, so a seek to a given
    // progress puts the screen that fraction of its width along (see PREDICTIVE_SLIDE_MILLIS).
    val followSlide: FiniteAnimationSpec<IntOffset> =
        tween(durationMillis = PREDICTIVE_SLIDE_MILLIS, easing = LinearEasing)
    // The `_` is the swipe edge, which Kanso does not need.
    return { _ ->
        // The screen underneath comes back as the pop brings it: from a quarter of its width to the
        // left, fading in. It is drawn behind the top screen, so it shows only where that one has
        // moved aside.
        val enter = slideInHorizontally(animationSpec = slide, initialOffsetX = { fullWidth -> -fullWidth / 4 }) +
            fadeIn(animationSpec = fade)
        // The top screen slides out of the right edge at the gesture's pace. No fadeOut here: it
        // stays opaque while it slides, so the person sees what they would leave.
        val exit = slideOutHorizontally(animationSpec = followSlide, targetOffsetX = { fullWidth -> fullWidth })
        enter togetherWith exit
    }
}
