@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)

package com.aquiles.crosschapp.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.aquiles.crosschapp.presentation.viewmodel.DynamicSplashViewModel
import com.aquiles.crosschapp.presentation.viewmodel.SplashState
import kotlinx.coroutines.delay

@Composable
fun VideoSplashScreen(
    onVideoEnded: () -> Unit,
    viewModel: DynamicSplashViewModel = viewModel(),
    canProceed: Boolean = true // CONTROL DE SINCRONIZACIÓN
) {
    val splashState by viewModel.splashState.collectAsState()

    // Controlamos el lanzamiento de la carga
    LaunchedEffect(canProceed) {
        if (canProceed) {
            // Solo cargamos si ya tenemos permiso (sesión checkeada)
            // Pequeño delay opcional para dar tiempo a UserSession a propagar si acaba de setearse
            delay(100) 
            viewModel.loadVideoUrl()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        // Si no podemos proceder o estamos cargando, mostramos spinner
        if (!canProceed) {
             CircularProgressIndicator(color = Color.White)
        } else {
            when (val state = splashState) {
                is SplashState.Loading, SplashState.Idle -> {
                    CircularProgressIndicator(color = Color.White)
                }
                is SplashState.Error, is SplashState.Success -> {
                    val videoUrl = (state as? SplashState.Success)?.videoUrl

                    if (videoUrl.isNullOrBlank()) {
                        LaunchedEffect(Unit) {
                            delay(500)
                            onVideoEnded()
                        }
                    } else {
                        VideoPlayer(
                            videoUrl = videoUrl,
                            onVideoEnded = onVideoEnded
                        )
                        
                        // Botón de Saltar (Skip)
                        androidx.compose.material3.TextButton(
                            onClick = onVideoEnded,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(16.dp)
                                .windowInsetsPadding(androidx.compose.foundation.layout.WindowInsets.statusBars)
                        ) {
                            androidx.compose.material3.Text(
                                text = "SALTAR",
                                color = Color.White,
                                fontWeight =androidx.compose.ui.text.font.FontWeight.Bold
                            )
                        }
                    }
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