package com.example.ui.components

import android.app.DatePickerDialog
import android.app.TimePickerDialog
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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import com.example.data.model.MemoryType
import com.example.ui.theme.BlackInk
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.GrayText
import com.example.ui.theme.PastelCoral
import com.example.ui.theme.Typography
import com.example.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ManualEntryDialog(
    categories: List<Category>,
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

    var title by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf(categories.firstOrNull()?.id ?: "personal") }
    var selectedType by remember { mutableStateOf(MemoryType.TASK) }
    var selectedDate by remember { mutableStateOf(todayFormatted) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var selectedReminder by remember(selectedType) {
        mutableStateOf(if (selectedType == MemoryType.DEADLINE) "1 day before" else "1 hour before")
    }
    var notes by remember { mutableStateOf("") }

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var showReminderDropdown by remember { mutableStateOf(false) }

    val currentCategory = categories.find { it.id == selectedCategoryId }
    val catColor = try {
        if (currentCategory != null) Color(android.graphics.Color.parseColor(currentCategory.colorHex)) else PastelCoral
    } catch (_: Exception) {
        PastelCoral
    }

    val reminderOptions = listOf("At time of event", "10 minutes before", "1 hour before", "1 day before", "None")

    if (showCategoryPicker) {
        CategoryPickerBottomSheet(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategorySelected = { cat ->
                selectedCategoryId = cat.id
            },
            onDismiss = { showCategoryPicker = false }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "New Commitment",
                style = Typography.titleLarge.copy(fontWeight = FontWeight.Black)
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What do you want to remember?") },
                    placeholder = { Text("e.g. Submit project proposal") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = BlackInk,
                        unfocusedIndicatorColor = GrayText,
                        focusedContainerColor = CreamBackground,
                        unfocusedContainerColor = CreamBackground
                    )
                )

                // Category row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Category", style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Box(
                        modifier = Modifier
                            .background(catColor, RoundedCornerShape(12.dp))
                            .border(1.25.dp, BlackInk, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCategoryPicker = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "${currentCategory?.name ?: "Personal"} >",
                            style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = BlackInk)
                        )
                    }
                }

                // Type row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Type", style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Box {
                        Box(
                            modifier = Modifier
                                .background(CreamBackground, RoundedCornerShape(12.dp))
                                .border(1.25.dp, BlackInk, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showTypeDropdown = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                MemoryTypeLineIcon(type = selectedType, size = 15.dp, modifier = Modifier.padding(end = 4.dp))
                                Text(
                                    text = selectedType.displayName,
                                    style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        DropdownMenu(
                            expanded = showTypeDropdown,
                            onDismissRequest = { showTypeDropdown = false }
                        ) {
                            MemoryType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.displayName) },
                                    onClick = {
                                        selectedType = type
                                        if (type == MemoryType.DEADLINE && selectedReminder == "1 hour before") {
                                            selectedReminder = "1 day before"
                                        }
                                        showTypeDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Date row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Date", style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Box(
                        modifier = Modifier
                            .background(CreamBackground, RoundedCornerShape(12.dp))
                            .border(1.25.dp, BlackInk, RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                val parsed = DateUtils.parseLocalDate(selectedDate) ?: LocalDate.now()
                                DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val picked = LocalDate.of(year, month + 1, dayOfMonth)
                                        selectedDate = picked.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
                                    },
                                    parsed.year,
                                    parsed.monthValue - 1,
                                    parsed.dayOfMonth
                                ).show()
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedDate,
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                // Time row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Time", style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(CreamBackground, RoundedCornerShape(12.dp))
                                .border(1.25.dp, BlackInk, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val amPm = if (hourOfDay >= 12) "PM" else "AM"
                                            val hour12 = if (hourOfDay % 12 == 0) 12 else hourOfDay % 12
                                            selectedTime = String.format(Locale.US, "%02d:%02d %s", hour12, minute, amPm)
                                        },
                                        12, 0, false
                                    ).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedTime ?: "Optional",
                                    style = Typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTime != null) BlackInk else GrayText
                                    )
                                )
                            }
                        }
                        if (selectedTime != null) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(CreamBackground, CircleShape)
                                    .border(1.dp, BlackInk, CircleShape)
                                    .clip(CircleShape)
                                    .clickable { selectedTime = null },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }

                // Reminder row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Reminder", style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                    Box {
                        Box(
                            modifier = Modifier
                                .background(CreamBackground, RoundedCornerShape(12.dp))
                                .border(1.25.dp, BlackInk, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showReminderDropdown = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedReminder,
                                    style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = showReminderDropdown,
                            onDismissRequest = { showReminderDropdown = false }
                        ) {
                            reminderOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt) },
                                    onClick = {
                                        selectedReminder = opt
                                        showReminderDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Additional context...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 2,
                    maxLines = 4,
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
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            title.trim(),
                            selectedCategoryId,
                            selectedType,
                            selectedDate.ifBlank { null },
                            selectedTime?.ifBlank { null },
                            notes.ifBlank { null },
                            selectedReminder
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text(
                    "Create",
                    fontWeight = FontWeight.Black,
                    color = if (title.isNotBlank()) BlackInk else GrayText
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = GrayText)
            }
        },
        containerColor = CreamBackground,
        shape = RoundedCornerShape(20.dp)
    )
}
