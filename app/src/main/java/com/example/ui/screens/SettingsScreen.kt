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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoDottedBackground
import com.example.ui.theme.BlackInk
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.GrayText
import com.example.ui.theme.LightGray
import com.example.ui.theme.MainYellow
import com.example.ui.theme.PastelCoral
import com.example.ui.theme.PastelMint
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.Typography
import com.example.ui.theme.WarmWhite
import com.example.ui.viewmodel.MemoraViewModel

private fun maskApiKey(key: String, prefixLen: Int = 4, suffixLen: Int = 4): String {
    val trimmed = key.trim()
    if (trimmed.isBlank() || trimmed == "MY_GROQ_API_KEY" || trimmed == "MY_SARVAM_API_KEY" || trimmed == "MY_GEMINI_API_KEY") {
        return "Not configured"
    }
    if (trimmed.length <= prefixLen + suffixLen) {
        return "••••••••"
    }
    return "${trimmed.take(prefixLen)}••••••••${trimmed.takeLast(suffixLen)}"
}

@Composable
fun SettingsScreen(
    viewModel: MemoraViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isFloatingServiceEnabled by viewModel.isFloatingServiceEnabled.collectAsState()
    val userApiKey by viewModel.userApiKey.collectAsState()
    val groqApiKey by viewModel.groqApiKey.collectAsState()
    val sarvamApiKey by viewModel.sarvamApiKey.collectAsState()
    val userName by viewModel.userName.collectAsState()
    val userRole by viewModel.userRole.collectAsState()
    val userAvatar by viewModel.userAvatar.collectAsState()

    var showGroqDialog by remember { mutableStateOf(false) }
    var tempGroqKey by remember(groqApiKey) { mutableStateOf(groqApiKey) }
    var isGroqKeyVisible by remember { mutableStateOf(false) }

    var showSarvamDialog by remember { mutableStateOf(false) }
    var tempSarvamKey by remember(sarvamApiKey) { mutableStateOf(sarvamApiKey) }
    var isSarvamKeyVisible by remember { mutableStateOf(false) }

    var showProfileDialog by remember { mutableStateOf(false) }
    var tempName by remember(userName) { mutableStateOf(userName) }
    var tempRole by remember(userRole) { mutableStateOf(userRole) }
    var selectedAvatarKey by remember(userAvatar) { mutableStateOf(userAvatar) }

    val hasGroqKey = (groqApiKey.isNotBlank() && groqApiKey != "MY_GROQ_API_KEY") ||
            (userApiKey.startsWith("gsk_") && userApiKey != "MY_GROQ_API_KEY")
    val hasSarvamKey = sarvamApiKey.isNotBlank() && sarvamApiKey != "MY_SARVAM_API_KEY"

    if (showGroqDialog) {
        AlertDialog(
            onDismissRequest = { showGroqDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MainYellow, CircleShape)
                            .border(1.5.dp, BlackInk, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = BlackInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Groq Cloud API Key",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Black)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Powers instant 0.25s screenshot analysis using Qwen 27B Vision. Free API keys can be generated at console.groq.com/keys.",
                        style = Typography.bodySmall.copy(color = GrayText, lineHeight = 18.sp)
                    )

                    OutlinedTextField(
                        value = tempGroqKey,
                        onValueChange = { tempGroqKey = it },
                        label = { Text("Groq Key (starts with gsk_)") },
                        placeholder = { Text("gsk_...") },
                        visualTransformation = if (isGroqKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isGroqKeyVisible = !isGroqKeyVisible }) {
                                Icon(
                                    imageVector = if (isGroqKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isGroqKeyVisible) "Hide key" else "Show key",
                                    tint = BlackInk
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = BlackInk,
                            unfocusedIndicatorColor = GrayText
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Paste from Clipboard Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(WarmWhite, RoundedCornerShape(12.dp))
                                .border(1.5.dp, BlackInk, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        tempGroqKey = clip.trim()
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = BlackInk,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Paste Clipboard",
                                    style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = BlackInk)
                                )
                            }
                        }

                        // Clear Button
                        if (tempGroqKey.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(PastelCoral, RoundedCornerShape(12.dp))
                                    .border(1.5.dp, BlackInk, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { tempGroqKey = "" }
                                    .padding(vertical = 10.dp, horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear",
                                    tint = BlackInk,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val key = tempGroqKey.trim()
                        if (key.isBlank()) {
                            viewModel.clearGroqApiKey()
                        } else {
                            viewModel.updateGroqApiKey(key)
                        }
                        showGroqDialog = false
                    }
                ) {
                    Text("Save Key", fontWeight = FontWeight.Bold, color = BlackInk)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGroqDialog = false }) {
                    Text("Cancel", color = GrayText)
                }
            },
            containerColor = CreamBackground,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showSarvamDialog) {
        AlertDialog(
            onDismissRequest = { showSarvamDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(PastelMint, CircleShape)
                            .border(1.5.dp, BlackInk, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = BlackInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Sarvam AI API Key",
                        style = Typography.titleLarge.copy(fontWeight = FontWeight.Black)
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Powers Indic intelligence and multilingual refinement (Hindi, Tamil, Telugu, Hinglish, college slang, IITM, AMET, Anna Univ, and hackathon schedules). Get your subscription key from dashboard.sarvam.ai.",
                        style = Typography.bodySmall.copy(color = GrayText, lineHeight = 18.sp)
                    )

                    OutlinedTextField(
                        value = tempSarvamKey,
                        onValueChange = { tempSarvamKey = it },
                        label = { Text("Sarvam Subscription Key") },
                        placeholder = { Text("Paste Sarvam key here") },
                        visualTransformation = if (isSarvamKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { isSarvamKeyVisible = !isSarvamKeyVisible }) {
                                Icon(
                                    imageVector = if (isSarvamKeyVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isSarvamKeyVisible) "Hide key" else "Show key",
                                    tint = BlackInk
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = BlackInk,
                            unfocusedIndicatorColor = GrayText
                        )
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Paste from Clipboard Button
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(WarmWhite, RoundedCornerShape(12.dp))
                                .border(1.5.dp, BlackInk, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        tempSarvamKey = clip.trim()
                                    }
                                }
                                .padding(vertical = 10.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = null,
                                    tint = BlackInk,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Paste Clipboard",
                                    style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = BlackInk)
                                )
                            }
                        }

                        // Clear Button
                        if (tempSarvamKey.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .background(PastelCoral, RoundedCornerShape(12.dp))
                                    .border(1.5.dp, BlackInk, RoundedCornerShape(12.dp))
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { tempSarvamKey = "" }
                                    .padding(vertical = 10.dp, horizontal = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear",
                                    tint = BlackInk,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val key = tempSarvamKey.trim()
                        if (key.isBlank()) {
                            viewModel.clearSarvamApiKey()
                        } else {
                            viewModel.updateSarvamApiKey(key)
                        }
                        showSarvamDialog = false
                    }
                ) {
                    Text("Save Key", fontWeight = FontWeight.Bold, color = BlackInk)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSarvamDialog = false }) {
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

                // AI Intelligence Section Header
                item {
                    Text(
                        text = "AI INTELLIGENCE ENGINES",
                        style = Typography.labelLarge.copy(
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = GrayText
                        ),
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                    )
                }

                // AI Pipeline Status Card
                item {
                    NeoCard(
                        backgroundColor = if (hasGroqKey && hasSarvamKey) MainYellow else if (hasGroqKey || hasSarvamKey) PastelMint else WarmWhite,
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
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(BlackInk, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MainYellow,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = when {
                                        hasGroqKey && hasSarvamKey -> "Dual AI Intelligence Active"
                                        hasGroqKey -> "Groq High-Speed Vision Active"
                                        hasSarvamKey -> "Sarvam Indic Intelligence Active"
                                        else -> "Heuristic Local Fallback Active"
                                    },
                                    style = Typography.titleMedium.copy(fontWeight = FontWeight.Black)
                                )
                            }
                            Text(
                                text = when {
                                    hasGroqKey && hasSarvamKey ->
                                        "⚡ 0.25s Groq VLM screenshot parsing coupled with 🇮🇳 Sarvam AI multilingual refinement (Hindi/Tamil/Telugu/Hinglish & college jargon)."
                                    hasGroqKey ->
                                        "⚡ Groq Qwen 27B Vision extracts commitments at 0.25s speed. Add Sarvam AI key below for Indic & college slang refinement."
                                    hasSarvamKey ->
                                        "🇮🇳 Sarvam AI Indic refinement active. Add a free Groq Cloud key below for instant 0.25s screenshot analysis."
                                    else ->
                                        "Add your Groq and Sarvam API keys below to unlock sub-second vision extraction and Indic college commitment tracking."
                                },
                                style = Typography.bodySmall.copy(color = BlackInk.copy(alpha = 0.85f), lineHeight = 18.sp)
                            )
                        }
                    }
                }

                // Groq Cloud VLM Card
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(MainYellow, CircleShape)
                                            .border(1.5.dp, BlackInk, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Bolt,
                                            contentDescription = null,
                                            tint = BlackInk,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Groq Cloud VLM",
                                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Black)
                                        )
                                        Text(
                                            text = "qwen/qwen3.8-27b (0.25s VLM)",
                                            style = Typography.bodySmall.copy(color = GrayText, fontSize = 11.sp)
                                        )
                                    }
                                }

                                // Status Badge
                                Box(
                                    modifier = Modifier
                                        .background(if (hasGroqKey) SuccessGreen.copy(alpha = 0.2f) else PastelCoral, RoundedCornerShape(8.dp))
                                        .border(1.dp, if (hasGroqKey) SuccessGreen else BlackInk, RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(if (hasGroqKey) SuccessGreen else BlackInk, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (hasGroqKey) "0.25s Active" else "Not Set",
                                            style = Typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = BlackInk,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }

                            // Masked Key Preview Pill
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CreamBackground, RoundedCornerShape(10.dp))
                                    .border(1.dp, BlackInk.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Key: ${maskApiKey(groqApiKey)}",
                                        style = Typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (hasGroqKey) BlackInk else GrayText
                                        )
                                    )
                                    if (hasGroqKey) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Active",
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "High-speed vision analyzer for extracting dates, deadlines, tasks, and events with 250ms latency.",
                                style = Typography.bodySmall.copy(color = GrayText, lineHeight = 16.sp)
                            )

                            NeoButton(
                                text = if (hasGroqKey) "Update Groq Key" else "Configure Groq API Key",
                                onClick = {
                                    tempGroqKey = groqApiKey
                                    showGroqDialog = true
                                },
                                backgroundColor = if (hasGroqKey) WarmWhite else MainYellow,
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

                // Sarvam AI Indic Engine Card
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(PastelMint, CircleShape)
                                            .border(1.5.dp, BlackInk, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Translate,
                                            contentDescription = null,
                                            tint = BlackInk,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "Sarvam AI (Indic Engine)",
                                            style = Typography.titleMedium.copy(fontWeight = FontWeight.Black)
                                        )
                                        Text(
                                            text = "sarvam-2b (Indic / Multilingual)",
                                            style = Typography.bodySmall.copy(color = GrayText, fontSize = 11.sp)
                                        )
                                    }
                                }

                                // Status Badge
                                Box(
                                    modifier = Modifier
                                        .background(if (hasSarvamKey) SuccessGreen.copy(alpha = 0.2f) else LightGray, RoundedCornerShape(8.dp))
                                        .border(1.dp, if (hasSarvamKey) SuccessGreen else BlackInk.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(if (hasSarvamKey) SuccessGreen else GrayText, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (hasSarvamKey) "Indic Active" else "Optional",
                                            style = Typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = BlackInk,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }

                            // Masked Key Preview Pill
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(CreamBackground, RoundedCornerShape(10.dp))
                                    .border(1.dp, BlackInk.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Key: ${maskApiKey(sarvamApiKey)}",
                                        style = Typography.bodySmall.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (hasSarvamKey) BlackInk else GrayText
                                        )
                                    )
                                    if (hasSarvamKey) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Active",
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "Refines Indian college jargon (IITM, AMET, Anna Univ), hackathons, and Indic languages (Hindi, Tamil, Telugu, Hinglish).",
                                style = Typography.bodySmall.copy(color = GrayText, lineHeight = 16.sp)
                            )

                            NeoButton(
                                text = if (hasSarvamKey) "Update Sarvam Key" else "Configure Sarvam API Key",
                                onClick = {
                                    tempSarvamKey = sarvamApiKey
                                    showSarvamDialog = true
                                },
                                backgroundColor = if (hasSarvamKey) WarmWhite else PastelMint,
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
