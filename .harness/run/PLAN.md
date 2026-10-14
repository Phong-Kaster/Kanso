# PLAN

> Machine-owned execution strategy. The human never reviews this file - only the Definition of Done.

## Strategy

One shared seam first (T-001), then one task per tool with its own `data/edit/<tool>/` package, `ui/tool/<Tool>Panel.kt`, strings file and test package, so tool Workers never write the same file. Each engine step sits behind a small interface in `app/`; host tests prove the Kotlin steps (crop, colour matrix, mask, blend, size caps, failures, Save) with fakes, because the engine is native; the Verifier drives the real engine on the device (DoD 7, 9, 11, 13). The Iteration wires the shared files after each tool Phase: `ui/tool/ToolPanel.kt` (dispatch to the panel), `di/AppModule.kt` (processor registry entries) and an interim README note. T-007 records pictures, rewrites the README and audits it.

Engine facts the plan rests on (analysts, from Retica's call sites; AAR signatures unconfirmed until each tool's spike): Retica's Retouch screen was object removal, so skin smoothing follows its Edit-screen Beauty path; Retica's 12 style images are absent from the repo (Q1); ML Kit subject segmentation downloads at first use (Q2); Real-ESRGAN is 4x so Enhance caps the input (Q3); the engine cannot load on the host JVM.

## Task Graph

- T-001 - shared pipeline, undo, progress, failure, Save writes the edit (depends on: -) - scope: `data/edit/`, `ui/tool/*`, `data/photo/*`, `di/AppModule.kt`, strings_tool - tier: Capable (new cross-module contract)
- T-002 - Edit (depends on: T-001) - scope: `data/edit/edit/`, `ui/tool/EditPanel.kt`, `strings_edit.xml`, `tool/edit/` tests - tier: Capable
- T-003 - Remove background (depends on: T-001) - scope: `data/edit/cutout/`, `ui/tool/{CutoutPanel,Checkerboard}.kt`, `strings_cutout.xml`, `tool/cutout/` tests - tier: Capable
- T-004 - Enhance (depends on: T-001) - scope: `data/edit/enhance/`, `ui/tool/{EnhancePanel,EnhanceCompare}.kt`, `strings_enhance.xml`, `tool/enhance/` tests - tier: Capable
- T-005 - Retouch (depends on: T-001) - scope: `data/edit/retouch/`, `ui/tool/RetouchPanel.kt`, `strings_retouch.xml`, `tool/retouch/` tests - tier: Capable
- T-006 - Artistic (depends on: T-001) - scope: `data/edit/artistic/`, `assets/styles/`, `ui/tool/ArtisticPanel.kt`, `strings_artistic.xml`, `tool/artistic/` tests - tier: Capable
- T-007 - pictures, README, audit (depends on: T-002..T-006) - scope: `ToolScreenshotTest.kt`, baselines, `README.md` - tier: Capable

No task is Fast: each embeds an engine or architecture judgement (the criteria in POLICIES.md fail).

## Phase Grouping

| Phase | Tasks | Shared files the Iteration wires itself |
|---|---|---|
| 1 | T-001 | - (the task owns them) |
| 2 | T-002, T-003, T-004 | `ui/tool/ToolPanel.kt`, `di/AppModule.kt`, README interim note |
| 3 | T-005, T-006 | `ui/tool/ToolPanel.kt`, `di/AppModule.kt`, README interim note |
| 4 | T-007 | - |

Tasks larger than 6 files are dispatched in slices (production first, tests last).

## Known Risks

- The AAR APIs are guessed from Retica; each tool task starts with a read of the class files and records the truth in `knowledge/reference/`.
- GL beauty shader may not run offscreen; fallback is OpenCV (T-005).
- Device checks need the A13 or the emulator free; if neither can be driven, the Verifier reports "not driven" and DONE is impossible (DONE_PARTIAL).
- Moving `ToolUiState`/`ToolViewModel` signatures breaks tests that build them by name or position (new parameters last, defaulted).
