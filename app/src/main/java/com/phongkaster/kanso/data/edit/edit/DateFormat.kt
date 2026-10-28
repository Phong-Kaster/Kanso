package com.phongkaster.kanso.data.edit.edit

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The 20 ways a date layer ([LayerContent.Date]) writes its moment, in the order the Date sheet
 * lists them and a tap on a selected date cycles through them (ADR-006): fourteen with the day
 * only, then six with a time. Each sample below is Wednesday 28 October 2026 at 14:30.
 *
 * **Always English.** Every format writes with `Locale.ENGLISH`, whatever the phone's language,
 * so a date reads like the rest of Kanso, which has only English words.
 *
 * **DateTimeFormatter.** `java.time`'s formatter turns a pattern such as `d MMMM yyyy` into words:
 * `d` is the day of the month, `MMMM` the month's full name, `MMM` its short name, `EEEE` the
 * weekday's full name, `EEE` its short name, `yyyy` the year, `HH:mm` a 24-hour clock, `h:mm a`
 * a 12-hour one, and anything between single quotes is written as it is. One format,
 * [WEEKDAY_ORDINAL], needs an English ordinal ("28th") that no pattern letter gives, so it has
 * no pattern and is written by hand in [write].
 *
 * It is plain Kotlin with no Android types, so every format can be tested on the JVM.
 *
 * @param pattern the formatter's pattern, or null for the one format written by hand.
 * @param upperCase true when the words are written in capitals (the stamp look).
 */
enum class DateFormat(private val pattern: String?, private val upperCase: Boolean = false) {

    /** `Wed, 28 October 2026`: the default for a new date. */
    WEEKDAY_FULL("EEE, d MMMM yyyy"),

    /** `Wednesday, 28th October`. */
    WEEKDAY_ORDINAL(null),

    /** `28 October 2026`. */
    DAY_MONTH_YEAR("d MMMM yyyy"),

    /** `October 28, 2026`, the American order. */
    MONTH_DAY_YEAR("MMMM d, yyyy"),

    /** `28 Oct 2026`. */
    DAY_SHORT_MONTH_YEAR("d MMM yyyy"),

    /** `28 Oct`. */
    DAY_SHORT_MONTH("d MMM"),

    /** `October 2026`. */
    MONTH_YEAR("MMMM yyyy"),

    /** `Wednesday`. */
    WEEKDAY("EEEE"),

    /** `2026`. */
    YEAR("yyyy"),

    /** `28/10/2026`. */
    NUMERIC_SLASH("dd/MM/yyyy"),

    /** `28.10.26`. */
    NUMERIC_DOT("dd.MM.yy"),

    /** `2026-10-28`, the ISO order. */
    ISO("yyyy-MM-dd"),

    /** `WED 28 OCT`, in capitals. */
    STAMP("EEE d MMM", upperCase = true),

    /** `'26 10 28`, the date an old film camera printed in the corner of a photo. */
    FILM("''yy MM dd"),

    /** `14:30`. */
    TIME_24("HH:mm"),

    /** `2:30 PM`. */
    TIME_12("h:mm a"),

    /** `Wed, 28 Oct · 14:30`. */
    WEEKDAY_TIME_24("EEE, d MMM '·' HH:mm"),

    /** `Wednesday · 2:30 PM`. */
    WEEKDAY_TIME_12("EEEE '·' h:mm a"),

    /** `28/10/2026 14:30`. */
    NUMERIC_TIME_24("dd/MM/yyyy HH:mm"),

    /** `28 October 2026, 2:30 PM`. */
    DAY_MONTH_YEAR_TIME_12("d MMMM yyyy, h:mm a"),
    ;

    /**
     * The formatter for [pattern], made once per format. `by lazy` builds it on first use and
     * keeps it; a `DateTimeFormatter` is immutable, so one is safe to share between threads.
     */
    private val formatter: DateTimeFormatter? by lazy {
        pattern?.let { DateTimeFormatter.ofPattern(it, Locale.ENGLISH) }
    }

    /** [moment] written in this format, in English. */
    fun write(moment: LocalDateTime): String {
        val words = formatter?.format(moment) ?: weekdayOrdinal(moment)
        return if (upperCase) words.uppercase(Locale.ENGLISH) else words
    }

    /** The format after this one, the first coming after the last: a tap on a date moves it on. */
    fun next(): DateFormat = entries[(ordinal + 1) % entries.size]

    /** The formats, and the hand-written one. */
    companion object {
        /** The weekday's full name, which [WEEKDAY_ORDINAL] writes before its ordinal. */
        private val WEEKDAY_NAME = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)

        /** The month's full name, which [WEEKDAY_ORDINAL] writes after its ordinal. */
        private val MONTH_NAME = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)

        /** `Wednesday, 28th October`: the weekday, the day with its English ordinal, the month. */
        private fun weekdayOrdinal(moment: LocalDateTime): String =
            "${WEEKDAY_NAME.format(moment)}, ${ordinal(moment.dayOfMonth)} ${MONTH_NAME.format(moment)}"

        /**
         * [day] with its English ending: 1st, 2nd, 3rd, then th, except that 11, 12 and 13 take
         * th too (eleventh, not "elevenst"), while 21, 22, 23 and 31 go back to st, nd, rd.
         */
        fun ordinal(day: Int): String {
            val ending = when {
                day % 100 in 11..13 -> "th"
                day % 10 == 1 -> "st"
                day % 10 == 2 -> "nd"
                day % 10 == 3 -> "rd"
                else -> "th"
            }
            return "$day$ending"
        }
    }
}
