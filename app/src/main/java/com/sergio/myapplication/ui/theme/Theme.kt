package com.sergio.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AppColorScheme = darkColorScheme(
    primary = Color(0xFF5b9bd5),
    onPrimary = Color(0xFFe8f4ff),
    secondary = Color(0xFF1a4a8a),
    onSecondary = Color(0xFFe8f4ff),
    background = Color(0xFF0a1628),
    onBackground = Color(0xFFe8f4ff),
    surface = Color(0xFF0f1e35),
    onSurface = Color(0xFFe8f4ff),
    error = Color(0xFFe24b4a),
    onError = Color.White
)

@Composable
fun FishStockTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        content = content
    )
}