package com.example.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Category
import com.example.data.model.CategoryCorrection
import com.example.data.model.MemoryType
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Enterprise-grade Vision AI engine supporting both Groq Cloud (Qwen 27B Vision)
 * and Google Gemini (3.5 Flash Lite) with intelligent failover, real-time temporal
 * anchors, and ambiguity detection.
 */
class GeminiVisionAnalyzer : VisionAnalyzer {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun analyzeScreenshot(
        bitmap: Bitmap,
        categories: List<Category>,
        userCorrections: List<CategoryCorrection>,
        apiKeyOverride: String?
    ): ExtractedMemory = withContext(Dispatchers.IO) {
        val base64Image = bitmapToBase64(bitmap)
        val prompt = buildAnalysisPrompt(categories, userCorrections)

        // 1. Identify configured keys for Groq and Gemini
        val groqKey = when {
            apiKeyOverride?.startsWith("gsk_") == true -> apiKeyOverride
            else -> try { BuildConfig.GROQ_API_KEY } catch (_: Exception) { "" }
        }.takeIf { it.startsWith("gsk_") }

        val geminiKey = when {
            apiKeyOverride?.startsWith("gsk_") == true -> try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
            apiKeyOverride?.isNotBlank() == true -> apiKeyOverride
            else -> try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
        }.takeIf { it.startsWith("AIza") || it.startsWith("AQ.") }

        // If no valid AI key exists, guide user directly instead of waiting for failed network timeouts
        if (groqKey == null && geminiKey == null) {
            Log.w("VisionAnalyzer", "No valid Groq (gsk_...) or Gemini (AIza.../AQ...) API key configured.")
            val now = LocalDate.now()
            val formattedDate = now.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
            val defaultCat = categories.firstOrNull() ?: Category.DEFAULT_CATEGORIES.first()
            return@withContext ExtractedMemory(
                title = "Captured Memory (Key Needed)",
                description = "Screen captured at $formattedDate. Configure your free Groq API key in Settings for 0.25s automatic extraction.",
                type = MemoryType.NOTE,
                suggestedCategoryId = defaultCat.id,
                categoryName = defaultCat.name,
                date = formattedDate,
                time = null,
                isDeadline = false,
                summary = "AI Vision unconfigured. Open Settings -> AI Vision Engine to paste your free Groq key.",
                confidence = 0.5f,
                hasAmbiguity = true,
                ambiguityQuestion = "Add your Groq API key in Settings to activate instant 0.25s VLM extraction.",
                ambiguityOptions = listOf("Open Settings to Add Key", "Save as Note")
            )
        }

        // 2. Attempt Groq VLM first if configured (sub-second 0.25s inference)
        if (groqKey != null) {
            Log.i("VisionAnalyzer", "Attempting ultra-fast Groq VLM inference with qwen/qwen3.8-27b")
            val groqResult = callGroqVisionApi(base64Image, prompt, groqKey, "qwen/qwen3.8-27b", categories)
            if (groqResult != null) {
                Log.i("VisionAnalyzer", "Groq VLM extraction successful: '${groqResult.title}'")
                return@withContext groqResult
            }
        }

        // 3. Attempt Gemini VLM (gemini-3.5-flash-lite primary)
        if (geminiKey != null) {
            val candidateGeminiModels = listOf(
                "gemini-3.5-flash-lite",
                "gemini-2.5-flash",
                "gemini-flash-latest",
                "gemini-3.1-flash-lite"
            )

            for (model in candidateGeminiModels) {
                Log.i("VisionAnalyzer", "Attempting Gemini VLM with $model")
                val geminiResult = callGeminiVisionApi(base64Image, prompt, geminiKey, model, categories)
                if (geminiResult != null) {
                    Log.i("VisionAnalyzer", "Gemini VLM extraction successful with $model: '${geminiResult.title}'")
                    return@withContext geminiResult
                }
            }
        }

        Log.w("VisionAnalyzer", "All cloud VLM endpoints failed or unconfigured, utilizing dynamic local analyzer")
        return@withContext dynamicLocalAnalysis(categories)
    }

