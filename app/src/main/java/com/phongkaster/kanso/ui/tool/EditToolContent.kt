package com.phongkaster.kanso.ui.tool

import android.graphics.Bitmap
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import com.phongkaster.kanso.data.edit.edit.EditColourMatrix
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.phongkaster.kanso.R
import com.phongkaster.kanso.data.edit.edit.EditSettings
import com.phongkaster.kanso.data.edit.edit.EditTool
import com.phongkaster.kanso.data.settings.ThemeMode
import com.phongkaster.kanso.navigation.Tool
import com.phongkaster.kanso.ui.theme.KansoTheme

/** Test tag of the open tool's panel, which appears just above the row of tools while a tool is open. */
const val EDIT_SHEET_TAG = "edit-sheet"

/**
 * The fixed height of the panel area above the row of tools, open or not. It fits the tallest
 * panel, Adjust's or Filters' (a row of chips with a 48 dp touch target, plus one 48 dp slider
 * row), with the surface's padding and the gap above the row.
 */
private val SHEET_HEIGHT = 136.dp

/**
 * The Edit screen's photo area, stateless: the photo (or, while Crop is open, the cropper) filling
 * all the space between the top bar and the bottom bar. It draws [state] and reports nothing: Undo
 * and Compare live in the bottom bar's row ([EditToolRow]).
 *
 * **Why the photo never changes size.** The row of tools and the open tool's panel live in the
 * screen's bottom bar ([EditBottomBar]). `Scaffold` measures that bar first and hands this area
 * what is left. The bar is one fixed height whether a tool is open or not and whichever tool it
 * is, so the photo keeps one size while the person moves between tools. Nothing ever covers or
 * dims it.
 *
 * [photo] draws the photo (the screen passes its photo card); it is a slot, so this function knows
 * nothing of bitmaps or processors and a preview can pass a plain sample.
 *
 * [cropper], when given, takes the photo's place while the Crop panel is open (the screen passes
 * [EditCropper]); while Compare is on, or with no cropper given, [photo] is drawn as usual.
 */
@Composable
fun EditPhotoArea(
    state: ToolUiState,
    photo: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    cropper: (@Composable () -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxSize()) {
        // While Crop is open the photo card becomes the cropper (handles, drag, pinch); while
        // comparing, the plain photo is shown instead, so the original can be seen.
        if (state.openTool == EditTool.CROP && !state.comparing && cropper != null) {
            cropper()
        } else {
            photo()
        }
    }
}

/**
 * The Edit screen's bottom bar, the lowest thing on screen: one row of Crop, Rotate, Adjust,
 * Filters and Compare, and just above it the fixed-height panel area, which holds the open tool's
 * panel ([sheet]) or a hint when none is open. It is passed to `Scaffold`'s `bottomBar` slot, so
 * the photo area above always ends where this bar begins, and since the bar never changes height
 * neither does the photo.
 *
 * The bar keeps clear of the navigation bar and of the bottom gesture area: with swipe gestures
 * the navigation bar can be 0 dp tall while a swipe up from the edge still means "go home".
 *
 * **Why the panel is not a modal bottom sheet.** Material's `ModalBottomSheet` dims everything
 * behind it and swallows touches outside it, so the entries of the row could not
 * be tapped while a panel is open. Here the panel is an ordinary surface in the bar: nothing dims
 * the photo and the row stays in reach.
 *
 * [onOpenTool] is a tool entry tapped (the ViewModel opens that tool, or closes it if it was
 * open). While Compare is on, or Done is rendering, the row is disabled; the screen also passes
 * that flag to the panel's controls, which disable themselves.
 */
@Composable
fun EditBottomBar(
    state: ToolUiState,
    onOpenTool: (EditTool) -> Unit,
    sheet: @Composable (EditTool) -> Unit,
    modifier: Modifier = Modifier,
    onCompare: (Boolean) -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.navigationBars.union(WindowInsets.systemGestures.only(WindowInsetsSides.Bottom)),
            )
            .padding(bottom = 8.dp),
    ) {
        EditSheet(
            openTool = state.openTool,
            sheet = sheet,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        // The session's settings tell the row which tools hold a change (their dot).
        EditToolRow(
            openTool = state.openTool,
            enabled = !state.comparing && !state.finishing,
            onOpen = onOpenTool,
            settings = state.settings,
            comparing = state.comparing,
            // Compare is the way back from comparing, so only Done's render switches it off.
            compareEnabled = !state.finishing,
            onCompare = onCompare,
        )
    }
}

