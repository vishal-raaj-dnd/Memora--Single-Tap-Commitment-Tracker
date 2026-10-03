package com.example.ui.screens

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.service.MediaProjectionPermissionActivity
import com.example.ui.components.CommitmentDialog
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoDottedBackground
import com.example.ui.components.VoiceCaptureDialog
import com.example.ui.theme.BlackInk
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.GrayText
import com.example.ui.theme.MainYellow
import com.example.ui.theme.PastelCoral
import com.example.ui.theme.PastelYellow
import com.example.ui.theme.Typography
import com.example.ui.theme.WarmWhite
import com.example.ui.viewmodel.MemoraViewModel

@Composable
fun CaptureScreen(
    viewModel: MemoraViewModel,
    onClose: () -> Unit,
    onNavigateToProcessing: () -> Unit,
    onNavigateToReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFloatingServiceEnabled by viewModel.isFloatingServiceEnabled.collectAsState()
    val categories by viewModel.allCategories.collectAsState()
    val sarvamKey by viewModel.sarvamApiKey.collectAsState()
    var showManualDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }

    if (showVoiceDialog) {
        VoiceCaptureDialog(
            categories = categories,
            onDismiss = { showVoiceDialog = false },
            onParseVoice = { text, engine -> viewModel.parseVoiceInput(text, engine) },
            onTranscribeAudio = { file, engine -> viewModel.transcribeAndParseAudio(file, engine) },
            hasSarvamKey = sarvamKey.isNotBlank(),
            onSaveCommitment = { title, catId, type, date, time, notes, isDeadline, reminder ->
                viewModel.saveVoiceCommitment(
                    title = title,
                    categoryId = catId,
                    type = type,
                    date = date,
                    time = time,
                    notes = notes,
                    isDeadline = isDeadline,
                    reminderTime = reminder,
                    onSaved = {
                        showVoiceDialog = false
                        onClose()
                    }
                )
            }
        )
    }

    if (showManualDialog) {
        CommitmentDialog(
            categories = categories,
            existingItem = null,
            onDismiss = { showManualDialog = false },
            onSave = { title, catId, type, date, time, notes, reminderTime ->
                viewModel.createManualMemory(
                    title = title,
                    categoryId = catId,
                    type = type,
                    date = date,
                    time = time,
                    notes = notes,
                    reminderTime = reminderTime,
                    onSaved = {
                        showManualDialog = false
                        onClose()
                    }
                )
            }
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                try {
                    val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        val source = ImageDecoder.createSource(context.contentResolver, uri)
                        ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            decoder.isMutableRequired = true
                        }
                    } else {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                    if (bitmap != null) {
                        viewModel.startCaptureAndAnalysis(
                            providedBitmap = bitmap,
                            onNavigateToProcessing = onNavigateToProcessing,
                            onNavigateToReview = onNavigateToReview
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    NeoDottedBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Symmetrical, balanced top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .testTag("capture_close_button")
                        .size(42.dp)
                        .background(WarmWhite, CircleShape)
                        .border(1.75.dp, BlackInk, CircleShape)
                        .clip(CircleShape)
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = BlackInk,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "Capture & Add",
                    style = Typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = BlackInk
                    )
                )

                // Spacer for exact horizontal symmetry
                Spacer(modifier = Modifier.size(42.dp))
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Capture Commitments",
                style = Typography.headlineSmall.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 23.sp,
                    color = BlackInk,
                    textAlign = TextAlign.Center
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Speak your schedule, pick screenshots, or add tasks manually.",
                style = Typography.bodyMedium.copy(
                    color = GrayText,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Action 1: Voice Hero Card (Sarvam & Groq AI)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(pulseScale)
                    .clickable { showVoiceDialog = true }
                    .testTag("capture_voice_card")
            ) {
                NeoCard(
                    backgroundColor = MainYellow,
                    cornerRadius = 22.dp,
                    shadowOffset = 3.5.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(WarmWhite, CircleShape)
                                .border(2.dp, BlackInk, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Voice",
                                tint = BlackInk,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Speak Commitment",
                                    style = Typography.titleMedium.copy(
                                        fontWeight = FontWeight.Black,
                                        fontSize = 17.sp,
                                        color = BlackInk
                                    )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .background(BlackInk, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "AI Voice",
                                        style = Typography.labelSmall.copy(
                                            color = MainYellow,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "English, Hindi, or Hinglish · Instant smart parsing",
                                style = Typography.bodySmall.copy(
                                    color = BlackInk.copy(alpha = 0.8f),
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action 2: Screenshot OCR Analyzer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .testTag("capture_main_action_button")
            ) {
                NeoCard(
                    backgroundColor = WarmWhite,
                    cornerRadius = 22.dp,
                    shadowOffset = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(PastelYellow, CircleShape)
                                .border(2.dp, BlackInk, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = "Screenshot",
                                tint = BlackInk,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Analyze Screenshot",
                                style = Typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = BlackInk
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Pick from device gallery · Extracts tasks & dates",
                                style = Typography.bodySmall.copy(
                                    color = GrayText,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action 3: Quick Manual Entry
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showManualDialog = true }
                    .testTag("capture_add_manually_button")
            ) {
                NeoCard(
                    backgroundColor = WarmWhite,
                    cornerRadius = 22.dp,
                    shadowOffset = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(PastelCoral.copy(alpha = 0.45f), CircleShape)
                                .border(2.dp, BlackInk, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = BlackInk,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Add Manually",
                                style = Typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 17.sp,
                                    color = BlackInk
                                )
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Custom commitment, deadline, date & reminder",
                                style = Typography.bodySmall.copy(
                                    color = GrayText,
                                    fontSize = 12.sp
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Floating Button Switch Card (System-wide overlay)
            NeoCard(
                backgroundColor = WarmWhite,
                cornerRadius = 20.dp,
                shadowOffset = 3.dp,
                testTag = "floating_service_card"
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(CreamBackground, CircleShape)
                                .border(1.5.dp, BlackInk, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = "Floating Overlay",
                                tint = BlackInk,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Floating Capture Button",
                                style = Typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Works over WhatsApp, Chrome, Gmail",
                                style = Typography.bodySmall.copy(fontSize = 11.sp, color = GrayText)
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
                                        MediaProjectionPermissionActivity.launch(context)
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = BlackInk,
                                checkedTrackColor = MainYellow,
                                uncheckedThumbColor = GrayText,
                                uncheckedTrackColor = CreamBackground
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Memora draws a draggable button over other apps so you can capture tasks instantly without switching.",
                        style = Typography.bodySmall.copy(fontSize = 12.sp, color = GrayText, lineHeight = 16.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
