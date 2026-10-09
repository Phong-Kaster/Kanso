# Baseline differences

One line per screen picture that moved because of a library step. Each line names the step and what
differs; the matching `<name>_phase<N>_compare.png` sits next to this file.

- **Phase 1 (Android Gradle Plugin 9.4.1, Gradle 9.6.0):** no baseline moved (`evidence/phase1-roborazzi-summary.json`: 16 total, 16 unchanged).
- **Phase 2 (Compose 1.12.1, Navigation 3 1.2.0, lifecycle-viewmodel-navigation3 2.11.0, Kotlin 2.4.21):** no baseline moved. `verifyRoborazziDebug` after the moves reported 16 total, 0 changed, 16 unchanged (`evidence/phase2-roborazzi-summary.json`, copied from `app/build/test-results/roborazzi/debug/results-summary.json`), so nothing was re-recorded and no comparison image exists for this step.
