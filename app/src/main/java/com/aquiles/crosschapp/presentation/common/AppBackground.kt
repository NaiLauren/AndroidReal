package com.aquiles.crosschapp.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.aquiles.crosschapp.R
import com.aquiles.crosschapp.presentation.viewmodel.UserSession

@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // 0. Leemos la configuración del Gimnasio actual desde la Sesión
    val currentGym by UserSession.currentGym.collectAsState()
    val backgroundUrl = currentGym?.backgroundUrl

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
        // 1. Cargamos el fondo dinámico del Gym, o el oscuro por defecto
        if (!backgroundUrl.isNullOrBlank()) {
            coil.compose.AsyncImage(
                model = backgroundUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.6f // Mantener este nivel de transparencia para legibilidad
            )
        } else {
            Image(
                painter = painterResource(id = R.drawable.fondog),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                alpha = 0.4f
            )
        }

        // 3. Contenido de las pantallas encima
        content()
    }
}