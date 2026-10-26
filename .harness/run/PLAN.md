# PLAN — Run 10: stickers on the Edit screen

> Machine-owned execution strategy. The human never reviews this file - only the Definition of Done.
> Evolves through Tier-1 amendments (logged in AMENDMENTS.md) and Tier-2 queued decisions.

Paths: `main/` = `app/src/main/java/com/phongkaster/kanso/`, `test/` = `app/src/test/java/com/phongkaster/kanso/`,
`res/` = `app/src/main/res/`. Exact scopes live in each `TASKS/T-00N.md`.

## Strategy

The PRD fixes five commits; each is one engine Phase. Phases 1 to 4 each hold one task executed in sequential
slices of at most 6 files (production, then tests, then docs); Phase 5 holds two disjoint tasks. Every Phase
updates `README.md` and the feature docs for the behaviour it adds (CLAUDE.md: same change); Phase 5 completes
them (mermaid flows, folder table, `CONTEXT.md`, ADR-004). Phase 1 is observable only through JVM tests and
Phase 2's added sticker only through the dot, Undo and Done (nothing draws it until Phase 3): the PRD's commit
order requires it, and no device check before Phase 3 expects to see a sticker.

### Contracts fixed here (no Worker invents a cross-module contract)

Data (`main/data/edit/edit/`):
- `Sticker.kt`: `data class Sticker(val key: Long, val pictureId: String, val centreX: Float, val centreY: Float,
  val width: Float, val degrees: Float, val mirrored: Boolean)`. Centre as fractions of the ORIGINAL photo;
  `width` a fraction of the original's width; height follows the picture's own aspect; `degrees` clockwise on
  the original. `key` is identity for selection, unique for the life of the screen.
- `EditSettings.stickers: List<Sticker> = emptyList()`, oldest first (later = drawn on top);
  `canAddSticker: Boolean get() = stickers.size < MAX_STICKERS`; `MAX_STICKERS = 20` in `EditSettings`' companion.
  Phase 1: `isNeutral()` (no tool) gains `&& stickers.isEmpty()`; Phase 2 replaces that with an
  `EditTool.STICKERS -> stickers.isEmpty()` branch of `isNeutral(tool)`.
- `EditSession`: `addSticker(sticker)`, `deleteSticker(key)`, `duplicateSticker(key, newKey, offsetX, offsetY)`
  (offset in original fractions, computed by the caller), `flipSticker(key)` - each one `change` (one undo
  entry); an add or duplicate while `!canAddSticker` returns the session unchanged. A gesture uses the existing
  `preview` / `commit` pair. Keys are never generated in `EditSession` (a data class): the ViewModel keeps a
  private monotonic counter.
- `StickerPictures.kt`: `fun interface StickerPictures { fun picture(pictureId: String, widthPx: Int): Bitmap? }`
  (decode at about that width). `EditProcessor` takes it as a constructor parameter with a default that draws
  nothing (shape of `FilterLut.Source`), so every existing call site and test compiles.
- `EditProcessor`: stickers drawn after the crop rectangle is cut out, shifted by its offset - each sticker into
  its own bounding-box bitmap (Canvas + Matrix: scale, mirror, rotate), alpha-blended into the crop pixels, then
  recycled; then turns, flips, filter, sliders as today. No second full-size copy.
