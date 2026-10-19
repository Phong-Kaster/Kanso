# RESUME BLOCK

> Derived cache; loses to task files and git.

- **Stage:** Phase 8 (T-012) complete. PARTIAL-candidate recorded. The next invocation is the Verifier (third): re-prove every machine criterion, drive what can be driven, then DONE_PARTIAL.
- **Next Phase:** none. All tasks complete.
- **Criteria cited (quoted):**
  - 34 [machine-then-human] "The written record is true. Proof: the Verifier reads `README.md` 'How each editing feature works' and `docs/features/edit.md` against the code and notes every sentence it could not back; then a person reads them and signs. A claim the code does not back fails the run."
  - 32 [machine] "`README.md` and the three feature docs describe the app as the code now is. Proof: ... every backticked path exists (a script checks each); `grep -n -i \"springs back\|Saved screen\|range slider\"` of those four files finds nothing (outside a sentence recording the removal)."
- **Open ISSUES items naming these files:** criterion 34 gap (T-012) only.
- **Queued decisions:** 0. **Abandoned / unreachable:** none.
- **Device note:** the A13 is attached as R58W1120QVA. `foreman-device.ps1` has no `gfxinfo` operation, so criterion 20's jank number cannot be driven. Criteria 21, 22 and 34 still need a person.
- **Verified commands:** build: `cd /d/Valve/Personal/Kanso && timeout 1500 ./gradlew :app:assembleDebug` | test: `./gradlew :app:testDebugUnitTest` | lint: `./gradlew :app:lintDebug` | pictures: `verifyRoborazziDebug`
- **Model tiers:** fast: `sonnet` | capable: `opus`
