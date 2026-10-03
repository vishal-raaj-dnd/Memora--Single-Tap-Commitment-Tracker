package com.example.ai

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Category
import com.example.data.model.MemoryType
import com.example.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Intelligent Voice Commitment Parser.
 * Converts spoken natural language (English, Indic, Hinglish) into structured commitments
 * using Groq / Sarvam / Gemini when configured, or deterministic rule-based parsing offline.
 */
class VoiceCommitmentParser(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()
) {

    /**
     * Directly transcribes audio recordings using Sarvam AI's Saaras Speech-to-Text API.
     * Supports WAV, MP3, M4A, AAC with automatic Indic language and code-mixing detection.
     */
    suspend fun transcribeAudioWithSarvam(
        audioFile: File,
        apiKey: String,
        languageCode: String = "unknown"
    ): String? = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank() || !audioFile.exists() || audioFile.length() == 0L) {
            return@withContext null
        }

        val modelsToTry = listOf("saaras:v3", "saaras:v4", "saarika:v2.5")
        for (modelName in modelsToTry) {
            try {
                val mediaType = when (audioFile.extension.lowercase()) {
                    "m4a", "mp4" -> "audio/mp4".toMediaType()
                    "wav" -> "audio/wav".toMediaType()
                    "mp3" -> "audio/mpeg".toMediaType()
                    "aac" -> "audio/aac".toMediaType()
                    else -> "audio/mp4".toMediaType()
                }

                val requestBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", audioFile.name, audioFile.asRequestBody(mediaType))
                    .addFormDataPart("model", modelName)
                    .addFormDataPart("language_code", languageCode)
                    .build()

                val request = Request.Builder()
                    .url("https://api.sarvam.ai/speech-to-text")
                    .header("api-subscription-key", cleanKey)
                    .header("Authorization", "Bearer $cleanKey")
                    .post(requestBody)
                    .build()

                Log.d("VoiceCommitmentParser", "Transcribing audio with Sarvam model: $modelName (${audioFile.length()} bytes)")
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    val json = JSONObject(responseBody)
                    val transcript = json.optString("transcript", "").trim()
                    val detectedLang = json.optString("language_code", "")
                    Log.d("VoiceCommitmentParser", "Sarvam STT success! Detected lang: $detectedLang, transcript: \"$transcript\"")
                    if (transcript.isNotBlank()) {
                        return@withContext transcript
                    }
                } else {
                    Log.w("VoiceCommitmentParser", "Sarvam STT model $modelName returned HTTP ${response.code}: $responseBody")
                }
            } catch (e: Exception) {
                Log.w("VoiceCommitmentParser", "Sarvam STT error with $modelName: ${e.message}")
            }
        }
        return@withContext null
    }

    suspend fun parseVoiceCommitment(
        spokenText: String,
        categories: List<Category>,
        groqApiKey: String? = null,
        sarvamApiKey: String? = null,
        geminiApiKey: String? = null,
        preferredEngine: String? = null
    ): ExtractedMemory = withContext(Dispatchers.IO) {
        val trimmed = spokenText.trim()
        if (trimmed.isBlank()) {
            return@withContext fallbackLocalExtraction("New Commitment", categories).copy(engineUsed = "Smart Local")
        }

        if (preferredEngine == "LOCAL") {
            return@withContext fallbackLocalExtraction(trimmed, categories).copy(engineUsed = "Smart Local")
        }

        // Check configured API keys
        val activeGroqKey = groqApiKey?.takeIf { it.isNotBlank() && it != "MY_GROQ_API_KEY" }
            ?: try { BuildConfig.GROQ_API_KEY } catch (_: Exception) { "" }?.takeIf { it.startsWith("gsk_") }

        val activeSarvamKey = sarvamApiKey?.takeIf { it.isNotBlank() && it != "MY_SARVAM_API_KEY" }
            ?: try {
                val field = BuildConfig::class.java.getField("SARVAM_API_KEY")
                field.get(null) as? String
            } catch (_: Exception) { "" }?.takeIf { it.isNotBlank() && it != "MY_SARVAM_API_KEY" }

        // 1. If Sarvam is preferred or configured, try Sarvam AI
        if (activeSarvamKey != null && (preferredEngine == "SARVAM" || preferredEngine == null)) {
            val sarvamResult = callSarvamParser(trimmed, categories, activeSarvamKey)
            if (sarvamResult != null) return@withContext sarvamResult.copy(engineUsed = "Sarvam AI")
        }

        // 2. Try Groq Cloud if preferred or fallback
        if (activeGroqKey != null && (preferredEngine == "GROQ" || preferredEngine == null)) {
            val groqResult = callGroqParser(trimmed, categories, activeGroqKey)
            if (groqResult != null) return@withContext groqResult.copy(engineUsed = "Groq Cloud")
        }

        // 3. Try Sarvam if it wasn't called yet
        if (activeSarvamKey != null && preferredEngine != "SARVAM") {
            val sarvamResult = callSarvamParser(trimmed, categories, activeSarvamKey)
            if (sarvamResult != null) return@withContext sarvamResult.copy(engineUsed = "Sarvam AI")
        }

        // High quality deterministic offline fallback
        return@withContext fallbackLocalExtraction(trimmed, categories).copy(engineUsed = "Smart Local")
    }

    private fun callGroqParser(
        spokenText: String,
        categories: List<Category>,
        apiKey: String
    ): ExtractedMemory? {
        return try {
            val catListStr = categories.joinToString(", ") { "${it.id} (${it.name})" }
            val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))

            val systemPrompt = """
                You are a voice commitment parser for an Android productivity app.
                Today's date is: $todayStr.
                Available categories: [$catListStr].
                
                Given a spoken commitment in English, Hindi, or Hinglish, extract the exact structured data.
                Respond STRICTLY in JSON format with NO markdown fences, matching this schema:
                {
                  "title": "Clean, concise title of the commitment",
                  "categoryId": "one of the available category IDs",
                  "type": "TASK" | "EVENT" | "DEADLINE" | "NOTE",
                  "date": "MMM dd, yyyy" (e.g. "Oct 04, 2026") or null,
                  "time": "h:mm a" (e.g. "5:00 PM") or null,
                  "isDeadline": boolean,
                  "reminderTime": "1 hour before" | "1 day before" | "At time of event" | "None",
                  "notes": "Full spoken transcript or extra details"
                }
            """.trimIndent()

            val jsonBody = JSONObject().apply {
                put("model", "llama-3.3-70b-versatile")
                val messages = JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", spokenText)
                    })
                }
                put("messages", messages)
                put("temperature", 0.1)
                put("response_format", JSONObject().put("type", "json_object"))
            }

            val request = Request.Builder()
                .url("https://api.groq.com/openai/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                val json = JSONObject(responseBody)
                val choices = json.optJSONArray("choices")
                if (choices != null && choices.length() > 0) {
                    val content = choices.getJSONObject(0).getJSONObject("message").getString("content")
                    return parseExtractedJson(content, categories, spokenText).copy(engineUsed = "Groq Cloud")
                }
            }
            null
        } catch (e: Exception) {
            Log.w("VoiceCommitmentParser", "Groq voice parsing fallback: ${e.message}")
            null
        }
    }

    private fun callSarvamParser(
        spokenText: String,
        categories: List<Category>,
        apiKey: String
    ): ExtractedMemory? {
        val modelsToTry = listOf("sarvam-105b", "sarvam-105b-conversations", "deepseekv4-flash")
        val catListStr = categories.joinToString(", ") { "${it.id} (${it.name})" }
        val todayStr = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))

        val systemPrompt = """
            You are a voice commitment parser for an Indian context (handling English, Hindi, Tamil, Telugu, Hinglish, college jargon like IITM, AMET, semester tests, hackathons).
            Today is $todayStr. Available categories: [$catListStr].
            Extract the commitment into strict JSON without any markdown formatting:
            {"title": "...", "categoryId": "...", "type": "TASK"|"EVENT"|"DEADLINE"|"NOTE", "date": "MMM dd, yyyy"|null, "time": "h:mm a"|null, "isDeadline": true|false, "reminderTime": "...", "notes": "..."}
        """.trimIndent()

        for (modelName in modelsToTry) {
            try {
                val jsonBody = JSONObject().apply {
                    put("model", modelName)
                    val messages = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "system")
                            put("content", systemPrompt)
                        })
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", spokenText)
                        })
                    }
                    put("messages", messages)
                    put("temperature", 0.1)
                }

                val cleanKey = apiKey.trim()
                val request = Request.Builder()
                    .url("https://api.sarvam.ai/v1/chat/completions")
                    .header("api-subscription-key", cleanKey)
                    .header("Authorization", "Bearer $cleanKey")
                    .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                    .build()

                Log.d("VoiceCommitmentParser", "Querying Sarvam AI with model: $modelName")
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && !responseBody.isNullOrBlank()) {
                    Log.d("VoiceCommitmentParser", "Sarvam response success ($modelName)")
                    val json = JSONObject(responseBody)
                    val choices = json.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val content = choices.getJSONObject(0).getJSONObject("message").getString("content")
                        val cleanJson = content.replace("```json", "").replace("```", "").trim()
                        return parseExtractedJson(cleanJson, categories, spokenText).copy(engineUsed = "Sarvam AI")
                    }
                } else {
                    Log.w("VoiceCommitmentParser", "Sarvam model $modelName returned HTTP ${response.code}: $responseBody")
                }
            } catch (e: Exception) {
                Log.w("VoiceCommitmentParser", "Sarvam model $modelName attempt failed: ${e.message}")
            }
        }
        return null
    }

    private fun parseExtractedJson(
        jsonString: String,
        categories: List<Category>,
        originalSpokenText: String
    ): ExtractedMemory {
        val parsed = JSONObject(jsonString)
        val title = parsed.optString("title").takeIf { it.isNotBlank() } ?: originalSpokenText
        val catId = parsed.optString("categoryId").takeIf { it.isNotBlank() }
            ?: categories.firstOrNull()?.id ?: "personal"
        val matchedCategory = categories.find { it.id.equals(catId, ignoreCase = true) }
            ?: categories.firstOrNull() ?: Category.DEFAULT_CATEGORIES.first()
        val typeStr = parsed.optString("type", "TASK")
        val memoryType = MemoryType.fromString(typeStr)
        val date = parsed.optString("date").takeIf { it.isNotBlank() && it != "null" }
            ?: LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
        val time = parsed.optString("time").takeIf { it.isNotBlank() && it != "null" }
        val isDeadline = parsed.optBoolean("isDeadline", memoryType == MemoryType.DEADLINE)
        val reminder = parsed.optString("reminderTime").takeIf { it.isNotBlank() && it != "null" }
            ?: if (isDeadline) "1 day before" else "1 hour before"

        return ExtractedMemory(
            title = title,
            description = originalSpokenText,
            type = memoryType,
            suggestedCategoryId = matchedCategory.id,
            categoryName = matchedCategory.name,
            date = date,
            time = time,
            isDeadline = isDeadline,
            summary = "Voice captured: \"$originalSpokenText\"",
            confidence = 0.95f
        )
    }

    /**
     * Deterministic offline natural language parser using date and temporal patterns.
     */
    fun fallbackLocalExtraction(
        spokenText: String,
        categories: List<Category>
    ): ExtractedMemory {
        val lower = spokenText.lowercase()
        val today = LocalDate.now()
        val fullDateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US)

        // 1. Detect relative or absolute date
        val resolvedDate: LocalDate = when {
            lower.contains("day after tomorrow") || lower.contains("parson") -> today.plusDays(2)
            lower.contains("tomorrow") || lower.contains("tmrw") || lower.contains("kal") -> today.plusDays(1)
            lower.contains("today") || lower.contains("aaj") || lower.contains("tonight") -> today
            lower.contains("monday") -> today.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
            lower.contains("tuesday") -> today.with(TemporalAdjusters.next(DayOfWeek.TUESDAY))
            lower.contains("wednesday") -> today.with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY))
            lower.contains("thursday") -> today.with(TemporalAdjusters.next(DayOfWeek.THURSDAY))
            lower.contains("friday") -> today.with(TemporalAdjusters.next(DayOfWeek.FRIDAY))
            lower.contains("saturday") -> today.with(TemporalAdjusters.next(DayOfWeek.SATURDAY))
            lower.contains("sunday") -> today.with(TemporalAdjusters.next(DayOfWeek.SUNDAY))
            else -> today
        }

        // 2. Detect time patterns (e.g., "5 pm", "3:30", "10 am", "morning", "evening")
        var extractedTime: String? = null
        val timeRegex = Pattern.compile("(\\b(?:1[0-2]|0?[1-9])(?::[0-5][0-9])?\\s*(?:am|pm)\\b|\\b(?:1[0-2]|0?[1-9])\\s*o'?clock\\b)", Pattern.CASE_INSENSITIVE)
        val matcher = timeRegex.matcher(spokenText)
        if (matcher.find()) {
            val rawTime = matcher.group(1)
            extractedTime = DateUtils.normalizeTimeString(rawTime)
        } else if (lower.contains("evening") || lower.contains("shaam")) {
            extractedTime = "6:00 PM"
        } else if (lower.contains("morning") || lower.contains("subah")) {
            extractedTime = "9:00 AM"
        } else if (lower.contains("afternoon") || lower.contains("dopahar")) {
            extractedTime = "2:00 PM"
        }

        // 3. Detect MemoryType
        val memoryType = when {
            lower.contains("deadline") || lower.contains("due") || lower.contains("submit") || lower.contains("submission") -> MemoryType.DEADLINE
            lower.contains("meeting") || lower.contains("meet") || lower.contains("sync") || lower.contains("call") || lower.contains("event") || lower.contains("party") || lower.contains("session") -> MemoryType.EVENT
            lower.contains("note") || lower.contains("remember that") || lower.contains("idea") -> MemoryType.NOTE
            else -> MemoryType.TASK
        }

        // 4. Match Category from available list
        val matchedCategory = categories.firstOrNull { cat ->
            lower.contains(cat.id.lowercase()) || lower.contains(cat.name.lowercase())
        } ?: categories.firstOrNull { cat ->
            when (cat.id.lowercase()) {
                "hackathon" -> lower.contains("hackathon") || lower.contains("coding") || lower.contains("project") || lower.contains("prototype")
                "academics", "iitm", "eduvia" -> lower.contains("exam") || lower.contains("quiz") || lower.contains("assignment") || lower.contains("class") || lower.contains("lecture") || lower.contains("college")
                "discussions", "discussion" -> lower.contains("discuss") || lower.contains("talk") || lower.contains("interview")
                "errands" -> lower.contains("buy") || lower.contains("groceries") || lower.contains("shop") || lower.contains("pick up")
                "fitness", "personal" -> lower.contains("gym") || lower.contains("workout") || lower.contains("run") || lower.contains("exercise") || lower.contains("health")
                else -> false
            }
        } ?: categories.firstOrNull() ?: Category.DEFAULT_CATEGORIES.first()

        // 5. Clean up spoken title
        var cleanTitle = spokenText
            .replace("(?i)\\b(remind me to|remember to|don't forget to|add task to|add commitment to)\\b".toRegex(), "")
            .trim()
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString() }

        if (cleanTitle.isBlank()) cleanTitle = spokenText

        return ExtractedMemory(
            title = cleanTitle,
            description = spokenText,
            type = memoryType,
            suggestedCategoryId = matchedCategory.id,
            categoryName = matchedCategory.name,
            date = resolvedDate.format(fullDateFormatter),
            time = extractedTime,
            isDeadline = memoryType == MemoryType.DEADLINE,
            summary = "Voice: $spokenText",
            confidence = 0.88f,
            engineUsed = "Smart Local"
        )
    }
}
