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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.ui.components.CategoryLineIcon
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoDottedBackground
import com.example.ui.theme.BlackInk
import com.example.ui.theme.GrayText
import com.example.ui.theme.MainYellow
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.Typography
import com.example.ui.theme.WarmWhite
import com.example.ui.viewmodel.MemoraViewModel
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

    val displayMemories = when (currentTab) {
        "Completed" -> memories.filter { it.isCompleted }
        else -> memories.filter { !it.isCompleted }
    }

    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val todayStr = today.format(DateTimeFormatter.ofPattern("MMM dd", Locale.US))
    val tomorrowStr = tomorrow.format(DateTimeFormatter.ofPattern("MMM dd", Locale.US))

    val todayList = displayMemories.filter {
        com.example.util.DateUtils.isToday(it.date)
    }

    val tomorrowList = displayMemories.filter {
        it !in todayList && com.example.util.DateUtils.isTomorrow(it.date)
    }

    val laterList = displayMemories.filter {
        it !in todayList && it !in tomorrowList
    }

    NeoDottedBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header
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
                        .size(42.dp)
                        .background(WarmWhite, CircleShape)
                        .border(1.75.dp, BlackInk, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Calendar",
                        tint = BlackInk,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Tabs (Upcoming, Calendar, Completed)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
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
                            .padding(vertical = 10.dp),
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

            // Timeline Items
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                    text = "Timeline is empty",
                                    style = Typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        color = BlackInk
                                    )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Capture something important to start building your timeline.",
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
                    // Section Today
                    if (todayList.isNotEmpty()) {
                        item {
                            TimelineDateHeader(title = "Today · $todayStr")
                        }
                        items(todayList) { item ->
                            TimelineNodeItem(
                                item = item,
                                categories = categories,
                                onClick = { onNavigateToDetail(item.id) }
                            )
                        }
                    }

                    // Section Tomorrow
                    if (tomorrowList.isNotEmpty()) {
                        item {
                            TimelineDateHeader(title = "Tomorrow · $tomorrowStr")
                        }
                        items(tomorrowList) { item ->
                            TimelineNodeItem(
                                item = item,
                                categories = categories,
                                onClick = { onNavigateToDetail(item.id) }
                            )
                        }
                    }

                    // Section Later
                    if (laterList.isNotEmpty()) {
                        item {
                            TimelineDateHeader(title = "This Week & Upcoming")
                        }
                        items(laterList) { item ->
                            TimelineNodeItem(
                                item = item,
                                categories = categories,
                                onClick = { onNavigateToDetail(item.id) }
                            )
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
    onClick: () -> Unit
) {
    val category = categories.find { it.id == item.categoryId }
    val catColor = try {
        if (category != null) Color(android.graphics.Color.parseColor(category.colorHex)) else PastelBlue
    } catch (e: Exception) {
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
                    .background(MainYellow, CircleShape)
                    .border(2.dp, BlackInk, CircleShape)
            )
        }

        // Timeline content card
        NeoCard(
            backgroundColor = WarmWhite,
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
                    Text(
                        text = if (item.isCompleted) "Completed" else (item.time ?: item.date ?: "Upcoming"),
                        style = Typography.bodySmall.copy(
                            color = if (item.isCompleted) BlackInk else GrayText,
                            fontWeight = if (item.isCompleted) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 12.sp
                        )
                    )
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
