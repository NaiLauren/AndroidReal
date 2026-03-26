package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.ui.theme.LocalPrimaryColor
import java.text.SimpleDateFormat
import java.util.Locale

// --- DESIGN SYSTEM CONSTANTS ---
val WDesignColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.65f)
val WDesignColorGlassInput = Color(0xFFFFFFFF).copy(alpha = 0.10f)
val WDesignColorTextPrimary = Color.White
val WDesignColorTextSecondary = Color(0xFFAAAAAA)
val WDesignColorBorder = Color.White.copy(alpha = 0.15f)
val WDesignColorError = Color(0xFFEF5350)

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title.uppercase(), 
        style = MaterialTheme.typography.labelSmall, 
        fontWeight = FontWeight.Bold, 
        letterSpacing = 1.2.sp, 
        color = WDesignColorTextSecondary, 
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    )
}

@Composable
fun GlassTextField(
    value: String, 
    onValueChange: (String) -> Unit, 
    label: String,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = WDesignColorTextSecondary) },
        modifier = Modifier.fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = WDesignColorGlassInput, 
            unfocusedContainerColor = WDesignColorGlassInput, 
            focusedBorderColor = Color.White.copy(alpha = 0.5f), 
            unfocusedBorderColor = WDesignColorBorder, 
            focusedTextColor = WDesignColorTextPrimary, 
            unfocusedTextColor = WDesignColorTextPrimary, 
            cursorColor = LocalPrimaryColor.current
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
fun GlassLoadingCard(height: androidx.compose.ui.unit.Dp = 200.dp) {
    com.aquiles.crosschapp.presentation.components.GlassCard(modifier = Modifier.height(height)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = LocalPrimaryColor.current)
        }
    }
}

@Composable
fun InfoCardSmall(icon: ImageVector, title: String, message: String) {
    com.aquiles.crosschapp.presentation.components.GlassCard(modifier = Modifier.height(140.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp), 
            verticalArrangement = Arrangement.Center, 
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = WDesignColorTextSecondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = WDesignColorTextSecondary)
            Text(text = message, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = WDesignColorTextSecondary.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun AccessBlockedCard(title: String, message: String, onClick: () -> Unit = {}) {
    com.aquiles.crosschapp.presentation.components.GlassCard(
        modifier = Modifier.height(140.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().clickable(onClick = onClick).padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = Icons.Default.Lock, contentDescription = title, modifier = Modifier.size(24.dp), tint = WDesignColorError)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = WDesignColorError)
            Text(text = message, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, color = WDesignColorError.copy(alpha = 0.8f))
        }
    }
}

@Composable
fun BenchmarkAccessCard(onClick: () -> Unit) {
    com.aquiles.crosschapp.presentation.components.GlassCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "REGISTRAR TU PROGRESO", style = MaterialTheme.typography.labelSmall, color = LocalPrimaryColor.current, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "Ir a Benchmarks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = Color.White)
                Text(text = "Registra tus PRs: Fran, Murph, Max Lifts...", style = MaterialTheme.typography.bodySmall, color = WDesignColorTextSecondary)
            }
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(LocalPrimaryColor.current),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color.White)
            }
        }
    }
}

@Composable
fun BadgeValidation(text: String, icon: ImageVector?, bgColor: Color, textColor: Color) {
    Row(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (icon != null) Icon(icon, null, tint = textColor, modifier = Modifier.size(10.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = textColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun NextBookingInnerColumn(gymClass: GymClass, onClick: () -> Unit) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Tu próxima clase",
                style = MaterialTheme.typography.labelSmall,
                color = WDesignColorTextSecondary
            )
            Text(
                text = gymClass.name,
                style = MaterialTheme.typography.titleMedium,
                color = WDesignColorTextPrimary,
                fontWeight = FontWeight.Bold
            )
            val dayName = gymClass.dateTime?.let { dayFormat.format(it).uppercase() } ?: ""
            val timeStr = gymClass.dateTime?.let { timeFormat.format(it) } ?: ""
            Text(
                text = "$dayName $timeStr - ${gymClass.coachName}",
                style = MaterialTheme.typography.bodySmall,
                color = WDesignColorTextSecondary
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = LocalPrimaryColor.current,
            modifier = Modifier.size(20.dp)
        )
    }
}

