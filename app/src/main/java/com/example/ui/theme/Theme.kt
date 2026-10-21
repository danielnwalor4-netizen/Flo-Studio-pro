package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.example.flo.ui.theme.FloCyanAccent
import com.example.flo.ui.theme.FloDarkBg
import com.example.flo.ui.theme.FloDarkCard
import com.example.flo.ui.theme.FloDarkSurface
import com.example.flo.ui.theme.FloDarkSurfaceVariant
import com.example.flo.ui.theme.FloPurpleLight
import com.example.flo.ui.theme.FloPurplePrimary
import com.example.flo.ui.theme.FloTextPrimary
import com.example.flo.ui.theme.FloTextSecondary

private val FloStudioColorScheme = darkColorScheme(
    primary = FloPurplePrimary,
    onPrimary = FloDarkBg,
    primaryContainer = FloDarkCard,
    onPrimaryContainer = FloPurpleLight,
    secondary = FloCyanAccent,
    onSecondary = FloDarkBg,
    tertiary = FloPurpleLight,
    background = FloDarkBg,
    onBackground = FloTextPrimary,
    surface = FloDarkSurface,
    onSurface = FloTextPrimary,
    surfaceVariant = FloDarkSurfaceVariant,
    onSurfaceVariant = FloTextSecondary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    // Flo Studio is a professional dark digital audio workstation
    MaterialTheme(
        colorScheme = FloStudioColorScheme,
        typography = Typography,
        content = content
    )
}
