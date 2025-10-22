package com.example.pumpcontrol.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    //primary = Color(0xFF00897B),           // Verde Hidroponía
    primary = Color(0xFF00796B),           // Verde institucional
    onPrimary = Color.White,
    secondary = Color(0xFF4DB6AC),
    onSecondary = Color.Black,
    background = Color(0xFFE7EEEB),
    onBackground = Color.Black,
    surface = Color(0xFFF3E9E9),
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE0F2F1),  // Fondo de diálogos, tarjetas, etc.
    onSurfaceVariant = Color(0xFF004D40)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4DB6AC),
    onPrimary = Color.Black,
    secondary = Color(0xFF00897B),
    onSecondary = Color.White,
    background = Color(0xFF121212),
    onBackground = Color.White,
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF37474F),
    onSurfaceVariant = Color(0xFFB2DFDB)
)

@Composable
fun PumpControlPumpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    //dynamicColor: Boolean = true, // <-- Habilitar dynamic
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,  // Puedes definir uno personalizado aquí
        content = content
    )
}
