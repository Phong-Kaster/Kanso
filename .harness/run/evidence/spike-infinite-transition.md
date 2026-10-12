# Spike: does a rememberInfiniteTransition keep a Compose test from going idle? (Run 7, Iteration 2, 2026-10-12)

Throwaway test (not committed): `createAndroidComposeRule<ComponentActivity>()`, SDK 35, `mainClock.autoAdvance = true` (the default), a `Box` whose `drawBehind` reads the value of `rememberInfiniteTransition().animateFloat(0f, 1f, infiniteRepeatable(tween(1000)))`. Command: `./gradlew :app:testDebugUnitTest --tests 'com.phongkaster.kanso.spike.InfiniteTransitionSpikeTest' -i`; BUILD SUCCESSFUL in 1m 44s. Output lines:

```
SPIKE waitForIdle returned after 2 ms; autoAdvance=true; drawn phase=0.0
SPIKE phase before=0.0 after advanceTimeBy(500)=0.0
```

Reading: `waitForIdle()` returned at once (2 ms) with the infinite transition declared, and the value drawn stayed 0.0 even after `advanceTimeBy(500)`: the test rule's infinite-animation policy cancels the animation, so it neither blocks idle nor moves. So a whole-app test needs no paused clock around Home's skeleton, and no test can observe the pulse moving (a still frame at the initial phase is what every test and picture shows). A preview has no test rule and no such policy: previews still never read an infinite transition (PROJECT.md Constraint).
