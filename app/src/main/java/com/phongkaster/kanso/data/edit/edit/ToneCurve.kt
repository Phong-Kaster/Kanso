package com.phongkaster.kanso.data.edit.edit

import kotlin.math.roundToInt

/**
 * A tone curve: a smooth line through a few handles that says what each input brightness becomes.
 * The straight diagonal from (0, 0) to (1, 1) changes nothing; pulling it up brightens, down darkens.
 *
 * The rules, checked when one is made, so no code anywhere holds a broken curve:
 *
 * - from 2 to [MAX_POINTS] points, sorted by x, each at least [MIN_GAP] to the right of the last;
 * - every x and y is in 0..1. The end points are free: they may sit away from x = 0 and x = 1, and
 *   the curve stays flat (at the end point's y) beyond them.
 *
 * Between the points the line is a natural cubic spline (see [valueAt]). It is smooth but it may
 * overshoot between handles, as in Meitu; the result is clipped to 0..1.
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
        require(points.all { it.y in 0f..1f && it.x in 0f..1f }) { "every x and y must be in 0..1" }
        for (i in 1 until points.size) {
            require(points[i].x - points[i - 1].x >= MIN_GAP - GAP_SLACK) {
                "points ${i - 1} and $i are closer than $MIN_GAP"
            }
        }
    }

    /**
     * The spline's second derivative at each point, solved once (lazily) and reused by [valueAt].
     * It is not a constructor property, so it does not take part in `==`. The natural end condition
     * is a second derivative of zero at the first and the last point. The lazy is synchronized (the
     * default), because one curve is read on the main thread (graph drawing) and on
     * Dispatchers.Default (preview render) at once.
     */
    private val secondDerivatives: DoubleArray by lazy {
        val n = points.size
        val x = DoubleArray(n) { points[it].x.toDouble() }
        val y = DoubleArray(n) { points[it].y.toDouble() }
        val m = DoubleArray(n)
        if (n > 2) {
            // Thomas algorithm for the tridiagonal system of the interior points.
            val c = DoubleArray(n)
            val d = DoubleArray(n)
            for (i in 1 until n - 1) {
                val h0 = x[i - 1].let { x[i] - it }
                val h1 = x[i + 1] - x[i]
                val diag = 2.0 * (h0 + h1)
                val rhs = 6.0 * ((y[i + 1] - y[i]) / h1 - (y[i] - y[i - 1]) / h0)
                val denom = diag - h0 * c[i - 1]
                c[i] = h1 / denom
                d[i] = (rhs - h0 * d[i - 1]) / denom
            }
            for (i in n - 2 downTo 1) m[i] = d[i] - c[i] * m[i + 1]
        }
        m
    }

    /**
     * True when the curve is exactly the untouched two-point diagonal. A curve with an extra point
     * counts as changed, even if that point sits on the diagonal, so it gets an Undo step and
     * Reset stays enabled.
     */
    val isNeutral: Boolean get() = points == NEUTRAL_POINTS

    /**
     * The curve's output (0..1) for the input [x] (0..1).
     *
     * A natural cubic spline passes exactly through every point and has a zero second derivative at
     * the first and last point. With two points that is a straight segment. Left of the first point
     * the answer is the first point's y, right of the last it is the last point's y. The result is
     * clipped to 0..1, so an overshoot becomes pure black or white instead of an invalid value.
     */
    internal fun valueAt(x: Float): Float {
        val first = points.first()
        val last = points.last()
        if (x <= first.x) return first.y
        if (x >= last.x) return last.y
        var i = 0
        while (i < points.size - 2 && x > points[i + 1].x) i++
        val x0 = points[i].x.toDouble()
        val x1 = points[i + 1].x.toDouble()
        val y0 = points[i].y.toDouble()
        val y1 = points[i + 1].y.toDouble()
        val m0 = secondDerivatives[i]
        val m1 = secondDerivatives[i + 1]
        val h = x1 - x0
        val a = (x1 - x) / h
        val b = (x - x0) / h
        val v = a * y0 + b * y1 + ((a * a * a - a) * m0 + (b * b * b - b) * m1) * h * h / 6.0
        return v.toFloat().coerceIn(0f, 1f)
    }

    /**
     * The 256-entry lookup table of this curve: entry `i` is the output (0..255) for input `i`,
     * that is `round(255 * valueAt(i / 255))`. Each call returns a new array, so a caller may
     * change it freely.
     */
    fun table(): IntArray {
        if (points == NEUTRAL_POINTS) return IntArray(TABLE_SIZE) { it }
        return IntArray(TABLE_SIZE) { i ->
            (valueAt(i / (TABLE_SIZE - 1).toFloat()) * 255f).roundToInt().coerceIn(0, 255)
        }
    }

    /**
     * A copy with [point] inserted in its sorted place. Returns this curve unchanged when it already
     * holds [MAX_POINTS] points or [point]'s x is within [MIN_GAP] of an existing point. Its y is
     * clamped to 0..1. A point left of the first or right of the last becomes the new end point.
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
     * at least [MIN_GAP] away from the neighbours, so a point can never pass one. The first point
     * may slide from 0 up to the next point's x minus the gap, the last from the previous point's x
     * plus the gap up to 1. An index out of range changes nothing.
     *
     * When the two neighbours are themselves less than two gaps apart (float rounding can leave
     * them a hair short, which the rules allow), there is no x that keeps a full gap on both sides;
     * the point then keeps its own x, which is already a valid place, and only its y moves.
     */
    fun moved(index: Int, x: Float, y: Float): ToneCurve {
        if (index !in points.indices || x.isNaN() || y.isNaN()) return this
        val newY = y.coerceIn(0f, 1f)
        val lowest = if (index == 0) 0f else points[index - 1].x + MIN_GAP
        val highest = if (index == points.lastIndex) 1f else points[index + 1].x - MIN_GAP
        // coerceIn throws on an empty range, so an empty one keeps the current x instead.
        val newX = if (lowest <= highest) x.coerceIn(lowest, highest) else points[index].x
        val changed = points.toMutableList()
        changed[index] = CurvePoint(newX, newY)
        return ToneCurve(changed)
    }

    /**
     * A copy without the point at [index], end points included. A curve keeps at least two points:
     * with only two left, or an index out of range, this returns itself.
     */
    fun removed(index: Int): ToneCurve {
        if (points.size <= 2 || index !in points.indices) return this
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
    }
}
