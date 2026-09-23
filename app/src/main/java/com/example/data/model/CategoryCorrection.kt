package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores human corrections when the user overrides the AI-suggested category.
 * Used to build a personalized context layer for future Gemini Vision prompts.
 */
@Entity(tableName = "category_corrections")
data class CategoryCorrection(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val textSnippet: String,
    val suggestedCategory: String,
    val correctedCategoryId: String,
    val correctedCategoryName: String,
    val timestamp: Long = System.currentTimeMillis()
)
