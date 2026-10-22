package com.angel.fitgains.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val ProteinColor = Color(0xFFE57373)
val CarbsColor = Color(0xFF64B5F6)
val FatColor = Color(0xFFFFB74D)

private val Light = lightColorScheme(
    primary = Color(0xFF2E7D32),
    secondary = Color(0xFF558B2F),
    tertiary = Color(0xFF00897B),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF81C784),
    secondary = Color(0xFFAED581),
    tertiary = Color(0xFF4DB6AC),
)

@Composable
fun FitGainsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content,
    )
}
