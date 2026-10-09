# Phase 1 comparison pictures (DoD 20(b))

Each `*_compare.png` is Roborazzi's reference | difference | new, side by side, copied from `app/build/outputs/roborazzi/` after `./gradlew :app:compareRoborazziDebug` (log `../../p1-compare-2.log`, summary: 18 total, 15 changed, 2 added, 1 unchanged) and before the pictures were re-recorded with `foreman-record-baselines.ps1` (log `../../p1-record.log`).

| Picture | Why it moved in Phase 1 |
|---|---|
| `1_home_light`, `2_home_dark` | colour only: Kanso blue replaces the purple (Home's own redesign is Phase 2) |
| `4_settings_light`, `5_settings_dark` | colour; the colour rows read Kanso blue and Custom colour (SDK 30 draws no wallpaper row); the layout is Phase 3's |
| `6_settings_custom_colour` | the picker's first seed is Kanso blue; the rows changed (0.32 % of the pixels) |
| `10_gallery_light`, `11_gallery_dark`, `12`-`15` (gallery and camera refused) | colour only |
| `16_tool_photo_light`, `17_tool_photo_dark` | the editor: top bar with Back and Save, photo, panel, toolbar of five tools |
| `18_saved_light`, `19_saved_dark` | Share, Edit another and Home as one button group with icons |
| `22_editor_enhance_light`, `23_editor_enhance_dark` | new: the editor with Enhance selected |

`3_home_custom_colour` did not change (a custom green Home, unaffected by the default colour). The Iteration opened every new picture; nothing in them showed a defect in: blue accents, text and icons readable in light and dark, the editor following the theme, the toolbar's selected tool, the button group's three buttons in one row. This is not a signature: criterion 20's person looks at `after/` beside `before/` at the end of the run.
