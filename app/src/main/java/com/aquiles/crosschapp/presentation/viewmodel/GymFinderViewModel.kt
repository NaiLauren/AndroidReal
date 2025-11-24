// RUTA: presentation/viewmodel/GymFinderViewModel.kt
// VERSIÓN MODIFICADA Y LIMPIA

package com.aquiles.crosschapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
// --- CAMBIO: IMPORTAMOS EL DATA CLASS DESDE SU NUEVO ARCHIVO ---
import com.aquiles.crosschapp.data.model.Gym
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// --- CAMBIO: EL DATA CLASS GYM SE HA MOVIDO A data/model/Gym.kt ---

// Estado de la UI para la pantalla del buscador
data class GymFinderUiState(
    val selectedCountry: String? = null,
    val states: List<String> = emptyList(),
    val selectedState: String? = null,
    val cities: List<String> = emptyList(),
    val selectedCity: String? = null,
    val gyms: List<Gym> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

class GymFinderViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val _uiState = MutableStateFlow(GymFinderUiState())
    val uiState = _uiState.asStateFlow()

    fun onCountrySelected(countryName: String) {
        if (countryName == _uiState.value.selectedCountry) return

        _uiState.update {
            it.copy(
                selectedCountry = countryName, isLoading = true, error = null,
                states = emptyList(), selectedState = null, cities = emptyList(),
                selectedCity = null, gyms = emptyList()
            )
        }
        loadStatesForCountry(countryName)
    }

    private fun loadStatesForCountry(countryName: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("gyms")
                    .whereEqualTo("country", countryName).get().await()
                val states = snapshot.documents
                    .mapNotNull { it.getString("state") }.distinct().sorted()
                _uiState.update { it.copy(states = states, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al cargar provincias.", isLoading = false) }
            }
        }
    }

    fun onStateSelected(stateName: String) {
        _uiState.update {
            it.copy(
                selectedState = stateName, isLoading = true, error = null,
                cities = emptyList(), selectedCity = null, gyms = emptyList()
            )
        }
        loadCitiesForState(stateName)
    }

    private fun loadCitiesForState(stateName: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("gyms")
                    .whereEqualTo("country", _uiState.value.selectedCountry)
                    .whereEqualTo("state", stateName).get().await()
                val cities = snapshot.documents
                    .mapNotNull { it.getString("city") }.distinct().sorted()
                _uiState.update { it.copy(cities = cities, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al cargar ciudades.", isLoading = false) }
            }
        }
    }

    fun onCitySelected(cityName: String) {
        _uiState.update {
            it.copy(selectedCity = cityName, isLoading = true, error = null, gyms = emptyList())
        }
        loadGymsForCity(cityName)
    }

    private fun loadGymsForCity(cityName: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("gyms")
                    .whereEqualTo("country", _uiState.value.selectedCountry)
                    .whereEqualTo("state", _uiState.value.selectedState)
                    .whereEqualTo("city", cityName).get().await()

                // --- CAMBIO: AHORA INCLUIMOS EL ID DEL DOCUMENTO ---
                val gyms = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Gym::class.java)?.copy(id = doc.id)
                }
                _uiState.update { it.copy(gyms = gyms, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Error al cargar gimnasios.", isLoading = false) }
            }
        }
    }
}