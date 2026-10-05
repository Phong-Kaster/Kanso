# HISTORY

### Iteration 1 - 2026-10-05 (bootstrap)
- **Phase:** none (bootstrap)
- Attempted: read the PRD and engine modules; wrote DoD, PLAN and five tasks; one critique analyst (Capable) reviewed the draft.
- Learned: latest stable Navigation 3 is 1.2.0 and Koin 4.2.2; engine modules use the AGP 8 DSL; `core/common` uses Firebase; `HSVColorPicker` hard-codes black text and parses hex with `toLong()`.
- Reconciled: applied the critique's findings (KDoc moved into every task, picker driven through a ViewModel seam, ML Kit's transitive Firebase allowed, stronger persistence proof, material-kolor for seed colours).

## Archived Decisions

- D-001 (DoD approval): approved as written, 2026-10-05; assumptions A-001 to A-004 not overturned.

### Iteration 2 - 2026-10-05
- **Phase:** T-001
- Recovered: found uncommitted T-001 work from the previous invocation; build, tests and lint verified green before salvaging.
- Reviewer (Capable) found READ_PHONE_STATE merged in, undocumented Firebase infrastructure and version pins, and a grep scope trap. Fixed all; tried nav3 1.2.0 / lifecycle 2.11 / BOM 2026.09, all need AGP 9.1, reverted.
- Learned: AGP 8.13 caps versions; recorded in PROJECT.md.

## Iteration: phase 3 (T-002)
One Worker (Capable) wrote the Nav3 shell. Iteration wired Application/Activity, fixed Koin restart across Robolectric tests (stopKoin before startKoin), moved Home-exit into goBack, DoD 7 test checks isFinishing. Review fixes: double-tap guard.

## Iteration: phase 4 (T-003)
One Worker (Capable). Round 1 failed: DataStore rename on Windows (fixed with OkioStorage) and captureToImage timeout (fixed by drawing the view into a Bitmap). Reviewer found a weak DoD 13 proof, no corruption handler and a nav-bar scrim problem below API 29; all fixed. Wiring (strings, appModule, MainActivity) done by the Iteration.

## Iteration: phase 5 (T-004)
One Worker (Capable) wrote the theme colour model, MaterialKolor generator, KansoTheme mapping, Settings section with picker, and 5 test classes. Iteration wired MainActivity (saved colour before first frame) and fixed one test: the DataStore write runs off the test clock, so it polls in real time. 55+ tests, lint, build green. Review: no blocking findings.

## Iteration: phase 6 (T-005)
One Worker (Capable) wrote ScreenshotTest (nine tests). One fix by the Iteration: wrong Roborazzi import package. Nine baselines recorded by the wrapper, each opened and judged. verify, tests, lint, build green. Review: no blocking findings. All tasks complete: DONE-candidate.

## Iteration 8: Verifier (Capable)
Re-proved every machine criterion on fresh evidence except DoD 2's emulator half: clean debug+release build and lint green, 64 unit tests and 9 Roborazzi references pass, manifests, dependency reports, greps and catalog loop hold. `foreman-device.ps1 -Op install` failed twice on emulator-5554 with an empty adb reason; the debug APK is 287 MB (four ABIs). Filed T-006, cleared DONE-candidate. Filled A-001 to A-004 checkpoint SHAs.

## Iteration 9: phase 7 (T-006, attempt 1)
One Worker (Capable) added the debug ABI filter. APK 287 -> 188 MB; install, clear, start exit 0, no FATAL. Dump refused (focus `Application`), so the task stays open at 1/3. Tests, Roborazzi, lint green.
