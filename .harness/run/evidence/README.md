# Evidence

Base evidence captured at Iteration 1 (bootstrap), 2026-10-12, from commit `a720960` (Run 7 base; the app code is that of `1e82f8d`, the last commit under `app/src/main`, `core`, `opencv`, `libs` and the build files):

- `base-testnames.txt` - the 368 `@Test` names (`File.kt:name`, no duplicates), made with `testnames.sh` (DoD 35).
- `testnames.sh` - the script: `./testnames.sh <rev>` prints the sorted `File.kt:name` list of a revision.
- `base-badging.txt` - the `uses-permission` / `uses-feature` lines of `aapt2 dump badging` on the debug APK built 2026-10-10 05:59 from that app code; DoD 21 diffs against it. The same APK: `native-code: 'arm64-v8a' 'armeabi-v7a' 'x86_64'`, `minSdkVersion:'27'`, `targetSdkVersion:'36'`, `compileSdkVersion='37'`.
- `base-deps-debugRuntime.txt` - `./gradlew :app:dependencies --configuration debugRuntimeClasspath` at the base (1923 lines; Material3 `1.5.0-beta01`); DoD 40 compares the normalised module set.
- `base-unused-resources.txt` and `base-lint.log` - the `UnusedResources` entries of a forced lint run at the base (`gallery_album_label`, `gallery_body_placeholder`) and its log (BUILD SUCCESSFUL, 0 errors); DoD R9 compares against them.
- `baseline-diffs/` - comparison pictures kept per phase (`phase<N>/`), and the `before/` and `after/` folders the Verifier writes (DoD 29).
- `symbols/` - provenance of the Material Symbols drawables (DoD 40).

Later files are written by the Iterations and the Verifier: `test-edits.md`, `m3-scan.md`, `fixed-colours.md`, `home-reach.md`, `orphans.sh`, `head-*` files and logs of every Gradle run.
