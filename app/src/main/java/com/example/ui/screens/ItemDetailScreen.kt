package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MemoryItem
import com.example.ui.components.CategoryLineIcon
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
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
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
    } catch (e: Exception) {
        PastelCoral
    }

    var showMenu by remember { mutableStateOf(false) }
    var notesText by remember(item.notes) { mutableStateOf(item.notes ?: "") }
    var isEditingNotes by remember { mutableStateOf(false) }

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
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
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
                            text = { Text("Delete Memory") },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                viewModel.deleteMemory(item)
                                showMenu = false
                                onBack()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Original Screenshot Card
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

            // Action Buttons: Add to Calendar and Mark Done
            NeoButton(
                text = if (item.calendarEventId != null) "Added to Google Calendar" else "Add to Google Calendar",
                onClick = {
                    viewModel.addToCalendar(context, item)
                    Toast.makeText(context, "Opening Google Calendar...", Toast.LENGTH_SHORT).show()
                },
                backgroundColor = MainYellow,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Calendar",
                        tint = BlackInk,
                        modifier = Modifier.size(18.dp)
                    )
                },
                testTag = "detail_add_to_calendar_button"
            )

            Spacer(modifier = Modifier.height(12.dp))

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
