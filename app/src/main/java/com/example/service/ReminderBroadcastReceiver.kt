package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.local.MemoraDatabase
import com.example.data.model.MemoryItem
import com.example.data.model.MemoryType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver responsible for firing deadline / commitment notifications
 * when an AlarmManager trigger fires, and rescheduling all active reminders
 * upon device reboot (BOOT_COMPLETED).
 */
class ReminderBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_REMINDER_ALARM = "com.example.ACTION_REMINDER_ALARM"
        private const val TAG = "ReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        when (intent.action) {
            ACTION_REMINDER_ALARM -> {
                val memoryId = intent.getLongExtra("memory_id", -1L)
                val title = intent.getStringExtra("memory_title") ?: "Commitment Reminder"
                val desc = intent.getStringExtra("memory_desc") ?: ""
                val typeName = intent.getStringExtra("memory_type") ?: "Task"

                Log.i(TAG, "Alarm triggered for memory #$memoryId: '$title'")

                val reminderManager = ReminderManager(context)
                val item = MemoryItem(
                    id = if (memoryId != -1L) memoryId else 0L,
                    title = title,
                    description = desc,
                    type = try { MemoryType.fromString(typeName) } catch (_: Exception) { MemoryType.TASK }
                )
                reminderManager.showReminderNotification(item)
            }

            Intent.ACTION_BOOT_COMPLETED -> {
                Log.i(TAG, "Device rebooted. Rescheduling all uncompleted memory reminders...")
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val database = MemoraDatabase.getDatabase(context, this)
                        val uncompleted = database.memoryDao().getUncompletedMemoriesSync()
                        val reminderManager = ReminderManager(context)
                        var rescheduledCount = 0
                        for (memory in uncompleted) {
                            if (!memory.date.isNullOrBlank()) {
                                val scheduled = reminderManager.scheduleReminder(memory)
                                if (scheduled) rescheduledCount++
                            }
                        }
                        Log.i(TAG, "Successfully rescheduled $rescheduledCount reminders after reboot.")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to reschedule reminders after reboot", e)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
