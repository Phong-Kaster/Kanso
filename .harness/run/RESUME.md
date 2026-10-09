# RESUME BLOCK

> Regenerated every iteration. The first thing a fresh iteration reads, and usually the only thing
> it needs before selecting work.
>
> **A derived cache, never a source of truth.** On any disagreement with the task files or with git,
> this file is the one that is wrong: correct it and trust the source. Recovery from a dirty tree
> always reads ground truth, never this.

- **Stage:** Phase 2 complete and checkpointed (the two guards). Run Mode: Autonomous. Branch `loop/kanso-one-class-per-file`, base `4d5f239`.
- **Next Phase (3):** T-006, T-007 (both Fast tier, T-004 met, scopes disjoint; read each task file for the full list and the table of what moves where)
  - T-006 - scope (M = `app/src/main/java/com/phongkaster/kanso`): new `M/data/media/{Photo,Album}.kt`, `M/data/photo/{LoadedPhoto,SaveResult,FullSizeDecoder}.kt`, `M/data/settings/RetryingMoveFileSystem.kt` (private -> internal), `M/ui/permission/PermissionAccess.kt`, `M/navigation/{Home,Camera,Gallery,ToolPhoto,Saved,Settings}.kt`; edit `PhotoSource.kt`, `PhotoLoader.kt` (keeps `sampleSizeFor`, `displayMaxEdgePx`), `PhotoSaver.kt`, `UprightJpeg.kt`, `SettingsStore.kt`, `PermissionGate.kt`, `Destinations.kt` (reduced to its `package` line).
  - T-007 - scope (T = `app/src/test/java/com/phongkaster/kanso`): new `T/gallery/{FakePhotoSource,RecordingRegistry}.kt`, `T/save/{FakePhotoSaver,SinglePermissionRegistry}.kt`, `T/settings/{PickerDefaultAlphaTest,PickerMatchers,ScreenSample}.kt`, `T/tool/FakePhotoLoader.kt`, `T/screenshots/SolidColourFetcherFactory.kt`; edit `GalleryGridTest`, `GalleryPermissionTest`, `SaveTestSupport`, `PickerNoAlphaTest`, `ScreenPixels`, `ToolFixtures`, `GalleryGridScreenshotTest`.
  - after the Phase builds: the Iteration `git rm`s `M/navigation/Destinations.kt`, empties `ALLOWED` in `T/OneTypePerFileTest.kt` (the test then fails for any file still breaking the rule), updates `knowledge/PROJECT.md` (fake locations, Saved's loader). Then a DONE-candidate or PARTIAL-candidate is recorded and the next invocation is the Verifier.
  - Briefs must carry every Constraint verbatim, including the two new ones (no Gradle from inside `app/build`; no comment opener without its closer) and the `@Composable`-text rule for new non-composable main files (`PermissionAccess.kt`, the data files).
- **Queued decisions:** 0
- **Abandoned:** none
- **Unreachable:** none
- **Verified commands:** build: `./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | UI: `./gradlew :app:verifyRoborazziDebug` | lint: `./gradlew :app:lintDebug` (all verified 2026-10-09 at the Phase 2 tree: one combined run `cd /d/Valve/Personal/Kanso && ./gradlew :app:cleanTestDebugUnitTest :app:testDebugUnitTest :app:verifyRoborazziDebug :app:lintDebug :app:assembleDebug --continue` takes 2 to 5 minutes; redirect to `.harness/run/evidence/`, run in the background, wait inside the turn, never pipe an exit code, never leave the shell inside `app/build`)
- **Model tiers (resolved from `.harness/loop/models.json`):** fast: `sonnet` | capable: `opus` — pass `model` explicitly on every dispatch; the Reviewer is always Capable.
- **Tests now:** 231 `@Test` (207 + `OneTypePerFileTest` 10 + `ScreenStateTest` 14); all pass. Baselines (16) untouched. Allow-list holds the 14 files Phase 3 splits.
