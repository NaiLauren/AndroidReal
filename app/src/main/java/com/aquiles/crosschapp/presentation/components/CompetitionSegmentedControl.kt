package com.aquiles.crosschapp.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aquiles.crosschapp.ui.theme.BrandOrange

@Composable
fun CompetitionSegmentedControl(
    selectedIndex: Int,
    items: List<String>,
    onIndexChanged: (Int) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape)
            .padding(4.dp)
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            items.forEachIndexed { index, label ->
                val isSelected = selectedIndex == index
                val animatedColor by animateColorAsState(
                    targetValue = if (isSelected) BrandOrange else Color.Transparent,
                    label = "tabColor"
                )
                val textColor by animateColorAsState(
                    targetValue = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
                    label = "tabText"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(animatedColor)
                        .clickable { onIndexChanged(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        color = textColor,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }
    }
}
