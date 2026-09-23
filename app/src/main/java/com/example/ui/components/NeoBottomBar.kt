package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.BlackInk
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.GrayText
import com.example.ui.theme.MainYellow
import com.example.ui.theme.WarmWhite

enum class BottomNavDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    TIMELINE("Timeline", Icons.Outlined.CalendarToday),
    CAPTURE("Capture", Icons.Default.CameraAlt),
    CATEGORIES("Categories", Icons.Outlined.Category),
    SEARCH("Search", Icons.Outlined.Search)
}

@Composable
fun NeoBottomBar(
    currentDestination: BottomNavDestination,
    onNavigate: (BottomNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Offset shadow
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = 3.dp)
                .background(BlackInk, RoundedCornerShape(32.dp))
        )

        // Main navigation pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WarmWhite, RoundedCornerShape(32.dp))
                .border(2.dp, BlackInk, RoundedCornerShape(32.dp))
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavDestination.entries.forEach { destination ->
                val isSelected = destination == currentDestination
                val isCapture = destination == BottomNavDestination.CAPTURE

                if (isCapture) {
                    // Center prominent circular capture button
                    Box(
                        modifier = Modifier
                            .testTag("nav_capture_button")
                            .size(48.dp)
                            .background(MainYellow, CircleShape)
                            .border(2.dp, BlackInk, CircleShape)
                            .clip(CircleShape)
                            .clickable { onNavigate(destination) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Capture",
                            tint = BlackInk,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .then(
                                if (isSelected) {
                                    Modifier.background(MainYellow, RoundedCornerShape(20.dp))
                                } else Modifier
                            )
                            .clickable { onNavigate(destination) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = destination.icon,
                            contentDescription = destination.label,
                            tint = if (isSelected) BlackInk else GrayText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
