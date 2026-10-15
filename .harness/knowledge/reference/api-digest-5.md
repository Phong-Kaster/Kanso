## Q. Infinite animations and tests

- `LoadingIndicator.kt:440-450` reads `coroutineContext[InfiniteAnimationPolicy]` and wraps its loops in `policy.onInfiniteOperation { }`; `CircularProgressIndicator` uses a plain `rememberInfiniteTransition()` (`ProgressIndicator.kt:630-645`), and `PROJECT.md` records that it does go idle under the Compose test rule. What `ComposeUiTest`'s policy does to `rememberInfiniteTransition` with `autoAdvance` true or false is NOT confirmed (the ui-test sources are not in the scratch folder): the Run 7 spike settles it.
- Repository fact: a spinner hung `PreviewsRenderTest` in Run 3 (previews are drawn in a plain activity with no Compose test rule, `PreviewsRenderTest.kt:141-157`). Never let an infinite animation run in a `@Preview`: read an animated value only from the stateful wrapper, default to a still frame (`LocalSkeletonPhase`, `ProvideSkeletonPulse(pulsing = false)`).
- "Remove animations": a still phase when `ValueAnimator.areAnimatorsEnabled()` is false (API 26+; memory, not confirmed). `PROJECT.md` already says Robolectric cannot drive that setting, so a person checks it on the phone.

## R. Blur and the backdrop (memory, not from source)

- `Modifier.blur(radius)` needs API 31 (RenderEffect) and does nothing below; under Robolectric's software capture it is very likely ignored on every SDK. The Run 7 backdrop therefore draws a tiny scaled-down copy stretched back up (halve with `Bitmap.createScaledBitmap(..., filter = true)` until both sides are at most 64 px; copy a `HARDWARE` bitmap to a software config first) and puts `Modifier.blur` on top. Coil's transformations library is not in the build (`gradle/libs.versions.toml` has only `coil-compose` and `coil-gif`), and the Tool and Saved screens draw a `Bitmap` from the ViewModel, not through Coil.

