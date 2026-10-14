# Edit tool: engine and asset facts (Run 8, T-002)

The Edit tool uses NO engine code: crop, turn, flip, the four sliders and the filters are plain
Kotlin on `IntArray` pixels (`data/edit/edit/EditProcessor.kt`), so they run on the host JVM.
Only the filters' look-up IMAGES come from the engine's AARs.

## Look-up images (merged assets, read with `context.assets.open(path)`)

Seen in `app/build/intermediates/assets/debug/mergeDebugAssets/` (so AAR assets do merge):

- `lookup/` (imageprocessor.aar): `lookup_light.png` and `lookup_custom.png` are 512x512
  standard GPUImage colour cubes (8x8 tiles of 64x64; x = red, y = green, tile = blue).
  `lookup_origin.png` and `lookup_skin.png` are 64x64 = 16-level cubes in 4x4 tiles
  (`lookup_origin` looks like the identity). `lookup_gray.png` is a thin grey ramp strip
  (likely near-identity as curves: not used).
- `filter_materials/map/` (editimage.aar): `1977map, amaroMap, earlybirdMap, hefeMap, hudsonMap,
  inkwellMap, kelvinMap, lomoMap, nashvilleMap, riseMap, sierraMap, valenciaMap, waldenMap,
  xproMap` (+ overlay/gradient/vignette maps). They are NOT cubes: thin strips about 256 px wide
  and a few px tall (Instagram "IF" filter maps). Exact heights were not measured (no shell).

## How a look-up image becomes a filter (`FilterLut.kt`)

- Square image with n^3 pixels and (w/n)^2 == n tiles -> colour cube (nearest level per channel).
- Anything else -> three tone curves: red from the row at 1/6 of the height, green at 1/2, blue at
  5/6 (the IF shaders sample `vec2(texel.r, .16666)`, `(texel.g, .5)`, `(texel.b, .83333)`);
  column = level * (width-1) / 255. A 1-row strip uses the same row for all three.
  Filters whose IF shader does more (overlay/blowout blending, `inkwellMap` as a luminance curve)
  are approximated by the curves only. Chosen presets: LIGHT (lookup_light cube), 1977, KELVIN,
  NASHVILLE, WALDEN, HUDSON, AMARO (strips).
- Intensity t mixes per channel: in + (filtered - in) * t, rounded. t = 0 or NONE = input exactly.

## Seam contract chosen (apply compounds)

The ViewModel applies each processor to the photo currently shown, so `EditPanel` hands over only
the one change just made: crop/turn/flip on tap; the four sliders when released
(`onValueChangeFinished`), then they spring back to the middle; a filter on chip tap at the
current intensity. Each is one Undo step. `ToolViewModel.apply` drops a request while an edit
runs, so a change made during a running edit is lost (no queue).

## Testing facts

- `FilterLut(bitmap)` takes any Bitmap, tests pass generated cubes/strips; the real source is
  `FilterLut.fromAssets { path -> context.assets.open(path) }` (caches decoded LUTs).
- `EditFilterTest` reads the real assets under Robolectric (`isIncludeAndroidResources = true`).
- M3 `Slider`'s semantics `SetProgress` calls `onValueChange` then `onValueChangeFinished`, which
  is how `EditToolTest` commits a slider change.
