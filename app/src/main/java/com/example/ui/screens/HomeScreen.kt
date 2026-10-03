package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.MemoryItem
import com.example.ui.components.CommitmentCard
import com.example.ui.components.CommitmentDialog
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoDottedBackground
import com.example.ui.components.ProfileCustomizerDialog
import com.example.ui.components.UserProfileAvatar
import com.example.ui.components.VoiceCaptureDialog
import com.example.ui.theme.BlackInk
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.GrayText
import com.example.ui.theme.LightGray
import com.example.ui.theme.MainYellow
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelCoral
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelYellow
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.Typography
import com.example.ui.theme.WarmWhite
import com.example.ui.viewmodel.MemoraViewModel
import com.example.util.DateUtils

/**
 * Restructured, lightning-fast Neo-Brutalist Home Screen.
 * Solves UX friction by providing:
 * 1. Prominent Voice Commitment Capture (🎙️ Speak & Add) with Sarvam & Groq AI
 * 2. Instant inline task creation & editing (✏️)
 * 3. 1-Tap Checkbox completion toggles (✅)
 * 4. Dedicated Today's Focus & Upcoming Horizon (zero completed clutter)
 * 5. Distinct Settings vs Profile customizer triggers
 * 6. Clean, responsive top action dock
 */
@Composable
fun HomeScreen(
    viewModel: MemoraViewModel,
    onNavigateToCapture: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFriends: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val memories by viewModel.allMemories.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val userAvatar by viewModel.userAvatar.collectAsState()
    val sarvamKey by viewModel.sarvamApiKey.collectAsState()

    // Dialog states
    var showVoiceDialog by remember { mutableStateOf(false) }
    var showCommitmentDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MemoryItem?>(null) }
    var deletingItem by remember { mutableStateOf<MemoryItem?>(null) }

    // Filter and search states - Default focused on Today!
    var selectedFilterTab by remember { mutableStateOf("Today") }
    var searchQuery by remember { mutableStateOf("") }
    var showCompletedDrawer by remember { mutableStateOf(false) }

    // Pure active commitments for home page (no completed clutter)
    val pendingMemories = memories.filter { !it.isCompleted }
    val completedMemories = memories.filter { it.isCompleted }
    val overdueMemories = memories.filter { !it.isCompleted && DateUtils.isOverdue(it.date, false) }
    val todayMemories = memories.filter { !it.isCompleted && !DateUtils.isOverdue(it.date, false) && DateUtils.isToday(it.date) }
    val upcomingMemories = memories.filter { !it.isCompleted && !DateUtils.isOverdue(it.date, false) && !DateUtils.isToday(it.date) }
    val overdueCount = overdueMemories.size

    val activeBaseList = when (selectedFilterTab) {
        "Today" -> todayMemories + overdueMemories
        "Upcoming" -> upcomingMemories
        else -> pendingMemories
    }

    val filteredList = activeBaseList.filter { item ->
        searchQuery.isBlank() ||
                item.title.contains(searchQuery, ignoreCase = true) ||
                (item.notes?.contains(searchQuery, ignoreCase = true) == true) ||
                (item.categoryId.contains(searchQuery, ignoreCase = true))
    }

    // Profile Customizer Dialog
    if (showProfileDialog) {
        ProfileCustomizerDialog(
            initialName = userName,
            initialRole = userRole,
            initialAvatar = userAvatar,
            onDismiss = { showProfileDialog = false },
            onSave = { name, role, avatarKey ->
                viewModel.updateProfile(name, role, avatarKey)
                showProfileDialog = false
            }
        )
    }

    // Voice Capture Dialog
    if (showVoiceDialog) {
        VoiceCaptureDialog(
            categories = categories,
            onDismiss = { showVoiceDialog = false },
            onParseVoice = { text, engine -> viewModel.parseVoiceInput(text, engine) },
            onTranscribeAudio = { file, engine -> viewModel.transcribeAndParseAudio(file, engine) },
            hasSarvamKey = sarvamKey.isNotBlank(),
            onSaveCommitment = { title, catId, type, date, time, notes, isDeadline, reminder ->
                viewModel.saveVoiceCommitment(
                    title = title,
                    categoryId = catId,
                    type = type,
                    date = date,
                    time = time,
                    notes = notes,
                    isDeadline = isDeadline,
                    reminderTime = reminder,
                    onSaved = { showVoiceDialog = false }
                )
            }
        )
    }

    // Add / Edit Commitment Dialog
    if (showCommitmentDialog) {
        CommitmentDialog(
            categories = categories,
            existingItem = editingItem,
            onDismiss = {
                showCommitmentDialog = false
                editingItem = null
            },
            onSave = { title, catId, type, date, time, notes, reminder ->
                val currentEdit = editingItem
                if (currentEdit != null) {
                    viewModel.updateMemoryDetails(
                        id = currentEdit.id,
                        title = title,
                        categoryId = catId,
                        type = type,
                        date = date,
                        time = time,
                        notes = notes,
                        reminderTime = reminder
                    )
                } else {
                    viewModel.createManualMemory(
                        title = title,
                        categoryId = catId,
                        type = type,
                        date = date,
                        time = time,
                        notes = notes,
                        reminderTime = reminder,
                        onSaved = {}
                    )
                }
                showCommitmentDialog = false
                editingItem = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (deletingItem != null) {
        val target = deletingItem!!
        AlertDialog(
            onDismissRequest = { deletingItem = null },
            title = {
                Text(
                    text = "Delete Commitment",
                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete \"${target.title}\"? This action cannot be undone.",
                    style = Typography.bodyMedium.copy(color = BlackInk)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteMemory(target)
                        deletingItem = null
                    }
                ) {
                    Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingItem = null }) {
                    Text("Cancel", color = GrayText)
                }
            },
            containerColor = CreamBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }

    NeoDottedBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header: Memora Branding + Greeting + Settings
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_memora_icon),
                            contentDescription = "Memora Logo",
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Column {
                            Text(
                                text = "Memora",
                                style = Typography.displayMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 24.sp,
                                    color = BlackInk
                                )
                            )
                            Text(
                                text = "Hi, $userName",
                                style = Typography.bodySmall.copy(
                                    color = GrayText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }

                    // Settings & Avatar actions
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(WarmWhite, CircleShape)
                                .border(1.5.dp, BlackInk, CircleShape)
                                .clip(CircleShape)
                                .clickable { onNavigateToSettings() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = BlackInk,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .testTag("home_avatar_button")
                                .clickable { showProfileDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            UserProfileAvatar(avatarKey = userAvatar, size = 40.dp)
                        }
                    }
                }
            }

            // Top Quick Action Hub (Front & Center!)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Voice Capture Hero Button
                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .background(MainYellow, RoundedCornerShape(16.dp))
                            .border(2.dp, BlackInk, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showVoiceDialog = true }
                            .padding(vertical = 12.dp, horizontal = 12.dp)
                            .testTag("home_voice_action_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice",
                                tint = BlackInk,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Voice Add",
                                style = Typography.labelMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 14.sp,
                                    color = BlackInk
                                )
                            )
                        }
                    }

                    // 2. Quick Task Add Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(WarmWhite, RoundedCornerShape(16.dp))
                            .border(2.dp, BlackInk, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                editingItem = null
                                showCommitmentDialog = true
                            }
                            .padding(vertical = 12.dp, horizontal = 10.dp)
                            .testTag("home_add_task_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = BlackInk,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Task",
                                style = Typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BlackInk
                                )
                            )
                        }
                    }

                    // 3. Screen Capture Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(PastelYellow, RoundedCornerShape(16.dp))
                            .border(2.dp, BlackInk, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onNavigateToCapture() }
                            .padding(vertical = 12.dp, horizontal = 10.dp)
                            .testTag("home_capture_nav_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Screen",
                                tint = BlackInk,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Screen",
                                style = Typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = BlackInk
                                )
                            )
                        }
                    }
                }
            }

            // Compact Live Metrics Strip
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(WarmWhite, RoundedCornerShape(14.dp))
                        .border(1.5.dp, BlackInk, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📌 ${pendingMemories.size} Pending",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BlackInk)
                    )
                    Box(modifier = Modifier.size(4.dp).background(GrayText, CircleShape))
                    Text(
                        text = "✅ ${completedMemories.size} Done",
                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = SuccessGreen)
                    )
                    if (overdueCount > 0) {
                        Box(modifier = Modifier.size(4.dp).background(GrayText, CircleShape))
                        Text(
                            text = "⏰ $overdueCount Overdue",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Black, color = Color.Red)
                        )
                    }
                }
            }

            // Search Bar (Compact & Sleek)
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search commitments...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedIndicatorColor = BlackInk,
                        unfocusedIndicatorColor = GrayText.copy(alpha = 0.5f),
                        focusedContainerColor = WarmWhite,
                        unfocusedContainerColor = WarmWhite
                    )
                )
            }

            // Interactive Focused Tabs: Today, Upcoming, All Active (zero completed clutter)
            item {
                val tabs = listOf(
                    "Today" to (todayMemories.size + overdueCount),
                    "Upcoming" to upcomingMemories.size,
                    "All Active" to pendingMemories.size
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tabs.forEach { (tabName, count) ->
                        val isSelected = selectedFilterTab == tabName
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isSelected) BlackInk else WarmWhite,
                                    RoundedCornerShape(12.dp)
                                )
                                .border(1.5.dp, BlackInk, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedFilterTab = tabName }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                                .testTag("filter_tab_$tabName"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$tabName ($count)",
                                style = Typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) MainYellow else BlackInk,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            // Contextual Hero Focus Card
            item {
                when (selectedFilterTab) {
                    "Today" -> {
                        NeoCard(
                            backgroundColor = if (overdueCount > 0) PastelCoral.copy(alpha = 0.25f) else PastelYellow.copy(alpha = 0.45f),
                            cornerRadius = 16.dp,
                            shadowOffset = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(if (overdueCount > 0) PastelCoral else MainYellow, CircleShape)
                                        .border(1.5.dp, BlackInk, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (overdueCount > 0) "⚠️" else "🎯",
                                        fontSize = 18.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (overdueCount > 0) "Overdue & Today's Commitments" else "Today's Focus",
                                        style = Typography.titleSmall.copy(fontWeight = FontWeight.Black, color = BlackInk)
                                    )
                                    Text(
                                        text = when {
                                            overdueCount > 0 -> "$overdueCount overdue + ${todayMemories.size} scheduled for today."
                                            todayMemories.isNotEmpty() -> "${todayMemories.size} commitments to conquer today."
                                            else -> "No tasks scheduled for today! You're completely caught up."
                                        },
                                        style = Typography.bodySmall.copy(fontSize = 12.sp, color = BlackInk.copy(alpha = 0.8f))
                                    )
                                }
                            }
                        }
                    }
                    "Upcoming" -> {
                        NeoCard(
                            backgroundColor = PastelBlue.copy(alpha = 0.35f),
                            cornerRadius = 16.dp,
                            shadowOffset = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(PastelBlue, CircleShape)
                                        .border(1.5.dp, BlackInk, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "📅", fontSize = 18.sp)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Upcoming Horizon",
                                        style = Typography.titleSmall.copy(fontWeight = FontWeight.Black, color = BlackInk)
                                    )
                                    Text(
                                        text = if (upcomingMemories.isNotEmpty())
                                            "${upcomingMemories.size} scheduled commitments on your radar."
                                        else
                                            "No future commitments logged yet.",
                                        style = Typography.bodySmall.copy(fontSize = 12.sp, color = BlackInk.copy(alpha = 0.8f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Commitments List
            if (filteredList.isEmpty()) {
                item {
                    NeoCard(
                        backgroundColor = WarmWhite,
                        cornerRadius = 20.dp,
                        shadowOffset = 2.5.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(PastelYellow, CircleShape)
                                    .border(2.dp, BlackInk, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = BlackInk,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = if (memories.isEmpty()) "No commitments yet" else "No matching commitments",
                                style = Typography.titleMedium.copy(fontWeight = FontWeight.Black, fontSize = 17.sp),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (memories.isEmpty())
                                    "Speak your first commitment or tap '+ Add Task' to get started."
                                else "Try clearing your search or switching filter tabs.",
                                style = Typography.bodySmall.copy(color = GrayText, textAlign = TextAlign.Center)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(0.9f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                NeoButton(
                                    text = "🎙️ Speak Now",
                                    onClick = { showVoiceDialog = true },
                                    backgroundColor = MainYellow,
                                    modifier = Modifier.weight(1f)
                                )
                                NeoButton(
                                    text = "+ Add Task",
                                    onClick = {
                                        editingItem = null
                                        showCommitmentDialog = true
                                    },
                                    backgroundColor = WarmWhite,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredList, key = { it.id }) { item ->
                    val category = categories.find { it.id == item.categoryId }
                    CommitmentCard(
                        item = item,
                        category = category,
                        onToggleComplete = { viewModel.toggleComplete(item) },
                        onEdit = {
                            editingItem = item
                            showCommitmentDialog = true
                        },
                        onDelete = { deletingItem = item },
                        onClick = { onNavigateToDetail(item.id) }
                    )
                }
            }

            // Completed Archive (Collapsible, neat & non-intrusive)
            if (completedMemories.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCompletedDrawer = !showCompletedDrawer }
                    ) {
                        NeoCard(
                            backgroundColor = WarmWhite,
                            cornerRadius = 14.dp,
                            shadowOffset = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = "✅", fontSize = 14.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Completed Archive (${completedMemories.size})",
                                        style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = BlackInk)
                                    )
                                }
                                Text(
                                    text = if (showCompletedDrawer) "Hide ▲" else "View ▼",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GrayText)
                                )
                            }
                        }
                    }
                }

                if (showCompletedDrawer) {
                    items(completedMemories, key = { "completed_${it.id}" }) { item ->
                        val category = categories.find { it.id == item.categoryId }
                        CommitmentCard(
                            item = item,
                            category = category,
                            onToggleComplete = { viewModel.toggleComplete(item) },
                            onEdit = {
                                editingItem = item
                                showCommitmentDialog = true
                            },
                            onDelete = { deletingItem = item },
                            onClick = { onNavigateToDetail(item.id) }
                        )
                    }
                }
            }
        }
    }
}
