package com.astronex.photoedit.core

/**
 * Everything the Edit screen has done to the photo so far, as one immutable value: the crop
 * rectangle, quarter turns, flips, four colour sliders, a filter, the Face and Body reshapes, the
 * texts and dates laid on the photo ([layers]), the three RGB channels, the HSL tool's amounts
 * ([hsl]) and the four tone curves ([curves]). There is one such value for
 * the whole editing session (held by [EditHistory]); every tap or slider move makes a new one.
 *
 * Immutable means a value never changes after it is made; a change makes a new value with
 * `copy(...)`. That is what Compose wants for state (a new value is a visible change) and what
 * lets a processor hold its settings safely while it runs on another thread.
 *
 * **The whole state, never a change.** [PhotoRenderer] always renders a complete value against the
 * ORIGINAL photo, so a value describes the finished picture, not a step towards it: choosing a
 * second filter replaces the first one (it does not pile on top), and a slider at 70 means 70, not
 * "70 more". That is also why Undo can simply go back to an earlier value (see [EditHistory]).
 *
 * The default value changes nothing: the whole photo, no turn, no flip, every slider at its middle
 * and no filter ([isNeutral]).
 *
 * **Reshapes come first.** The reshapes ([FaceReshape], [BodyReshape]) move the ORIGINAL's pixels
 * before anything else is done, so the crop rectangle, which is in fractions of the original, keeps
 * the same part of the photo whatever the reshapes do.
 */
