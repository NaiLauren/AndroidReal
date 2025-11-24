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
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Tu imagen de fondo original
        Image(
            painter = painterResource(id = R.drawable.fondo_principal), // Nombre de tu fondo
            contentDescription = "Fondo de la aplicación",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. LA CAPA DE OSCURECIMIENTO (El truco para mejorar el contraste)
        // Este Box se dibuja encima de la imagen pero debajo del contenido.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    // Creamos un degradado radial sutil
                    Brush.radialGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f), // El centro es un 40% oscuro
                            Color.Black.copy(alpha = 0.7f)  // Los bordes son un 70% oscuros
                        ),
                        radius = 1200f // Un radio grande para que el degradado sea muy suave
                    )
                )
        )

        // 3. El contenido de tu app (tus pantallas) se dibuja encima de todo
        content()
    }
}