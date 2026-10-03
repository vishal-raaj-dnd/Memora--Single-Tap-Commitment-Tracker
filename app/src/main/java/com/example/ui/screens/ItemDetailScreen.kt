package com.example.ui.screens

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemoryItem
import com.example.data.model.MemoryType
import com.example.ui.components.CategoryLineIcon
import com.example.ui.components.CategoryPickerBottomSheet
import com.example.ui.components.MemoryTypeLineIcon
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoDottedBackground
import com.example.ui.components.ScreenshotImage
import com.example.ui.theme.BlackInk
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.GrayText
import com.example.ui.theme.MainYellow
import com.example.ui.theme.PastelCoral
import com.example.ui.theme.PastelYellow
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.Typography
import com.example.ui.theme.WarmWhite
import com.example.ui.viewmodel.MemoraViewModel
import com.example.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun ItemDetailScreen(
    itemId: Long,
    viewModel: MemoraViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val memories by viewModel.allMemories.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val item = memories.find { it.id == itemId } ?: viewModel.selectedMemory.collectAsState().value

    if (item == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Memory not found")
            Spacer(modifier = Modifier.height(12.dp))
            NeoButton(text = "Go Back", onClick = onBack)
        }
        return
    }

    val category = categories.find { it.id == item.categoryId }
    val catColor = try {
        if (category != null) Color(android.graphics.Color.parseColor(category.colorHex)) else PastelCoral
    } catch (_: Exception) {
        PastelCoral
    }

    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var notesText by remember(item.notes) { mutableStateOf(item.notes ?: "") }
    var isEditingNotes by remember { mutableStateOf(false) }

    // Edit commitment dialog states
    var editTitle by remember(item) { mutableStateOf(item.title) }
    var editCategoryId by remember(item) { mutableStateOf(item.categoryId) }
    var editType by remember(item) { mutableStateOf(item.type) }
    var editDate by remember(item) { mutableStateOf(item.date ?: "") }
    var editTime by remember(item) { mutableStateOf(item.time ?: "") }
    var editReminder by remember(item) { mutableStateOf(item.reminderTime ?: if (item.isDeadline) "1 day before" else "1 hour before") }
    var editNotes by remember(item) { mutableStateOf(item.notes ?: "") }
    var showEditCategoryPicker by remember { mutableStateOf(false) }
    var showEditTypeDropdown by remember { mutableStateOf(false) }
    var showEditReminderDropdown by remember { mutableStateOf(false) }

    val reminderOptions = listOf("At time of event", "10 minutes before", "1 hour before", "1 day before", "None")

    fun shareCommitment() {
        val shareText = buildString {
            appendLine("Memora Commitment:")
            appendLine("• ${item.title}")
            appendLine("• Category: ${category?.name ?: "General"}")
            if (!item.date.isNullOrBlank()) appendLine("• Date: ${item.date}")
            if (!item.time.isNullOrBlank()) appendLine("• Time: ${item.time}")
            if (!item.notes.isNullOrBlank()) appendLine("• Notes: ${item.notes}")
            appendLine("\nCaptured with Memora - Single-Tap Commitment Tracker")
        }
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Commitment: ${item.title}")
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share Commitment"))
    }

    // Category picker bottom sheet in edit dialog
    if (showEditCategoryPicker) {
        CategoryPickerBottomSheet(
            categories = categories,
            selectedCategoryId = editCategoryId,
            onCategorySelected = { cat -> editCategoryId = cat.id },
            onDismiss = { showEditCategoryPicker = false }
        )
    }

    // Edit Dialog
    if (showEditDialog) {
        val editCategoryObj = categories.find { it.id == editCategoryId }
        val editCatColor = try {
            if (editCategoryObj != null) Color(android.graphics.Color.parseColor(editCategoryObj.colorHex)) else PastelCoral
        } catch (_: Exception) {
            PastelCoral
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Edit Commitment",
                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Title field
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Title") },
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
                                .background(editCatColor, RoundedCornerShape(12.dp))
                                .border(1.25.dp, BlackInk, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { showEditCategoryPicker = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${editCategoryObj?.name ?: "General"} >",
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
                                    .clickable { showEditTypeDropdown = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = editType.displayName,
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
                                expanded = showEditTypeDropdown,
                                onDismissRequest = { showEditTypeDropdown = false }
                            ) {
                                MemoryType.entries.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.displayName) },
                                        onClick = {
                                            editType = type
                                            showEditTypeDropdown = false
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
                                    val parsed = DateUtils.parseLocalDate(editDate) ?: LocalDate.now()
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val picked = LocalDate.of(year, month + 1, dayOfMonth)
                                            editDate = picked.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
                                        },
                                        parsed.year,
                                        parsed.monthValue - 1,
                                        parsed.dayOfMonth
                                    ).show()
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (editDate.isNotBlank()) editDate else "Select Date",
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
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
                                                editTime = String.format(Locale.US, "%02d:%02d %s", hour12, minute, amPm)
                                            },
                                            12, 0, false
                                        ).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (editTime.isNotBlank()) editTime else "Select Time",
                                    style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            if (editTime.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(CreamBackground, CircleShape)
                                        .border(1.dp, BlackInk, CircleShape)
                                        .clip(CircleShape)
                                        .clickable { editTime = "" },
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
                                    .clickable { showEditReminderDropdown = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = editReminder,
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
                                expanded = showEditReminderDropdown,
                                onDismissRequest = { showEditReminderDropdown = false }
                            ) {
                                reminderOptions.forEach { opt ->
                                    DropdownMenuItem(
                                        text = { Text(opt) },
                                        onClick = {
                                            editReminder = opt
                                            showEditReminderDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Notes field
                    OutlinedTextField(
                        value = editNotes,
                        onValueChange = { editNotes = it },
                        label = { Text("Notes") },
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
                        viewModel.updateMemoryDetails(
                            id = item.id,
                            title = editTitle.trim().ifBlank { item.title },
                            categoryId = editCategoryId,
                            type = editType,
                            date = editDate.ifBlank { null },
                            time = editTime.ifBlank { null },
                            notes = editNotes.ifBlank { null },
                            reminderTime = editReminder
                        )
                        showEditDialog = false
                    }
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold, color = BlackInk)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = GrayText)
                }
            },
            containerColor = CreamBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = {
                Text(
                    text = "Delete Memory?",
                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${item.title}'? This action cannot be undone.",
                    style = Typography.bodyMedium.copy(color = BlackInk)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMemory(item)
                        showDeleteConfirm = false
                        onBack()
                    }
                ) {
                    Text("Delete", fontWeight = FontWeight.Black, color = PastelCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = GrayText)
                }
            },
            containerColor = CreamBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }

    NeoDottedBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(WarmWhite, CircleShape)
                        .border(1.75.dp, BlackInk, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BlackInk
                        )
                    }
                }

                Text(
                    text = item.title,
                    style = Typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp),
                    maxLines = 1
                )

                Box {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(WarmWhite, CircleShape)
                            .border(1.75.dp, BlackInk, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu",
                                tint = BlackInk
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Commitment") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                showEditDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Share Commitment") },
                            leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                shareCommitment()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Memory", color = PastelCoral) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = PastelCoral) },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Original Screenshot Card (Only if screenshot URI exists)
            if (!item.originalScreenshotUri.isNullOrBlank()) {
                NeoCard(
                    backgroundColor = WarmWhite,
                    cornerRadius = 18.dp,
                    shadowOffset = 3.dp,
                    testTag = "detail_screenshot_card"
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        ScreenshotImage(
                            screenshotUri = item.originalScreenshotUri,
                            aspectRatio = 1.35f
                        )
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            // Details card
            NeoCard(
                backgroundColor = WarmWhite,
                cornerRadius = 20.dp,
                shadowOffset = 3.dp,
                testTag = "detail_metadata_card"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Title & Category badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            style = Typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = BlackInk
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .background(catColor, RoundedCornerShape(12.dp))
                                .border(1.5.dp, BlackInk, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryLineIcon(iconKey = category?.icon, size = 15.dp, modifier = Modifier.padding(end = 6.dp))
                                Text(
                                    text = category?.name ?: "General",
                                    style = Typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BlackInk
                                    )
                                )
                            }
                        }
                    }

                    // Metadata rows
                    DetailRow(label = "Date", value = item.date ?: "Not specified")
                    DetailRow(label = "Type", value = item.type.displayName)
                    if (!item.time.isNullOrBlank()) {
                        DetailRow(label = "Time", value = item.time)
                    }
                    DetailRow(label = "Reminder", value = item.reminderTime ?: "1 hour before")
                    DetailRow(label = "Source", value = "${item.source} (${item.sourceApp ?: "Screen"})")

                    if (!item.aiSummary.isNullOrBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PastelYellow.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                .border(1.5.dp, BlackInk, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "AI Summary",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Black)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.aiSummary,
                                style = Typography.bodyMedium.copy(color = BlackInk, fontSize = 13.sp)
                            )
                        }
                    }

                    // Notes Section
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Personal Notes",
                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GrayText)
                            )
                            Text(
                                text = if (isEditingNotes) "Done" else "Edit",
                                style = Typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = BlackInk
                                ),
                                modifier = Modifier.clickable {
                                    if (isEditingNotes) {
                                        viewModel.updateNotes(item.id, notesText)
                                        isEditingNotes = false
                                    } else {
                                        isEditingNotes = true
                                    }
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (isEditingNotes) {
                            OutlinedTextField(
                                value = notesText,
                                onValueChange = { notesText = it },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = CreamBackground,
                                    unfocusedContainerColor = CreamBackground
                                )
                            )
                        } else {
                            Text(
                                text = if (notesText.isNotBlank()) notesText else "No notes added yet.",
                                style = Typography.bodyMedium.copy(
                                    color = if (notesText.isNotBlank()) BlackInk else GrayText
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons: Mark Done

            NeoButton(
                text = if (item.isCompleted) "Mark as Pending" else "Mark as Completed",
                onClick = { viewModel.toggleComplete(item) },
                backgroundColor = WarmWhite,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Complete",
                        tint = if (item.isCompleted) SuccessGreen else BlackInk,
                        modifier = Modifier.size(18.dp)
                    )
                },
                testTag = "detail_toggle_complete_button"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = Typography.bodyMedium.copy(color = GrayText)
        )
        Text(
            text = value,
            style = Typography.bodyMedium.copy(
                fontWeight = FontWeight.Bold,
                color = BlackInk
            )
        )
    }
}
