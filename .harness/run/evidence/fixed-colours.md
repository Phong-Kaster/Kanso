# Fixed colours in `app/src/main` (DoD 23)

Written in Iteration 14. `git grep -n -E 'Color\(0x|Color\.(Black|White)' -- app/src/main` prints these eight lines, and no others:

| Line | What it is | Inside |
|---|---|---|
| `ui/camera/CameraScreen.kt:855` | three stand-in colours for a fake live picture | `SampleLivePicture` (line 851), a preview-only sample |
| `ui/camera/CameraScreen.kt:865` | the sample sun | `SampleLivePicture` |
| `ui/camera/CameraScreen.kt:887` | the sample sky gradient | `sampleShotBitmap` (line 877), the sample shot for previews |
| `ui/camera/CameraScreen.kt:888` | the sample sun | `sampleShotBitmap` |
| `ui/camera/CameraScreen.kt:889` | the sample hill | `sampleShotBitmap` |
| `ui/camera/CameraScreen.kt:890` | the sample hill | `sampleShotBitmap` |
| `ui/settings/SettingsScreen.kt:140` | `PickerSurfaceColor = Color.White` | the one allowed constant: the white card behind the engine's colour picker, whose own text is dark; the picker is not Kanso's to edit, so the card matches it instead of the theme (the KDoc at line 134 says so) |
| `ui/settings/SettingsScreen.kt:738` | `sampleWallpaperColour = Color(0xFF8B5000)` | a `sample...` declaration used only by the Settings previews (it was line 733 at Iteration 12; the file grew by five lines) |

Nothing else paints a fixed colour, and none is a teal of InFlux's.