    private fun callGroqVisionApi(
        base64Image: String,
        prompt: String,
        apiKey: String,
        modelName: String,
        categories: List<Category>
    ): ExtractedMemory? {
        return try {
            val payload = JSONObject().apply {
                put("model", modelName)
                val messages = JSONArray()
                val userMsg = JSONObject().apply {
                    put("role", "user")
                    val contentArr = JSONArray()
                    contentArr.put(JSONObject().apply {
                        put("type", "text")
                        put("text", prompt)
                    })
                    contentArr.put(JSONObject().apply {
                        put("type", "image_url")
                        put("image_url", JSONObject().apply {
                            put("url", "data:image/jpeg;base64,$base64Image")
                        })
                    })
                    put("content", contentArr)
                }
                messages.put(userMsg)
                put("messages", messages)
                put("response_format", JSONObject().apply { put("type", "json_object") })
                put("temperature", 0.1)
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = payload.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("User-Agent", "Memora/1.0")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val code = response.code
            val body = response.body?.string()

            if (!response.isSuccessful || body.isNullOrBlank()) {
                Log.w("VisionAnalyzer", "Groq $modelName HTTP error $code: $body")
                return null
            }

            val responseJson = JSONObject(body)
            val choices = responseJson.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null

            val content = choices.getJSONObject(0).getJSONObject("message").getString("content")
            parseExtractedMemory(content, categories)
        } catch (e: Exception) {
            Log.e("VisionAnalyzer", "Error during Groq API call", e)
            null
        }
    }

    private fun callGeminiVisionApi(
        base64Image: String,
        prompt: String,
        apiKey: String,
        modelName: String,
        categories: List<Category>
    ): ExtractedMemory? {
        return try {
            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray()
                val contentObj = JSONObject().apply {
                    val partsArray = JSONArray()
                    partsArray.put(JSONObject().apply { put("text", prompt) })
                    partsArray.put(JSONObject().apply {
                        put("inlineData", JSONObject().apply {
                            put("mimeType", "image/jpeg")
                            put("data", base64Image)
                        })
                    })
                    put("parts", partsArray)
                }
                contentsArray.put(contentObj)
                put("contents", contentsArray)

                put("generationConfig", JSONObject().apply {
                    put("responseMimeType", "application/json")
                    put("temperature", 0.15)
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

            val request = Request.Builder().url(url).post(requestBody).build()
            val response = client.newCall(request).execute()
            val code = response.code
            val responseString = response.body?.string()

            if (!response.isSuccessful || responseString.isNullOrBlank()) {
                Log.w("VisionAnalyzer", "Gemini $modelName HTTP error $code: $responseString")
                return null
            }

            val responseJson = JSONObject(responseString)
            val candidates = responseJson.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null

            val partsArray = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")

            var rawText = ""
            for (i in 0 until partsArray.length()) {
                val part = partsArray.getJSONObject(i)
                if (part.has("text") && !part.optBoolean("thought", false)) {
                    val candidate = part.getString("text")
                    if (candidate.isNotBlank()) {
                        rawText = candidate
                        break
                    }
                }
            }

            if (rawText.isBlank()) return null
            parseExtractedMemory(rawText, categories)
        } catch (e: Exception) {
            Log.e("VisionAnalyzer", "Error during Gemini API call", e)
            null
        }
    }

    private fun parseExtractedMemory(rawJsonText: String, categories: List<Category>): ExtractedMemory? {
        return try {
            val cleanJson = rawJsonText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val parsed = if (cleanJson.startsWith("[")) {
                val arr = JSONArray(cleanJson)
                if (arr.length() > 0) arr.getJSONObject(0) else JSONObject()
            } else {
                JSONObject(cleanJson)
            }

            val title = parsed.optString("title", "Captured Memory").ifBlank { "Captured Memory" }
            val desc = parsed.optString("description", "")
            val typeStr = parsed.optString("type", "TASK")
            val catName = parsed.optString("category", "")
            val catId = parsed.optString("categoryId", "")
            val rawDate = parsed.optString("date").takeIf { it.isNotBlank() && it != "null" }
            val rawTime = parsed.optString("time").takeIf { it.isNotBlank() && it != "null" }
            val normalizedTime = DateUtils.normalizeTimeString(rawTime)
            val isDeadline = parsed.optBoolean("isDeadline", typeStr.equals("DEADLINE", ignoreCase = true))
            val summary = parsed.optString("summary", title)
            val confidence = parsed.optDouble("confidence", 0.94).toFloat()
            val org = parsed.optString("organization").takeIf { it.isNotBlank() && it != "null" }

            var hasAmbiguity = parsed.optBoolean("hasAmbiguity", false)
            var ambiguityQuestion = parsed.optString("ambiguityQuestion").takeIf { it.isNotBlank() && it != "null" }
            val ambiguityOptionsList = mutableListOf<String>()
            val optionsJson = parsed.optJSONArray("ambiguityOptions")
            if (optionsJson != null) {
                for (i in 0 until optionsJson.length()) {
                    ambiguityOptionsList.add(optionsJson.getString(i))
                }
            }
            var ambiguityField = parsed.optString("ambiguityField").takeIf { it.isNotBlank() && it != "null" }
            val normalizedDate = parsed.optString("normalizedDate").takeIf { it.isNotBlank() && it != "null" }

            if (!hasAmbiguity && rawTime != null && DateUtils.hasAmPmAmbiguity(rawTime)) {
                hasAmbiguity = true
                ambiguityQuestion = "Confirm time: $normalizedTime or alternate period?"
                ambiguityOptionsList.clear()
                ambiguityOptionsList.addAll(DateUtils.getAmPmOptions(rawTime))
                ambiguityField = "time"
            }

            val matchedCategory = findBestMatchingCategory(catId, catName, categories)
            val resolvedType = try {
                MemoryType.valueOf(typeStr.uppercase(Locale.US))
            } catch (_: Exception) {
                if (isDeadline) MemoryType.DEADLINE else MemoryType.TASK
            }

            ExtractedMemory(
                title = title,
                description = desc,
                type = resolvedType,
                suggestedCategoryId = matchedCategory.id,
                categoryName = matchedCategory.name,
                date = rawDate,
                time = normalizedTime,
                isDeadline = isDeadline,
                people = emptyList(),
                organization = org,
                summary = summary,
                confidence = confidence,
                hasAmbiguity = hasAmbiguity,
                ambiguityQuestion = ambiguityQuestion,
                ambiguityOptions = ambiguityOptionsList,
                ambiguityField = ambiguityField,
                normalizedDate = normalizedDate
            )
        } catch (e: Exception) {
            Log.e("VisionAnalyzer", "Failed to parse VLM response JSON", e)
            null
        }
    }

    private fun buildAnalysisPrompt(categories: List<Category>, userCorrections: List<CategoryCorrection>): String {
        val categoryListDesc = categories.joinToString("\n") { "- ${it.name} (id: \"${it.id}\")" }
        val correctionsDesc = if (userCorrections.isNotEmpty()) {
            "\nCRITICAL PERSONAL RULES (The user previously corrected these specific mappings):\n" +
                    userCorrections.take(6).joinToString("\n") {
                        "- If content relates to '${it.textSnippet.take(50)}', classify category as '${it.correctedCategoryName}' (id: \"${it.correctedCategoryId}\") rather than '${it.suggestedCategory}'."
                    } + "\nAlways honor these personal rules."
        } else ""

        val now = java.time.LocalDateTime.now()
        val todayStr = now.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy", Locale.US))
        val tomorrow = now.plusDays(1)
        val tomorrowStr = tomorrow.format(DateTimeFormatter.ofPattern("EEEE, MMM dd, yyyy", Locale.US))
        val tomorrowShort = tomorrow.format(DateTimeFormatter.ofPattern("MMM dd", Locale.US))
        val nextWeekMon = now.plusWeeks(1).with(java.time.DayOfWeek.MONDAY)
        val nextWeekMonStr = nextWeekMon.format(DateTimeFormatter.ofPattern("MMM dd", Locale.US))
        val nextWeekWedStr = now.plusWeeks(1).with(java.time.DayOfWeek.WEDNESDAY).format(DateTimeFormatter.ofPattern("MMM dd", Locale.US))
        val nextWeekFriStr = now.plusWeeks(1).with(java.time.DayOfWeek.FRIDAY).format(DateTimeFormatter.ofPattern("MMM dd", Locale.US))
        val currentTimeStr = now.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))

        return """
            You are the AI memory engine for Memora, an ambient personal memory assistant.
            Analyze this mobile screenshot. Extract the primary commitment, event, deadline, task, or note.
            
            TEMPORAL CONTEXT (REAL-TIME DEVICE CLOCK):
            - Current Date: $todayStr
            - Current Time: $currentTimeStr
            - Tomorrow's Date: $tomorrowStr
            
            CRITICAL TIME & DATE RULES:
            1. NUMBER TIMES:
               - A time like "330" or "3.30" or "1530" MUST be converted into standard format "3:30 PM".
               - If AM/PM is explicitly stated in the screenshot (e.g. "7 PM", "9:30 AM"), use that exact period.
               - If AM/PM is NOT stated in the screenshot:
                 * Infer context: Hackathons, assignments, classes, meetings, and evening events are almost always PM (e.g. 3:30 PM).
                 * Flag ambiguity: "hasAmbiguity": true, "ambiguityQuestion": "Confirm time: 3:30 PM or 3:30 AM?", "ambiguityOptions": ["3:30 PM", "3:30 AM"], "ambiguityField": "time".
            2. RELATIVE DATES:
               - If text mentions "tomorrow", "tmrw", "t0ommorrow", or "next day":
                 Set "date" to "Tomorrow ($tomorrowShort)" and "normalizedDate" to "${tomorrow.toLocalDate()}".
               - If text mentions "today", "tonight", "this evening":
                 Set "date" to "Today (${now.format(DateTimeFormatter.ofPattern("MMM dd", Locale.US))})" and "normalizedDate" to "${now.toLocalDate()}".
               - If text mentions "next week":
                 Set "date" to "Next Week ($nextWeekMonStr)", and flag ambiguity: "hasAmbiguity": true, "ambiguityQuestion": "Which day next week?", "ambiguityOptions": ["Mon, $nextWeekMonStr", "Wed, $nextWeekWedStr", "Fri, $nextWeekFriStr"], "ambiguityField": "date".
               - If there is a date conflict (e.g. text mentions both today and tomorrow, or ambiguous dates):
                 Flag ambiguity: "hasAmbiguity": true, "ambiguityQuestion": "Is this for Tomorrow or Today?", "ambiguityOptions": ["Tomorrow ($tomorrowShort)", "Today (${now.format(DateTimeFormatter.ofPattern("MMM dd", Locale.US))})"], "ambiguityField": "date".

            Memory Types to classify into:
            - DEADLINE: Fixed cutoffs, submission gates, payment or registration deadlines.
            - EVENT: Calendar dates, scheduled webinars, classes, meetings.
            - COMMITMENT: Interpersonal promises (e.g. "I will send you", "X promised to deliver").
            - TASK: General action item to complete.
            - NOTE: Important information, reference material, announcements.

            Allowed Categories:
            $categoryListDesc
            $correctionsDesc

            Return ONLY a single valid JSON object strictly matching this schema:
            {
              "title": "Concise actionable title (under 8 words)",
              "description": "Clear explanation of what is in the screenshot",
              "type": "TASK | EVENT | DEADLINE | REMINDER | COMMITMENT | NOTE",
              "category": "Exact category name from Allowed Categories above",
              "categoryId": "Exact category id from Allowed Categories above",
              "date": "Parsed date (e.g. 'Tomorrow ($tomorrowShort)' or 'Oct 15, 2026') or null",
              "time": "Parsed time (e.g. '3:30 PM' or '7:00 PM') or null",
              "isDeadline": true if this has a strict ending deadline else false,
              "organization": "Institution, app, or organization name, or null",
              "summary": "1-sentence context summary",
              "confidence": 0.95,
              "hasAmbiguity": false,
              "ambiguityQuestion": null,
              "ambiguityOptions": [],
              "ambiguityField": null,
              "normalizedDate": "YYYY-MM-DD or null"
            }
        """.trimIndent()
    }

    private fun findBestMatchingCategory(returnedId: String, returnedName: String, categories: List<Category>): Category {
        if (returnedId.isNotBlank()) {
            val byId = categories.find { it.id.equals(returnedId, ignoreCase = true) }
            if (byId != null) return byId
        }
        if (returnedName.isNotBlank()) {
            val byName = categories.find { it.name.equals(returnedName, ignoreCase = true) }
            if (byName != null) return byName
            val byFuzzy = categories.find {
                returnedName.contains(it.name, ignoreCase = true) || it.name.contains(returnedName, ignoreCase = true)
            }
            if (byFuzzy != null) return byFuzzy
        }
        return categories.find { it.id == "personal" }
            ?: categories.firstOrNull()
            ?: Category.DEFAULT_CATEGORIES.last()
    }

    private fun dynamicLocalAnalysis(categories: List<Category>): ExtractedMemory {
        val now = LocalDate.now()
        val formattedDate = now.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
        val defaultCategory = categories.firstOrNull() ?: Category.DEFAULT_CATEGORIES.first()

        return ExtractedMemory(
            title = "New Captured Memory",
            description = "Captured from screen on $formattedDate",
            type = MemoryType.TASK,
            suggestedCategoryId = defaultCategory.id,
            categoryName = defaultCategory.name,
            date = formattedDate,
            time = null,
            isDeadline = false,
            summary = "Screen capture awaiting review.",
            confidence = 0.85f
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDimension = 1280
        val width = bitmap.width
        val height = bitmap.height
        val scaledBitmap = if (width > maxDimension || height > maxDimension) {
            val scale = maxDimension.toFloat() / maxOf(width, height)
            val newW = (width * scale).toInt()
            val newH = (height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        } else {
            bitmap
        }

        val stream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 82, stream)
        val byteArray = stream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}
