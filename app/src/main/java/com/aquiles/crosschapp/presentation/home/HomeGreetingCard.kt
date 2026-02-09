package com.aquiles.crosschapp.presentation.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aquiles.crosschapp.presentation.components.GlassCard
import java.util.Calendar

@Composable
fun HomeGreetingCard(userName: String) {
    val greeting = getGreetingMessage()
    val motivation = "Listo para darlo todo hoy. Revisa los Horarios y reserva tu clase."

    GlassCard(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Saludo y Nombre
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Normal
                    )) {
                        append("$greeting,\n")
                    }
                    withStyle(style = SpanStyle(
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )) {
                        append(userName)
                    }
                },
                lineHeight = 40.sp
            )

            // Separator line (optional, purely visual style)
            androidx.compose.material3.HorizontalDivider(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                color = Color.White.copy(alpha = 0.1f)
            )

            // Mensaje motivacional
            Text(
                text = motivation,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.8f),
                lineHeight = 22.sp
            )
        }
    }
}

private fun getGreetingMessage(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Buenos días"
        in 12..19 -> "Buenas tardes"
        else -> "Buenas noches"
    }
}
