package com.aquiles.crosschapp.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.aquiles.crosschapp.R

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2C2C2E), // Centro (Gris Áspero)
                        Color(0xFF050505)  // Bordes (Negro Profundo)
                    ),
                    radius = 1500f
                )
            )
    ) {
        // 1. Imagen de fondo (con opacidad para fusionarse con el gradiente)
        Image(
            painter = painterResource(id = R.drawable.fondo_principal), 
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            alpha = 0.4f // Sutil, para dar textura
        )

        // 2. Overlay opcional si se necesita más oscuridad (ya cubierto por el gradiente base)
        
        // 3. Contenido
        content()
    }
}