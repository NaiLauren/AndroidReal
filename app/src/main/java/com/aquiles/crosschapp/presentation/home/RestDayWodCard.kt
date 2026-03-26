package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

@Composable
fun RestDayWodCard(imageUrl: String?) {
    val fallbackImageUrl = "https://images.unsplash.com/photo-1549060279-7e168fcee0c2"
    com.aquiles.crosschapp.presentation.components.GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Box(modifier = Modifier.height(350.dp)) {
            SubcomposeAsyncImage(
                model = if (!imageUrl.isNullOrBlank()) imageUrl else fallbackImageUrl, 
                contentDescription = "Rest Day", 
                modifier = Modifier.fillMaxSize(), 
                contentScale = ContentScale.Crop
            )
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)))
            Column(
                modifier = Modifier.fillMaxSize(), 
                verticalArrangement = Arrangement.Center, 
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "REST DAY", 
                    style = MaterialTheme.typography.displayMedium, 
                    fontWeight = FontWeight.Black, 
                    color = Color.White, 
                    letterSpacing = 4.sp
                )
                Text(
                    text = "Recupera. Descansa. Repite.", 
                    style = MaterialTheme.typography.titleMedium, 
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
