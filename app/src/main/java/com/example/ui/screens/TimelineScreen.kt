package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.MemoryItem
import com.example.data.model.MemoryType
import com.example.ui.components.CategoryLineIcon
import com.example.ui.components.CommitmentCard
import com.example.ui.components.CommitmentDialog
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoChip
import com.example.ui.components.NeoDottedBackground
import com.example.ui.theme.BlackInk
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.GrayText
import com.example.ui.theme.MainYellow
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelCoral
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.Typography
import com.example.ui.theme.WarmWhite
import com.example.ui.viewmodel.MemoraViewModel
import com.example.util.DateUtils
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TimelineScreen(
    viewModel: MemoraViewModel,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val memories by viewModel.allMemories.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val currentTab by viewModel.timelineTab.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    var selectedCalendarDate by remember { mutableStateOf(LocalDate.now()) }
    var weekStartDate by remember {
        val today = LocalDate.now()
        mutableStateOf(today.minusDays(today.dayOfWeek.value.toLong() - 1))
    }

    var showCommitmentDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MemoryItem?>(null) }
    var deletingItem by remember { mutableStateOf<MemoryItem?>(null) }

    // Filter by tab: In Calendar show all commitments for that day
    val tabFilteredMemories = when (currentTab) {
        "Completed" -> memories.filter { it.isCompleted }
        "Calendar" -> memories
        else -> memories.filter { !it.isCompleted }
    }

    // Then filter by category if a category filter is active
    val displayMemories = if (selectedFilter == "All" || selectedFilter.isBlank()) {
        tabFilteredMemories
    } else {
        tabFilteredMemories.filter { item ->
            item.categoryId.equals(selectedFilter, ignoreCase = true) ||
                    categories.find { it.id.equals(selectedFilter, ignoreCase = true) }?.name.equals(item.categoryId, ignoreCase = true)
        }
    }

    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val todayStr = today.format(DateTimeFormatter.ofPattern("MMM dd", Locale.US))
    val tomorrowStr = tomorrow.format(DateTimeFormatter.ofPattern("MMM dd", Locale.US))

    // Upcoming tab groupings: Overdue, Today, Tomorrow, Upcoming
    val overdueList = displayMemories.filter {
        DateUtils.isOverdue(it.date, it.isCompleted)
    }

    val todayList = displayMemories.filter {
        it !in overdueList && DateUtils.isToday(it.date)
    }

    val tomorrowList = displayMemories.filter {
        it !in overdueList && it !in todayList && DateUtils.isTomorrow(it.date)
    }

    val upcomingList = displayMemories.filter {
        it !in overdueList && it !in todayList && it !in tomorrowList
    }

    // Calendar tab commitments for currently selected date
    val calendarSelectedList = displayMemories.filter {
        DateUtils.matchesDate(it.date, selectedCalendarDate)
    }

    // Add / Edit Commitment Dialog
    if (showCommitmentDialog) {
        val initialDate = if (editingItem != null) {
            editingItem?.date
        } else if (currentTab == "Calendar") {
            selectedCalendarDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.US))
        } else null

        val prefilledItem = editingItem ?: if (initialDate != null) {
            MemoryItem(
                title = "",
                categoryId = categories.firstOrNull()?.id ?: "personal",
                date = initialDate
            )
        } else null

        CommitmentDialog(
            categories = categories,
            existingItem = prefilledItem,
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header with interactive Add Task button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Timeline",
                    style = Typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    )
                )

                Box(
                    modifier = Modifier
                        .background(MainYellow, RoundedCornerShape(12.dp))
                        .border(1.75.dp, BlackInk, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            editingItem = null
                            showCommitmentDialog = true
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("timeline_add_button")
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
                                fontWeight = FontWeight.Black,
                                color = BlackInk
                            )
                        )
                    }
                }
            }

            // Tabs (Upcoming, Calendar, Completed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp)
                    .background(WarmWhite, RoundedCornerShape(18.dp))
                    .border(2.dp, BlackInk, RoundedCornerShape(18.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("Upcoming", "Calendar", "Completed").forEach { tab ->
                    val isSelected = tab == currentTab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .then(
                                if (isSelected) {
                                    Modifier
                                        .background(MainYellow, RoundedCornerShape(14.dp))
                                        .border(1.5.dp, BlackInk, RoundedCornerShape(14.dp))
                                } else Modifier
                            )
                            .clickable { viewModel.setTimelineTab(tab) }
                            .padding(vertical = 10.dp)
                            .testTag("timeline_tab_$tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            style = Typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = BlackInk
                            )
                        )
                    }
                }
            }

            // Category Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                item {
                    NeoChip(
                        label = "All",
                        isSelected = selectedFilter == "All" || selectedFilter.isBlank(),
                        onClick = { viewModel.setSelectedFilter("All") },
                        testTag = "filter_chip_all"
                    )
                }

                items(categories) { cat ->
                    val isSelected = selectedFilter.equals(cat.id, ignoreCase = true)
                    NeoChip(
                        label = cat.name,
                        isSelected = isSelected,
                        onClick = {
                            if (isSelected) {
                                viewModel.setSelectedFilter("All")
                            } else {
                                viewModel.setSelectedFilter(cat.id)
                            }
                        },
                        testTag = "filter_chip_${cat.id}"
                    )
                }
            }

            // Content Area depending on currentTab
            when (currentTab) {
                "Calendar" -> {
                    // Interactive Week Bar
                    NeoCard(
                        backgroundColor = WarmWhite,
                        cornerRadius = 18.dp,
                        shadowOffset = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // Week navigation row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { weekStartDate = weekStartDate.minusWeeks(1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Previous Week",
                                        tint = BlackInk,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                val monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.US)
                                Text(
                                    text = weekStartDate.format(monthYearFormatter),
                                    style = Typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = BlackInk
                                    )
                                )

                                IconButton(
                                    onClick = { weekStartDate = weekStartDate.plusWeeks(1) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = "Next Week",
                                        tint = BlackInk,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // 7 Days of the week row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                for (i in 0..6) {
                                    val dayDate = weekStartDate.plusDays(i.toLong())
                                    val isSelected = dayDate == selectedCalendarDate
                                    val isCurrentDay = dayDate == LocalDate.now()
                                    val hasMemories = memories.any { DateUtils.matchesDate(it.date, dayDate) }

                                    val dayName = dayDate.format(DateTimeFormatter.ofPattern("EEE", Locale.US)).take(3).uppercase()
                                    val dayNumber = dayDate.dayOfMonth.toString()

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 2.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                when {
                                                    isSelected -> MainYellow
                                                    isCurrentDay -> CreamBackground
                                                    else -> Color.Transparent
                                                },
                                                RoundedCornerShape(12.dp)
                                            )
                                            .border(
                                                width = if (isSelected) 1.75.dp else if (isCurrentDay) 1.dp else 0.dp,
                                                color = if (isSelected || isCurrentDay) BlackInk else Color.Transparent,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedCalendarDate = dayDate }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayName,
                                                style = Typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) BlackInk else GrayText
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = dayNumber,
                                                style = Typography.bodyMedium.copy(
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp,
                                                    color = BlackInk
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(
                                                        if (hasMemories) BlackInk else Color.Transparent,
                                                        CircleShape
                                                    )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Selected Date Commitments List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        item {
                            val headerDateStr = selectedCalendarDate.format(DateTimeFormatter.ofPattern("EEEE, MMM dd", Locale.US))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Schedule · $headerDateStr",
                                    style = Typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        color = BlackInk
                                    )
                                )

                                Box(
                                    modifier = Modifier
                                        .background(MainYellow, RoundedCornerShape(10.dp))
                                        .border(1.5.dp, BlackInk, RoundedCornerShape(10.dp))
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            editingItem = null
                                            showCommitmentDialog = true
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Add, contentDescription = "Add", tint = BlackInk, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add for Day", style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = BlackInk, fontSize = 11.sp))
                                    }
                                }
                            }
                        }

                        if (calendarSelectedList.isEmpty()) {
                            item {
                                NeoCard(
                                    backgroundColor = WarmWhite,
                                    cornerRadius = 16.dp,
                                    shadowOffset = 2.dp,
                                    modifier = Modifier.padding(top = 8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = "No commitments for this day",
                                            style = Typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 16.sp,
                                                color = BlackInk
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tap 'Add for Day' or select another date on the calendar above.",
                                            style = Typography.bodySmall.copy(
                                                color = GrayText,
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            items(calendarSelectedList) { item ->
                                CommitmentCard(
                                    item = item,
                                    category = categories.find { it.id == item.categoryId },
                                    onToggleComplete = { viewModel.toggleComplete(item) },
                                    onEdit = {
                                        editingItem = item
                                        showCommitmentDialog = true
                                    },
                                    onDelete = {
                                        deletingItem = item
                                    },
                                    onClick = { onNavigateToDetail(item.id) }
                                )
                            }
                        }
                    }
                }

                else -> {
                    // Regular Timeline (Upcoming or Completed)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        if (displayMemories.isEmpty()) {
                            item {
                                NeoCard(
                                    backgroundColor = WarmWhite,
                                    cornerRadius = 18.dp,
                                    shadowOffset = 2.5.dp,
                                    modifier = Modifier.padding(top = 16.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = if (currentTab == "Completed") "No completed items yet" else "Timeline is empty",
                                            style = Typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 17.sp,
                                                color = BlackInk
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = if (currentTab == "Completed") "Mark tasks or commitments as done to see them here." else "Capture something important to start building your timeline.",
                                            style = Typography.bodySmall.copy(
                                                color = GrayText,
                                                fontSize = 13.sp,
                                                textAlign = TextAlign.Center
                                            )
                                        )
                                    }
                                }
                            }
                        } else if (currentTab == "Completed") {
                            items(displayMemories) { item ->
                                CommitmentCard(
                                    item = item,
                                    category = categories.find { it.id == item.categoryId },
                                    onToggleComplete = { viewModel.toggleComplete(item) },
                                    onEdit = {
                                        editingItem = item
                                        showCommitmentDialog = true
                                    },
                                    onDelete = {
                                        deletingItem = item
                                    },
                                    onClick = { onNavigateToDetail(item.id) }
                                )
                            }
                        } else {
                            // Section: Overdue (Highlighted in coral warning)
                            if (overdueList.isNotEmpty()) {
                                item {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Overdue",
                                            tint = BlackInk,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Overdue (${overdueList.size})",
                                            style = Typography.titleMedium.copy(
                                                fontWeight = FontWeight.Black,
                                                fontSize = 17.sp,
                                                color = BlackInk
                                            )
                                        )
                                    }
                                }
                                items(overdueList) { item ->
                                    CommitmentCard(
                                        item = item,
                                        category = categories.find { it.id == item.categoryId },
                                        onToggleComplete = { viewModel.toggleComplete(item) },
                                        onEdit = {
                                            editingItem = item
                                            showCommitmentDialog = true
                                        },
                                        onDelete = {
                                            deletingItem = item
                                        },
                                        onClick = { onNavigateToDetail(item.id) }
                                    )
                                }
                            }

                            // Section: Today
                            if (todayList.isNotEmpty()) {
                                item {
                                    TimelineDateHeader(title = "Today · $todayStr")
                                }
                                items(todayList) { item ->
                                    CommitmentCard(
                                        item = item,
                                        category = categories.find { it.id == item.categoryId },
                                        onToggleComplete = { viewModel.toggleComplete(item) },
                                        onEdit = {
                                            editingItem = item
                                            showCommitmentDialog = true
                                        },
                                        onDelete = {
                                            deletingItem = item
                                        },
                                        onClick = { onNavigateToDetail(item.id) }
                                    )
                                }
                            }

                            // Section: Tomorrow
                            if (tomorrowList.isNotEmpty()) {
                                item {
                                    TimelineDateHeader(title = "Tomorrow · $tomorrowStr")
                                }
                                items(tomorrowList) { item ->
                                    CommitmentCard(
                                        item = item,
                                        category = categories.find { it.id == item.categoryId },
                                        onToggleComplete = { viewModel.toggleComplete(item) },
                                        onEdit = {
                                            editingItem = item
                                            showCommitmentDialog = true
                                        },
                                        onDelete = {
                                            deletingItem = item
                                        },
                                        onClick = { onNavigateToDetail(item.id) }
                                    )
                                }
                            }

                            // Section: Upcoming & Later
                            if (upcomingList.isNotEmpty()) {
                                item {
                                    TimelineDateHeader(title = "Upcoming & Later")
                                }
                                items(upcomingList) { item ->
                                    CommitmentCard(
                                        item = item,
                                        category = categories.find { it.id == item.categoryId },
                                        onToggleComplete = { viewModel.toggleComplete(item) },
                                        onEdit = {
                                            editingItem = item
                                            showCommitmentDialog = true
                                        },
                                        onDelete = {
                                            deletingItem = item
                                        },
                                        onClick = { onNavigateToDetail(item.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineDateHeader(title: String) {
    Text(
        text = title,
        style = Typography.titleMedium.copy(
            fontWeight = FontWeight.Black,
            fontSize = 17.sp,
            color = BlackInk
        ),
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    )
}

@Composable
private fun TimelineNodeItem(
    item: MemoryItem,
    categories: List<Category>,
    isOverdue: Boolean = false,
    onClick: () -> Unit
) {
    val category = categories.find { it.id == item.categoryId }
    val catColor = try {
        if (category != null) Color(android.graphics.Color.parseColor(category.colorHex)) else PastelBlue
    } catch (_: Exception) {
        PastelBlue
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Vertical dot node
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(end = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        when {
                            item.isCompleted -> SuccessGreen
                            isOverdue -> PastelCoral
                            else -> MainYellow
                        },
                        CircleShape
                    )
                    .border(2.dp, BlackInk, CircleShape)
            )
        }

        // Timeline content card
        NeoCard(
            backgroundColor = if (isOverdue) PastelCoral.copy(alpha = 0.2f) else WarmWhite,
            cornerRadius = 16.dp,
            shadowOffset = 2.dp,
            onClick = onClick,
            modifier = Modifier.weight(1f),
            testTag = "timeline_item_${item.id}"
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        style = Typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BlackInk
                        )
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isOverdue) {
                            Box(
                                modifier = Modifier
                                    .background(PastelCoral, RoundedCornerShape(6.dp))
                                    .border(1.dp, BlackInk, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Overdue",
                                    style = Typography.labelSmall.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 10.sp,
                                        color = BlackInk
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = when {
                                item.isCompleted -> "Completed"
                                !item.time.isNullOrBlank() && !item.date.isNullOrBlank() -> "${item.date} · ${item.time}"
                                !item.date.isNullOrBlank() -> item.date
                                !item.time.isNullOrBlank() -> item.time
                                else -> "Upcoming"
                            },
                            style = Typography.bodySmall.copy(
                                color = if (item.isCompleted) SuccessGreen else if (isOverdue) BlackInk else GrayText,
                                fontWeight = if (item.isCompleted || isOverdue) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        )
                    }
                }

                // Pastel Category badge
                Box(
                    modifier = Modifier
                        .background(catColor, RoundedCornerShape(12.dp))
                        .border(1.25.dp, BlackInk, RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryLineIcon(
                            iconKey = category?.icon,
                            size = 14.dp,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text(
                            text = category?.name ?: "General",
                            style = Typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = BlackInk
                            )
                        )
                    }
                }
            }
        }
    }
}