/**
 * The open tool's panel, just above the row of tools, in an area of one fixed height
 * ([SHEET_HEIGHT]) whether a tool is open or not and whichever tool it is. That is what keeps the
 * photo one size: the bottom bar never changes height, so the space `Scaffold` leaves the photo
 * never changes either. Every tool's controls are made to fit it (Adjust shows one slider at a
 * time, picked by chips); with very large text they scroll inside it rather than grow it.
 *
 * With [openTool] null the area shows a short hint to pick a tool. Otherwise it is a rounded
 * surface holding [sheet] for that tool, tagged [EDIT_SHEET_TAG] and described to screen readers
 * as the tool settings, its controls centred in it.
 *
 * `AnimatedContent` (Compose's own animation API) cross-fades between what it shows for one value
 * and the next: the hint and a tool's panel, or one tool's controls and another's. Only what is
 * inside the area fades; the area itself never moves.
 */
@Composable
private fun EditSheet(
    openTool: EditTool?,
    sheet: @Composable (EditTool) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetText = stringResource(R.string.edit_layout_sheet)
    AnimatedContent(
        targetState = openTool,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "edit-sheet",
        modifier = modifier
            .fillMaxWidth()
            .height(SHEET_HEIGHT)
            .padding(bottom = 8.dp),
    ) { tool ->
        if (tool != null) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(EDIT_SHEET_TAG)
                    .semantics { contentDescription = sheetText },
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    sheet(tool)
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.edit_sheet_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

/** Test tag of the Edit screen's live preview of the photo ([LivePreviewImage]). */
const val LIVE_PREVIEW_TAG = "live-preview"

/**
 * The colour matrix [LivePreviewImage] applies, as its 20 numbers (4 rows of 5, row after row):
 * exactly the identity matrix when no colour slider has moved. It lives in the node's semantics
 * (the description of the screen that tests and screen readers read), so a test can check which
 * colours are drawn without reading pixels.
 */
val LivePreviewMatrix = SemanticsPropertyKey<List<Float>>("LivePreviewMatrix")

/** Writes [LivePreviewMatrix] in a `semantics { }` block; reading it back is for tests only. */
var SemanticsPropertyReceiver.livePreviewMatrix by LivePreviewMatrix

/**
 * The geometry [LivePreviewImage] applies, as stable text such as
 * `turns=1;flipH=true;flipV=false;crop=0.0,0.0,1.0,1.0` (the quarter turns 0 to 3, the two flips,
 * then the crop's left, top, right and bottom as fractions of the photo), for tests to read.
 */
val LivePreviewGeometry = SemanticsPropertyKey<String>("LivePreviewGeometry")

/** Writes [LivePreviewGeometry] in a `semantics { }` block; reading it back is for tests only. */
var SemanticsPropertyReceiver.livePreviewGeometry by LivePreviewGeometry

/** The identity colour matrix's 20 numbers: ones on the diagonal (places 0, 6, 12, 18). */
private val IDENTITY_MATRIX = List(20) { index -> if (index % 6 == 0) 1f else 0f }

/**
 * Draws [bitmap] with the Edit [settings] applied live, every frame, by Compose itself, filling
 * the space [modifier] gives it (it fills its parent by default).
 *
 * [bitmap] is the original photo, or its small copy with the filter already applied (the
 * ViewModel's `filterPreview`): the filter is the one part Compose cannot draw. Everything else
 * is applied here while drawing, so the photo follows a slider while the finger is still on it
 * and no full-size picture is ever rendered before the edit is finished:
 *
 * 1. **Crop.** Only the rectangle `settings.crop` (fractions of the photo, turned into pixels the
 *    same way the final render does) is taken from [bitmap].
 * 2. **Quarter turns, then flips.** The cropped part is turned clockwise, then mirrored along
 *    the screen's axes, the same order as the final render (crop, turn, flip). It is scaled to
 *    fit inside the box whole, keeping its shape, and centred.
 * 3. **Colour sliders.** Brightness, contrast, saturation and warmth become one colour matrix
 *    ([EditColourMatrix.of], the very numbers the final render uses), handed to the GPU as a
 *    `ColorFilter.colorMatrix`; with every slider in the middle no filter is set at all.
 *
 * The node carries [LIVE_PREVIEW_TAG], and [LivePreviewMatrix] and [LivePreviewGeometry] say
 * what is applied, for tests.
 */
@Composable
fun LivePreviewImage(bitmap: Bitmap, settings: EditSettings, modifier: Modifier = Modifier) {
    // asImageBitmap wraps the Android bitmap for Compose without copying its pixels; remember
    // keeps the wrapper until another bitmap arrives.
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val matrix = remember(settings) { EditColourMatrix.of(settings) }
    val identity = remember(matrix) { EditColourMatrix.isIdentity(matrix) }
    val colourFilter = remember(matrix, identity) {
        if (identity) null else ColorFilter.colorMatrix(ColorMatrix(matrix))
    }
    val matrixNumbers = if (identity) IDENTITY_MATRIX else matrix.toList()
    val turns = ((settings.quarterTurns % 4) + 4) % 4
    val crop = cropPixels(bitmap.width, bitmap.height, settings.crop)
    val geometry = "turns=$turns;flipH=${settings.flipHorizontal};flipV=${settings.flipVertical};" +
        "crop=${settings.crop.left},${settings.crop.top},${settings.crop.right},${settings.crop.bottom}"
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag(LIVE_PREVIEW_TAG)
            .semantics {
                livePreviewMatrix = matrixNumbers
                livePreviewGeometry = geometry
            },
    ) {
        // A quarter or three-quarter turn swaps the picture's width and height on screen.
        val sideways = turns % 2 == 1
        val shownWidth = if (sideways) crop.height else crop.width
        val shownHeight = if (sideways) crop.width else crop.height
        // "Contain": the largest scale at which the whole turned picture fits in the box.
        val fit = min(size.width / shownWidth, size.height / shownHeight)
        val drawWidth = (crop.width * fit).roundToInt()
        val drawHeight = (crop.height * fit).roundToInt()
        val centre = center
        // withTransform applies its steps to the drawing in reverse order of the calls: the
        // picture, drawn centred on (0, 0), is first turned, then mirrored in the screen's axes,
        // then moved to the middle of the box. Both pivots are (0, 0), the picture's centre.
        withTransform({
            translate(centre.x, centre.y)
            scale(
                scaleX = if (settings.flipHorizontal) -1f else 1f,
                scaleY = if (settings.flipVertical) -1f else 1f,
                pivot = Offset.Zero,
            )
            rotate(degrees = 90f * turns, pivot = Offset.Zero)
        }) {
            drawImage(
                image = image,
                srcOffset = IntOffset(crop.left, crop.top),
                srcSize = IntSize(crop.width, crop.height),
                dstOffset = IntOffset(-drawWidth / 2, -drawHeight / 2),
                dstSize = IntSize(drawWidth, drawHeight),
                colorFilter = colourFilter,
            )
        }
    }
}

/**
 * The crop [rect] (fractions of the photo) as pixels of a [width] x [height] picture, rounded the
 * same way the final render rounds it: edges put in order, kept inside the picture, at least
 * 1 x 1. So the preview takes exactly the rectangle Done will keep.
 */
private fun cropPixels(width: Int, height: Int, rect: EditSettings.CropRect): IntRect {
    val left = (min(rect.left, rect.right).coerceIn(0f, 1f) * width).roundToInt().coerceIn(0, width - 1)
    val top = (min(rect.top, rect.bottom).coerceIn(0f, 1f) * height).roundToInt().coerceIn(0, height - 1)
    val right = (maxOf(rect.left, rect.right).coerceIn(0f, 1f) * width).roundToInt().coerceIn(left + 1, width)
    val bottom = (maxOf(rect.top, rect.bottom).coerceIn(0f, 1f) * height).roundToInt().coerceIn(top + 1, height)
    return IntRect(left, top, right, bottom)
}

/**
 * A made-up 320 x 240 photo for the previews: a diagonal blend from blue to orange, so the crop,
 * the turn and the colour change are all visible. Nothing is decoded from a file.
 */
private fun sampleGradientBitmap(): Bitmap {
    val bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
    val paint = Paint().apply {
        shader = LinearGradient(
            0f, 0f, 320f, 240f,
            0xFF1A73E8.toInt(), 0xFFF29900.toInt(),
            Shader.TileMode.CLAMP,
        )
    }
    android.graphics.Canvas(bitmap).drawRect(0f, 0f, 320f, 240f, paint)
    return bitmap
}

/**
 * The live preview of a photo cropped to its left two thirds, turned a quarter, mirrored and
 * made warmer and brighter, in light and in dark.
 */
@PreviewLightDark
@Composable
private fun LivePreviewImagePreview() {
    val bitmap = remember { sampleGradientBitmap() }
    KansoTheme(themeMode = ThemeMode.System) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
        ) {
            LivePreviewImage(
                bitmap = bitmap,
                settings = EditSettings(
                    crop = EditSettings.CropRect(0f, 0f, 0.66f, 1f),
                    quarterTurns = 1,
                    flipHorizontal = true,
                    brightness = 0.2f,
                    warmth = 0.5f,
                ),
            )
        }
    }
}

