# DECISION QUEUE

> Answer in `.harness/run/DECISIONS.md` under a heading naming the entry's id, never here.

---

## D-001 - Approve Run 8's Definition of Done, and which pictures may be re-recorded

- **Status:** pending
- **Type:** DoD approval
- **Iteration:** 1
- **Timestamp:** 2026-10-14
- **Blocks tasks:** T-001, T-002, T-003, T-004, T-005, T-006, T-007

### Question

Approve `.harness/run/DoD.md` (edit freely before approving): 35 criteria and 2 removals that make Edit, Remove background, Enhance, Retouch and Artistic edit the photo with the engine already in the repository. Approval also covers the readings Q1 to Q6 and the picture list below. No new dependency and no standing capability is requested (Autonomous mode runs under the Deny List).

### Context

Findings that shaped the DoD: Retica's Retouch screen was object removal (skin smoothing lived in its Edit screen), so Retouch follows that Edit path; Retica's 12 Artistic style images are not in the repository (Q1); one of the engine's two background-removal models downloads on first use, so Remove background is limited to people (Q2); Real-ESRGAN upscales 4x, so Enhance works on a 512 px copy (Q3). The engine cannot load on the host JVM, so host tests use fakes behind a seam and the Verifier drives the real engine on the device (criteria 7, 9, 11, 13).

### Baselines (criterion 27)

May be re-recorded (they show the old "coming in a later run" panel and change by design): `16_tool_photo_light.png`, `17_tool_photo_dark.png`, `22_editor_enhance_light.png`, `23_editor_enhance_dark.png`, `40_tool_narrow_photo_light.png`, `41_tool_narrow_photo_dark.png`, `42_tool_narrow_photo_sdk30_light.png`.

New pictures to record (numbers from 45): Remove background, Retouch and Artistic, light and dark each (6), plus Edit and Enhance showing their controls in light and dark (4).

No other picture may be re-recorded.

### Options Considered

1. Approve as written - consequences: Q1 (generated style images), Q2 (people only), Q4 (no face changes nothing) are accepted.
2. Edit the DoD first - consequences: change Q1/Q2/Q4 or any criterion, then approve.

### Engine Recommendation

Approve as written; the two worth a look are Q1 (supply your own style images instead if you prefer) and Q2 (people-only cut-out).

### Proposed Capabilities (if any)

None.

### Decision

Answered in `.harness/run/DECISIONS.md` under `## D-001`.

---
