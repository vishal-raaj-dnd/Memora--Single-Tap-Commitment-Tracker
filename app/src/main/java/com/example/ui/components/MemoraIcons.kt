package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Handshake
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.MemoryType
import com.example.ui.theme.BlackInk

object MemoraIcons {

    fun getCategoryIcon(key: String?): ImageVector {
        return when (key?.lowercase()) {
            "rocket", "hackathon" -> Icons.Outlined.RocketLaunch
            "chat", "discussion" -> Icons.Outlined.Chat
            "science", "incubation", "flask" -> Icons.Outlined.Science
            "school", "amet", "university" -> Icons.Outlined.AccountBalance
            "book", "eduvia" -> Icons.Outlined.MenuBook
            "graduation", "academics" -> Icons.Outlined.School
            "calendar", "iitm", "clock", "time" -> Icons.Outlined.CalendarToday
            "target", "grit" -> Icons.Outlined.TrackChanges
            "document", "mint", "exam" -> Icons.Outlined.Description
            "person", "personal" -> Icons.Outlined.Person
            else -> Icons.Outlined.Label
        }
    }

    fun getMemoryTypeIcon(type: MemoryType): ImageVector {
        return when (type) {
            MemoryType.TASK -> Icons.Outlined.CheckCircle
            MemoryType.EVENT -> Icons.Outlined.CalendarMonth
            MemoryType.DEADLINE -> Icons.Outlined.Timer
            MemoryType.REMINDER -> Icons.Outlined.Notifications
            MemoryType.COMMITMENT -> Icons.Outlined.Handshake
            MemoryType.NOTE -> Icons.Outlined.Description
        }
    }
}

@Composable
fun CategoryLineIcon(
    iconKey: String?,
    modifier: Modifier = Modifier,
    tint: Color = BlackInk,
    size: Dp = 18.dp,
    contentDescription: String? = null
) {
    Icon(
        imageVector = MemoraIcons.getCategoryIcon(iconKey),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size)
    )
}

@Composable
fun MemoryTypeLineIcon(
    type: MemoryType,
    modifier: Modifier = Modifier,
    tint: Color = BlackInk,
    size: Dp = 16.dp,
    contentDescription: String? = null
) {
    Icon(
        imageVector = MemoraIcons.getMemoryTypeIcon(type),
        contentDescription = contentDescription,
        tint = tint,
        modifier = modifier.size(size)
    )
}

data class AvatarOption(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val backgroundColor: Color
)

object AvatarPresets {
    val options = listOf(
        AvatarOption("builder", "Builder", Icons.Outlined.RocketLaunch, com.example.ui.theme.PastelYellow),
        AvatarOption("scholar", "Scholar", Icons.Outlined.School, com.example.ui.theme.PastelMint),
        AvatarOption("researcher", "Researcher", Icons.Outlined.Science, com.example.ui.theme.PastelBlue),
        AvatarOption("creator", "Creator", Icons.Outlined.TrackChanges, com.example.ui.theme.PastelCoral),
        AvatarOption("thinker", "Thinker", Icons.Outlined.Bookmarks, com.example.ui.theme.PastelLavender),
        AvatarOption("individual", "Individual", Icons.Outlined.Person, com.example.ui.theme.PastelPeach)
    )

    fun getOption(id: String): AvatarOption {
        return options.find { it.id.equals(id, ignoreCase = true) } ?: options.first()
    }
}

@Composable
fun UserProfileAvatar(
    avatarKey: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp
) {
    val option = AvatarPresets.getOption(avatarKey)
    Box(
        modifier = modifier
            .size(size)
            .background(option.backgroundColor, CircleShape)
            .border(2.dp, BlackInk, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = option.name,
            tint = BlackInk,
            modifier = Modifier.size(size * 0.55f)
        )
    }
}
