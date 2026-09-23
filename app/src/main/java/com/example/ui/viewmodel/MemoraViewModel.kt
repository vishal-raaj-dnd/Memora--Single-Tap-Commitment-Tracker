package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.ai.ExtractedMemory
import com.example.ai.GeminiVisionAnalyzer
import com.example.ai.VisionAnalyzer
import com.example.data.local.MemoraDatabase
import com.example.data.model.Category
import com.example.data.model.CategoryCorrection
import com.example.data.model.MemoryItem
import com.example.data.model.MemoryType
import com.example.data.repository.CategoryRepository
import com.example.data.repository.MemoryRepository
import com.example.service.CalendarManager
import com.example.service.FloatingCaptureService
import com.example.service.ReminderManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

data class MemoryStats(
    val taskPercentage: String = "0%",
    val eventPercentage: String = "0%",
    val deadlinePercentage: String = "0%",
    val notePercentage: String = "0%",
    val taskCount: Int = 0,
    val eventCount: Int = 0,
    val deadlineCount: Int = 0,
    val noteCount: Int = 0,
    val totalCount: Int = 0
)

class MemoraViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("memora_prefs", Context.MODE_PRIVATE)

    private val database = MemoraDatabase.getDatabase(application, viewModelScope)
    private val memoryRepo = MemoryRepository(database.memoryDao())
    private val categoryRepo = CategoryRepository(database.categoryDao())
    private val correctionDao = database.categoryCorrectionDao()
    private val visionAnalyzer: VisionAnalyzer = GeminiVisionAnalyzer()
    private val reminderManager = ReminderManager(application)

    val allMemories: StateFlow<List<MemoryItem>> = memoryRepo.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCategories: StateFlow<List<Category>> = categoryRepo.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Category.DEFAULT_CATEGORIES)

    private val _userApiKey = MutableStateFlow(
        prefs.getString("custom_api_key", null) ?: try { BuildConfig.GEMINI_API_KEY } catch (_: Exception) { "" }
    )
    val userApiKey = _userApiKey.asStateFlow()

    private val _userName = MutableStateFlow(
        prefs.getString("user_name", "Vishal") ?: "Vishal"
    )
    val userName = _userName.asStateFlow()

    private val _userRole = MutableStateFlow(
        prefs.getString("user_role", "Student & Builder") ?: "Student & Builder"
    )
    val userRole = _userRole.asStateFlow()

    private val _userAvatar = MutableStateFlow(
        prefs.getString("user_avatar", "builder") ?: "builder"
    )
    val userAvatar = _userAvatar.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter = _selectedFilter.asStateFlow()

    private val _timelineTab = MutableStateFlow("Upcoming")
    val timelineTab = _timelineTab.asStateFlow()

    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()

    private val _analysisFailed = MutableStateFlow(false)
    val analysisFailed = _analysisFailed.asStateFlow()

    private val _analysisStep = MutableStateFlow(0)
    val analysisStep = _analysisStep.asStateFlow()

    private val _currentExtracted = MutableStateFlow<ExtractedMemory?>(null)
    val currentExtracted = _currentExtracted.asStateFlow()

    private val _selectedMemory = MutableStateFlow<MemoryItem?>(null)
    val selectedMemory = _selectedMemory.asStateFlow()

    private val _lastSavedItem = MutableStateFlow<MemoryItem?>(null)
    val lastSavedItem = _lastSavedItem.asStateFlow()

    private val _isFloatingServiceEnabled = MutableStateFlow(FloatingCaptureService.isServiceRunning)
    val isFloatingServiceEnabled = _isFloatingServiceEnabled.asStateFlow()

    private val _activeCaptureMode = MutableStateFlow("Screenshot")
    val activeCaptureMode = _activeCaptureMode.asStateFlow()

    val memoryStats: StateFlow<MemoryStats> = allMemories.map { memories ->
        val total = memories.size
        if (total == 0) {
            MemoryStats()
        } else {
            val tasks = memories.count { it.type == MemoryType.TASK }
            val events = memories.count { it.type == MemoryType.EVENT }
            val deadlines = memories.count { it.type == MemoryType.DEADLINE }
            val notes = memories.count { it.type == MemoryType.NOTE }

            MemoryStats(
                taskPercentage = "${((tasks.toFloat() / total) * 100).toInt()}%",
                eventPercentage = "${((events.toFloat() / total) * 100).toInt()}%",
                deadlinePercentage = "${((deadlines.toFloat() / total) * 100).toInt()}%",
                notePercentage = "${((notes.toFloat() / total) * 100).toInt()}%",
                taskCount = tasks,
                eventCount = events,
                deadlineCount = deadlines,
                noteCount = notes,
                totalCount = total
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MemoryStats())

    val filteredMemories: StateFlow<List<MemoryItem>> = combine(
        allMemories,
        _searchQuery,
        _selectedFilter
    ) { memories, query, filter ->
        memories.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.description.contains(query, ignoreCase = true) ||
                (item.aiSummary?.contains(query, ignoreCase = true) == true) ||
                (item.notes?.contains(query, ignoreCase = true) == true) ||
                (item.organization?.contains(query, ignoreCase = true) == true) ||
                (item.people?.contains(query, ignoreCase = true) == true)

            val matchesFilter = when (filter) {
                "All" -> true
                "Tasks" -> item.type == MemoryType.TASK
                "Events" -> item.type == MemoryType.EVENT
                "Deadlines" -> item.type == MemoryType.DEADLINE
                "Notes" -> item.type == MemoryType.NOTE
                else -> item.categoryId.equals(filter, ignoreCase = true)
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateCustomApiKey(key: String) {
        _userApiKey.value = key
        prefs.edit().putString("custom_api_key", key).apply()
    }

    fun updateApiKey(key: String) = updateCustomApiKey(key)

    fun updateUserName(name: String) {
        val trimmed = name.trim().ifBlank { "User" }
        _userName.value = trimmed
        prefs.edit().putString("user_name", trimmed).apply()
    }

    fun updateUserRole(role: String) {
        val trimmed = role.trim().ifBlank { "Student & Builder" }
        _userRole.value = trimmed
        prefs.edit().putString("user_role", trimmed).apply()
    }

    fun updateUserAvatar(avatarKey: String) {
        _userAvatar.value = avatarKey
        prefs.edit().putString("user_avatar", avatarKey).apply()
    }

    fun updateProfile(name: String, role: String, avatarKey: String) {
        updateUserName(name)
        updateUserRole(role)
        updateUserAvatar(avatarKey)
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedFilter(filter: String) {
        _selectedFilter.value = filter
    }

    fun setTimelineTab(tab: String) {
        _timelineTab.value = tab
    }

    fun setActiveCaptureMode(mode: String) {
        _activeCaptureMode.value = mode
    }

    fun setSelectedMemory(item: MemoryItem?) {
        _selectedMemory.value = item
    }

    fun selectMemoryById(id: Long) {
        viewModelScope.launch {
            val item = allMemories.value.find { it.id == id }
            _selectedMemory.value = item
        }
    }

    fun startCaptureAndAnalysis(
        providedBitmap: Bitmap? = null,
        existingScreenshotPath: String? = null,
        onNavigateToProcessing: () -> Unit,
        onNavigateToReview: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.Main) {
            _isAnalyzing.value = true
            _analysisFailed.value = false
            _analysisStep.value = 0
            onNavigateToProcessing()

            val bitmap: Bitmap? = providedBitmap ?: existingScreenshotPath?.let { path ->
                try {
                    BitmapFactory.decodeFile(path)
                } catch (e: Exception) {
                    null
                }
            }

            if (bitmap == null) {
                _isAnalyzing.value = false
                _analysisFailed.value = true
                return@launch
            }

            val screenshotUri = existingScreenshotPath ?: withContext(Dispatchers.IO) {
                saveBitmapToPermanentStorage(bitmap)
            }

            _analysisStep.value = 1 // Identifying key information
            delay(150)

            val categories = allCategories.value
            val corrections = withContext(Dispatchers.IO) {
                correctionDao.getRecentCorrectionsSync()
            }

            _analysisStep.value = 2 // Running Vision Intelligence

            try {
                val result = withContext(Dispatchers.IO) {
                    visionAnalyzer.analyzeScreenshot(
                        bitmap = bitmap,
                        categories = categories,
                        userCorrections = corrections,
                        apiKeyOverride = _userApiKey.value
                    )
                }

                _analysisStep.value = 3 // Finalizing suggestions
                delay(100)
                _analysisStep.value = 4 // Done

                _currentExtracted.value = result.copy(originalScreenshotUri = screenshotUri)
                _isAnalyzing.value = false
                _analysisFailed.value = false
                onNavigateToReview()
            } catch (e: Exception) {
                e.printStackTrace()
                // Preserve screenshot evidence even if AI fails!
                _currentExtracted.value = ExtractedMemory(
                    title = "Captured Screenshot",
                    description = "Screen captured on ${LocalDate.now()}",
                    type = MemoryType.NOTE,
                    suggestedCategoryId = allCategories.value.firstOrNull()?.id ?: "personal",
                    categoryName = allCategories.value.firstOrNull()?.name ?: "Personal",
                    date = LocalDate.now().toString(),
                    time = null,
                    isDeadline = false,
                    people = emptyList(),
                    organization = null,
                    summary = "Screenshot evidence captured",
                    confidence = 0.5f,
                    originalScreenshotUri = screenshotUri
                )
                _isAnalyzing.value = false
                _analysisFailed.value = true
            }
        }
    }

    fun retryAnalysis(onNavigateToReview: () -> Unit) {
        val current = _currentExtracted.value ?: return
        val uri = current.originalScreenshotUri ?: return
        val file = File(uri)
        if (!file.exists()) return
        val bitmap = BitmapFactory.decodeFile(uri) ?: return
        startCaptureAndAnalysis(
            providedBitmap = bitmap,
            existingScreenshotPath = uri,
            onNavigateToProcessing = {},
            onNavigateToReview = onNavigateToReview
        )
    }

    fun saveScreenshotOnly(onNavigateToReview: () -> Unit) {
        _analysisFailed.value = false
        _isAnalyzing.value = false
        onNavigateToReview()
    }

    fun saveMemory(
        title: String,
        categoryId: String,
        type: MemoryType,
        date: String?,
        time: String?,
        notes: String?,
        isDeadline: Boolean,
        onSaved: () -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _currentExtracted.value

            // Feedback loop: Record human correction if user changed suggested category
            val suggestedId = current?.suggestedCategoryId
            if (!suggestedId.isNullOrBlank() && suggestedId != categoryId) {
                val correctedCat = allCategories.value.find { it.id == categoryId }
                val correction = CategoryCorrection(
                    textSnippet = title.ifBlank { current.title },
                    suggestedCategory = current.categoryName,
                    correctedCategoryId = categoryId,
                    correctedCategoryName = correctedCat?.name ?: categoryId
                )
                correctionDao.insertCorrection(correction)
            }

            val newItem = MemoryItem(
                title = title.ifBlank { current?.title ?: "Captured Memory" },
                description = current?.description ?: "",
                type = type,
                categoryId = categoryId,
                date = date ?: current?.date,
                time = time ?: current?.time,
                isDeadline = isDeadline,
                reminderTime = if (isDeadline) "1 day before" else "1 hour before",
                people = current?.people?.joinToString(", "),
                organization = current?.organization,
                source = "Screenshot",
                sourceApp = "Screen Capture",
                originalScreenshotUri = current?.originalScreenshotUri ?: "",
                aiSummary = current?.summary ?: current?.description,
                aiConfidence = current?.confidence ?: 0.94f,
                notes = notes
            )
            val generatedId = memoryRepo.insertMemory(newItem)
            val savedWithId = newItem.copy(id = generatedId)
            _lastSavedItem.value = savedWithId

            // Trigger notification confirmation
            reminderManager.showReminderNotification(savedWithId)

            viewModelScope.launch(Dispatchers.Main) {
                onSaved()
            }
        }
    }

    fun toggleComplete(item: MemoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.updateMemory(item.copy(isCompleted = !item.isCompleted, updatedAt = System.currentTimeMillis()))
            if (_selectedMemory.value?.id == item.id) {
                _selectedMemory.value = item.copy(isCompleted = !item.isCompleted)
            }
        }
    }

    fun updateNotes(id: Long, newNotes: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = allMemories.value.find { it.id == id } ?: return@launch
            val updated = item.copy(notes = newNotes, updatedAt = System.currentTimeMillis())
            memoryRepo.updateMemory(updated)
            _selectedMemory.value = updated
        }
    }

    fun deleteMemory(item: MemoryItem) {
        viewModelScope.launch(Dispatchers.IO) {
            item.originalScreenshotUri?.let { uri ->
                try {
                    val file = File(uri)
                    if (file.exists() && file.parentFile?.name == "memories") {
                        file.delete()
                    }
                } catch (_: Exception) {}
            }

            memoryRepo.deleteMemory(item)
            if (_selectedMemory.value?.id == item.id) {
                _selectedMemory.value = null
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            memoryRepo.deleteAll()
            categoryRepo.insertCategories(Category.DEFAULT_CATEGORIES)
            _selectedMemory.value = null
        }
    }

    fun addCategory(name: String, icon: String, colorHex: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = name.lowercase().replace(" ", "_").filter { it.isLetterOrDigit() || it == '_' }
            val newCategory = Category(
                id = id.ifBlank { "custom_${System.currentTimeMillis()}" },
                name = name,
                icon = icon.ifBlank { "tag" },
                colorHex = colorHex.ifBlank { "#FFE885" },
                displayOrder = allCategories.value.size
            )
            categoryRepo.insertCategory(newCategory)
        }
    }

    fun addToCalendar(context: Context, item: MemoryItem) {
        try {
            val intent = CalendarManager.createCalendarIntent(item)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)

            viewModelScope.launch(Dispatchers.IO) {
                val updated = item.copy(calendarEventId = "gcal_${System.currentTimeMillis()}")
                memoryRepo.updateMemory(updated)
                _selectedMemory.value = updated
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun refreshFloatingServiceState() {
        _isFloatingServiceEnabled.value = FloatingCaptureService.isServiceRunning
    }

    fun toggleFloatingService(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
            return false
        }

        if (FloatingCaptureService.isServiceRunning) {
            FloatingCaptureService.stop(context)
            _isFloatingServiceEnabled.value = false
        } else {
            FloatingCaptureService.start(context)
            _isFloatingServiceEnabled.value = true
        }
        return true
    }

    private fun saveBitmapToPermanentStorage(bitmap: Bitmap): String {
        val context = getApplication<Application>()
        val memoriesDir = File(context.filesDir, "memories").apply {
            if (!exists()) mkdirs()
        }
        val filename = "memora_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}.jpg"
        val file = File(memoriesDir, filename)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
        }
        return file.absolutePath
    }
}
