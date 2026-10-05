# Issues

## Open task

- **T-006** (pending, filed by the Verifier 2026-10-05): the debug app does not install on the emulator. `foreman-device.ps1 -Op install -Apk app/build/outputs/apk/debug/app-debug.apk` → `adb: failed to install ...: ` (empty reason), exit 1, twice. APK is 287 MB with four ABIs. Until fixed, DoD 2's emulator half, 19 and 22 cannot be driven.

## Awaiting a person (unsigned)

- [ ] 12 - Kill the app, open it from the launcher with theme mode on System → system splash (app icon on a plain background), then Home, with no extra screen, logo animation or flash of another colour.
- [ ] 19 - Android 12+ phone: Settings → theme colour → From wallpaper; change the phone's wallpaper → the app's accent colour follows. Not driven yet (T-006).
- [ ] 21 - Open Home, each of the six tools, and Settings in light, dark and with a picked colour; in Settings open the custom colour picker, drag, read its hex field in dark mode → plain surfaces, readable text, every control visible, picker text readable, no ornament.
- [ ] 22 - Open a tool, back gesture → Home; back gesture again → app closes to the launcher. Not driven yet (T-006).
- [ ] 26 - Read `app/src/main/java/com/phongkaster/kanso/navigation/` and the Home and Settings screen files → back stack, `NavDisplay`, `entryProvider`, per-entry ViewModels, `remember`/state and the theme are each explained in plain English where first used.

## Review notes recorded, not fixed

- T-002: `ViewModelClearedProbe` lives in main; `BackArrowIcon` package.
- T-004: picker `onColorChanged` writes DataStore on every drag move (stutter risk; debounce later); picker alpha bar does nothing (`core/colorpicker` read-only); four colour labels are constants, not `strings.xml`; tests use a throwaway ViewModel.
- T-005: custom-colour screenshots set the colour through a throwaway ViewModel without tapping the Custom row; helpers duplicate ThemeColourSettingsTest; waits match the literal title "Kanso".

## Assumptions

A-001 to A-004 (Tier 2 / missing information), see `.harness/run/ASSUMPTIONS.md`. None is Tier 3.

## Proof deviation noted by the Verifier

- DoD 7's test presses Back through `OnBackPressedDispatcher` and checks `isFinishing`, not `Espresso.pressBackUnconditionally()`; the Proof allows `isFinishing`, and DoD 22's emulator drive will check it with the real back key.
