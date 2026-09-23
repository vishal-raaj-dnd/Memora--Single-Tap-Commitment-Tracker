package com.example.ui.screens

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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.Typography
import com.example.ui.theme.WarmWhite
import com.example.ui.viewmodel.MemoraViewModel

@Composable
fun ReviewSaveScreen(
    viewModel: MemoraViewModel,
    onBack: () -> Unit,
    onSavedSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val extracted by viewModel.currentExtracted.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    val todayFormatted = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy", java.util.Locale.US))

    var editableTitle by remember(extracted) {
        mutableStateOf(extracted?.title ?: "Captured Memory")
    }
    var selectedCategoryId by remember(extracted) {
        mutableStateOf(extracted?.suggestedCategoryId ?: categories.firstOrNull()?.id ?: "personal")
    }
    var selectedType by remember(extracted) {
        mutableStateOf(extracted?.type ?: MemoryType.TASK)
    }
    var editableDate by remember(extracted) {
        mutableStateOf(extracted?.date ?: todayFormatted)
    }
    var editableTime by remember(extracted) {
        mutableStateOf(extracted?.time)
    }
    var notesText by remember { mutableStateOf("") }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }

    val currentCategory = categories.find { it.id == selectedCategoryId }
    val catColor = try {
        if (currentCategory != null) Color(android.graphics.Color.parseColor(currentCategory.colorHex)) else PastelCoral
    } catch (e: Exception) {
        PastelCoral
    }

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
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
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
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Review & Save",
                    style = Typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Screenshot Preview Card
            NeoCard(
                backgroundColor = WarmWhite,
                cornerRadius = 18.dp,
                shadowOffset = 2.5.dp
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    ScreenshotImage(
                        screenshotUri = extracted?.originalScreenshotUri,
                        aspectRatio = 1.35f
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Information fields card
            NeoCard(
                backgroundColor = WarmWhite,
                cornerRadius = 20.dp,
                shadowOffset = 3.dp,
                testTag = "review_details_card"
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title input field
                    Column {
                        Text(
                            text = "Title",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GrayText)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = editableTitle,
                            onValueChange = { editableTitle = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("review_title_input"),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = CreamBackground,
                                unfocusedContainerColor = CreamBackground,
                                focusedIndicatorColor = BlackInk,
                                unfocusedIndicatorColor = BlackInk.copy(alpha = 0.5f)
                            )
                        )
                    }

                    // Category row with clickable pastel badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Box(
                            modifier = Modifier
                                .testTag("review_category_chip")
                                .background(catColor, RoundedCornerShape(14.dp))
                                .border(1.5.dp, BlackInk, RoundedCornerShape(14.dp))
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { showCategoryPicker = true }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CategoryLineIcon(
                                    iconKey = currentCategory?.icon,
                                    size = 16.dp,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = currentCategory?.name ?: "Hackathon",
                                    style = Typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = BlackInk
                                    )
                                )
                                Text(
                                    text = " >",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BlackInk
                                )
                            }
                        }
                    }

                    // Type Row with dropdown selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Type",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Box {
                            Box(
                                modifier = Modifier
                                    .testTag("review_type_selector")
                                    .background(CreamBackground, RoundedCornerShape(12.dp))
                                    .border(1.5.dp, BlackInk, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showTypeDropdown = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    MemoryTypeLineIcon(
                                        type = selectedType,
                                        size = 15.dp,
                                        modifier = Modifier.padding(end = 6.dp)
                                    )
                                    Text(
                                        text = selectedType.displayName,
                                        style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand",
                                        tint = BlackInk,
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
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                MemoryTypeLineIcon(type = type, size = 16.dp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(type.displayName)
                                            }
                                        },
                                        onClick = {
                                            selectedType = type
                                            showTypeDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Date Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = editableDate,
                            style = Typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BlackInk
                            )
                        )
                    }

                    // Time Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Time",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = editableTime ?: "Not specified",
                            style = Typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (editableTime != null) BlackInk else GrayText
                            )
                        )
                    }

                    // Ambiguity Clarification Box (if present)
                    if (extracted?.hasAmbiguity == true && !extracted?.ambiguityQuestion.isNullOrBlank() && extracted?.ambiguityOptions?.isNotEmpty() == true) {
                        NeoCard(
                            backgroundColor = PastelCoral.copy(alpha = 0.35f),
                            cornerRadius = 14.dp,
                            shadowOffset = 2.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = extracted?.ambiguityQuestion ?: "Clarification needed",
                                    style = Typography.labelMedium.copy(fontWeight = FontWeight.Black, color = BlackInk)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    extracted?.ambiguityOptions?.forEach { option ->
                                        val isSelected = (option == editableTime || option == editableDate)
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (isSelected) MainYellow else WarmWhite,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .border(
                                                    if (isSelected) 2.dp else 1.25.dp,
                                                    BlackInk,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    if (extracted?.ambiguityField == "time" || option.contains("AM") || option.contains("PM")) {
                                                        editableTime = option
                                                    } else {
                                                        editableDate = option
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = option,
                                                style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BlackInk)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Confidence Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Confidence",
                            style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                progress = { extracted?.confidence ?: 0.94f },
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.5.dp,
                                color = SuccessGreen,
                                trackColor = CreamBackground
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${((extracted?.confidence ?: 0.94f) * 100).toInt()}%",
                                style = Typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    color = SuccessGreen
                                )
                            )
                        }
                    }

                    // Optional Notes Field
                    Column {
                        Text(
                            text = "Notes (Optional)",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GrayText)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            placeholder = { Text("Add any personal thoughts or reminders...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("review_notes_input"),
                            shape = RoundedCornerShape(14.dp),
                            minLines = 2,
                            maxLines = 4,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = CreamBackground,
                                unfocusedContainerColor = CreamBackground,
                                focusedIndicatorColor = BlackInk,
                                unfocusedIndicatorColor = BlackInk.copy(alpha = 0.5f)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            NeoButton(
                text = "Save Memory",
                onClick = {
                    viewModel.saveMemory(
                        title = editableTitle,
                        categoryId = selectedCategoryId,
                        type = selectedType,
                        date = editableDate,
                        time = editableTime,
                        notes = notesText.ifBlank { null },
                        isDeadline = selectedType == MemoryType.DEADLINE,
                        onSaved = onSavedSuccess
                    )
                },
                backgroundColor = MainYellow,
                testTag = "save_memory_button"
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
