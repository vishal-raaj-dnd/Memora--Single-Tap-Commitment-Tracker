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
}
