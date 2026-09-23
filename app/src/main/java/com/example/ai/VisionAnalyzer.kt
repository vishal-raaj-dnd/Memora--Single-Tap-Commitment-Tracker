package com.example.ai

import android.graphics.Bitmap
import com.example.data.model.Category
import com.example.data.model.CategoryCorrection

interface VisionAnalyzer {
    suspend fun analyzeScreenshot(
        bitmap: Bitmap,
        categories: List<Category> = Category.DEFAULT_CATEGORIES,
        userCorrections: List<CategoryCorrection> = emptyList(),
        apiKeyOverride: String? = null
    ): ExtractedMemory
}
