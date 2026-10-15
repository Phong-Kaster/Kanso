package com.phongkaster.kanso.launcher

/**
 * The launcher icons Kanso ships: the same "enso lens" mark on nine background colours.
 *
 * Android cannot repaint an app's launcher icon at run time, so each colour is a separate
 * launcher entry (an `activity-alias` in AndroidManifest.xml, named [aliasClassName]) with its own
 * icon, and [LauncherIconSwitcher] keeps exactly one of them enabled. [argb] is the background
 * colour of that icon, the same value as its `ic_launcher_*` colour resource; [closestTo] uses it
 * to pick the icon for any theme colour.
 */
enum class LauncherIcon(val aliasClassName: String, val argb: Int) {
    /** Kanso blue, the default theme colour; the icon a fresh install shows. */
    KansoBlue("com.phongkaster.kanso.LauncherKansoBlue", 0xFF1A73E8.toInt()),

    /** A deep teal, for blue-green theme colours. */
    Teal("com.phongkaster.kanso.LauncherTeal", 0xFF00796B.toInt()),

    /** A deep green. */
    Green("com.phongkaster.kanso.LauncherGreen", 0xFF2E7D32.toInt()),

    /** A dark amber, for yellow and gold theme colours. */
    Amber("com.phongkaster.kanso.LauncherAmber", 0xFFB26A00.toInt()),

    /** A deep orange. */
    Orange("com.phongkaster.kanso.LauncherOrange", 0xFFD84315.toInt()),

    /** A deep red. */
    Red("com.phongkaster.kanso.LauncherRed", 0xFFC62828.toInt()),

    /** A deep pink, for pink and magenta theme colours. */
    Pink("com.phongkaster.kanso.LauncherPink", 0xFFAD1457.toInt()),

    /** A deep purple, for violet theme colours. */
    Purple("com.phongkaster.kanso.LauncherPurple", 0xFF6A1B9A.toInt()),

    /** Near-black, for greys, black and white, which have no hue to match. */
    Ink("com.phongkaster.kanso.LauncherInk", 0xFF14171F.toInt());

    /** How an icon is chosen for a colour. */
    companion object {
        /**
         * Below this saturation (0 is grey, 1 is the purest colour) or this brightness (0 is
         * black), a colour has too little hue to match one of the coloured icons, so it gets [Ink].
         */
        private const val MIN_SATURATION = 0.18f

        /** See [MIN_SATURATION]. */
        private const val MIN_BRIGHTNESS = 0.15f

        /**
         * Returns the icon whose background is closest in hue to [argb], or [Ink] when [argb] is
         * nearly grey, black or white. Hue is the position on the colour wheel, so a pale pink
         * and a dark pink both match [Pink]: the icon follows which colour it is, not how light.
         */
        fun closestTo(argb: Int): LauncherIcon {
            val (hue, saturation, brightness) = hsv(argb)
            if (saturation < MIN_SATURATION || brightness < MIN_BRIGHTNESS) return Ink
            return entries.filter { it != Ink }.minBy { hueDistance(hue, hsv(it.argb)[0]) }
        }

        /** The distance between two hues in degrees, going the short way round the wheel. */
        private fun hueDistance(a: Float, b: Float): Float {
            val d = kotlin.math.abs(a - b) % 360f
            return if (d > 180f) 360f - d else d
        }

        /**
         * Splits [argb] into hue (0 to 360 degrees), saturation and brightness (each 0 to 1).
         * Written out here rather than with android.graphics.Color so it runs in plain JVM tests.
         */
        private fun hsv(argb: Int): FloatArray {
            val r = (argb shr 16 and 0xFF) / 255f
            val g = (argb shr 8 and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            val max = maxOf(r, g, b)
            val delta = max - minOf(r, g, b)
            val hue = when {
                delta == 0f -> 0f
                max == r -> 60f * (((g - b) / delta) % 6f)
                max == g -> 60f * ((b - r) / delta + 2f)
                else -> 60f * ((r - g) / delta + 4f)
            }
            val saturation = if (max == 0f) 0f else delta / max
            return floatArrayOf((hue + 360f) % 360f, saturation, max)
        }
    }
}
