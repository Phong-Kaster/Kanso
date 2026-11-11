# The Edit tool row has two levels: RGB, HSL and Curves live under Colour

The Edit screen's row had grown to ten entries, three of them (RGB, HSL, Curves) about colour. The owner
asked for them to be gathered under one entry, the way Beautify gathers Face and Body. This record keeps
what was decided and why. Paths are under `app/src/main/java/com/phongkaster/kanso/`.

Status: accepted (Run 13, D-001).

## What was decided

- **First row:** Crop, Rotate, Adjust, Colour, Filters, Stickers, Text, Date, with Compare pinned at the
  right. Tapping Colour switches the row to Back (pinned left), RGB, HSL, Curves, Compare. Colour opens
  no tool; Back closes an open colour sheet and keeps the change.
- **The model is separate from `EditTool`.** `EditTool` keeps its ten cases; `ui/tool/model/EditToolGroup.kt`
  and `EditRowEntry.kt` describe the row's shape, so processing code is untouched.
- **"Colour open" is a view flag**, `rememberSaveable` inside `EditToolRow` (Beautify's precedent), not
  part of `ToolUiState`. The row shows the group when the flag is set or a colour tool is open. It is
  lost when a layer's sheet (Stickers, Text, Date, or a touched sticker/text layer) replaces the row.
- **No new callback:** the row still has only the toggling `onOpen(EditTool)`; closing a sheet calls it
  with the open tool.
- **Shared with Beautify:** `ui/tool/component/GroupBackEntry.kt` and `GroupRowTransition.kt`. Beautify
  looks and works as before.
- **The phone's Back gesture is unchanged** on the Colour group (it does not step back a level).
- **Icons:** Colour uses the palette; RGB gets Material Symbols Rounded `instant_mix`.

## Cost

One more tap to reach RGB, HSL or Curves; in return the first row is eight entries and Colour's dot shows
a change inside the group.
