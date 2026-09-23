package com.example.ai

import com.example.data.model.MemoryType

data class ExtractedMemory(
    val title: String,
    val description: String,
    val type: MemoryType,
    val suggestedCategoryId: String,
    val categoryName: String,
    val date: String? = null,
    val time: String? = null,
    val isDeadline: Boolean = false,
    val people: List<String> = emptyList(),
    val organization: String? = null,
    val summary: String = "",
    val confidence: Float = 0.94f,
    val originalScreenshotUri: String? = null,
    val hasAmbiguity: Boolean = false,
    val ambiguityQuestion: String? = null,
    val ambiguityOptions: List<String> = emptyList(),
    val ambiguityField: String? = null,
    val normalizedDate: String? = null
)
