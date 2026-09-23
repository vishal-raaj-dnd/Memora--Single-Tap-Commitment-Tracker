package com.example.service

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.example.data.model.MemoryItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object CalendarManager {

    fun createCalendarIntent(item: MemoryItem): Intent {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, item.title)
            putExtra(CalendarContract.Events.DESCRIPTION, "${item.description}\n\nCaptured by Memora")
            item.organization?.let { putExtra(CalendarContract.Events.EVENT_LOCATION, it) }

            val startTimeMillis = parseDateTime(item.date, item.time) ?: System.currentTimeMillis() + 3600000L
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startTimeMillis + (60 * 60 * 1000L)) // default 1 hour
            putExtra(CalendarContract.Events.ACCESS_LEVEL, CalendarContract.Events.ACCESS_PRIVATE)
            putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        }
        return intent
    }

    private fun parseDateTime(dateStr: String?, timeStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        return try {
            val cal = Calendar.getInstance()
            // Try standard formats like "Sep 30, 2026" or "2026-09-30"
            val formatters = listOf(
                SimpleDateFormat("MMM dd, yyyy", Locale.US),
                SimpleDateFormat("yyyy-MM-dd", Locale.US),
                SimpleDateFormat("MMMM dd, yyyy", Locale.US)
            )
            var parsedDate: java.util.Date? = null
            for (fmt in formatters) {
                try {
                    parsedDate = fmt.parse(dateStr)
                    if (parsedDate != null) break
                } catch (_: Exception) {}
            }
            if (parsedDate != null) {
                cal.time = parsedDate
                if (!timeStr.isNullOrBlank()) {
                    val timeFormatters = listOf(
                        SimpleDateFormat("hh:mm a", Locale.US),
                        SimpleDateFormat("HH:mm", Locale.US)
                    )
                    for (tFmt in timeFormatters) {
                        try {
                            val timePart = tFmt.parse(timeStr)
                            if (timePart != null) {
                                val timeCal = Calendar.getInstance().apply { time = timePart }
                                cal.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
                                cal.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
                                break
                            }
                        } catch (_: Exception) {}
                    }
                }
                cal.timeInMillis
            } else null
        } catch (_: Exception) {
            null
        }
    }
}
