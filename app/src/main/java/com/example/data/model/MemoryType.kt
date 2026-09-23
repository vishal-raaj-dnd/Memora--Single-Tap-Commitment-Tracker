package com.example.data.model

enum class MemoryType(val displayName: String, val icon: String) {
    TASK("Task", "check"),
    EVENT("Event", "event"),
    DEADLINE("Deadline", "deadline"),
    REMINDER("Reminder", "reminder"),
    COMMITMENT("Commitment", "commitment"),
    NOTE("Note", "note");

    companion object {
        fun fromString(value: String?): MemoryType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: TASK
        }
    }
}
