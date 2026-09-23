package com.example.ui.screens

import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.CategoryLineIcon
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoChip
import com.example.ui.components.NeoDottedBackground
import com.example.ui.components.ScreenshotImage
import com.example.ui.theme.BlackInk
import com.example.ui.theme.GrayText
import com.example.ui.theme.Typography
import com.example.ui.theme.WarmWhite
import com.example.ui.viewmodel.MemoraViewModel

@Composable
fun SearchScreen(
    viewModel: MemoraViewModel,
    onNavigateToDetail: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val filteredMemories by viewModel.filteredMemories.collectAsState()
    val categories by viewModel.allCategories.collectAsState()

    val filters = listOf("All", "Tasks", "Events", "Deadlines", "Notes")

    NeoDottedBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Search Input
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Search",
                    style = Typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search your memories, apps, notes...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = BlackInk
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = BlackInk)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_input"),
                    shape = RoundedCornerShape(18.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = WarmWhite,
                        unfocusedContainerColor = WarmWhite,
                        focusedIndicatorColor = BlackInk,
                        unfocusedIndicatorColor = BlackInk
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter chips row
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filters) { filter ->
                        NeoChip(
                            label = filter,
                            isSelected = filter == selectedFilter,
                            onClick = { viewModel.setSelectedFilter(filter) },
                            testTag = "filter_chip_$filter"
                        )
                    }
                }
            }

            // Results count or empty status
            if (filteredMemories.isNotEmpty()) {
                Text(
                    text = "${filteredMemories.size} memories found",
                    style = Typography.bodySmall.copy(color = GrayText),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                )
            }

            // Results list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredMemories.isEmpty()) {
                    item {
                        NeoCard(
                            backgroundColor = WarmWhite,
                            cornerRadius = 18.dp,
                            shadowOffset = 2.dp,
                            modifier = Modifier.padding(top = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = GrayText,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "Search is ready" else "No memories found",
                                    style = Typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (searchQuery.isBlank()) "Type to search your captured memories." else "No memories match '$searchQuery'.",
                                    style = Typography.bodySmall.copy(color = GrayText, textAlign = TextAlign.Center)
                                )
                            }
                        }
                    }
                } else {
                    items(filteredMemories) { item ->
                        val category = categories.find { it.id == item.categoryId }

                        NeoCard(
                            backgroundColor = WarmWhite,
                            cornerRadius = 16.dp,
                            shadowOffset = 2.dp,
                            onClick = { onNavigateToDetail(item.id) },
                            testTag = "search_item_${item.id}"
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.5.dp, BlackInk, RoundedCornerShape(12.dp))
                                ) {
                                    ScreenshotImage(
                                        screenshotUri = item.originalScreenshotUri,
                                        cornerRadius = 12.dp,
                                        aspectRatio = 1f
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.title,
                                        style = Typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.date ?: "Upcoming",
                                            style = Typography.bodySmall.copy(color = GrayText, fontSize = 12.sp)
                                        )
                                        Text(
                                            text = " · ",
                                            style = Typography.bodySmall.copy(color = GrayText)
                                        )
                                        CategoryLineIcon(iconKey = category?.icon, size = 13.dp, modifier = Modifier.padding(end = 4.dp))
                                        Text(
                                            text = category?.name ?: "General",
                                            style = Typography.bodySmall.copy(
                                                color = BlackInk,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
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
    }
}