- Phase 2: `StickerCategory.kt` (enum FOOD, MOOD, QUOTE, EMOJI, each with its tab-name string), `CatalogSticker.kt`
  (`pictureId` = the drawable's name, e.g. `img_sticker_mood_3`; `@DrawableRes res`; category),
  `StickerCatalog.kt` (object: the 60 entries in Pinity's order, `R.drawable.*` references - never
  `getIdentifier`), `DrawableStickerPictures.kt` (the real `StickerPictures`, decoding the catalogue's drawables),
  `StickerMapping.kt` (pure Kotlin, no `android.` import, no `@Composable` text: the ONE mapping between the shown
  picture - cropped, turned, flipped; 0..1 of the shown picture - and the original's fractions, both ways, for
  points, angles and mirror, from the original's pixel size and `EditSettings`). `main/ui/tool/EditProcessorFactory.kt`:
  `editProcessorFor(context): (Bitmap) -> EditProcessor` with the real `FilterLut` and `DrawableStickerPictures`;
  both `EditPanel.kt` and `ToolScreen.kt` (the cropper's recipe) call it, so Done never loses stickers.

Screen state (`ToolUiState`, all `val`): `stickerCategory: StickerCategory = FOOD` (Phase 2), `selectedStickerKey:
Long? = null` (Phase 2 sets it on add; Phase 3 adds selection, pruning a key that no longer exists after undo,
delete or any settings change, and clearing it when the panel closes), `stickerLimitShown: Boolean = false`
(Phase 2: the refused 21st add sets it; `photocard/StickerLimitMessage.kt`, a timed message on the photo shaped
like `FailureMessage.kt`, shows it and calls `onStickerLimitMessageShown()`), `touchedStickerKey: Long? = null`
(Phase 4).

Placement: add puts the sticker at the shown picture's centre, width a third of the shown width, both converted
through `StickerMapping`. Duplicate offset: +4% of the shown width right and down, converted through it.

Overlay (Phase 3), in `main/ui/tool/components/photocard/stickeroverlay/`, inside `PhotoCard`'s `TOOL_IMAGE_TAG`
box (so its coordinates are the shown picture's) and clipped to it; composed with pointer input only while
`openTool == STICKERS`, Compare off and not finishing. ONE overlay-level `pointerInput` handler (raw
`awaitPointerEventScope`, no touch slop, no Pinity halving of zoom, pan or rotation) hit-tests newest-first through
`StickerGeometry`: down on a sticker selects it and starts a gesture (one finger moves, two fingers pinch and
twist about the centroid), each move calls `previewSettings`, the last finger up calls `commitSettings`; down
on empty photo selects none. The four corner buttons are separate composables (`StickerCornerButton`: a
`MaterialTheme.colorScheme` `Surface` disc, 48 dp touch target, a white-fill glyph drawn with
`Icon(painterResource)`, a content description); the bottom-right handle has its own drag handler (scale by
distance from the centre, turn by angle about it). In Phase 3 the overlay also draws the sticker pictures
(clipped, unfiltered); Phase 4 removes that drawing from the overlay (frame, buttons and hit areas stay), so
nothing is drawn twice.

Live preview (Phase 4): `FilterPreviewRenderer` renders the small copy with every sticker except
`touchedStickerKey`, before the filter, and renders whenever stickers exist even with no filter; the
ViewModel's request key adds the stickers minus the touched one. `LivePreviewImage` takes
`touchedSticker: Sticker?` and `touchedPicture: ImageBitmap?` (decoded in `PhotoCard` through
`DrawableStickerPictures(LocalContext.current)` in a `remember(pictureId)`) and draws it inside the same crop,
turn and flip with the slider colour matrix, exposing a semantics property beside `LivePreviewGeometry`.
`EditCropper` takes `stickers: List<Sticker>` and draws them over its original image in original coordinates
(DoD 20).

Row (Phase 2): the entry width shrinks from 84 dp so five fit beside Compare at 411 dp (DoD 2); every row picture
(51, 52, 55-68) is re-recorded in Phase 2, with old copies in `evidence/baseline-diffs/`, once the human lists
them under `## Baselines` in `DECISIONS.md`.

Handle icons (Phase 3 Wiring): Pinity's `ic_close_circle`, `ic_flip`, `ic_resize_circle` are ported as glyph
paths only, single white fill, no disc and no stroke colours, no `android:tint`; `ic_duplicate.xml` is new,
Material Symbols "content_copy", viewport 960. The disc is the themed `Surface`, so the icons read as Kanso.

## Task Graph

- T-001 - Done draws stickers first, and sticker actions undo (depends on: -) - 11 files, 2 slices - tier: Fast
- T-002 - Open Stickers, choose a tab, tap a sticker onto the photo (depends on: T-001) - ~26 files, 5 slices - tier: Fast
- T-003 - Select, move, pinch, twist, delete, duplicate, flip a sticker (depends on: T-002) - ~19 files, 4 slices - tier: Fast
- T-004 - Stickers in the live preview and the cropper (depends on: T-003) - ~13 files, 3 slices - tier: Fast
- T-005 - The approved pictures 69-82 (depends on: T-004) - 2 files - tier: Fast
- T-006 - The written record describes Stickers (depends on: T-004) - 4 files - tier: Fast

## Phase Grouping

| Phase | Tasks | Shared files the Wiring Worker (or the Iteration) writes |
|---|---|---|
| 1 | T-001 | Iteration: `.harness/run/evidence/test-edits.md` |
| 2 | T-002 | Iteration, before the Workers: copies the 60 `res/drawable/img_sticker_*.webp`; Wiring Worker, before the Workers: `res/drawable/ic_stickers.xml` (Material Symbols "add_reaction", viewport 960). Iteration, after: row baselines re-recorded (D-001 `## Baselines`), `evidence/baseline-diffs/`, `test-edits.md` |
| 3 | T-003 | Wiring Worker, before the Workers: `res/drawable/ic_close_circle.xml`, `ic_flip.xml`, `ic_resize_circle.xml`, `ic_duplicate.xml`; Iteration: `test-edits.md` |
| 4 | T-004 | Iteration: `test-edits.md` |
| 5 | T-005, T-006 | Wiring Worker: `docs/adr/ADR-004-sticker-is-part-of-the-photo.md`; Iteration: records pictures 69-82 with `foreman-record-baselines.ps1`, judges each, `test-edits.md` |

`PreviewsRenderTest.requiredPreviewNames`, `PreviewCoverageTest.EXEMPT`, `ScreenStateTest`, `AccessibilityWalkTest`
and `ThemeFollowTest` need no edit if the plan is followed (new state is `val`; every composable has a preview);
if one does, it is the Iteration's (PROJECT.md Constraint on shared files).

## Known Risks

- Galaxy A13 (32-bit): memory in Done (one bounding-box bitmap per sticker, decoded at its target width, recycled)
  and drag smoothness with 20 stickers (only the touched sticker draws live; the small copy re-renders on
  finger-up). DoD 24 is a person on the phone.
- Robolectric: webp decode and Matrix drawing need `@GraphicsMode(NATIVE)`; processor tests use a fake picture
  source with solid bitmaps; pixels through `ScreenPixels.kt`, never `captureToImage()`; multi-pointer gestures
  with `performTouchInput { down(0, …); down(1, …); moveTo(…) }`.
- The 136 dp sheet (`EditSheet.kt:38`): tabs plus a vertically scrolling grid must fit inside it, and
  `EditLayoutTest:157` (photo height equal with every tool open) must still hold.
- Phase 2 is the largest (5 slices plus 60 copied files plus 16 re-recorded pictures). If its re-recordings are
  not yet approved when Phase 2 lands, those 16 pictures fail `verifyRoborazziDebug`; the Iteration then queues
  the baseline decision and keeps the phase's code.
- Done's resolution (DoD 18, Q4): if the human edits it to the file's full resolution, Phase 1 grows a full-size
  decode path (decided at the gate, not later).
