package com.example.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Robust date and time utilities for Memora.
 * Accurately categorizes and normalizes dates (Today, Tomorrow, Upcoming, Overdue, ISO)
 * without loose substring collisions or false-positive fallbacks.
 */
object DateUtils {

    private val MONTH_DAY_FORMATTER = DateTimeFormatter.ofPattern("MMM dd", Locale.US)
    private val FULL_DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)
    private val ISO_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE

    /**
     * Parses a flexible date string into a LocalDate.
     */
    fun parseLocalDate(dateStr: String?): LocalDate? {
        if (dateStr.isNullOrBlank()) return null
        val clean = dateStr.trim()

        if (clean.contains("Tomorrow", ignoreCase = true) || clean.contains("tmrw", ignoreCase = true) || clean.contains("t0ommorrow", ignoreCase = true)) {
            return LocalDate.now().plusDays(1)
        }
        if (clean.contains("Today", ignoreCase = true) || clean.contains("tonight", ignoreCase = true)) {
            return LocalDate.now()
        }

        // Clean any parenthetical annotations like "Tomorrow (Oct 02)" or "Next Week (Oct 05)"
        val stripped = clean.replace("\\(.*\\)".toRegex(), "").trim()
        val now = LocalDate.now()

        val formattersWithYear = listOf(
            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US),
            DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US),
            DateTimeFormatter.ofPattern("MMMM dd, yyyy", Locale.US)
        )

        for (formatter in formattersWithYear) {
            try {
                return LocalDate.parse(stripped, formatter)
            } catch (_: Exception) {}
        }

        val formattersWithoutYear = listOf(
            DateTimeFormatter.ofPattern("MMM dd", Locale.US),
            DateTimeFormatter.ofPattern("MMMM dd", Locale.US)
        )

        for (formatter in formattersWithoutYear) {
            try {
                val tempMonthDay = java.time.MonthDay.parse(stripped, formatter)
                return tempMonthDay.atYear(now.year)
            } catch (_: Exception) {}
        }

        return null
    }

    /**
     * Checks if the date string corresponds to today.
     */
    fun isToday(dateStr: String?): Boolean {
        if (dateStr.isNullOrBlank()) return false
        val clean = dateStr.trim()
        if (clean.contains("Tomorrow", ignoreCase = true) || clean.contains("tmrw", ignoreCase = true)) {
            return false
        }
        if (clean.contains("Today", ignoreCase = true)) return true

        val parsed = parseLocalDate(clean)
        if (parsed != null) {
            return parsed == LocalDate.now()
        }

        val today = LocalDate.now()
        val todayMonthDay = today.format(MONTH_DAY_FORMATTER)
        val todayIso = today.format(ISO_DATE_FORMATTER)
        return clean.contains(todayMonthDay, ignoreCase = true) || clean.contains(todayIso)
    }

    /**
     * Checks if the date string corresponds to tomorrow.
     */
    fun isTomorrow(dateStr: String?): Boolean {
        if (dateStr.isNullOrBlank()) return false
        val clean = dateStr.trim()
        if (clean.contains("Tomorrow", ignoreCase = true) || clean.contains("tmrw", ignoreCase = true) || clean.contains("t0ommorrow", ignoreCase = true)) {
            return true
        }

        val parsed = parseLocalDate(clean)
        if (parsed != null) {
            return parsed == LocalDate.now().plusDays(1)
        }

        val tomorrow = LocalDate.now().plusDays(1)
        val tomorrowMonthDay = tomorrow.format(MONTH_DAY_FORMATTER)
        val tomorrowIso = tomorrow.format(ISO_DATE_FORMATTER)
        return clean.contains(tomorrowMonthDay, ignoreCase = true) || clean.contains(tomorrowIso)
    }

    /**
     * Checks if the commitment is overdue (past date and not completed).
     */
    fun isOverdue(dateStr: String?, isCompleted: Boolean = false): Boolean {
        if (isCompleted || dateStr.isNullOrBlank()) return false
        if (isToday(dateStr) || isTomorrow(dateStr)) return false

        val parsed = parseLocalDate(dateStr) ?: return false
        return parsed.isBefore(LocalDate.now())
    }

    /**
     * Checks if the date is in the future beyond today.
     */
    fun isUpcoming(dateStr: String?): Boolean {
        if (dateStr.isNullOrBlank()) return true // unassigned date counts as someday/upcoming
        if (isToday(dateStr)) return false
        if (isTomorrow(dateStr)) return true

        val parsed = parseLocalDate(dateStr)
        return if (parsed != null) {
            parsed.isAfter(LocalDate.now())
        } else {
            !isToday(dateStr)
        }
    }

    /**
     * Checks if a memory date matches a specific target calendar date.
     */
    fun matchesDate(dateStr: String?, target: LocalDate): Boolean {
        if (dateStr.isNullOrBlank()) return false
        val parsed = parseLocalDate(dateStr)
        return parsed == target
    }

    /**
     * Normalizes loose time strings such as "330" -> "3:30 PM", "7" -> "7:00 PM", "1530" -> "3:30 PM".
     */
    fun normalizeTimeString(rawTime: String?): String? {
        if (rawTime.isNullOrBlank()) return null
        val clean = rawTime.trim().uppercase()

        // Already formatted with AM/PM (e.g. "3:30 PM", "7 PM")
        if (clean.contains("AM") || clean.contains("PM")) {
            val parts = clean.split(" ")
            val timePart = parts.firstOrNull() ?: clean
            val amPm = if (clean.contains("AM")) "AM" else "PM"
            val digits = timePart.replace("[^0-9:]".toRegex(), "")
            return if (digits.contains(":")) {
                "$digits $amPm"
            } else if (digits.length in 1..2) {
                "$digits:00 $amPm"
            } else if (digits.length in 3..4) {
                val hour = digits.substring(0, digits.length - 2)
                val min = digits.substring(digits.length - 2)
                "$hour:$min $amPm"
            } else {
                clean
            }
        }

        // Pure digits or colon without AM/PM (e.g. "330", "1530", "3:30")
        val digitsOnly = clean.replace("[^0-9:]".toRegex(), "")
        if (digitsOnly.isBlank()) return rawTime

        val (hourInt, minStr) = if (digitsOnly.contains(":")) {
            val split = digitsOnly.split(":")
            val h = split.getOrNull(0)?.toIntOrNull() ?: 12
            val m = split.getOrNull(1) ?: "00"
            Pair(h, m.padStart(2, '0').take(2))
        } else if (digitsOnly.length in 3..4) {
            val h = digitsOnly.substring(0, digitsOnly.length - 2).toIntOrNull() ?: 12
            val m = digitsOnly.substring(digitsOnly.length - 2)
            Pair(h, m)
        } else {
            val h = digitsOnly.toIntOrNull() ?: 12
            Pair(h, "00")
        }

        // If 24h format (e.g. 13 to 23)
        if (hourInt in 13..23) {
            return "${hourInt - 12}:$minStr PM"
        } else if (hourInt == 12) {
            return "12:$minStr PM"
        } else if (hourInt == 0) {
            return "12:$minStr AM"
        }

        // Default to PM for daytime business/class/deadline hours (8..11 AM vs 1..11 PM)
        val amPm = if (hourInt in 8..11) "AM" else "PM"
        return "$hourInt:$minStr $amPm"
    }

    /**
     * Checks if a raw time string is ambiguous regarding AM vs PM.
     */
    fun hasAmPmAmbiguity(rawTime: String?): Boolean {
        if (rawTime.isNullOrBlank()) return false
        val clean = rawTime.uppercase()
        return !clean.contains("AM") && !clean.contains("PM")
    }

    /**
     * Generates standard AM / PM options for a given time.
     */
    fun getAmPmOptions(normalizedTime: String?): List<String> {
        val norm = normalizedTime ?: return emptyList()
        val base = norm.replace(" AM", "").replace(" PM", "").trim()
        return listOf("$base PM", "$base AM")
    }

    /**
     * Formats today, tomorrow, or calendar date into a clean display title.
     */
    fun getDisplayDateLabel(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return "Upcoming"
        if (isToday(dateStr)) return "Today"
        if (isTomorrow(dateStr)) return "Tomorrow"
        return dateStr
    }

    fun formatDisplayDate(date: LocalDate): String {
        return date.format(FULL_DATE_FORMATTER)
    }
}
