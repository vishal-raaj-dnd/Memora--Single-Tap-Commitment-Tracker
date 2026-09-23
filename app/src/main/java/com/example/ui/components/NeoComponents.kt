package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlackInk
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.GrayText
import com.example.ui.theme.MainYellow
import com.example.ui.theme.ShadowBlack
import com.example.ui.theme.Typography

/**
 * Neo-brutalist tactile card with physical offset shadow and crisp black border.
 */
@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White,
    borderColor: Color = BlackInk,
    borderWidth: Dp = 1.75.dp,
    cornerRadius: Dp = 20.dp,
    shadowOffset: Dp = 3.dp,
    onClick: (() -> Unit)? = null,
    testTag: String? = null,
    content: @Composable () -> Unit
) {
    val shape: Shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.985f else 1f,
        label = "cardScale"
    )

    Box(
        modifier = modifier
            .scale(animatedScale)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        // Offset black shadow layer
        if (shadowOffset > 0.dp) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = shadowOffset, y = shadowOffset)
                    .background(ShadowBlack, shape = shape)
            )
        }

        // Foreground content card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, shape = shape)
                .border(borderWidth, borderColor, shape = shape)
                .clip(shape)
                .then(
                    if (onClick != null) {
                        Modifier.clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick
                        )
                    } else Modifier
                )
        ) {
            content()
        }
    }
}

/**
 * Neo-brutalist button with bold black border and tactile press response.
 */
@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MainYellow,
    textColor: Color = BlackInk,
    cornerRadius: Dp = 16.dp,
    shadowOffset: Dp = 3.dp,
    testTag: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(cornerRadius)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        label = "btnScale"
    )

    Box(
        modifier = modifier
            .scale(animatedScale)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        if (shadowOffset > 0.dp) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = shadowOffset, y = shadowOffset)
                    .background(BlackInk, shape = shape)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor, shape = shape)
                .border(2.dp, BlackInk, shape = shape)
                .clip(shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .padding(vertical = 14.dp, horizontal = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = text,
                    style = Typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                )
            }
        }
    }
}

/**
 * Neo-brutalist pastel pill chip for categories, filters, and status badges.
 */
@Composable
fun NeoChip(
    label: String,
    modifier: Modifier = Modifier,
    icon: String? = null,
    backgroundColor: Color = Color.White,
    isSelected: Boolean = false,
    onClick: (() -> Unit)? = null,
    testTag: String? = null
) {
    val shape = RoundedCornerShape(50)
    val bg = if (isSelected) MainYellow else backgroundColor

    Box(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .background(bg, shape = shape)
            .border(1.5.dp, BlackInk, shape = shape)
            .clip(shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Text(
                    text = icon,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
            Text(
                text = label,
                style = Typography.labelMedium.copy(
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = BlackInk
                )
            )
        }
    }
}

/**
 * Background dotted grid texture that elevates the tactile notebook feel.
 */
@Composable
fun NeoDottedBackground(
    modifier: Modifier = Modifier,
    dotColor: Color = Color(0x18111111),
    spacing: Float = 24f,
    radius: Float = 1.5f,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CreamBackground)
            .drawBehind {
                val step = spacing * density
                val dotRadius = radius * density
                var x = step / 2f
                while (x < size.width) {
                    var y = step / 2f
                    while (y < size.height) {
                        drawCircle(
                            color = dotColor,
                            radius = dotRadius,
                            center = Offset(x, y)
                        )
                        y += step
                    }
                    x += step
                }
            }
    ) {
        content()
    }
}

/**
 * Stat block component matching the 2x2 grid from screen 1.
 */
@Composable
fun StatBlock(
    percentage: String,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    NeoCard(
        modifier = modifier,
        backgroundColor = backgroundColor,
        cornerRadius = 16.dp,
        shadowOffset = 2.5.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = percentage,
                    style = Typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 22.sp
                    ),
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.White.copy(alpha = 0.5f), shape = CircleShape)
                        .border(1.25.dp, BlackInk, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = BlackInk,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = Typography.labelMedium.copy(color = BlackInk)
            )
        }
    }
}
