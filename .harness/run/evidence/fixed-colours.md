# Fixed colours in `app/src/main` (DoD 16)

Command, run 2026-10-10 on the tree after Phase 5 (HEAD `aaeba85` plus the Phase 5 working tree):

    git grep -n -E 'Color\(0x|Color\.(Black|White)' -- app/src/main

It finds exactly eight lines, one of them the named exception and seven inside declarations that only previews and tests call:

| File:line | Text | Why it is allowed |
|---|---|---|
| `ui/settings/SettingsScreen.kt:128` | `private val PickerSurfaceColor = Color.White` | **The one named exception.** The colour picker's own card: `core/colorpicker` (source may not be edited, Constraint in `PROJECT.md`) draws fixed black text, so the card behind it must be light in both modes. |
| `ui/settings/SettingsScreen.kt:653` | `private val sampleWallpaperColour = Color(0xFF8B5000)` | A `sample…` declaration (a stand-in wallpaper colour for the Settings previews); no screen reads it. |
| `ui/camera/CameraScreen.kt:862` | `listOf(Color(0xFF7FB3D5), Color(0xFFF5CBA7), Color(0xFF58804A))` | Inside `private fun SampleLivePicture` (declared at :858), the stand-in for the camera's live picture in previews. |
| `ui/camera/CameraScreen.kt:872` | `.background(Color(0xFFFFE08A), CircleShape)` | Same function (`SampleLivePicture`). |
| `ui/camera/CameraScreen.kt:894` | `drawRect(Brush.verticalGradient(listOf(Color(0xFF5DADE2), Color(0xFFD6EAF8))))` | Inside `private fun sampleShotBitmap` (declared at :884), the stand-in for a taken shot. |
| `ui/camera/CameraScreen.kt:895` | `drawCircle(color = Color(0xFFF7DC6F), …)` | Same function (`sampleShotBitmap`). |
| `ui/camera/CameraScreen.kt:896` | `drawCircle(color = Color(0xFF52BE80), …)` | Same function. |
| `ui/camera/CameraScreen.kt:897` | `drawCircle(color = Color(0xFF27AE60), …)` | Same function. |

Not found by the grep, and so not listed: Kanso blue's seed, which is declared as `private const val KANSO_BLUE_SEED_ARGB = 0xFF1A73E8` and turned into a colour by `Color(KANSO_BLUE_SEED_ARGB)` (assumption A-013): the seed is the input of the colour generator, not a colour a screen paints.
