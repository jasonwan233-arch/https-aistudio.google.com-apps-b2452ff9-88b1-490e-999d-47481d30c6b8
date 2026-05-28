package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val SleekColorScheme = darkColorScheme(
    primary = SleekLavender,
    secondary = SleekLightPurple,
    tertiary = SleekGreen,
    background = SleekBg,
    surface = SleekSurface,
    onPrimary = SleekDeepPurple,
    onSecondary = SleekText,
    onTertiary = SleekDeepPurple,
    onBackground = SleekText,
    onSurface = SleekText,
    outline = SleekBorder,
    error = SignalLedRed
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SleekColorScheme,
        typography = Typography,
        content = content
    )
}
