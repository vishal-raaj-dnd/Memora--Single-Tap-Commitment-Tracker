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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.ui.components.CategoryLineIcon
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoDottedBackground
import com.example.ui.theme.BlackInk
import com.example.ui.theme.CreamBackground
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

private val DEFAULT_CATEGORY_IDS = setOf("personal", "work", "study", "finance", "health", "urgent")

private val AVAILABLE_PALETTES = listOf(
    "#FFE885" to PastelYellow,
    "#FFB3B3" to PastelCoral,
    "#B3E5FC" to PastelBlue,
    "#C8E6C9" to PastelMint,
    "#E1BEE7" to PastelLavender,
    "#FFE0B2" to PastelPeach
)

private val AVAILABLE_ICONS = listOf("tag", "rocket", "chat", "book", "target", "star", "briefcase", "heart")

@Composable
fun CategoriesScreen(
    viewModel: MemoraViewModel,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories by viewModel.allCategories.collectAsState()
    val memories by viewModel.allMemories.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    var newCatName by remember { mutableStateOf("") }
    var newCatIcon by remember { mutableStateOf("tag") }
    var newCatColor by remember { mutableStateOf("#FFE885") }

    // Add Category Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "Add Category",
                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = newCatName,
                        onValueChange = { newCatName = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Side Project, Fitness...") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = BlackInk,
                            unfocusedIndicatorColor = GrayText,
                            focusedContainerColor = CreamBackground,
                            unfocusedContainerColor = CreamBackground
                        )
                    )

                    // Color Swatches
                    Column {
                        Text(
                            text = "Color Theme",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GrayText)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            AVAILABLE_PALETTES.forEach { (hex, composeColor) ->
                                val isSelected = newCatColor.equals(hex, ignoreCase = true)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(composeColor, CircleShape)
                                        .border(
                                            width = if (isSelected) 2.5.dp else 1.dp,
                                            color = if (isSelected) BlackInk else BlackInk.copy(alpha = 0.4f),
                                            shape = CircleShape
                                        )
                                        .clickable { newCatColor = hex }
                                )
                            }
                        }
                    }

                    // Icon Selector
                    Column {
                        Text(
                            text = "Icon",
                            style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = GrayText)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(AVAILABLE_ICONS) { iconKey ->
                                val isSelected = newCatIcon == iconKey
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(if (isSelected) MainYellow else WarmWhite, RoundedCornerShape(10.dp))
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = BlackInk,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .clickable { newCatIcon = iconKey },
                                    contentAlignment = Alignment.Center
                                ) {
                                    CategoryLineIcon(iconKey = iconKey, size = 18.dp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            viewModel.addCategory(newCatName.trim(), newCatIcon, newCatColor)
                            newCatName = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Add", fontWeight = FontWeight.Bold, color = BlackInk)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = GrayText)
                }
            },
            containerColor = CreamBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Confirmation Dialog
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = {
                Text(
                    text = "Delete Category?",
                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${cat.name}'? Any commitments in this category will be safely reassigned to 'Personal'.",
                    style = Typography.bodyMedium.copy(color = BlackInk)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCategory(cat)
                        categoryToDelete = null
                    }
                ) {
                    Text("Delete", fontWeight = FontWeight.Black, color = PastelCoral)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
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
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Categories",
                    style = Typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 28.sp
                    )
                )

                // Add button
                Box(
                    modifier = Modifier
                        .testTag("add_category_header_button")
                        .size(42.dp)
                        .background(MainYellow, CircleShape)
                        .border(1.75.dp, BlackInk, CircleShape)
                        .clip(CircleShape)
                        .clickable { showAddDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Category",
                        tint = BlackInk,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(categories) { category ->
                    val color = try {
                        Color(android.graphics.Color.parseColor(category.colorHex))
                    } catch (_: Exception) {
                        WarmWhite
                    }

                    val displayCount = memories.count { it.categoryId.equals(category.id, ignoreCase = true) }
                    val isCustomCategory = Category.DEFAULT_CATEGORIES.none { it.id.equals(category.id, ignoreCase = true) }

                    NeoCard(
                        backgroundColor = color,
                        cornerRadius = 16.dp,
                        shadowOffset = 2.dp,
                        onClick = { onCategoryClick(category.id) },
                        testTag = "category_card_${category.id}"
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                CategoryLineIcon(
                                    iconKey = category.icon,
                                    size = 20.dp,
                                    modifier = Modifier.padding(end = 12.dp)
                                )
                                Text(
                                    text = category.name,
                                    style = Typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = BlackInk
                                    )
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Memory count pill
                                Box(
                                    modifier = Modifier
                                        .background(WarmWhite.copy(alpha = 0.85f), RoundedCornerShape(12.dp))
                                        .border(1.25.dp, BlackInk, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = "$displayCount",
                                        style = Typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = BlackInk
                                        )
                                    )
                                }

                                // Delete icon for user-created custom categories
                                if (isCustomCategory) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(WarmWhite, CircleShape)
                                            .border(1.25.dp, BlackInk, CircleShape)
                                            .clip(CircleShape)
                                            .clickable { categoryToDelete = category },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete Category",
                                            tint = BlackInk,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    NeoButton(
                        text = "+ Add New Category",
                        onClick = { showAddDialog = true },
                        backgroundColor = WarmWhite,
                        testTag = "add_new_category_button"
                    )
                }
            }
        }
    }
}
