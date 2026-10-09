# Evidence

Base evidence captured at Iteration 1 (bootstrap), 2026-10-09, from commit `76522d7` (Run 6 base):

- `base-testnames.txt` - the 238 `@Test` names (`File.kt:name`), made with the awk of DoD 25.
- `base-badging.txt` - the `uses-permission` / `uses-feature` lines of `aapt2 dump badging` on the debug APK of the base app code (built 2026-10-09 18:12, identical app code); DoD 15 diffs against it.
- `base-deps-debugRuntime.txt` - `./gradlew :app:dependencies --configuration debugRuntimeClasspath` at the base (Material3 `1.3.1 -> 1.5.0-beta01`); DoD 27 compares the normalised module set.
- `baseline-diffs/` - comparison pictures kept per phase (`phase<N>/`), and the before/after folders the Verifier writes (DoD 20).
- `symbols/` - provenance of the Material Symbols drawables (DoD 28).

Later files are written by the Iterations and the Verifier: `test-edits.md`, `expressive-scan.md`, `fixed-colours.md`, logs of every Gradle run.
