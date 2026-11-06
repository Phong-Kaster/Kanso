package com.phongkaster.kanso.data.edit.edit

import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A tone curve: a smooth line through a few handles that says what each input brightness becomes.
 * The straight diagonal from (0, 0) to (1, 1) changes nothing; pulling it up brightens, down darkens.
 *
 * The rules, checked when one is made, so no code anywhere holds a broken curve:
 *
 * - from 2 to [MAX_POINTS] points, sorted by x, each at least [MIN_GAP] to the right of the last;
 * - the first point is at x = 0 and the last at x = 1, so every brightness has an answer;
 * - every y is in 0..1.
 *
 * The value is immutable and holds a list (never an array) so `==` compares curves by content.
 * [added], [moved] and [removed] return a new curve and never break the rules, which is what the
 * Curves screen needs while the user drags.
 */
data class ToneCurve(
    /** The points, left to right. */
    val points: List<CurvePoint>,
) {

    /** Checks the rules in the class comment. */
    init {
        require(points.size in 2..MAX_POINTS) { "a curve needs 2..$MAX_POINTS points, got ${points.size}" }
        require(points.first().x == 0f) { "the first point must be at x = 0" }
        require(points.last().x == 1f) { "the last point must be at x = 1" }
        require(points.all { it.y in 0f..1f && it.x in 0f..1f }) { "every x and y must be in 0..1" }
        for (i in 1 until points.size) {
            require(points[i].x - points[i - 1].x >= MIN_GAP - GAP_SLACK) {
                "points ${i - 1} and $i are closer than $MIN_GAP"
            }
        }
    }

    /**
     * True when the curve is exactly the untouched two-point diagonal. A curve with an extra point
     * counts as changed, even if that point sits on the diagonal, so it gets an Undo step and
     * Reset stays enabled.
     */
    val isNeutral: Boolean get() = points == NEUTRAL_POINTS

    /**
     * The 256-entry lookup table of this curve: entry `i` is the output (0..255) for input `i`.
     *
     * It uses monotone cubic interpolation (Fritsch-Carlson): a smooth curve that passes exactly
     * through every point and never overshoots between two points, so it can never dip below the
     * lower neighbour or rise above the higher one (no halos, and never darker than black or
     * brighter than white). Every entry is also clamped to 0..255. Each call returns a new array,
     * so a caller may change it freely.
     */
    fun table(): IntArray {
        if (points == NEUTRAL_POINTS) return IntArray(TABLE_SIZE) { it }
        val n = points.size
        val xs = FloatArray(n) { points[it].x }
        val ys = FloatArray(n) { points[it].y }
        val tangents = tangents(xs, ys)
        val table = IntArray(TABLE_SIZE)
        var segment = 0
        for (i in 0 until TABLE_SIZE) {
            val x = i / (TABLE_SIZE - 1).toFloat()
            while (segment < n - 2 && x > xs[segment + 1]) segment++
            val width = xs[segment + 1] - xs[segment]
            val t = ((x - xs[segment]) / width).coerceIn(0f, 1f)
            val t2 = t * t
            val t3 = t2 * t
            // The cubic Hermite basis: two values and two slopes of the segment.
            val y = (2 * t3 - 3 * t2 + 1) * ys[segment] +
                (t3 - 2 * t2 + t) * width * tangents[segment] +
                (-2 * t3 + 3 * t2) * ys[segment + 1] +
                (t3 - t2) * width * tangents[segment + 1]
            // Clamping to the segment's own two values keeps float error from overshooting.
            val low = minOf(ys[segment], ys[segment + 1])
            val high = maxOf(ys[segment], ys[segment + 1])
            table[i] = (y.coerceIn(low, high) * 255f).roundToInt().coerceIn(0, 255)
        }
        return table
    }

    /**
     * A copy with [point] inserted in its sorted place. Returns this curve unchanged when it already
     * holds [MAX_POINTS] points or [point]'s x is within [MIN_GAP] of an existing point. Its y is
     * clamped to 0..1.
     */
    fun added(point: CurvePoint): ToneCurve {
        if (points.size >= MAX_POINTS) return this
        if (point.x.isNaN() || point.y.isNaN()) return this
        if (points.any { kotlin.math.abs(it.x - point.x) < MIN_GAP }) return this
        val clean = CurvePoint(point.x.coerceIn(0f, 1f), point.y.coerceIn(0f, 1f))
        return ToneCurve((points + clean).sortedBy { it.x })
    }

    /**
     * A copy with the point at [index] moved to ([x], [y]). The y is clamped to 0..1. The x is kept
     * at least [MIN_GAP] away from both neighbours, so a point can never pass one; the two end
     * points keep their x and only move up and down. An index out of range changes nothing.
     *
     * When the two neighbours are themselves less than two gaps apart (float rounding can leave
     * them a hair short, which the rules allow), there is no x that keeps a full gap on both sides;
     * the point then keeps its own x, which is already a valid place, and only its y moves.
     */
    fun moved(index: Int, x: Float, y: Float): ToneCurve {
        if (index !in points.indices || x.isNaN() || y.isNaN()) return this
        val newY = y.coerceIn(0f, 1f)
        val newX = if (index == 0 || index == points.lastIndex) {
            points[index].x
        } else {
            val lowest = points[index - 1].x + MIN_GAP
            val highest = points[index + 1].x - MIN_GAP
            // coerceIn throws on an empty range, so an empty one keeps the current x instead.
            if (lowest <= highest) x.coerceIn(lowest, highest) else points[index].x
        }
        val changed = points.toMutableList()
        changed[index] = CurvePoint(newX, newY)
        return ToneCurve(changed)
    }

    /** A copy without the point at [index]. The two end points cannot be removed: this returns itself. */
    fun removed(index: Int): ToneCurve {
        if (index <= 0 || index >= points.lastIndex) return this
        return ToneCurve(points.filterIndexed { i, _ -> i != index })
    }

    /** The shared limits and the neutral curve. */
    companion object {
        /** The most points a curve may hold. */
        const val MAX_POINTS = 10

        /** The least distance in x between two neighbouring points. */
        const val MIN_GAP = 0.02f

        /** Float slack so a gap computed as 0.7 - 0.68 is not refused for rounding error. */
        private const val GAP_SLACK = 1e-4f

        /** Table length: one entry for each 8-bit brightness. */
        private const val TABLE_SIZE = 256

        private val NEUTRAL_POINTS = listOf(CurvePoint(0f, 0f), CurvePoint(1f, 1f))

        /** The straight diagonal: nothing changed. */
        val NEUTRAL = ToneCurve(NEUTRAL_POINTS)

        /**
         * The slope of the curve at each point, chosen the Fritsch-Carlson way: the average of the
         * two neighbouring secants, zero at a turning point, and scaled down wherever it would let
         * the curve overshoot. That is what makes the interpolation monotone between points.
         */
        private fun tangents(xs: FloatArray, ys: FloatArray): FloatArray {
            val n = xs.size
            val secants = FloatArray(n - 1) { (ys[it + 1] - ys[it]) / (xs[it + 1] - xs[it]) }
            val m = FloatArray(n)
            m[0] = secants[0]
            m[n - 1] = secants[n - 2]
            for (k in 1 until n - 1) {
                m[k] = if (secants[k - 1] * secants[k] <= 0f) 0f else (secants[k - 1] + secants[k]) / 2f
            }
            for (k in 0 until n - 1) {
                if (secants[k] == 0f) {
                    m[k] = 0f
                    m[k + 1] = 0f
                } else {
                    val a = m[k] / secants[k]
                    val b = m[k + 1] / secants[k]
                    val s = a * a + b * b
                    if (s > 9f) {
                        val tau = 3f / sqrt(s)
                        m[k] = tau * a * secants[k]
                        m[k + 1] = tau * b * secants[k]
                    }
                }
            }
            return m
        }
    }
}
