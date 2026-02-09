package com.aquiles.crosschapp.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Colors mimicking iOS "Ultra Thin Material" Dark Mode
private val GlassSurfaceStart = Color(0xFF252525).copy(alpha = 0.70f)
private val GlassSurfaceEnd = Color(0xFF151515).copy(alpha = 0.85f)
private val GlassBorderStart = Color.White.copy(alpha = 0.15f)
private val GlassBorderEnd = Color.White.copy(alpha = 0.05f)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(GlassSurfaceStart, GlassSurfaceEnd)
                ),
                shape = shape
            )
            .border(
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        colors = listOf(GlassBorderStart, GlassBorderEnd)
                    )
                ),
                shape = shape
            )
    ) {
        content()
    }
}

// Variant specifically for Clickable Cards (if needed separate)
@Composable
fun GlassCardSurface(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent), // We handle background manually
        border = null // We handle border manually
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(GlassSurfaceStart, GlassSurfaceEnd)
                    )
                )
                .border(
                    border = BorderStroke(
                        width = 1.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(GlassBorderStart, GlassBorderEnd)
                        )
                    ),
                    shape = shape
                )
        ) {
            content()
        }
    }
}
