package com.phongkaster.kanso.tool.edit

import com.phongkaster.kanso.data.edit.edit.DateFormat
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Proves the 20 date formats (ADR-006): each writes Wednesday 28 October 2026 at 14:30 exactly as the
 * owner chose, in English, the ordinal endings are right at their awkward days, and a tap's "next
 * format" walks all 20 and comes back to the first. Plain JVM, no Android.
 */
class DateFormatTest {

    /** Every format, in the sheet's order, writes the sample moment as agreed. */
    @Test
    fun every_format_writes_the_sample_moment_as_agreed() {
        val expected = listOf(
            "Wed, 28 October 2026",
            "Wednesday, 28th October",
            "28 October 2026",
            "October 28, 2026",
            "28 Oct 2026",
            "28 Oct",
            "October 2026",
            "Wednesday",
            "2026",
            "28/10/2026",
            "28.10.26",
            "2026-10-28",
            "WED 28 OCT",
            "'26 10 28",
            "14:30",
            "2:30 PM",
            "Wed, 28 Oct · 14:30",
            "Wednesday · 2:30 PM",
            "28/10/2026 14:30",
            "28 October 2026, 2:30 PM",
        )
        assertEquals(expected, DateFormat.entries.map { it.write(SAMPLE) })
    }

    /** A morning hour reads AM on the 12-hour clock and keeps its leading zero on the 24-hour one. */
    @Test
    fun a_morning_time_reads_am_and_keeps_its_zero() {
        val morning = LocalDateTime.of(2026, 1, 5, 9, 5)
        assertEquals("09:05", DateFormat.TIME_24.write(morning))
        assertEquals("9:05 AM", DateFormat.TIME_12.write(morning))
        assertEquals("05/01/2026", DateFormat.NUMERIC_SLASH.write(morning))
    }

    /** 1st, 2nd, 3rd, 4th; 11th, 12th, 13th; 21st, 22nd, 23rd; 31st. */
    @Test
    fun ordinals_take_their_english_endings() {
        val days = listOf(1, 2, 3, 4, 11, 12, 13, 21, 22, 23, 24, 30, 31)
        assertEquals(
            listOf("1st", "2nd", "3rd", "4th", "11th", "12th", "13th", "21st", "22nd", "23rd", "24th", "30th", "31st"),
            days.map { DateFormat.ordinal(it) },
        )
        assertEquals("Friday, 1st May", DateFormat.WEEKDAY_ORDINAL.write(LocalDateTime.of(2026, 5, 1, 0, 0)))
    }

    /** Tapping 20 times from the first format visits every format once and comes back. */
    @Test
    fun next_walks_every_format_and_wraps_round() {
        val visited = generateSequence(DateFormat.entries.first(), nextFunction = { it.next() })
            .take(DateFormat.entries.size + 1)
            .toList()
        assertEquals(DateFormat.entries, visited.dropLast(1))
        assertEquals(DateFormat.entries.first(), visited.last())
    }

    /** Values shared by the tests. */
    private companion object {
        /** Wednesday 28 October 2026 at 14:30, the moment of every sample in ADR-006. */
        val SAMPLE: LocalDateTime = LocalDateTime.of(2026, 10, 28, 14, 30)
    }
}
