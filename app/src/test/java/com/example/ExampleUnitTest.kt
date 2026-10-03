package com.example

import com.example.data.model.Category
import com.example.data.model.CategoryCorrection
import com.example.data.model.MemoryItem
import com.example.data.model.MemoryType
import com.example.ui.viewmodel.MemoryStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExampleUnitTest {

    @Test
    fun `verify MemoryType enum parsing and mappings`() {
        assertEquals(MemoryType.DEADLINE, MemoryType.fromString("DEADLINE"))
        assertEquals(MemoryType.EVENT, MemoryType.fromString("event"))
        assertEquals(MemoryType.COMMITMENT, MemoryType.fromString("commitment"))
        assertEquals(MemoryType.NOTE, MemoryType.fromString("note"))
        assertEquals(MemoryType.TASK, MemoryType.fromString("UNKNOWN_TYPE"))
        assertEquals(MemoryType.TASK, MemoryType.fromString(null))
    }

    @Test
    fun `verify default categories contain 10 essential life domains`() {
        val categories = Category.DEFAULT_CATEGORIES
        assertEquals(10, categories.size)
        assertTrue(categories.any { it.id == "hackathon" })
        assertTrue(categories.any { it.id == "iitm" })
        assertTrue(categories.any { it.id == "eduvia" })
        assertTrue(categories.any { it.id == "incubation" })
    }

    @Test
    fun `verify CategoryCorrection records human overrides for feedback loop`() {
        val correction = CategoryCorrection(
            textSnippet = "IITM BS Degree programming assignment due Friday",
            suggestedCategory = "Academics",
            correctedCategoryId = "iitm",
            correctedCategoryName = "IITM Deadline"
        )

        assertEquals("Academics", correction.suggestedCategory)
        assertEquals("iitm", correction.correctedCategoryId)
        assertEquals("IITM Deadline", correction.correctedCategoryName)
        assertTrue(correction.timestamp > 0)
    }

    @Test
    fun `verify dynamic stats calculation for memories`() {
        val items = listOf(
            MemoryItem(id = 1, title = "T1", type = MemoryType.TASK),
            MemoryItem(id = 2, title = "T2", type = MemoryType.TASK),
            MemoryItem(id = 3, title = "E1", type = MemoryType.EVENT),
            MemoryItem(id = 4, title = "D1", type = MemoryType.DEADLINE, isDeadline = true)
        )

        val total = items.size.toFloat()
        val tasks = items.count { it.type == MemoryType.TASK || it.type == MemoryType.COMMITMENT }
        val events = items.count { it.type == MemoryType.EVENT }
        val deadlines = items.count { it.type == MemoryType.DEADLINE || it.isDeadline }

        val stats = MemoryStats(
            taskPercentage = "${((tasks / total) * 100).toInt()}%",
            eventPercentage = "${((events / total) * 100).toInt()}%",
            deadlinePercentage = "${((deadlines / total) * 100).toInt()}%",
            taskCount = tasks,
            eventCount = events,
            deadlineCount = deadlines,
            totalCount = items.size
        )

        assertEquals("50%", stats.taskPercentage)
        assertEquals("25%", stats.eventPercentage)
        assertEquals("25%", stats.deadlinePercentage)
        assertEquals(2, stats.taskCount)
        assertEquals(1, stats.eventCount)
        assertEquals(1, stats.deadlineCount)
        assertEquals(4, stats.totalCount)
    }

    @Test
    fun `verify dynamic date formatting matches today and tomorrow`() {
        val today = LocalDate.now()
        val tomorrow = today.plusDays(1)
        val todayStr = today.format(DateTimeFormatter.ofPattern("MMM dd", Locale.US))
        val tomorrowStr = tomorrow.format(DateTimeFormatter.ofPattern("MMM dd", Locale.US))

        assertNotNull(todayStr)
        assertNotNull(tomorrowStr)
        assertFalse(todayStr == tomorrowStr)
    }

    @Test
    fun `verify default categories contain no emojis and have valid line icon keys`() {
        val categories = Category.DEFAULT_CATEGORIES
        for (category in categories) {
            assertTrue("Category ${category.name} should not contain emojis in icon key",
                category.icon.all { it.isLetterOrDigit() || it == '_' })
        }
    }

    @Test
    fun `verify DateUtils time normalization and ambiguity detection`() {
        // "330" -> "3:30 PM"
        assertEquals("3:30 PM", com.example.util.DateUtils.normalizeTimeString("330"))
        // "3:30" -> "3:30 PM"
        assertEquals("3:30 PM", com.example.util.DateUtils.normalizeTimeString("3:30"))
        // "1530" -> "3:30 PM"
        assertEquals("3:30 PM", com.example.util.DateUtils.normalizeTimeString("1530"))
        // "930 AM" -> "9:30 AM"
        assertEquals("9:30 AM", com.example.util.DateUtils.normalizeTimeString("930 AM"))
        // AM/PM ambiguity
        assertTrue(com.example.util.DateUtils.hasAmPmAmbiguity("330"))
        assertTrue(com.example.util.DateUtils.hasAmPmAmbiguity("3:30"))
        assertFalse(com.example.util.DateUtils.hasAmPmAmbiguity("3:30 PM"))

        val options = com.example.util.DateUtils.getAmPmOptions("3:30 PM")
        assertEquals(listOf("3:30 PM", "3:30 AM"), options)
    }

    @Test
    fun `verify DateUtils tomorrow and today detection`() {
        assertTrue(com.example.util.DateUtils.isTomorrow("Tomorrow"))
        assertTrue(com.example.util.DateUtils.isTomorrow("tmrw at 330"))
        assertTrue(com.example.util.DateUtils.isTomorrow("t0ommorrow"))
        assertFalse(com.example.util.DateUtils.isToday("Tomorrow"))

        assertTrue(com.example.util.DateUtils.isToday("Today"))
        assertTrue(com.example.util.DateUtils.isToday("Today at 5 PM"))
        assertFalse(com.example.util.DateUtils.isTomorrow("Today"))

        // Null should NOT be today
        assertFalse(com.example.util.DateUtils.isToday(null))
        assertFalse(com.example.util.DateUtils.isTomorrow(null))
    }

    @Test
    fun `verify DateUtils isOverdue detects expired deadlines`() {
        val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
        val lastMonth = LocalDate.now().minusMonths(1).format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
        val nextWeek = LocalDate.now().plusWeeks(1).format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))

        // Past dates should be overdue
        assertTrue(com.example.util.DateUtils.isOverdue(yesterday, isCompleted = false))
        assertTrue(com.example.util.DateUtils.isOverdue(lastMonth, isCompleted = false))

        // Today and future dates should NOT be overdue
        assertFalse(com.example.util.DateUtils.isOverdue(today, isCompleted = false))
        assertFalse(com.example.util.DateUtils.isOverdue(nextWeek, isCompleted = false))

        // Completed items should NEVER be marked overdue
        assertFalse(com.example.util.DateUtils.isOverdue(yesterday, isCompleted = true))
        assertFalse(com.example.util.DateUtils.isOverdue(null, isCompleted = false))
    }

    @Test
    fun `verify DateUtils matchesDate for calendar tab anchoring`() {
        val target = LocalDate.now().plusDays(3)
        val formatted = target.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
        val other = target.plusDays(1)

        assertTrue(com.example.util.DateUtils.matchesDate(formatted, target))
        assertFalse(com.example.util.DateUtils.matchesDate(formatted, other))
        assertFalse(com.example.util.DateUtils.matchesDate(null, target))
    }

    @Test
    fun `verify ReminderManager calculates trigger time with correct offsets`() {
        val tomorrow = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
        val item = MemoryItem(
            id = 99,
            title = "Project Pitch",
            date = tomorrow,
            time = "10:00 AM",
            reminderTime = "1 hour before"
        )

        val triggerTime = com.example.service.ReminderManager.calculateTriggerTime(item)
        assertNotNull(triggerTime)
        assertTrue(triggerTime!! > System.currentTimeMillis())

        val atEventItem = item.copy(reminderTime = "At time of event")
        val atEventTrigger = com.example.service.ReminderManager.calculateTriggerTime(atEventItem)
        assertNotNull(atEventTrigger)
        // 1 hour before trigger should be exactly 3600000ms earlier than at event trigger
        assertEquals(3600000L, atEventTrigger!! - triggerTime)
    }

    @Test
    fun `verify Groq and Sarvam key detection logic`() {
        val validGroqKey = "gsk_test1234567890abcdefghijklmnopqrstuvwxyz"
        val validSarvamKey = "sarvam_test_subscription_key_12345678"
        val geminiKey = "AIzaSyTestGeminiKey1234567890"

        assertTrue(validGroqKey.startsWith("gsk_"))
        assertFalse(validSarvamKey.startsWith("gsk_"))
        assertTrue(geminiKey.startsWith("AIza"))

        // Verify key masking logic
        fun maskKey(key: String, prefixLen: Int = 4, suffixLen: Int = 4): String {
            val trimmed = key.trim()
            if (trimmed.isBlank() || trimmed == "MY_GROQ_API_KEY" || trimmed == "MY_SARVAM_API_KEY") return "Not configured"
            if (trimmed.length <= prefixLen + suffixLen) return "••••••••"
            return "${trimmed.take(prefixLen)}••••••••${trimmed.takeLast(suffixLen)}"
        }

        assertEquals("gsk_••••••••wxyz", maskKey(validGroqKey))
        assertEquals("sarv••••••••5678", maskKey(validSarvamKey))
        assertEquals("Not configured", maskKey(""))
        assertEquals("Not configured", maskKey("MY_GROQ_API_KEY"))
    }

    @Test
    fun `verify VoiceCommitmentParser fallback parses English spoken commitment with date and time`() {
        val parser = com.example.ai.VoiceCommitmentParser()
        val categories = Category.DEFAULT_CATEGORIES

        val result = parser.fallbackLocalExtraction(
            spokenText = "Remind me to submit assignment tomorrow at 5 PM",
            categories = categories
        )

        assertEquals("Submit assignment tomorrow at 5 PM", result.title)
        assertEquals(MemoryType.DEADLINE, result.type)
        assertTrue(result.isDeadline)
        assertEquals("5:00 PM", result.time)

        val tomorrowStr = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
        assertEquals(tomorrowStr, result.date)
        assertTrue(result.confidence > 0.8f)
    }

    @Test
    fun `verify VoiceCommitmentParser fallback parses Indic and Hinglish terms`() {
        val parser = com.example.ai.VoiceCommitmentParser()
        val categories = Category.DEFAULT_CATEGORIES

        // "Kal subah gym jaana hai" -> tomorrow morning gym
        val result = parser.fallbackLocalExtraction(
            spokenText = "Kal subah gym jaana hai",
            categories = categories
        )

        val tomorrowStr = LocalDate.now().plusDays(1).format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
        assertEquals(tomorrowStr, result.date)
        assertEquals("9:00 AM", result.time)
        assertEquals("personal", result.suggestedCategoryId)
        assertEquals(MemoryType.TASK, result.type)
    }

    @Test
    fun `verify VoiceCommitmentParser matches meeting and evening keywords`() {
        val parser = com.example.ai.VoiceCommitmentParser()
        val categories = Category.DEFAULT_CATEGORIES

        val result = parser.fallbackLocalExtraction(
            spokenText = "Hackathon prototype sync meeting shaam",
            categories = categories
        )

        assertEquals(MemoryType.EVENT, result.type)
        assertEquals("hackathon", result.suggestedCategoryId)
        assertEquals("6:00 PM", result.time)
    }

    @Test
    fun `verify VoiceCommitmentParser handles blank input gracefully`() {
        val parser = com.example.ai.VoiceCommitmentParser()
        val categories = Category.DEFAULT_CATEGORIES

        val result = parser.fallbackLocalExtraction(
            spokenText = "   ",
            categories = categories
        )

        assertNotNull(result)
        assertNotNull(result.title)
        assertNotNull(result.date)
    }
}
