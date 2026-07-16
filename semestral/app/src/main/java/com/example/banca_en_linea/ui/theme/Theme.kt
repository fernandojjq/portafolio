package com.example.banca_en_linea.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = VerdeUTPB80,
    onPrimary = Color(0xFF00390D),
    secondary = VerdeClaro80,
    tertiary = VerdeAcento80,
    background = FondoOscuro,
    surface = FondoOscuro,
)

private val LightColorScheme = lightColorScheme(
    primary = VerdeUTPB40,
    onPrimary = Color.White,
    primaryContainer = VerdeContenedor,
    onPrimaryContainer = Color(0xFF052E0A),
    secondary = VerdeClaro40,
    onSecondary = Color.White,
    tertiary = VerdeAcento40,
    background = FondoClaro,
    surface = Color.White,
)

@Composable
fun Banca_En_LineaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // dynamicColor desactivado a propósito: en Android 12+ el "dynamic color"
    // toma los colores del wallpaper del usuario y taparía nuestra identidad
    // verde UTPB. Para una app de marca queremos colores fijos.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
