package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memories")
data class MemoryItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val type: MemoryType = MemoryType.TASK,
    val categoryId: String = "personal",
    val date: String? = null,
    val time: String? = null,
    val endDate: String? = null,
    val endTime: String? = null,
    val isDeadline: Boolean = false,
    val isCompleted: Boolean = false,
    val reminderTime: String? = null,
    val people: String? = null,
    val organization: String? = null,
    val source: String = "Screenshot",
    val sourceApp: String? = null,
    val originalScreenshotUri: String? = null,
    val aiSummary: String? = null,
    val aiConfidence: Float = 0.9f,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val calendarEventId: String? = null,
    val notes: String? = null
)
