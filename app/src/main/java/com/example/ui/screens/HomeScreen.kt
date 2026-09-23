package com.example.ui.screens

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.CategoryLineIcon
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoDottedBackground
import com.example.ui.components.StatBlock
import com.example.ui.theme.BlackInk
import com.example.ui.theme.GrayText
import com.example.ui.theme.MainYellow
import com.example.ui.theme.PastelBlue
import com.example.ui.theme.PastelCoral
import com.example.ui.theme.PastelLavender
import com.example.ui.theme.PastelMint
import com.example.ui.theme.PastelPeach
import com.example.ui.theme.PastelYellow
import com.example.ui.theme.Typography
import com.example.ui.theme.WarmWhite
import com.example.ui.viewmodel.MemoraViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: MemoraViewModel,
    onNavigateToCapture: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFriends: () -> Unit,
    modifier: Modifier = Modifier
) {
    val memories by viewModel.allMemories.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val stats by viewModel.memoryStats.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val userAvatar by viewModel.userAvatar.collectAsState()

    val todayMemories = memories.filter {
        com.example.util.DateUtils.isToday(it.date)
    }.take(4)

    val tomorrowMemories = memories.filter {
        it !in todayMemories && com.example.util.DateUtils.isTomorrow(it.date)
    }.take(4)

    val upcomingMemories = memories.filter {
        it !in todayMemories && it !in tomorrowMemories
    }.take(4)

    NeoDottedBackground(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: Memora + avatar
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Memora",
                            style = Typography.displayMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                color = BlackInk
                            )
                        )
                        Text(
                            text = "See it. Capture it. Remember it.",
                            style = Typography.bodySmall.copy(
                                color = GrayText,
                                fontSize = 13.sp
                            )
                        )
                    }

                    // Avatar button
                    Box(
                        modifier = Modifier
                            .testTag("home_avatar_button")
                            .clickable { onNavigateToSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        com.example.ui.components.UserProfileAvatar(avatarKey = userAvatar, size = 46.dp)
                    }
                }
            }

            // Yellow Greeting Card (Zero emojis)
            item {
                NeoCard(
                    backgroundColor = MainYellow,
                    cornerRadius = 20.dp,
                    shadowOffset = 3.dp,
                    testTag = "greeting_card"
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
                    ) {
                        Text(
                            text = "Hi, $userName",
                            style = Typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 21.sp,
                                color = BlackInk
                            )
                        )
                        if (userRole.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = userRole,
                                style = Typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = BlackInk.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nothing slips away now.",
                            style = Typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium,
                                color = BlackInk.copy(alpha = 0.85f),
                                fontSize = 14.sp
                            )
                        )
                    }
                }
            }

            // Highlighted Camera CTA Card
            item {
                NeoCard(
                    backgroundColor = WarmWhite,
                    cornerRadius = 20.dp,
                    shadowOffset = 3.dp,
                    onClick = onNavigateToCapture,
                    testTag = "home_tap_to_capture_card"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(PastelYellow, RoundedCornerShape(14.dp))
                                .border(1.75.dp, BlackInk, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera",
                                tint = BlackInk,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tap to capture",
                                style = Typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                            )
                            Text(
                                text = "anything on your screen",
                                style = Typography.bodySmall.copy(
                                    color = GrayText,
                                    fontSize = 13.sp
                                )
                            )
                        }

                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = "Capture Arrow",
                            tint = BlackInk,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // PURE EMPTY STATE ON FIRST RUN (Specification 4 & 13)
            if (memories.isEmpty()) {
                item {
                    NeoCard(
                        backgroundColor = WarmWhite,
                        cornerRadius = 20.dp,
                        shadowOffset = 3.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(PastelYellow, CircleShape)
                                    .border(2.dp, BlackInk, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = BlackInk,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Nothing to remember yet.",
                                style = Typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = BlackInk
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Capture something important\nand Memora will organize it for you.",
                                style = Typography.bodyMedium.copy(
                                    color = GrayText,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            NeoButton(
                                text = "Capture",
                                onClick = onNavigateToCapture,
                                modifier = Modifier.fillMaxWidth(0.6f)
                            )
                        }
                    }
                }
            } else {
                // Dynamically calculated live metrics (Only when actual items exist)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatBlock(
                                percentage = stats.taskPercentage,
                                title = "Tasks (${stats.taskCount})",
                                icon = Icons.Outlined.CheckCircle,
                                backgroundColor = PastelCoral,
                                modifier = Modifier.weight(1f)
                            )
                            StatBlock(
                                percentage = stats.eventPercentage,
                                title = "Events (${stats.eventCount})",
                                icon = Icons.Outlined.CalendarMonth,
                                backgroundColor = PastelMint,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            StatBlock(
                                percentage = stats.deadlinePercentage,
                                title = "Deadlines (${stats.deadlineCount})",
                                icon = Icons.Outlined.Timer,
                                backgroundColor = PastelLavender,
                                modifier = Modifier.weight(1f)
                            )
                            StatBlock(
                                percentage = stats.notePercentage,
                                title = "Notes (${stats.noteCount})",
                                icon = Icons.Outlined.Description,
                                backgroundColor = PastelPeach,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Section "Today"
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Today",
                            style = Typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp
                            )
                        )
                        Box(
                            modifier = Modifier
                                .size(26.dp)
                                .background(WarmWhite, CircleShape)
                                .border(1.5.dp, BlackInk, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${todayMemories.size}",
                                style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }

                if (todayMemories.isNotEmpty()) {
                    items(todayMemories) { item ->
                        val category = categories.find { it.id == item.categoryId }
                        val catColor = try {
                            if (category != null) Color(android.graphics.Color.parseColor(category.colorHex)) else PastelBlue
                        } catch (_: Exception) {
                            PastelBlue
                        }

                        NeoCard(
                            backgroundColor = WarmWhite,
                            cornerRadius = 16.dp,
                            shadowOffset = 2.dp,
                            onClick = { onNavigateToDetail(item.id) },
                            testTag = "today_item_${item.id}"
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(catColor, RoundedCornerShape(12.dp))
                                        .border(1.5.dp, BlackInk, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CategoryLineIcon(
                                        iconKey = category?.icon,
                                        size = 20.dp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = Typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = BlackInk
                                        )
                                    )
                                    Text(
                                        text = category?.name ?: "General",
                                        style = Typography.bodySmall.copy(
                                            color = GrayText,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                if (!item.time.isNullOrBlank()) {
                                    Text(
                                        text = item.time,
                                        style = Typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BlackInk,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Section "Tomorrow"
                if (tomorrowMemories.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tomorrow",
                                style = Typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 19.sp
                                )
                            )
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(WarmWhite, CircleShape)
                                    .border(1.5.dp, BlackInk, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${tomorrowMemories.size}",
                                    style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    items(tomorrowMemories) { item ->
                        val category = categories.find { it.id == item.categoryId }
                        val catColor = try {
                            if (category != null) Color(android.graphics.Color.parseColor(category.colorHex)) else PastelBlue
                        } catch (_: Exception) {
                            PastelBlue
                        }

                        NeoCard(
                            backgroundColor = WarmWhite,
                            cornerRadius = 16.dp,
                            shadowOffset = 2.dp,
                            onClick = { onNavigateToDetail(item.id) },
                            testTag = "tomorrow_item_${item.id}"
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(catColor, RoundedCornerShape(12.dp))
                                        .border(1.5.dp, BlackInk, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CategoryLineIcon(
                                        iconKey = category?.icon,
                                        size = 20.dp
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = Typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = BlackInk
                                        )
                                    )
                                    Text(
                                        text = category?.name ?: "General",
                                        style = Typography.bodySmall.copy(
                                            color = GrayText,
                                            fontSize = 12.sp
                                        )
                                    )
                                }

                                if (!item.time.isNullOrBlank()) {
                                    Text(
                                        text = item.time,
                                        style = Typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = BlackInk,
                                            fontSize = 12.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Section "Upcoming"
                if (upcomingMemories.isNotEmpty()) {
                    item {
                        Text(
                            text = "Upcoming",
                            style = Typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 19.sp
                            ),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(upcomingMemories) { item ->
                        val category = categories.find { it.id == item.categoryId }
                        val catColor = try {
                            if (category != null) Color(android.graphics.Color.parseColor(category.colorHex)) else PastelBlue
                        } catch (_: Exception) {
                            PastelBlue
                        }

                        NeoCard(
                            backgroundColor = WarmWhite,
                            cornerRadius = 16.dp,
                            shadowOffset = 2.dp,
                            onClick = { onNavigateToDetail(item.id) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(catColor, RoundedCornerShape(10.dp))
                                            .border(1.5.dp, BlackInk, RoundedCornerShape(10.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CategoryLineIcon(
                                            iconKey = category?.icon,
                                            size = 18.dp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = item.title,
                                            style = Typography.bodyMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                        )
                                        Text(
                                            text = category?.name ?: "",
                                            style = Typography.bodySmall.copy(fontSize = 12.sp, color = GrayText)
                                        )
                                    }
                                }

                                Text(
                                    text = item.date ?: "Upcoming",
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
    }
}
