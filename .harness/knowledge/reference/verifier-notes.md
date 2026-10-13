# Run 6 Verifier and Phase 6 notes: tooling facts, proof timing, Kanso blue surfaces

- Reference (Run 6, the Verifier of Iteration 7, 2026-10-10):
  - `aapt2` 36.0.0 `dump badging` prints the minimum version as `minSdkVersion:'27'`, not `sdkVersion:`. Write proofs against that name.
  - A test-name list in the form `File.kt:name` is made by the script in `.harness/run/evidence/p7-verifier.md` (criterion 25). It rebuilds the base list exactly.
  - `:app:clean`, then the whole set, takes 3 min 33 s.
  - `rm -rf` is refused by the permission rules: use a fresh `mktemp -d`.
  - A stray `python -` waits on stdin until the Bash timeout.
  - A literal "prints nothing" search over `app` also matches comments in `app/src/test`.
- Reference (Run 6, Phase 6, Iteration 8, 2026-10-10):
  - Compose's `waitUntil(timeoutMillis)` looks at its condition once before it reads its clock, so a condition that first becomes true after the deadline still passes. A strict real-time limit needs its own check of `System.currentTimeMillis()` after the wait (`ThemeSwatchTest.choose`).
  - A Proof's "within N s of the tap" is one budget from the tap: two stacked waits of N s each prove "within 2N". Take one deadline at the tap and give each wait what is left.
  - A tap that applies a choice's colour scheme is proved in the seven roles of DoD 1 (`primary`, `onPrimary`, `primaryContainer`, `secondary`, `tertiary`, `background`, `surface`); `rolesOf` in `ThemeSwatchTest.kt` and `KansoBlueThemeTest.kt` hold the same list, so change both together.
  - The whole set on the finished Phase 6 tree: 3 min 9 s warm after `cleanTestDebugUnitTest`, 368 tests.
- Reference (Run 6, the Verifier of Iteration 9, 2026-10-10):
  - `foreman-device.ps1` refuses every operation, `focus` included, while no debug APK is built. After `:app:clean`, check the phone only once `assembleDebug` has run.
  - `.harness/run/evidence/symbols/check.sh` ends with `rm -rf "$TMP"`. Run a copy without that line (`sed '/^rm -rf/d'`).
  - Kanso blue's `surface` and `background` are one colour in each mode: `#F9F9FF` in light and `#111318` in dark, the window backgrounds of DoD 1 (d). So a test that compares with `background` also proves `surface`.
  - A PNG's pixels can be read with a small pure-Python decoder (zlib and struct, no PIL), run as `python -I <script file>`.
  - `:app:clean`, then the whole set, takes 3 min 30 s. Each Proof's own command takes: test 2 min 41 s; `verifyRoborazziDebug` 2 min 45 s; lint with forced analysis 23 s; `compileDebugKotlin --rerun-tasks` 19 s.
