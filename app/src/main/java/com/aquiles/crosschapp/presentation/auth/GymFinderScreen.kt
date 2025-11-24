package com.aquiles.crosschapp.presentation.auth

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aquiles.crosschapp.R
import com.aquiles.crosschapp.data.model.Gym
import com.aquiles.crosschapp.presentation.viewmodel.GymFinderViewModel

data class Country(
    val name: String,
    @DrawableRes val flagResId: Int
)

val supportedCountries = listOf(
    Country("Argentina", R.drawable.flag_argentina),
    Country("Chile", R.drawable.flag_chile),
    Country("Uruguay", R.drawable.flag_uruguay),
    Country("Colombia", R.drawable.flag_colombia),
    Country("Perú", R.drawable.flag_peru),
    Country("Paraguay", R.drawable.flag_paraguay)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymFinderScreen(
    gymFinderViewModel: GymFinderViewModel = viewModel(),
    onGymSelected: (String) -> Unit
) {
    val uiState by gymFinderViewModel.uiState.collectAsState()
    val primaryOrange = Color(0xFFF57E54)

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.fondo5),
            contentDescription = "Fondo de pantalla",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    text = "Elige tu lugar de entrenamiento",
                    style = MaterialTheme.typography.headlineMedium,
                    color = primaryOrange,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                )
            }

            items(supportedCountries) { country ->
                GlowingCircularFlag(
                    country = country,
                    glowColor = if (uiState.selectedCountry == country.name) Color.Green else primaryOrange,
                    textColor = primaryOrange,
                    onClick = { gymFinderViewModel.onCountrySelected(country.name) }
                )
            }

            if (uiState.states.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AnimatedVisibility(visible = true) {
                        DropdownSelector(
                            label = "Provincia / Estado",
                            options = uiState.states,
                            selectedOption = uiState.selectedState ?: "",
                            onOptionSelected = { gymFinderViewModel.onStateSelected(it) }
                        )
                    }
                }
            }
            if (uiState.cities.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AnimatedVisibility(visible = true) {
                        DropdownSelector(
                            label = "Ciudad",
                            options = uiState.cities,
                            selectedOption = uiState.selectedCity ?: "",
                            onOptionSelected = { gymFinderViewModel.onCitySelected(it) }
                        )
                    }
                }
            }

            if (uiState.gyms.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Gimnasios encontrados:", color = Color.White, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                }
                items(uiState.gyms, span = { GridItemSpan(maxLineSpan) }) { gym ->
                    GymListItem(gym = gym, onClick = { onGymSelected(gym.id) })
                }
            }

            if (uiState.isLoading || uiState.error != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(color = primaryOrange)
                        }
                        if (uiState.error != null) {
                            Text(text = uiState.error!!, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = isExpanded,
        onExpandedChange = { isExpanded = it },
        modifier = Modifier.padding(top = 16.dp)
    ) {
        OutlinedTextField(
            value = selectedOption.ifEmpty { "Selecciona una opción" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF4500),
                unfocusedBorderColor = Color.White.copy(alpha = 0.5f),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedLabelColor = Color.White,
                unfocusedLabelColor = Color.White.copy(alpha = 0.7f),
                cursorColor = Color(0xFFFF4500),
                unfocusedContainerColor = Color.Black.copy(alpha = 0.1f),
                focusedContainerColor = Color.Black.copy(alpha = 0.1f),
            )
        )
        ExposedDropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        isExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun GymListItem(gym: Gym, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        // --- INICIO DE LA CORRECCIÓN VISUAL ---
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = gym.name,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            // Solo mostramos la dirección si no está vacía
            if (gym.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Dirección",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = gym.address,
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        // --- FIN DE LA CORRECCIÓN VISUAL ---
    }
}

@Composable
fun GlowingCircularFlag(
    country: Country,
    glowColor: Color,
    textColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Image(
            painter = painterResource(id = country.flagResId),
            contentDescription = "Bandera de ${country.name}",
            modifier = Modifier
                .size(100.dp)
                .shadow(
                    elevation = 15.dp,
                    shape = CircleShape,
                    spotColor = glowColor,
                    ambientColor = glowColor
                )
                .clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = country.name,
            color = textColor,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GymFinderScreenPreview() {
    Box(Modifier.background(Color.Black)) {
        GymFinderScreen(onGymSelected = {})
    }
}