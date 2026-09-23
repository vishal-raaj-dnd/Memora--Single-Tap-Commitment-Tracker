package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoDottedBackground
import com.example.ui.components.ScreenshotImage
import com.example.ui.theme.BlackInk
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.GrayText
import com.example.ui.theme.MainYellow
import com.example.ui.theme.PastelCoral
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.Typography
import com.example.ui.theme.WarmWhite
import com.example.ui.viewmodel.MemoraViewModel

@Composable
fun ProcessingScreen(
    viewModel: MemoraViewModel,
    onBack: () -> Unit,
    onNavigateToReview: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val analysisStep by viewModel.analysisStep.collectAsState()
    val extracted by viewModel.currentExtracted.collectAsState()
    val analysisFailed by viewModel.analysisFailed.collectAsState()

    val steps = listOf(
        "Extracting text",
        "Identifying key information",
        "Finding date & context",
        "Suggesting category",
        "Almost there..."
    )

    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val spinAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinAngle"
    )

    NeoDottedBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
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
                    text = if (analysisFailed) "Processing Result" else "Analyzing your screenshot...",
                    style = Typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Screenshot preview card (Never lost)
            NeoCard(
                backgroundColor = WarmWhite,
                cornerRadius = 20.dp,
                shadowOffset = 3.dp,
                testTag = "processing_screenshot_preview"
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    ScreenshotImage(
                        screenshotUri = extracted?.originalScreenshotUri,
                        aspectRatio = 1.3f
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (analysisFailed) {
                // Analysis Failure Fallback Card (Specification 7)
                NeoCard(
                    backgroundColor = WarmWhite,
                    cornerRadius = 20.dp,
                    shadowOffset = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(PastelCoral, CircleShape)
                                .border(1.75.dp, BlackInk, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = BlackInk,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Could not understand this screenshot.",
                            style = Typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "The original screenshot is securely stored. You can retry or save it directly as a note.",
                            style = Typography.bodySmall.copy(
                                color = GrayText,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            NeoButton(
                                text = "Try Again",
                                onClick = { viewModel.retryAnalysis(onNavigateToReview) },
                                backgroundColor = WarmWhite,
                                modifier = Modifier.weight(1f)
                            )

                            NeoButton(
                                text = "Save Screenshot",
                                onClick = { viewModel.saveScreenshotOnly(onNavigateToReview) },
                                backgroundColor = MainYellow,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                // Checklist steps card
                NeoCard(
                    backgroundColor = WarmWhite,
                    cornerRadius = 20.dp,
                    shadowOffset = 3.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        steps.forEachIndexed { index, stepText ->
                            val isDone = index < analysisStep
                            val isCurrent = index == analysisStep

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            when {
                                                isDone -> SuccessGreen
                                                isCurrent -> MainYellow
                                                else -> CreamBackground
                                            },
                                            CircleShape
                                        )
                                        .border(1.5.dp, BlackInk, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        isDone -> {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Done",
                                                tint = BlackInk,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        isCurrent -> {
                                            CircularProgressIndicator(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .rotate(spinAngle),
                                                strokeWidth = 2.dp,
                                                color = BlackInk
                                            )
                                        }
                                        else -> {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .background(GrayText.copy(alpha = 0.5f), CircleShape)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Text(
                                    text = stepText,
                                    style = Typography.bodyLarge.copy(
                                        fontWeight = if (isDone || isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isDone || isCurrent) BlackInk else GrayText,
                                        fontSize = 15.sp
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
