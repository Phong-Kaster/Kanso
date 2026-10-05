# Issues

- Unsigned `human-only` criteria: 12, 26 (and any machine-then-human) await a person at the end.
- Minor review notes on T-002 (ViewModelClearedProbe in main, BackArrowIcon package) left as is.
- T-004 review (non-blocking, not fixed): picker `onColorChanged` fires on every drag move and each call writes DataStore and re-themes the app (stutter risk; debounce the save in a later task); the picker's alpha bar does nothing (alpha dropped; `core/colorpicker` is read-only); the four colour labels are constants in `SettingsScreen.kt` not `strings.xml`; tests use a throwaway ViewModel.