data class PhotoEdit(
    /**
     * The rectangle of the original photo to keep, as fractions of its width and height. Every
     * crop is this one rectangle, whatever ratio was chosen: a fixed ratio simply sets a rectangle
     * of that shape (see [EditHistory.cropRatioChosen]). [CropRect.FULL] keeps the whole photo.
     */
    val crop: CropRect = CropRect.FULL,
    /**
     * The shape the crop handles are locked to, or null (or [CropAspect.FREE]) when the person may
     * drag them to any shape. It only constrains how [crop] is edited; the processor never reads it.
     */
    val cropRatio: CropAspect? = null,
    /**
     * How many quarter turns to turn the photo: positive is clockwise (to the right), negative
     * anticlockwise. Four turns, in either direction, give the photo back as it was.
     */
    val quarterTurns: Int = 0,
    /** True to mirror the photo left to right. */
    val flipHorizontal: Boolean = false,
    /** True to mirror the photo top to bottom. */
    val flipVertical: Boolean = false,
    /** Brightness, from [MIN] (darker) through [MIDDLE] (unchanged) to [MAX] (lighter). */
    val brightness: Float = MIDDLE,
    /** Contrast, from [MIN] (flatter) through [MIDDLE] (unchanged) to [MAX] (punchier). */
    val contrast: Float = MIDDLE,
    /** Saturation, from [MIN] (black and white) through [MIDDLE] (unchanged) to [MAX] (vivid). */
    val saturation: Float = MIDDLE,
    /** Warmth, from [MIN] (cooler, bluer) through [MIDDLE] (unchanged) to [MAX] (warmer, redder). */
    val warmth: Float = MIDDLE,
    /** The one colour filter laid over the photo; [PhotoFilter.NONE] for none. */
    val filter: PhotoFilter = PhotoFilter.NONE,
    /**
     * How strongly the filter is applied, from 0 (not at all: the photo as it was) to 1 (fully).
     * In between, each pixel is mixed from the unfiltered and the fully filtered colour.
     */
    val filterIntensity: Float = 1f,
    /** Face > Width, from [MIN] (wider) through [MIDDLE] (unchanged) to [MAX] (narrower). */
    val faceWidth: Float = MIDDLE,
    /** Face > Lift, from [MIN] (the jaw dropped) through [MIDDLE] (unchanged) to [MAX] (lifted). */
    val faceLift: Float = MIDDLE,
    /** Face > Temple, from [MIN] (hollower) through [MIDDLE] (unchanged) to [MAX] (fuller). */
    val faceTemple: Float = MIDDLE,
    /** Face > Cheekbones, from [MIN] (wider) through [MIDDLE] (unchanged) to [MAX] (slimmer). */
    val faceCheekbones: Float = MIDDLE,
    /** Face > Chin length, from [MIN] (longer) through [MIDDLE] (unchanged) to [MAX] (shorter). */
    val faceChinLength: Float = MIDDLE,
    /** Face > Hairline, from [MIN] (higher) through [MIDDLE] (unchanged) to [MAX] (lower). */
    val faceHairline: Float = MIDDLE,
    /** Body > Waist, from [MIN] (wider) through [MIDDLE] (unchanged) to [MAX] (slimmer). */
    val bodyWaist: Float = MIDDLE,
    /** Body > Arms, from [MIN] (thicker) through [MIDDLE] (unchanged) to [MAX] (slimmer). */
    val bodyArms: Float = MIDDLE,
    /** Body > Chest, from [MIN] (flatter) through [MIDDLE] (unchanged) to [MAX] (fuller). */
    val bodyChest: Float = MIDDLE,
    /** Body > Legs, from [MIN] (thicker) through [MIDDLE] (unchanged) to [MAX] (slimmer). */
    val bodyLegs: Float = MIDDLE,
    /**
     * The texts and dates on the photo, oldest first: a later one is drawn on top of an earlier
     * one. At most [MAX_LAYERS]. Which one is selected is not kept here (see [PhotoLayer]).
     */
    val layers: List<PhotoLayer> = emptyList(),
    /**
     * RGB > Red: red is multiplied by `1 + red`, from [MIN] (x0, no red: the photo turns cyan)
     * through [MIDDLE] (unchanged) to [MAX] (x2). Added after [layers] so calls that pass the
     * earlier fields by position still compile.
     */
    val red: Float = MIDDLE,
    /** RGB > Green: green is multiplied by `1 + green`, from x0 (magenta) through x1 to x2. */
    val green: Float = MIDDLE,
    /** RGB > Blue: blue is multiplied by `1 + blue`, from x0 (yellow) through x1 to x2. */
    val blue: Float = MIDDLE,
    /**
     * The HSL tool's 27 amounts: hue, saturation and lightness for each of nine colour ranges.
     * [HslAdjustments.NEUTRAL] changes nothing.
     */
    val hsl: HslAdjustments = HslAdjustments.NEUTRAL,
    /** The Curves tool's four tone curves (master, red, green, blue). [ToneCurves.NEUTRAL] changes nothing. */
    val curves: ToneCurves = ToneCurves.NEUTRAL,
    /**
     * Face > Eyes > Size, from [MIN] (smaller eyes) through [MIDDLE] (unchanged) to [MAX] (bigger).
     * Added after [curves] so calls that pass the earlier fields by position still compile.
     */
    val faceEyeSize: Float = MIDDLE,
    /** Face > Nose > Size, from [MIN] (wider) through [MIDDLE] (unchanged) to [MAX] (narrower). */
    val faceNoseSize: Float = MIDDLE,
    /** Face > Lips > Size, from [MIN] (bigger lips) through [MIDDLE] (unchanged) to [MAX] (smaller). */
    val faceLipSize: Float = MIDDLE,
    /** Head size, from [MIN] (a bigger head) through [MIDDLE] (unchanged) to [MAX] (smaller). */
    val faceHeadSize: Float = MIDDLE,
    /** Face > Jawline, from [MIN] (a wider jaw) through [MIDDLE] (unchanged) to [MAX] (narrower). */
    val faceJawline: Float = MIDDLE,
    /** Face > Chin shape, from [MIN] (a wider chin) through [MIDDLE] (unchanged) to [MAX] (narrower). */
    val faceChinShape: Float = MIDDLE,
    /**
     * Face > Smooth, from [MIN] (the outline's bumps stronger) through [MIDDLE] (unchanged) to [MAX]
     * (smoothest). Added after [faceChinShape] so calls that pass the earlier fields by position still
     * compile.
     */
    val faceSmooth: Float = MIDDLE,
    /** Face > Chin tip, from [MIN] (a rounder chin) through [MIDDLE] (unchanged) to [MAX] (more pointed). */
    val faceChinTip: Float = MIDDLE,
    /** Face > Jaw angle, from [MIN] (a squarer jaw) through [MIDDLE] (unchanged) to [MAX] (corners raised, drawn in). */
    val faceJawAngle: Float = MIDDLE,
    /** Face > Double chin, from [MIN] (the jaw's lower edge lowered) through [MIDDLE] (unchanged) to [MAX] (lifted). */
    val faceDoubleChin: Float = MIDDLE,
) {

    /** True while the photo holds fewer than [MAX_LAYERS] layers, so another may be added. */
    val canAddLayer: Boolean
        get() = layers.size < MAX_LAYERS

    /**
     * True when at least one of Adjust's four colour sliders is away from its middle. The RGB
     * channels are not counted here: see [hasRgbChange].
     */
    val hasColourChange: Boolean
        get() = brightness != MIDDLE || contrast != MIDDLE || saturation != MIDDLE || warmth != MIDDLE

    /** True when at least one of the three RGB channels is away from its middle. */
    val hasRgbChange: Boolean
        get() = red != MIDDLE || green != MIDDLE || blue != MIDDLE

    /** True when at least one of the HSL tool's 27 amounts is away from 0. */
    val hasHslChange: Boolean
        get() = !hsl.isNeutral

    /** True when at least one of the four tone curves is bent away from the straight diagonal. */
    val hasCurveChange: Boolean
        get() = !curves.isNeutral

    /**
     * True when the settings hold per-pixel work the GPU cannot draw (a filter, HSL or a curve), so
     * the live preview needs its small CPU copy ([FilterPreviewer]).
     */
    internal val hasPreviewStage: Boolean
        get() = hasFilter || hasHslChange || hasCurveChange

    /**
     * Only the settings the preview's CPU copy is rendered from (filter and its intensity, HSL, curves),
     * every other setting at its default; the intensity is left at its default while no filter
     * changes anything, so two settings whose copies would be alike give equal values.
     */
    internal val previewStage: PhotoEdit
        get() = PhotoEdit(
            filter = if (hasFilter) filter else PhotoFilter.NONE,
            filterIntensity = if (hasFilter) filterIntensity else 1f,
            hsl = hsl,
            curves = curves,
        )

    /** The amount of the RGB channel [channel], from [MIN] to [MAX]. */
    fun amountOf(channel: RgbChannel): Float = when (channel) {
        RgbChannel.RED -> red
        RgbChannel.GREEN -> green
        RgbChannel.BLUE -> blue
    }

    /** These settings with the RGB channel [channel] set to [amount] (not clamped here). */
    fun withAmount(channel: RgbChannel, amount: Float): PhotoEdit = when (channel) {
        RgbChannel.RED -> copy(red = amount)
        RgbChannel.GREEN -> copy(green = amount)
        RgbChannel.BLUE -> copy(blue = amount)
    }

    /** True when a filter is chosen and its intensity is above 0, so it changes the photo. */
    val hasFilter: Boolean
        get() = filter != PhotoFilter.NONE && filterIntensity > 0f

    /** True when at least one Face reshape is away from its middle. */
    val hasFaceReshape: Boolean
        get() = FaceReshape.entries.any { amountOf(it) != MIDDLE }

    /** True when at least one Body reshape is away from its middle. */
    val hasBodyReshape: Boolean
        get() = BodyReshape.entries.any { amountOf(it) != MIDDLE }

    /** The value of the Face reshape [reshape], from [MIN] to [MAX]. */
    fun amountOf(reshape: FaceReshape): Float = when (reshape) {
        FaceReshape.WIDTH -> faceWidth
        FaceReshape.LIFT -> faceLift
        FaceReshape.TEMPLE -> faceTemple
        FaceReshape.CHEEKBONES -> faceCheekbones
        FaceReshape.CHIN_LENGTH -> faceChinLength
        FaceReshape.HAIRLINE -> faceHairline
        FaceReshape.EYE_SIZE -> faceEyeSize
        FaceReshape.NOSE_SIZE -> faceNoseSize
        FaceReshape.LIP_SIZE -> faceLipSize
        FaceReshape.HEAD_SIZE -> faceHeadSize
        FaceReshape.JAWLINE -> faceJawline
        FaceReshape.CHIN_SHAPE -> faceChinShape
        FaceReshape.SMOOTH -> faceSmooth
        FaceReshape.CHIN_TIP -> faceChinTip
        FaceReshape.JAW_ANGLE -> faceJawAngle
        FaceReshape.DOUBLE_CHIN -> faceDoubleChin
    }

    /** The value of the Body reshape [reshape], from [MIN] to [MAX]. */
    fun amountOf(reshape: BodyReshape): Float = when (reshape) {
        BodyReshape.WAIST -> bodyWaist
        BodyReshape.ARMS -> bodyArms
        BodyReshape.CHEST -> bodyChest
        BodyReshape.LEGS -> bodyLegs
    }

    /** These settings with the Face reshape [reshape] set to [amount] (not clamped here). */
    fun withAmount(reshape: FaceReshape, amount: Float): PhotoEdit = when (reshape) {
        FaceReshape.WIDTH -> copy(faceWidth = amount)
        FaceReshape.LIFT -> copy(faceLift = amount)
        FaceReshape.TEMPLE -> copy(faceTemple = amount)
        FaceReshape.CHEEKBONES -> copy(faceCheekbones = amount)
        FaceReshape.CHIN_LENGTH -> copy(faceChinLength = amount)
        FaceReshape.HAIRLINE -> copy(faceHairline = amount)
        FaceReshape.EYE_SIZE -> copy(faceEyeSize = amount)
        FaceReshape.NOSE_SIZE -> copy(faceNoseSize = amount)
        FaceReshape.LIP_SIZE -> copy(faceLipSize = amount)
        FaceReshape.HEAD_SIZE -> copy(faceHeadSize = amount)
        FaceReshape.JAWLINE -> copy(faceJawline = amount)
        FaceReshape.CHIN_SHAPE -> copy(faceChinShape = amount)
        FaceReshape.SMOOTH -> copy(faceSmooth = amount)
        FaceReshape.CHIN_TIP -> copy(faceChinTip = amount)
        FaceReshape.JAW_ANGLE -> copy(faceJawAngle = amount)
        FaceReshape.DOUBLE_CHIN -> copy(faceDoubleChin = amount)
    }

    /** These settings with the Body reshape [reshape] set to [amount] (not clamped here). */
    fun withAmount(reshape: BodyReshape, amount: Float): PhotoEdit = when (reshape) {
        BodyReshape.WAIST -> copy(bodyWaist = amount)
        BodyReshape.ARMS -> copy(bodyArms = amount)
        BodyReshape.CHEST -> copy(bodyChest = amount)
        BodyReshape.LEGS -> copy(bodyLegs = amount)
    }

    /**
     * Only the reshape amounts of these settings (every other setting at its default): what the
     * reshape render depends on, so two settings that reshape alike give equal values.
     */
    val reshapeOnly: PhotoEdit
        get() = PhotoEdit(
            faceWidth = faceWidth,
            faceLift = faceLift,
            faceTemple = faceTemple,
            faceCheekbones = faceCheekbones,
            faceChinLength = faceChinLength,
            faceHairline = faceHairline,
            bodyWaist = bodyWaist,
            bodyArms = bodyArms,
            bodyChest = bodyChest,
            bodyLegs = bodyLegs,
            faceEyeSize = faceEyeSize,
            faceNoseSize = faceNoseSize,
            faceLipSize = faceLipSize,
            faceHeadSize = faceHeadSize,
            faceJawline = faceJawline,
            faceChinShape = faceChinShape,
            faceSmooth = faceSmooth,
            faceChinTip = faceChinTip,
            faceJawAngle = faceJawAngle,
            faceDoubleChin = faceDoubleChin,
        )

    /**
     * True when nothing is changed: every tool's controls change nothing right now (see
     * [isNeutral] for one tool). A crop shape that is only locked, four turns that come back
     * round, or a filter at intensity 0 therefore still count as neutral, matching the dots on the
     * tool row. The screen uses it to know whether there is anything to save or to lose.
     */
    val isNeutral: Boolean
        get() = PhotoTool.entries.all { isNeutral(it) }

    /**
     * True when the controls of [tool]'s sheet change nothing right now, so the tool row can mark
     * the tools that do hold a change:
     *
     * - [PhotoTool.CROP]: the crop keeps the whole photo;
     * - [PhotoTool.ROTATE]: the turns come back round to upright and neither flip is on;
     * - [PhotoTool.ADJUST]: all four sliders sit at their middle;
     * - [PhotoTool.RGB]: all three channels sit at their middle;
     * - [PhotoTool.HSL]: all 27 HSL amounts sit at 0;
     * - [PhotoTool.CURVES]: all four curves are the straight diagonal;
     * - [PhotoTool.FILTERS]: no filter, or one at intensity 0;
     * - [PhotoTool.FACE]: every Face reshape sits at its middle;
     * - [PhotoTool.BODY]: every Body reshape sits at its middle;
     * - [PhotoTool.TEXT]: no text with words is on the photo;
     * - [PhotoTool.DATE]: no date is on the photo.
     */
    fun isNeutral(tool: PhotoTool): Boolean = when (tool) {
        PhotoTool.CROP -> crop == CropRect.FULL
        PhotoTool.ROTATE -> quarterTurns % 4 == 0 && !flipHorizontal && !flipVertical
        PhotoTool.ADJUST -> !hasColourChange
        PhotoTool.RGB -> !hasRgbChange
        PhotoTool.HSL -> !hasHslChange
        PhotoTool.CURVES -> !hasCurveChange
        PhotoTool.FILTERS -> !hasFilter
        PhotoTool.FACE -> !hasFaceReshape
        PhotoTool.BODY -> !hasBodyReshape
        // A text still waiting for its first letter is not kept, so it changes nothing yet.
        PhotoTool.TEXT -> layers.none(predicate = { (it.content as? LayerVisual.Text)?.isBlank == false })
        PhotoTool.DATE -> layers.none(predicate = { it.content is LayerVisual.Date })
    }

    /**
     * A rectangle inside the photo, each edge given as a fraction from 0 to 1 of the photo's
     * width ([left], [right]) or height ([top], [bottom]), measured from its top-left corner.
     * Fractions, rather than pixels, keep the rectangle right whatever size the photo is worked on.
     */
    data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {

        /** The whole-photo rectangle. */
        companion object {
            /** The rectangle that keeps the whole photo. */
            val FULL = CropRect(0f, 0f, 1f, 1f)
        }
    }

    /** The slider range shared by the colour sliders and the reshapes, and the layer limit. */
    companion object {
        /** The lowest value of a colour slider. */
        const val MIN = -1f

        /** The middle of a colour slider: no change at all. */
        const val MIDDLE = 0f

        /** The highest value of a colour slider. */
        const val MAX = 1f

        /** The most layers (texts and dates together) one photo may hold. */
        const val MAX_LAYERS = 20
    }
}
