package com.wren.ide.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryObsidian = Color(0xFF050607)
val SecondaryCard = Color(0xFF101215)
val TextLight = Color(0xFFF3F5F7)
val TextMuted = Color(0xFF9CA3AF)
val ElectricCyan = Color(0xFF27E7FF)
val TerminalGreen = Color(0xFF3CE57B)
val EditorYellow = Color(0xFFFFD36A)
val BorderGray = Color(0xFF232833)
val ErrorRed = Color(0xFFFF6474)

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = PrimaryObsidian,
    secondary = SecondaryCard,
    onSecondary = TextLight,
    background = PrimaryObsidian,
    onBackground = TextLight,
    surface = SecondaryCard,
    onSurface = TextLight,
    error = ErrorRed,
    onError = TextLight
)

@Composable
fun WrenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
