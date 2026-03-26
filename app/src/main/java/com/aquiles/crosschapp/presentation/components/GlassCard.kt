package com.aquiles.crosschapp.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// Premium Glass Effect Colors - Dual tone approach
private val GlassSurfaceStart = Color(0xFFFFFFFF).copy(alpha = 0.22f) // Un poco más sólido para estructura
private val GlassSurfaceEnd = Color(0xFF000000).copy(alpha = 0.25f)   // Base más oscura
private val GlassBorderLight = Color.White.copy(alpha = 0.35f)
private val GlassBorderDark = Color.Black.copy(alpha = 0.22f)

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderBrush: Brush? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val defaultBorderBrush = Brush.linearGradient(
        colors = listOf(GlassBorderLight, GlassBorderDark),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GlassSurfaceStart,
                        Color(0xFF1C1C1E).copy(alpha = 0.82f), // Aumentada opacidad (era 0.56f)
                        GlassSurfaceEnd
                    ),
                    radius = 600f
                ),
                shape = shape
            )
            .border(
                border = BorderStroke(
                    width = 1.2.dp,
                    brush = borderBrush ?: defaultBorderBrush
                ),
                shape = shape
            )
    ) {
        content()
    }
}

// Modifier animado con press scale
@Composable
fun Modifier.glassmorphicInteractive(
    onClick: () -> Unit = {}
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "glassScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = ripple(color = Color.White.copy(alpha = 0.5f)),
            onClick = onClick
        )
}




// Variant specifically for Clickable Cards (if needed separate)
@Composable
fun GlassCardSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
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
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlassSurfaceStart,
                            Color(0xFF1C1C1E).copy(alpha = 0.82f), // Aumentada opacidad (era 0.56f)
                            GlassSurfaceEnd
                        ),
                        radius = 600f
                    )
                )
                .border(
                    border = BorderStroke(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(GlassBorderLight, GlassBorderDark),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    ),
                    shape = shape
                )
        ) {
            content()
        }
    }
}

// Dialog / Popup con estilo Glassmorfismo
@Composable
fun GlassAlertDialog(
    onDismissRequest: () -> Unit,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismissRequest) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlassSurfaceStart,
                            Color(0xFF1C1C1E).copy(alpha = 0.82f), // Aumentada opacidad (era 0.56f)
                            GlassSurfaceEnd
                        ),
                        radius = 600f
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .border(
                    border = BorderStroke(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(GlassBorderLight, GlassBorderDark),
                            start = androidx.compose.ui.geometry.Offset(0f, 0f),
                            end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                        )
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier.padding(24.dp)
            ) {
                if (title != null) {
                    Box(modifier = Modifier.padding(bottom = 16.dp)) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.material3.LocalTextStyle provides MaterialTheme.typography.headlineSmall.copy(
                                color = Color.White
                            )
                        ) {
                            title()
                        }
                    }
                }
                if (text != null) {
                    Box(modifier = Modifier.padding(bottom = 24.dp)) {
                        androidx.compose.runtime.CompositionLocalProvider(
                            androidx.compose.material3.LocalTextStyle provides MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        ) {
                            text()
                        }
                    }
                }
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End
                ) {
                    if (dismissButton != null) {
                        dismissButton()
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    }
                    confirmButton()
                }
            }
        }
    }
}
