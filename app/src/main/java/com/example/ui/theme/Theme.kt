package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val MemoraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val MemoraColorScheme = lightColorScheme(
    primary = MainYellow,
    onPrimary = BlackInk,
    primaryContainer = SecondaryYellow,
    onPrimaryContainer = BlackInk,
    secondary = PastelBlue,
    onSecondary = BlackInk,
    secondaryContainer = PastelMint,
    onSecondaryContainer = BlackInk,
    tertiary = PastelCoral,
    onTertiary = BlackInk,
    background = CreamBackground,
    onBackground = BlackInk,
    surface = WarmWhite,
    onSurface = BlackInk,
    surfaceVariant = PureCream,
    onSurfaceVariant = BlackInk,
    outline = BlackInk,
    outlineVariant = LightGray
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Memora is strictly designed with warm cream editorial identity
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MemoraColorScheme,
        typography = Typography,
        shapes = MemoraShapes,
        content = content
    )
}
