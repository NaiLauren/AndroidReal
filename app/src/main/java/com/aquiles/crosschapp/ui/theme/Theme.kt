package com.aquiles.crosschapp.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat


private val AppDarkColorScheme = darkColorScheme(
    primary = BrandOrange,
    onPrimary = NeutralWhite,
    secondary = NeutralLightGrey,
    onSecondary = NeutralBlack,
    primaryContainer = BrandOrange,
    onPrimaryContainer = NeutralWhite,
    secondaryContainer = NeutralMediumGrey,
    onSecondaryContainer = NeutralWhite,
    error = ErrorRed,
    onError = NeutralWhite,
    background = NeutralBlack,
    onBackground = NeutralWhite,
    surface = NeutralDarkGrey,
    onSurface = NeutralWhite,
    surfaceVariant = NeutralMediumGrey,
    onSurfaceVariant = NeutralLightGrey,
    outline = NeutralMediumGrey,
    surfaceTint = BrandOrange
)

@Composable
fun CrossChAppTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    overridePrimaryColor: Color? = null, // NEW: Dynamic Theming
    content: @Composable () -> Unit
) {
    // Si hay un color personalizado, creamos un esquema en base a él
    // Si no, usamos el por defecto (Orange)
    val colorScheme = if (overridePrimaryColor != null) {
        darkColorScheme(
            primary = overridePrimaryColor,
            onPrimary = NeutralWhite,
            secondary = NeutralLightGrey,
            onSecondary = NeutralBlack,
            primaryContainer = overridePrimaryColor,
            onPrimaryContainer = NeutralWhite,
            secondaryContainer = NeutralMediumGrey,
            onSecondaryContainer = NeutralWhite,
            error = ErrorRed,
            onError = NeutralWhite,
            background = NeutralBlack,
            onBackground = NeutralWhite,
            surface = NeutralDarkGrey,
            onSurface = NeutralWhite,
            surfaceVariant = NeutralMediumGrey,
            onSurfaceVariant = NeutralLightGrey,
            outline = NeutralMediumGrey,
            surfaceTint = overridePrimaryColor
        )
    } else {
        AppDarkColorScheme
    }

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            window.navigationBarColor = Color.Transparent.toArgb()


            // Nos aseguramos de que los iconos de AMBAS barras sean claros
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}