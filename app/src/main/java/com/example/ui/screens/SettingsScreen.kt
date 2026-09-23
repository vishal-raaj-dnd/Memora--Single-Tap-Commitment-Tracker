package com.example.ui.screens

import com.example.BuildConfig
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoDottedBackground
import com.example.ui.theme.BlackInk
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.GrayText
import com.example.ui.theme.MainYellow
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.Typography
import com.example.ui.theme.WarmWhite
import com.example.ui.viewmodel.MemoraViewModel

@Composable
fun SettingsScreen(
    viewModel: MemoraViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFloatingServiceEnabled by viewModel.isFloatingServiceEnabled.collectAsState()
    val userApiKey by viewModel.userApiKey.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val userAvatar by viewModel.userAvatar.collectAsState()

    var showKeyDialog by remember { mutableStateOf(false) }
    var tempKey by remember(userApiKey) { mutableStateOf(userApiKey) }

    var showProfileDialog by remember { mutableStateOf(false) }
    var tempName by remember(userName) { mutableStateOf(userName) }
    var tempRole by remember(userRole) { mutableStateOf(userRole) }
    var selectedAvatarKey by remember(userAvatar) { mutableStateOf(userAvatar) }

    val hasConfiguredKey = (userApiKey.isNotBlank() && userApiKey != "MY_GEMINI_API_KEY") ||
            try { BuildConfig.GROQ_API_KEY.isNotBlank() && BuildConfig.GROQ_API_KEY != "MY_GROQ_API_KEY" } catch (_: Exception) { false } ||
            try { BuildConfig.GEMINI_API_KEY.isNotBlank() && BuildConfig.GEMINI_API_KEY != "MY_GEMINI_API_KEY" } catch (_: Exception) { false }

    val activeModelDescription = when {
        userApiKey.startsWith("gsk_") -> "Groq Qwen 27B Vision (0.25s High-Speed)"
        try { BuildConfig.GROQ_API_KEY.startsWith("gsk_") } catch (_: Exception) { false } -> "Groq Qwen 27B Vision (0.25s High-Speed)"
        userApiKey.isNotBlank() -> "Gemini 3.5 Flash Lite VLM"
        else -> "Groq Qwen 27B & Gemini 3.5 Flash Lite"
    }

    if (showKeyDialog) {
        AlertDialog(
            onDismissRequest = { showKeyDialog = false },
            title = {
                Text(
                    text = "Configure AI Vision Key",
                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Paste your Groq API key (starts with 'gsk_') or Google Gemini key (starts with 'AIza...' or 'AQ.'). Free Groq keys can be generated at console.groq.com.",
                        style = Typography.bodySmall.copy(color = GrayText)
                    )
                    OutlinedTextField(
                        value = tempKey,
                        onValueChange = { tempKey = it },
                        label = { Text("AI Vision API Key") },
                        placeholder = { Text("Paste gsk_... or AIza... key here") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = BlackInk,
                            unfocusedIndicatorColor = GrayText
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateApiKey(tempKey.trim())
                        showKeyDialog = false
                    }
                ) {
                    Text("Save Key", fontWeight = FontWeight.Bold, color = BlackInk)
                }
            },
            dismissButton = {
                TextButton(onClick = { showKeyDialog = false }) {
                    Text("Cancel", color = GrayText)
                }
            },
            containerColor = CreamBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showProfileDialog) {
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Text(
                    text = "Customize Profile",
                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Black)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "Choose Avatar",
                        style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = GrayText)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.example.ui.components.AvatarPresets.options.forEach { option ->
                            val isSelected = option.id == selectedAvatarKey
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(option.backgroundColor, CircleShape)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.5.dp,
                                        color = if (isSelected) BlackInk else BlackInk.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    )
                                    .clickable { selectedAvatarKey = option.id },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = option.icon,
                                    contentDescription = option.name,
                                    tint = BlackInk,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = BlackInk,
                            unfocusedIndicatorColor = GrayText
                        )
                    )

                    OutlinedTextField(
                        value = tempRole,
                        onValueChange = { tempRole = it },
                        label = { Text("Role / Headline (e.g. Student, Developer)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = BlackInk,
                            unfocusedIndicatorColor = GrayText
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (tempName.isNotBlank()) {
                            viewModel.updateProfile(tempName.trim(), tempRole.trim(), selectedAvatarKey)
                            showProfileDialog = false
                        }
                    }
                ) {
                    Text("Save Profile", fontWeight = FontWeight.Bold, color = BlackInk)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) {
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(WarmWhite, CircleShape)
                        .border(1.75.dp, BlackInk, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BlackInk
                        )
                    }
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = "Settings",
                    style = Typography.displayMedium.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 24.sp
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Card
                item {
                    NeoCard(
                        backgroundColor = WarmWhite,
                        cornerRadius = 20.dp,
                        shadowOffset = 2.5.dp,
                        onClick = { showProfileDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            com.example.ui.components.UserProfileAvatar(
                                avatarKey = userAvatar,
                                size = 54.dp
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = userName,
                                        style = Typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            fontSize = 18.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit Profile",
                                        tint = GrayText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = userRole,
                                    style = Typography.labelMedium.copy(
                                        color = BlackInk.copy(alpha = 0.75f),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Text(
                                    text = "Tap to customize profile & avatar",
                                    style = Typography.bodySmall.copy(color = GrayText, fontSize = 11.sp)
                                )
                            }
                        }
                    }
                }

                // AI Engine status & API Key Card
                item {
                    NeoCard(
                        backgroundColor = WarmWhite,
                        cornerRadius = 20.dp,
                        shadowOffset = 2.5.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "AI",
                                    tint = BlackInk,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "AI Vision Engine",
                                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }

                            Text(
                                text = "Model: $activeModelDescription",
                                style = Typography.bodyMedium.copy(color = BlackInk, fontWeight = FontWeight.SemiBold)
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(if (hasConfiguredKey) SuccessGreen else MainYellow, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (hasConfiguredKey) "AI Vision Active (Ready to Analyze)" else "API Key Missing (Local Fallback)",
                                    style = Typography.bodySmall.copy(
                                        color = if (hasConfiguredKey) BlackInk else GrayText,
                                        fontWeight = if (hasConfiguredKey) FontWeight.Bold else FontWeight.Normal
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            NeoButton(
                                text = if (hasConfiguredKey) "Change AI Vision Key" else "Set AI Vision Key",
                                onClick = { showKeyDialog = true },
                                backgroundColor = if (hasConfiguredKey) WarmWhite else MainYellow,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = BlackInk,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Capture overlay setting
                item {
                    NeoCard(
                        backgroundColor = WarmWhite,
                        cornerRadius = 20.dp,
                        shadowOffset = 2.5.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Layers,
                                    contentDescription = "Floating Button",
                                    tint = BlackInk,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Floating Capture Button",
                                        style = Typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Shows a small yellow button over other apps",
                                        style = Typography.bodySmall.copy(color = GrayText)
                                    )
                                }
                                Switch(
                                    checked = isFloatingServiceEnabled,
                                    onCheckedChange = {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                        } else {
                                            if (isFloatingServiceEnabled) {
                                                viewModel.toggleFloatingService(context)
                                            } else {
                                                com.example.service.MediaProjectionPermissionActivity.launch(context)
                                            }
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = BlackInk,
                                        checkedTrackColor = MainYellow
                                    )
                                )
                            }
                        }
                    }
                }

                // Privacy & Local Storage
                item {
                    NeoCard(
                        backgroundColor = WarmWhite,
                        cornerRadius = 20.dp,
                        shadowOffset = 2.5.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Privacy",
                                    tint = BlackInk,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Privacy & Local Ownership",
                                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                            Text(
                                text = "• Original screenshots are stored permanently on your device.\n• Metadata and AI extractions are kept in your local Room database.\n• Custom category corrections are preserved locally to personalize future analysis.",
                                style = Typography.bodySmall.copy(color = GrayText, lineHeight = 18.sp)
                            )
                        }
                    }
                }

                // Reset Database to Clean State
                item {
                    var showResetConfirm by remember { mutableStateOf(false) }
                    if (showResetConfirm) {
                        AlertDialog(
                            onDismissRequest = { showResetConfirm = false },
                            title = { Text("Reset to Clean State", fontWeight = FontWeight.Black) },
                            text = { Text("This will permanently remove all stored memories and reset the app to an empty state. Are you sure?") },
                            confirmButton = {
                                TextButton(onClick = {
                                    viewModel.clearAllData()
                                    showResetConfirm = false
                                }) {
                                    Text("Reset Everything", color = androidx.compose.ui.graphics.Color.Red, fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showResetConfirm = false }) {
                                    Text("Cancel", color = GrayText)
                                }
                            },
                            containerColor = CreamBackground,
                            shape = RoundedCornerShape(20.dp)
                        )
                    }

                    NeoCard(
                        backgroundColor = WarmWhite,
                        cornerRadius = 20.dp,
                        shadowOffset = 2.5.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Clean State Management",
                                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Completely wipe local data and reset to a clean slate with 0 memories.",
                                style = Typography.bodySmall.copy(color = GrayText)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            NeoButton(
                                text = "Reset Database to Empty",
                                onClick = { showResetConfirm = true },
                                backgroundColor = WarmWhite
                            )
                        }
                    }
                }
            }
        }
    }
}
