package com.phongkaster.kanso.data.edit.edit

/**
 * The HSL tool's 27 values: a hue, a saturation and a lightness amount for each of the nine
 * [ColourRange]s. Each is stored from -1 (the tool's -100) through 0 (no change) to +1 (+100).
 *
 * Each list holds nine numbers, and the index of a range is its [ColourRange.ordinal]. The value is
 * immutable: [with] makes a new one, which is what [EditSettings] wants for state.
 */
data class HslAdjustments(
    /** The hue amount of each range, nine values in [ColourRange] order. */
    val hue: List<Float> = ZEROS,
    /** The saturation amount of each range, nine values in [ColourRange] order. */
    val saturation: List<Float> = ZEROS,
    /** The lightness amount of each range, nine values in [ColourRange] order. */
    val lightness: List<Float> = ZEROS,
) {

    /** Checks that each channel holds exactly one value per colour range. */
    init {
        val expected = ColourRange.entries.size
        require(hue.size == expected) { "hue needs $expected values, got ${hue.size}" }
        require(saturation.size == expected) { "saturation needs $expected values, got ${saturation.size}" }
        require(lightness.size == expected) { "lightness needs $expected values, got ${lightness.size}" }
    }

    /** The amount stored for [channel] of [range], from -1 to 1. */
    operator fun get(range: ColourRange, channel: HslChannel): Float = list(channel)[range.ordinal]

    /**
     * A copy with [channel] of [range] set to [value], clamped to -1..1 so no caller can store an
     * out-of-range amount. A NaN is stored as 0, so a bad number never gets in.
     */
    fun with(range: ColourRange, channel: HslChannel, value: Float): HslAdjustments {
        val changed = list(channel).toMutableList()
        changed[range.ordinal] = (if (value.isNaN()) 0f else value).coerceIn(-1f, 1f)
        return when (channel) {
            HslChannel.HUE -> copy(hue = changed)
            HslChannel.SATURATION -> copy(saturation = changed)
            HslChannel.LIGHTNESS -> copy(lightness = changed)
        }
    }

    /** True when all 27 values are 0, so the tool changes nothing. */
    val isNeutral: Boolean
        get() = hue.all { it == 0f } && saturation.all { it == 0f } && lightness.all { it == 0f }

    /** The list that holds [channel]. */
    private fun list(channel: HslChannel): List<Float> = when (channel) {
        HslChannel.HUE -> hue
        HslChannel.SATURATION -> saturation
        HslChannel.LIGHTNESS -> lightness
    }

    /** The shared neutral value. */
    companion object {
        /** Nine zeros: the starting list of every channel. */
        private val ZEROS: List<Float> = List(ColourRange.entries.size) { 0f }

        /** All 27 values at 0: nothing changed. */
        val NEUTRAL = HslAdjustments()
    }
}
