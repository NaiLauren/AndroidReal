// RUTA: presentation/VideoSplashScreen.kt
// VERSIÓN DINÁMICA CON VIEWMODEL

package com.aquiles.crosschapp.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aquiles.crosschapp.R
import com.aquiles.crosschapp.presentation.viewmodel.DynamicSplashViewModel
import com.aquiles.crosschapp.presentation.viewmodel.SplashState
import kotlinx.coroutines.delay

@UnstableApi
@Composable
fun VideoSplashScreen(
    onVideoEnded: () -> Unit,
    viewModel: DynamicSplashViewModel = viewModel() // <-- CAMBIO 1: Añadir ViewModel
) {
    val context = LocalContext.current
    val splashState by viewModel.splashState.collectAsState()

    // Lanzamos la carga de la URL solo una vez
    LaunchedEffect(Unit) {
        viewModel.loadVideoUrl()
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black), // Fondo negro por defecto
        contentAlignment = Alignment.Center
    ) {
        when (val state = splashState) {
            is SplashState.Loading, SplashState.Idle -> {
                // Muestra un indicador de carga mientras busca la URL
                CircularProgressIndicator()
            }
            // Si hay un error o no se encuentra la URL, navega directamente.
            is SplashState.Error, is SplashState.Success -> {
                val videoUrl = (state as? SplashState.Success)?.videoUrl

                // Si no hay URL, esperamos un instante y navegamos.
                if (videoUrl.isNullOrBlank()) {
                    LaunchedEffect(Unit) {
                        delay(500) // Pequeña pausa para evitar un salto brusco
                        onVideoEnded()
                    }
                } else {
                    // Si hay URL, mostramos el reproductor de video.
                    VideoPlayer(
                        videoUrl = videoUrl,
                        onVideoEnded = onVideoEnded
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoPlayer(
    videoUrl: String,
    onVideoEnded: () -> Unit
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Usa la URL dinámica obtenida del ViewModel
            val mediaItem = MediaItem.fromUri(videoUrl.toUri())
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onVideoEnded()
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        factory = {
            PlayerView(it).apply {
                player = exoPlayer
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}