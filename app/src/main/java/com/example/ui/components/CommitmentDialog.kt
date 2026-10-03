package com.example.ui.components

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.MemoryItem
import com.example.data.model.MemoryType
import com.example.ui.theme.BlackInk
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.GrayText
import com.example.ui.theme.MainYellow
import com.example.ui.theme.PastelCoral
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelYellow
import com.example.ui.theme.Typography
import com.example.ui.theme.WarmWhite
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Universal Neo-Brutalist modal for easily Adding or Editing a commitment.
 * Features inline voice dictation, tactile category picker, date/time chips, and reminder controls.
 */
@Composable
fun CommitmentDialog(
    categories: List<Category>,
    existingItem: MemoryItem? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        categoryId: String,
        type: MemoryType,
        date: String?,
        time: String?,
        notes: String?,
        reminderTime: String?
    ) -> Unit
) {
    val context = LocalContext.current
    val todayFormatted = LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))

    val isEditing = existingItem != null

    var title by remember { mutableStateOf(existingItem?.title ?: "") }
    var selectedCategoryId by remember {
        mutableStateOf(existingItem?.categoryId ?: categories.firstOrNull()?.id ?: "personal")
    }
    var selectedType by remember { mutableStateOf(existingItem?.type ?: MemoryType.TASK) }
    var selectedDate by remember { mutableStateOf(existingItem?.date ?: todayFormatted) }
    var selectedTime by remember { mutableStateOf(existingItem?.time) }
    var selectedReminder by remember(selectedType) {
        mutableStateOf(
            existingItem?.reminderTime ?: if (selectedType == MemoryType.DEADLINE) "1 day before" else "1 hour before"
        )
    }
    var notes by remember { mutableStateOf(existingItem?.notes ?: "") }

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showReminderDropdown by remember { mutableStateOf(false) }

    val currentCategory = categories.find { it.id == selectedCategoryId }
    val catColor = try {
        if (currentCategory != null) Color(android.graphics.Color.parseColor(currentCategory.colorHex)) else PastelCoral
    } catch (_: Exception) {
        PastelCoral
    }

    // Speech-to-text launcher for dictating into title
    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val spokenMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = spokenMatches?.firstOrNull()?.trim()
            if (!text.isNullOrBlank()) {
                title = if (title.isBlank()) text else "$title $text"
            }
        }
    }

    val reminderOptions = listOf("At time of event", "10 minutes before", "1 hour before", "1 day before", "None")

    if (showCategoryPicker) {
        CategoryPickerBottomSheet(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = { cat -> selectedCategoryId = cat.id },
            onDismiss = { showCategoryPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEditing) "Edit Commitment" else "New Commitment",
                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = GrayText)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title Field with Dictation Mic
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title *") },
                    placeholder = { Text("What do you need to do?") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                try {
                                    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                        putExtra(RecognizerIntent.EXTRA_PROMPT, "Dictate title...")
                                    }
                                    speechLauncher.launch(intent)
                                } catch (_: Exception) {}
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Dictate",
                                tint = BlackInk,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = BlackInk,
                        unfocusedIndicatorColor = GrayText,
                        focusedContainerColor = CreamBackground,
                        unfocusedContainerColor = CreamBackground
                    )
                )

                // Type Chips: Task, Deadline, Event, Note
                Text(
                    text = "TYPE",
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Black, color = GrayText)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(MemoryType.TASK, MemoryType.DEADLINE, MemoryType.EVENT, MemoryType.NOTE).forEach { t ->
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

                // Category Selector
                Text(
                    text = "CATEGORY",
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Black, color = GrayText)
                )
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
                            text = currentCategory?.name ?: "Personal",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold, color = BlackInk)
                        )
                    }
                    Text(
                        text = "Change",
                        style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = GrayText)
                    )
                }

                // Date & Time Row
                Text(
                    text = "DATE & TIME",
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Black, color = GrayText)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Date
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
                                text = selectedDate ?: "No Date",
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            )
                        }
                    }

                    // Time
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
                            if (selectedTime != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { selectedTime = null }
                                )
                            }
                        }
                    }
                }

                // Reminder Dropdown
                Text(
                    text = "REMINDER",
                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Black, color = GrayText)
                )
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showReminderDropdown = true }
                            .background(WarmWhite, RoundedCornerShape(12.dp))
                            .border(1.5.dp, BlackInk, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = selectedReminder,
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = showReminderDropdown,
                        onDismissRequest = { showReminderDropdown = false }
                    ) {
                        reminderOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedReminder = option
                                    showReminderDropdown = false
                                }
                            )
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    maxLines = 3,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = BlackInk,
                        unfocusedIndicatorColor = GrayText,
                        focusedContainerColor = CreamBackground,
                        unfocusedContainerColor = CreamBackground
                    )
                )
            }
        },
        confirmButton = {
            NeoButton(
                text = if (isEditing) "Save Changes" else "Create Commitment",
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            title.trim(),
                            selectedCategoryId,
                            selectedType,
                            selectedDate,
                            selectedTime,
                            notes.trim().ifBlank { null },
                            selectedReminder
                        )
                        onDismiss()
                    }
                },
                backgroundColor = MainYellow
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GrayText, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = CreamBackground,
        shape = RoundedCornerShape(24.dp)
    )
}
