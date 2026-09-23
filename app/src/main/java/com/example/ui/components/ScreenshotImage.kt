package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.ui.theme.BlackInk
import com.example.ui.theme.GrayText
import com.example.ui.theme.WarmWhite
import java.io.File

/**
 * High-performance hardware-accelerated screenshot renderer.
 * Strictly zero mock data: renders the actual captured screenshot file.
 * If no image exists, displays a minimal neo-brutalist camera placeholder.
 */
@Composable
fun ScreenshotImage(
    screenshotUri: String?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    aspectRatio: Float = 1.35f
) {
    val context = LocalContext.current
    val shape = RoundedCornerShape(cornerRadius)

    val file = if (!screenshotUri.isNullOrBlank()) File(screenshotUri) else null
    val hasValidFile = file != null && file.exists()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .background(WarmWhite, shape)
            .border(2.dp, BlackInk, shape)
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        if (hasValidFile) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(file)
                    .crossfade(200)
                    .build(),
                contentDescription = "Original Screenshot Evidence",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = GrayText,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Original Screenshot Evidence",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = GrayText
                )
            }
        }
    }
}