/**
 * A sample Edit screen for the previews, laid out as the real one: a `Scaffold` whose bottom bar
 * is [EditBottomBar], with [EditPhotoArea] above it. Plain coloured boxes stand in for the photo
 * and the cropper, and a line of text for the panel, so nothing touches a ViewModel or decodes a
 * photo.
 */
@Composable
private fun SampleEditToolContent(state: ToolUiState) {
    KansoTheme(themeMode = ThemeMode.System) {
        Scaffold(
            bottomBar = {
                EditBottomBar(
                    state = state,
                    onOpenTool = {},
                    sheet = { tool -> Text(text = tool.name) },
                )
            },
        ) { innerPadding ->
            EditPhotoArea(
                state = state,
                photo = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                    )
                },
                // A box of another colour stands in for the cropper, so Crop's swap is visible.
                cropper = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.secondaryContainer),
                    )
                },
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
        }
    }
}

/**
 * The Edit screen with Adjust's panel open above the row of tools and an edit to undo, in light
 * and in dark: the sample settings raise brightness, so Adjust's entry carries the "changed" dot.
 */
@PreviewLightDark
@Composable
private fun EditToolContentPreview() {
    SampleEditToolContent(
        ToolUiState(
            tool = Tool.EDIT,
            photoUri = "content://sample/photo/1",
            canUndo = true,
            openTool = EditTool.ADJUST,
            settings = EditSettings(brightness = 0.3f),
        ),
    )
}

/**
 * The Edit screen with no tool open, in light and in dark: the row of tools sits alone at the
 * bottom and the photo takes all the space above it.
 */
@PreviewLightDark
@Composable
private fun EditToolContentClosedPreview() {
    SampleEditToolContent(ToolUiState(tool = Tool.EDIT, photoUri = "content://sample/photo/1"))
}

/**
 * The Edit screen with Crop open, in light and in dark: the cropper (here a plain stand-in box)
 * takes the photo's place, and Crop's panel above the row of tools.
 */
@PreviewLightDark
@Composable
private fun EditToolContentCropPreview() {
    SampleEditToolContent(
        ToolUiState(
            tool = Tool.EDIT,
            photoUri = "content://sample/photo/1",
            openTool = EditTool.CROP,
        ),
    )
}

/** The Edit screen while Compare is on, in light and in dark: the row and Undo are faint. */
@PreviewLightDark
@Composable
private fun EditToolContentComparingPreview() {
    SampleEditToolContent(
        ToolUiState(
            tool = Tool.EDIT,
            photoUri = "content://sample/photo/1",
            canUndo = true,
            openTool = EditTool.FILTERS,
            comparing = true,
        ),
    )
}
