package com.wren.ide.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Obsidian & Electric Color Palette
val PrimaryObsidian = Color(0xFF0F0F11)      // Deep background
val SecondaryCard = Color(0xFF18181C)        // Container panels
val TextLight = Color(0xFFF1F1F4)            // Off-white primary text
val TextMuted = Color(0xFF8E8E9F)            // Gray for file paths and captions
val ElectricCyan = Color(0xFF00F0FF)         // Active highlights, buttons
val TerminalGreen = Color(0xFF4AF626)        // Terminal logs
val EditorYellow = Color(0xFFFFD700)         // Code annotations
val BorderGray = Color(0xFF2C2C35)           // Subtle structural separators
val ErrorRed = Color(0xFFFF5252)             // Destructive operations alert

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
    // Wren enforces a premium dark theme by default
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
