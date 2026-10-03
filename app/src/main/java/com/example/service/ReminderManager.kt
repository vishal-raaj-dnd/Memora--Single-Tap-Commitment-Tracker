package com.example.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.model.MemoryItem
import com.example.util.DateUtils
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale

class ReminderManager(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "memora_reminders"
        const val CHANNEL_NAME = "Memora Reminders & Captures"
        private const val TAG = "ReminderManager"

        /**
         * Converts MemoryItem date, time, and reminder offset into epoch milliseconds.
         */
        fun calculateTriggerTime(item: MemoryItem): Long? =
            calculateTriggerTime(item.date, item.time, item.reminderTime)

        /**
         * Converts date string, time string, and reminder offset into epoch milliseconds.
         */
        fun calculateTriggerTime(dateStr: String?, timeStr: String?, reminderOffset: String?): Long? {
            if (dateStr.isNullOrBlank()) return null

            val cal = Calendar.getInstance()
            val now = LocalDate.now()

            // 1. Resolve calendar date
            val isToday = DateUtils.isToday(dateStr)
            val isTomorrow = DateUtils.isTomorrow(dateStr)

            if (isToday) {
                cal.set(Calendar.YEAR, now.year)
                cal.set(Calendar.MONTH, now.monthValue - 1)
                cal.set(Calendar.DAY_OF_MONTH, now.dayOfMonth)
            } else if (isTomorrow) {
                val tmrw = now.plusDays(1)
                cal.set(Calendar.YEAR, tmrw.year)
                cal.set(Calendar.MONTH, tmrw.monthValue - 1)
                cal.set(Calendar.DAY_OF_MONTH, tmrw.dayOfMonth)
            } else {
                // Clean date string: remove parens like "Tomorrow (Oct 02)" -> try extracting actual date
                val cleanDate = dateStr.replace("\\(.*\\)".toRegex(), "").trim()
                val formatters = listOf(
                    SimpleDateFormat("MMM dd, yyyy", Locale.US),
                    SimpleDateFormat("yyyy-MM-dd", Locale.US),
                    SimpleDateFormat("MMMM dd, yyyy", Locale.US),
                    SimpleDateFormat("MMM dd", Locale.US),
                    SimpleDateFormat("MMMM dd", Locale.US)
                )

                var parsedDate: java.util.Date? = null
                for (fmt in formatters) {
                    try {
                        parsedDate = fmt.parse(cleanDate)
                        if (parsedDate != null) {
                            val tempCal = Calendar.getInstance().apply { time = parsedDate }
                            if (fmt.toPattern().contains("yyyy")) {
                                cal.set(Calendar.YEAR, tempCal.get(Calendar.YEAR))
                            } else {
                                cal.set(Calendar.YEAR, now.year)
                            }
                            cal.set(Calendar.MONTH, tempCal.get(Calendar.MONTH))
                            cal.set(Calendar.DAY_OF_MONTH, tempCal.get(Calendar.DAY_OF_MONTH))
                            break
                        }
                    } catch (_: Exception) {}
                }
                if (parsedDate == null) return null
            }

            // 2. Resolve time (default to 9:00 AM if unstated)
            if (!timeStr.isNullOrBlank()) {
                val timeFormatters = listOf(
                    SimpleDateFormat("h:mm a", Locale.US),
                    SimpleDateFormat("hh:mm a", Locale.US),
                    SimpleDateFormat("HH:mm", Locale.US),
                    SimpleDateFormat("H:mm", Locale.US)
                )
                var timeParsed = false
                for (tFmt in timeFormatters) {
                    try {
                        val timePart = tFmt.parse(timeStr.trim())
                        if (timePart != null) {
                            val tCal = Calendar.getInstance().apply { time = timePart }
                            cal.set(Calendar.HOUR_OF_DAY, tCal.get(Calendar.HOUR_OF_DAY))
                            cal.set(Calendar.MINUTE, tCal.get(Calendar.MINUTE))
                            cal.set(Calendar.SECOND, 0)
                            cal.set(Calendar.MILLISECOND, 0)
                            timeParsed = true
                            break
                        }
                    } catch (_: Exception) {}
                }
                if (!timeParsed) {
                    cal.set(Calendar.HOUR_OF_DAY, 9)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                }
            } else {
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
            }

            // 3. Apply offset (e.g. 1 hour before)
            val offsetMillis = when {
                reminderOffset?.contains("1 day", ignoreCase = true) == true -> 24 * 3600 * 1000L
                reminderOffset?.contains("1 hour", ignoreCase = true) == true -> 3600 * 1000L
                reminderOffset?.contains("30 min", ignoreCase = true) == true -> 1800 * 1000L
                reminderOffset?.contains("10 min", ignoreCase = true) == true -> 600 * 1000L
                reminderOffset?.contains("at time", ignoreCase = true) == true -> 0L
                else -> 3600 * 1000L // default 1 hour before
            }

            return cal.timeInMillis - offsetMillis
        }
    }

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Memora captured deadlines, events, and reminders"
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Schedules an exact system alarm for the given MemoryItem based on its date,
     * time, and reminder offset (e.g. 1 hour before, 1 day before).
     * Returns true if successfully scheduled in the future, false otherwise.
     */
    fun scheduleReminder(item: MemoryItem): Boolean {
        if (item.isCompleted || item.date.isNullOrBlank()) {
            cancelReminder(item.id)
            return false
        }

        val triggerTime = calculateTriggerTime(item.date, item.time, item.reminderTime)
        val now = System.currentTimeMillis()

        if (triggerTime == null || triggerTime <= now) {
            Log.d(TAG, "Trigger time for '${item.title}' is null or in the past ($triggerTime vs $now), skipping alarm.")
            return false
        }

        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            action = ReminderBroadcastReceiver.ACTION_REMINDER_ALARM
            putExtra("memory_id", item.id)
            putExtra("memory_title", item.title)
            putExtra("memory_desc", item.description)
            putExtra("memory_type", item.type.displayName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
            Log.i(TAG, "Scheduled alarm for memory #${item.id} '${item.title}' at ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(triggerTime)}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm for memory #${item.id}", e)
            return false
        }
    }

    /**
     * Cancels any pending scheduled alarm for this memory item.
     */
    fun cancelReminder(itemId: Long) {
        try {
            val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
                action = ReminderBroadcastReceiver.ACTION_REMINDER_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                itemId.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.d(TAG, "Cancelled alarm for memory #$itemId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling alarm for memory #$itemId", e)
        }
    }

    /**
     * Shows high-priority notification when alarm triggers or memory is due.
     */
    fun showReminderNotification(item: MemoryItem) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("memory_id", item.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Memora: ${item.title}")
            .setContentText(item.description.ifBlank { "Upcoming ${item.type.displayName.lowercase()}" })
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(item.id.toInt(), notification)
    }

    /**
     * Shows quick feedback notification upon saving memory item.
     */
    fun showSavedConfirmationNotification(item: MemoryItem) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("memory_id", item.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("Memory Captured: ${item.title}")
            .setContentText("Added to your timeline${if (!item.date.isNullOrBlank()) " for ${item.date}" else ""}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(item.id.toInt(), notification)
    }

    /**
     * Converts MemoryItem date, time, and reminder offset into epoch milliseconds.
     */
    fun calculateTriggerTime(item: MemoryItem): Long? =
        Companion.calculateTriggerTime(item)

    /**
     * Converts date string, time string, and reminder offset into epoch milliseconds.
     */
    fun calculateTriggerTime(dateStr: String?, timeStr: String?, reminderOffset: String?): Long? =
        Companion.calculateTriggerTime(dateStr, timeStr, reminderOffset)
}
