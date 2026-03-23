package com.aquiles.crosschapp.presentation.auth

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aquiles.crosschapp.R
import com.aquiles.crosschapp.data.model.Gym
import com.aquiles.crosschapp.presentation.viewmodel.GymFinderViewModel

// --- COLORES Y ESTILOS ---
private val ColorPrimaryAction = Color(0xFFFC5200) // Tu naranja
private val ColorGlassSurface = Color(0xFF1C1C1E).copy(alpha = 0.65f)
private val ColorTextPrimary = Color.White
private val ColorTextSecondary = Color.White.copy(alpha = 0.7f)
private val ColorBorder = Color.White.copy(alpha = 0.2f)

data class Country(
    val name: String,
    @DrawableRes val flagResId: Int
)

val supportedCountries = listOf(
    Country("Argentina", R.drawable.flag_argentina),
    Country("Chile", R.drawable.flag_chile),
    Country("Uruguay", R.drawable.flag_uruguay),
    Country("Colombia", R.drawable.flag_colombia),
    Country("Peru", R.drawable.flag_peru),
    Country("Paraguay", R.drawable.flag_paraguay)
)

@Composable
fun GymFinderScreen(
    gymFinderViewModel: GymFinderViewModel = viewModel(),
    onGymSelected: (String) -> Unit
) {
    val uiState by gymFinderViewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. Fondo
        Image(
            painter = painterResource(id = R.drawable.fondo5), // O fondo3, el que prefieras
            contentDescription = "Fondo de pantalla",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        // Capa oscura para legibilidad
        Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)))

        // 2. Contenido Principal
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 60.dp, bottom = 40.dp, start = 20.dp, end = 20.dp),
            verticalArrangement = Arrangement.spacedBy(25.dp)
        ) {

            // --- TÍTULO (Tarjeta Cristal) ---
            item {
                GlassTitleCard(title = "Elige tu lugar de entrenamiento", subtitle = "¡BIENVENIDO!")
            }

            // --- GRID DE BANDERAS ---
            item {
                // Organizamos las banderas en filas de 3 manualmente para evitar conflictos de scroll
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    supportedCountries.chunked(3).forEach { rowCountries ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            rowCountries.forEach { country ->
                                FlagItem(
                                    country = country,
                                    isSelected = uiState.selectedCountry == country.name,
                                    onClick = { gymFinderViewModel.onCountrySelected(country.name) }
                                )
                            }
                        }
                    }
                }
            }

            // --- SELECTORES (Provincia y Ciudad) ---
            item {
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    AnimatedVisibility(visible = uiState.states.isNotEmpty()) {
                        GlassDropdownSelector(
                            label = "Provincia / Estado",
                            options = uiState.states,
                            selectedOption = uiState.selectedState,
                            onOptionSelected = { gymFinderViewModel.onStateSelected(it) }
                        )
                    }

                    AnimatedVisibility(visible = uiState.cities.isNotEmpty()) {
                        GlassDropdownSelector(
                            label = "Ciudad",
                            options = uiState.cities,
                            selectedOption = uiState.selectedCity,
                            onOptionSelected = { gymFinderViewModel.onCitySelected(it) }
                        )
                    }
                }
            }

            // --- ESTADO (Loading / Error) ---
            if (uiState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(20.dp), Alignment.Center) {
                        CircularProgressIndicator(color = ColorPrimaryAction)
                    }
                }
            }
            if (uiState.error != null) {
                item {
                    Text(
                        text = uiState.error!!,
                        color = Color.Red,
                        modifier = Modifier.background(ColorGlassSurface, RoundedCornerShape(8.dp)).padding(10.dp)
                    )
                }
            }

            // --- LISTA DE GIMNASIOS ---
            if (uiState.gyms.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Gimnasios disponibles", color = ColorTextPrimary, fontWeight = FontWeight.Bold)
                        Text("${uiState.gyms.size} encontrados", color = ColorTextSecondary, style = MaterialTheme.typography.bodySmall)
                    }
                }

                items(uiState.gyms) { gym ->
                    GymResultItem(gym = gym, onClick = { onGymSelected(gym.id) })
                    Spacer(modifier = Modifier.height(15.dp)) // Espacio entre items
                }
            }
        }
    }
}

// =====================================================
// COMPONENTES UI PERSONALIZADOS (Estilo iOS Glass)
// =====================================================

@Composable
fun GlassTitleCard(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorGlassSurface, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, ColorBorder), RoundedCornerShape(16.dp))
            .padding(vertical = 20.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = ColorPrimaryAction,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(5.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                ),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FlagItem(country: Country, isSelected: Boolean, onClick: () -> Unit) {
    val scale by animateFloatAsState(targetValue = if (isSelected) 1.15f else 1f, label = "scale")
    // Matriz para convertir a blanco y negro si no está seleccionado
    val grayscaleMatrix = ColorMatrix().apply { setToSaturation(0f) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(80.dp)
                .scale(scale)
        ) {
            // Anillo de selección
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(CircleShape)
                        .border(3.dp, ColorPrimaryAction, CircleShape)
                        .shadow(10.dp, CircleShape, spotColor = ColorPrimaryAction)
                )
            }

            // Imagen de la Bandera
            Image(
                painter = painterResource(id = country.flagResId),
                contentDescription = country.name,
                contentScale = ContentScale.Crop,
                // Aplicamos filtro de color si NO está seleccionado
                colorFilter = if (isSelected) null else ColorFilter.colorMatrix(grayscaleMatrix),
                modifier = Modifier
                    .size(if (isSelected) 72.dp else 76.dp)
                    .clip(CircleShape)
                    .background(Color.Gray) // Fondo por si la imagen tarda o es transparente
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = country.name,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) ColorPrimaryAction else ColorTextPrimary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun GlassDropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Label superior fuera de la caja
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = ColorPrimaryAction,
            modifier = Modifier.padding(start = 4.dp)
        )

        // Caja del selector (Glass)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(ColorGlassSurface, RoundedCornerShape(12.dp))
                .border(BorderStroke(1.dp, ColorBorder), RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedOption ?: "Selecciona una opción",
                    color = if (!selectedOption.isNullOrEmpty()) ColorTextPrimary else ColorTextSecondary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = ColorPrimaryAction
                )
            }

            // Menú desplegable
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF2C2C2E)) // Fondo oscuro sólido para el menú
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(option, color = Color.White)
                                if (option == selectedOption) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Default.Check, null, tint = ColorPrimaryAction, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun GymResultItem(gym: Gym, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)), // Fondo semitransparente oscuro
        border = BorderStroke(1.dp, ColorBorder)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono Decorativo
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(ColorPrimaryAction.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Usamos LocationOn o FitnessCenter si tienes la librería de iconos extendida
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = ColorPrimaryAction,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = gym.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorTextPrimary
                )
                if (gym.address.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = gym.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = ColorTextSecondary,
                        maxLines = 2
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = ColorTextSecondary
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GymFinderScreenPreview() {
    Box(Modifier.background(Color.Black)) {
        GymFinderScreen(onGymSelected = {})
    }
}