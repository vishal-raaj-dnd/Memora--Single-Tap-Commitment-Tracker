package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Category
import com.example.data.model.MemoryItem
import com.example.ui.theme.BlackInk
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.GrayText
import com.example.ui.theme.MainYellow
import com.example.ui.theme.PastelCoral
import com.example.ui.theme.PastelMint
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.Typography
import com.example.ui.theme.WarmWhite
import com.example.util.DateUtils

/**
 * Interactive Neo-Brutalist commitment card.
 * Direct 1-tap checkbox completion, quick-edit button, quick-delete button,
 * dynamic category pill, and temporal badges (Overdue / Today / Tomorrow).
 */
@Composable
fun CommitmentCard(
    item: MemoryItem,
    category: Category?,
    onToggleComplete: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val catColor = try {
        if (category != null) Color(android.graphics.Color.parseColor(category.colorHex)) else MainYellow
    } catch (_: Exception) {
        MainYellow
    }

    val isOverdue = DateUtils.isOverdue(item.date, item.isCompleted)
    val isToday = DateUtils.isToday(item.date)
    val isTomorrow = DateUtils.isTomorrow(item.date)

    val checkBgColor by animateColorAsState(
        targetValue = if (item.isCompleted) SuccessGreen else WarmWhite,
        label = "checkBg"
    )

    NeoCard(
        backgroundColor = if (item.isCompleted) WarmWhite.copy(alpha = 0.85f) else WarmWhite,
        cornerRadius = 16.dp,
        shadowOffset = if (item.isCompleted) 1.5.dp else 2.5.dp,
        onClick = onClick,
        modifier = modifier.testTag("commitment_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Interactive 1-Tap Completion Checkbox
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(checkBgColor, CircleShape)
                    .border(2.dp, BlackInk, CircleShape)
                    .clip(CircleShape)
                    .clickable { onToggleComplete() }
                    .testTag("commitment_checkbox_${item.id}"),
                contentAlignment = Alignment.Center
            ) {
                if (item.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = BlackInk,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main details
            Column(modifier = Modifier.weight(1f)) {
                // Title
                Text(
                    text = item.title,
                    style = Typography.bodyLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (item.isCompleted) GrayText else BlackInk,
                        textDecoration = if (item.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Metadata Row: Category dot + name + temporal badges
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category pill
                    Box(
                        modifier = Modifier
                            .background(catColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .border(1.dp, BlackInk.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = category?.name ?: "General",
                            style = Typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = BlackInk
                            )
                        )
                    }

                    // Urgency Badge (Overdue / Today / Tomorrow)
                    when {
                        isOverdue -> {
                            Box(
                                modifier = Modifier
                                    .background(PastelCoral, RoundedCornerShape(6.dp))
                                    .border(1.dp, BlackInk, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = BlackInk,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Overdue",
                                        style = Typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp, color = BlackInk)
                                    )
                                }
                            }
                        }
                        isToday -> {
                            Box(
                                modifier = Modifier
                                    .background(MainYellow, RoundedCornerShape(6.dp))
                                    .border(1.dp, BlackInk, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Today",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = BlackInk)
                                )
                            }
                        }
                        isTomorrow -> {
                            Box(
                                modifier = Modifier
                                    .background(PastelMint, RoundedCornerShape(6.dp))
                                    .border(1.dp, BlackInk, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Tomorrow",
                                    style = Typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp, color = BlackInk)
                                )
                            }
                        }
                    }

                    // Time display
                    if (!item.time.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = GrayText,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = item.time,
                                style = Typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = GrayText
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Quick Actions: Edit (✏️) and Delete (🗑️)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Quick Edit Button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("edit_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Commitment",
                        tint = BlackInk,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Quick Delete Button
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("delete_button_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Commitment",
                        tint = GrayText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
