package com.example.ui.components

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ai.ExtractedMemory
import com.example.data.model.Category
import com.example.data.model.MemoryType
import com.example.ui.theme.BlackInk
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.GrayText
import com.example.ui.theme.MainYellow
import com.example.ui.theme.PastelCoral
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelYellow
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.Typography
import com.example.ui.theme.WarmWhite
import com.example.util.AudioRecorderHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * State-of-the-Art Voice Commitment Capture Dialog.
 * Direct in-app audio recording powered by Sarvam AI's Saaras Speech-to-Text API,
 * with multi-engine fallback (Groq, Android System STT, Smart Local).
 */
@Composable
fun VoiceCaptureDialog(
    categories: List<Category>,
    onDismiss: () -> Unit,
    onParseVoice: suspend (spokenText: String, engine: String?) -> ExtractedMemory,
    onTranscribeAudio: (suspend (audioFile: File, engine: String?) -> ExtractedMemory)? = null,
    hasSarvamKey: Boolean = true,
    onSaveCommitment: (
        title: String,
        categoryId: String,
        type: MemoryType,
        date: String?,
        time: String?,
        notes: String?,
        isDeadline: Boolean,
        reminderTime: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioRecorder = remember { AudioRecorderHelper(context) }

    var spokenTranscript by remember { mutableStateOf("") }
    var selectedEngine by remember { mutableStateOf(if (hasSarvamKey) "SARVAM" else "AUTO") }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var isTranscribingAudio by remember { mutableStateOf(false) }
    var isSystemListening by remember { mutableStateOf(false) }
    var isParsing by remember { mutableStateOf(false) }
    var parsedMemory by remember { mutableStateOf<ExtractedMemory?>(null) }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Editable parsed fields
    var title by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "personal") }
    var selectedType by remember { mutableStateOf(MemoryType.TASK) }
    var selectedDate by remember { mutableStateOf<String?>(null) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var selectedNotes by remember { mutableStateOf("") }
    var showCategoryPicker by remember { mutableStateOf(false) }

    fun applyExtracted(extracted: ExtractedMemory, originalText: String) {
        parsedMemory = extracted
        spokenTranscript = if (extracted.description.isNotBlank()) extracted.description else originalText
        title = extracted.title
        selectedCategoryId = extracted.suggestedCategoryId
        selectedType = extracted.type
        selectedDate = extracted.date
        selectedTime = extracted.time
        selectedNotes = spokenTranscript
    }

    fun triggerTextParse(text: String, engine: String) {
        val cleanText = text.trim()
        if (cleanText.isBlank()) return
        spokenTranscript = cleanText
        isParsing = true
        scope.launch {
            try {
                val chosenEngine = if (engine == "AUTO") null else engine
                val extracted = onParseVoice(cleanText, chosenEngine)
                applyExtracted(extracted, cleanText)
            } catch (e: Exception) {
                Log.e("VoiceCaptureDialog", "Parse error: ${e.message}", e)
                title = cleanText
                selectedNotes = cleanText
            } finally {
                isParsing = false
            }
        }
    }

    // Direct Audio Recording Handlers for Sarvam STT
    fun stopAudioRecordingAndTranscribe() {
        val audioFile = audioRecorder.stopRecording()
        isRecordingAudio = false
        if (audioFile != null && audioFile.exists() && audioFile.length() > 0L) {
            isTranscribingAudio = true
            scope.launch {
                try {
                    val extracted = onTranscribeAudio?.invoke(audioFile, selectedEngine)
                    if (extracted != null && extracted.title.isNotBlank()) {
                        applyExtracted(extracted, extracted.description)
                    } else {
                        Toast.makeText(context, extracted?.description ?: "No speech recognized. Please speak clearly.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("VoiceCaptureDialog", "Sarvam transcription failed: ${e.message}", e)
                    Toast.makeText(context, "Sarvam STT failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    isTranscribingAudio = false
                    try { audioFile.delete() } catch (_: Exception) {}
                }
            }
        } else {
            Toast.makeText(context, "No audio recorded. Please hold and speak.", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            val file = audioRecorder.startRecording()
            if (file != null) {
                isRecordingAudio = true
                recordingSeconds = 0
            }
        } else {
            Toast.makeText(context, "Microphone permission is required for voice capture.", Toast.LENGTH_SHORT).show()
        }
    }

    fun startAudioRecording() {
        if (!hasAudioPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }
        val file = audioRecorder.startRecording()
        if (file != null) {
            isRecordingAudio = true
            recordingSeconds = 0
        } else {
            Toast.makeText(context, "Could not access microphone.", Toast.LENGTH_SHORT).show()
        }
    }

    // Recording duration timer
    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            while (isActive) {
                delay(1000)
                recordingSeconds++
                if (recordingSeconds >= 30) {
                    stopAudioRecordingAndTranscribe()
                    break
                }
            }
        }
    }

    // Fallback System Speech Recognizer launcher
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isSystemListening = false
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognizedText = spokenMatches?.firstOrNull()?.trim() ?: ""
            if (recognizedText.isNotBlank()) {
                triggerTextParse(recognizedText, selectedEngine)
            }
        }
    }

    fun startSystemSpeechRecognition() {
        try {
            isSystemListening = true
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-IN")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("en-IN", "hi-IN", "ta-IN", "te-IN"))
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your commitment...")
            }
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            isSystemListening = false
            Toast.makeText(context, "System speech recognizer not available.", Toast.LENGTH_SHORT).show()
        }
    }

    // Clean up recorder on dismiss
    DisposableEffect(Unit) {
        onDispose {
            if (audioRecorder.isCurrentlyRecording()) {
                audioRecorder.stopRecording()
            }
        }
    }

    // Pulse animation for recording
    val infiniteTransition = rememberInfiniteTransition(label = "voicePulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    if (showCategoryPicker) {
        CategoryPickerBottomSheet(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = { cat -> selectedCategoryId = cat.id },
            onDismiss = { showCategoryPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = {
            if (isRecordingAudio) audioRecorder.stopRecording()
            onDismiss()
        },
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MainYellow, CircleShape)
                            .border(1.5.dp, BlackInk, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = null,
                            tint = BlackInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Voice Commitment",
                            style = Typography.titleLarge.copy(fontWeight = FontWeight.Black, fontSize = 20.sp)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(if (hasSarvamKey) SuccessGreen else GrayText, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (hasSarvamKey) "Sarvam Saaras STT Ready" else "Standard Voice Mode",
                                style = Typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    color = if (hasSarvamKey) SuccessGreen else GrayText,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        if (isRecordingAudio) audioRecorder.stopRecording()
                        onDismiss()
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = GrayText)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Engine Selector Tabs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "SARVAM" to "🇮🇳 Sarvam STT",
                        "GROQ" to "⚡ Groq",
                        "AUTO" to "Auto AI",
                        "LOCAL" to "🛡️ Local"
                    ).forEach { (engineKey, label) ->
                        val isChosen = selectedEngine == engineKey
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .background(if (isChosen) MainYellow else WarmWhite, RoundedCornerShape(8.dp))
                                .border(1.dp, BlackInk, RoundedCornerShape(8.dp))
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedEngine = engineKey
                                    if (spokenTranscript.isNotBlank() && !isParsing && !isTranscribingAudio) {
                                        triggerTextParse(spokenTranscript, engineKey)
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = label,
                                style = Typography.labelSmall.copy(
                                    fontWeight = if (isChosen) FontWeight.Black else FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = BlackInk
                                )
                            )
                        }
                    }
                }

                // Primary Record / Stop Hero Button
                val isBusy = isTranscribingAudio || isParsing
                Box(
                    modifier = Modifier
                        .scale(if (isRecordingAudio) pulseScale else 1f)
                        .size(76.dp)
                        .background(
                            when {
                                isRecordingAudio -> Color(0xFFFF4D4D)
                                isBusy -> WarmWhite
                                else -> MainYellow
                            },
                            CircleShape
                        )
                        .border(2.5.dp, BlackInk, CircleShape)
                        .clip(CircleShape)
                        .clickable(enabled = !isBusy) {
                            if (isRecordingAudio) {
                                stopAudioRecordingAndTranscribe()
                            } else {
                                startAudioRecording()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isBusy -> {
                            CircularProgressIndicator(
                                color = BlackInk,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        isRecordingAudio -> {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = "Stop Recording",
                                tint = Color.White,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Start Speaking",
                                tint = BlackInk,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }

                // Status message & Timer
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = when {
                            isTranscribingAudio -> "Transcribing with Sarvam Saaras AI..."
                            isParsing -> "Structuring commitment details..."
                            isRecordingAudio -> "Listening... 🔴 00:${if (recordingSeconds < 10) "0$recordingSeconds" else "$recordingSeconds"}"
                            spokenTranscript.isNotBlank() -> "Tap mic to record again"
                            else -> "Tap microphone & speak naturally"
                        },
                        style = Typography.bodyMedium.copy(
                            fontWeight = if (isRecordingAudio) FontWeight.Black else FontWeight.Bold,
                            color = if (isRecordingAudio) Color(0xFFD32F2F) else BlackInk,
                            fontSize = 14.sp
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = when {
                            isRecordingAudio -> "Supports English, Hindi, Tamil, Telugu, Hinglish (Tap stop when done)"
                            isTranscribingAudio -> "Analyzing audio directly on Sarvam Cloud..."
                            else -> "Powered by Sarvam Saaras Indian STT"
                        },
                        style = Typography.bodySmall.copy(color = GrayText, fontSize = 11.sp),
                        textAlign = TextAlign.Center
                    )
                }

                // Engine Used badge
                parsedMemory?.engineUsed?.let { engine ->
                    Box(
                        modifier = Modifier
                            .background(PastelMint, RoundedCornerShape(8.dp))
                            .border(1.dp, BlackInk.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "✨ $engine",
                            style = Typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = BlackInk
                            )
                        )
                    }
                }

                // If spoken transcript is available, show parsed details
                if (spokenTranscript.isNotBlank() && !isBusy) {
                    // Spoken transcript quote
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(WarmWhite, RoundedCornerShape(12.dp))
                            .border(1.5.dp, BlackInk, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = BlackInk,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Sarvam Saaras Transcript",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GrayText)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "\"$spokenTranscript\"",
                                style = Typography.bodyMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = BlackInk,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            )
                        }
                    }

                    // Editable Title
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Commitment Title") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = BlackInk,
                            unfocusedIndicatorColor = GrayText,
                            focusedContainerColor = CreamBackground,
                            unfocusedContainerColor = CreamBackground
                        )
                    )

                    // Category Pill Selector
                    val curCategory = categories.find { it.id == selectedCategoryId }
                    val catColor = try {
                        if (curCategory != null) Color(android.graphics.Color.parseColor(curCategory.colorHex)) else PastelCoral
                    } catch (_: Exception) {
                        PastelCoral
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCategoryPicker = true }
                            .background(WarmWhite, RoundedCornerShape(12.dp))
                            .border(1.5.dp, BlackInk, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(catColor, CircleShape)
                                    .border(1.dp, BlackInk, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Category: ${curCategory?.name ?: "Select"}",
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = BlackInk)
                            )
                        }
                        Text(
                            text = "Change",
                            style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = GrayText)
                        )
                    }

                    // Date & Time Interactive Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Date Chip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(WarmWhite, RoundedCornerShape(12.dp))
                                .border(1.5.dp, BlackInk, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val now = LocalDate.now()
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val chosen = LocalDate.of(y, m + 1, d)
                                            selectedDate = chosen.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
                                        },
                                        now.year,
                                        now.monthValue - 1,
                                        now.dayOfMonth
                                    ).show()
                                }
                                .padding(vertical = 10.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedDate ?: "Set Date",
                                    style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                )
                            }
                        }

                        // Time Chip
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(WarmWhite, RoundedCornerShape(12.dp))
                                .border(1.5.dp, BlackInk, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val amPm = if (hourOfDay >= 12) "PM" else "AM"
                                            val hour12 = when {
                                                hourOfDay == 0 -> 12
                                                hourOfDay > 12 -> hourOfDay - 12
                                                else -> hourOfDay
                                            }
                                            selectedTime = String.format(Locale.US, "%d:%02d %s", hour12, minute, amPm)
                                        },
                                        12, 0, false
                                    ).show()
                                }
                                .padding(vertical = 10.dp, horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedTime ?: "Set Time",
                                    style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                )
                            }
                        }
                    }

                    // Commitment Type Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(MemoryType.TASK, MemoryType.DEADLINE, MemoryType.EVENT).forEach { t ->
                            val isSelected = selectedType == t
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(if (isSelected) MainYellow else WarmWhite, RoundedCornerShape(10.dp))
                                    .border(1.5.dp, BlackInk, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedType = t }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = t.name,
                                    style = Typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Fallback option: Google Speech or manual typing
                if (!isRecordingAudio && !isBusy) {
                    TextButton(
                        onClick = { startSystemSpeechRecognition() },
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Text(
                            text = "Or use Android Speech Recognizer",
                            style = Typography.labelSmall.copy(color = GrayText, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (spokenTranscript.isNotBlank() && !isTranscribingAudio && !isParsing) {
                NeoButton(
                    text = "Save Commitment",
                    onClick = {
                        val finalTitle = title.trim().ifBlank { spokenTranscript.trim() }
                        onSaveCommitment(
                            finalTitle,
                            selectedCategoryId,
                            selectedType,
                            selectedDate,
                            selectedTime,
                            selectedNotes,
                            selectedType == MemoryType.DEADLINE,
                            if (selectedType == MemoryType.DEADLINE) "1 day before" else "1 hour before"
                        )
                        onDismiss()
                    },
                    backgroundColor = MainYellow
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    if (isRecordingAudio) audioRecorder.stopRecording()
                    onDismiss()
                }
            ) {
                Text("Cancel", color = GrayText, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = CreamBackground,
        shape = RoundedCornerShape(24.dp)
    )
}
