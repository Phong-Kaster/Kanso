# Run 12 facts — Curves on the Edit screen (bootstrap survey, 2026-11-06, HEAD `069b5c3`)

Corrections to `run11-facts.md` and facts a Curves tool depends on. Paths under
`app/src/main/java/com/phongkaster/kanso/`.

- The Edit sheet is **220 dp** (`ui/tool/component/editbottombar/EditSheet.kt:41`), leaving 188 dp of
  content (`hslpanel/HslPanel.kt:20`); `run11-facts.md`'s 136 dp is stale.
- `EditTool` has 9 cases at HEAD (HSL at :33, FILTERS at :36). Tests counting them: `RgbMatrixTest.kt:132-133`
  (`HSL + 1 == FILTERS`, 9 entries), `StickerAddTest.kt:338`, `HslSheetTest.kt:411`; lists in
  `EditToolRowTest.kt:136,282`, exhaustive `when` at :512,:525; `EditLayoutTest.kt:196-206`.
- Highest screenshot picture number is **94** (`ToolScreenshotTest.kt` ~:425-430); new ones start at 95.
- `EditPanel.kt:239-242` wraps every tool except STICKERS and HSL in `verticalScroll`: a tool with
  vertical drags must join that exemption.
- Done order (`data/edit/edit/EditProcessor.kt:90-140`): layers, crop, turn, flip, filter, HSL
  (`EditHsl.apply`), colour matrix, opaque. Live preview CPU stage: `FilterPreviewRenderer.kt` early return
  :64, `filterOnly` :71-76, ≤ 640 px copy :112. ViewModel preview gates naming HSL: `ToolViewModel.kt:166`,
  `PreviewKey` :1424-1429/:1466-1471, `requestPreview` :1457, `stillWanted` :1518-1524.
- `EditHsl.hslToRgb` is `private` (:162) and RGB→HSL is inline in `apply` (:59-73); HSL skips greys
  (:57-58) and keeps L inside 0.05..0.95.
- `EditSession.change` compares settings with `==` (`EditSession.kt:58-60`): settings must hold Lists, not
  arrays. No `SavedStateHandle` in `app/src/main`: no Edit tool survives process death.
- `ScreenStateTest.kt:51` expects exactly seven ViewModels.
- Existing per-pixel curve code: `FilterLut.kt:27-33` reads a curve strip as three 256-entry tables;
  `AutoLighting.kt:46-57` applies one. No spline helper exists.
- No `app/src/androidTest`. The Galaxy A13 (`R58W1120QVA`, SM-A135F) was attached on 2026-11-06
  (`foreman-device.ps1 -Op devices`); the wrapper taps by resource id or text only.
- No ledger grants Gradle assemble/test/lint (`.harness/loop/capabilities/baseline.json` grants only
  verify/compare Roborazzi, the record wrapper, the compile check and the device wrapper).
- Next ADR number is 009.
