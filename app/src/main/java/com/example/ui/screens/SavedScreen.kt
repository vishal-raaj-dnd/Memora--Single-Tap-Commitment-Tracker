package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoDottedBackground
import com.example.ui.theme.BlackInk
import com.example.ui.theme.GrayText
import com.example.ui.theme.MainYellow
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.Typography
import com.example.ui.theme.WarmWhite
import com.example.ui.viewmodel.MemoraViewModel

@Composable
fun SavedScreen(
    viewModel: MemoraViewModel,
    onViewItem: (Long) -> Unit,
    onCaptureAnother: () -> Unit,
    onGoHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lastSavedItem by viewModel.lastSavedItem.collectAsState()

    NeoDottedBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Green Circle Checkmark
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(SuccessGreen, CircleShape)
                    .border(2.5.dp, BlackInk, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Success",
                    tint = BlackInk,
                    modifier = Modifier.size(52.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Saved!",
                style = Typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = 32.sp
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Added to your timeline.",
                style = Typography.bodyLarge.copy(
                    color = GrayText,
                    fontSize = 16.sp
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            // View Item Button
            NeoButton(
                text = "View item",
                onClick = {
                    val id = lastSavedItem?.id ?: 1L
                    onViewItem(id)
                },
                backgroundColor = MainYellow,
                testTag = "view_saved_item_button"
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Capture Another Button
            NeoButton(
                text = "Capture another",
                onClick = onCaptureAnother,
                backgroundColor = WarmWhite,
                testTag = "capture_another_button"
            )

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
