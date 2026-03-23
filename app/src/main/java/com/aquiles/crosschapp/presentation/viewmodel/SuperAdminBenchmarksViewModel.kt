package com.aquiles.crosschapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.BenchmarkWod
import com.aquiles.crosschapp.data.model.Gym
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SuperAdminBenchmarksViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _globalBenchmarks = MutableStateFlow<List<BenchmarkWod>>(emptyList())
    val globalBenchmarks: StateFlow<List<BenchmarkWod>> = _globalBenchmarks.asStateFlow()

    private val _allGyms = MutableStateFlow<List<Gym>>(emptyList())
    val allGyms: StateFlow<List<Gym>> = _allGyms.asStateFlow()

    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Idle)
    val loadingState: StateFlow<LoadingState> = _loadingState.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    sealed class LoadingState {
        object Idle : LoadingState()
        object Loading : LoadingState()
        object Success : LoadingState()
        data class Error(val message: String) : LoadingState()
    }

    sealed class SaveState {
        object Idle : SaveState()
        object Saving : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }

    private val isSuperAdmin: Boolean
        get() {
            val email = auth.currentUser?.email?.lowercase() ?: ""
            return email == "cam1@camaleon.com" || email == "nicolauren369@gmail.com" || email == "admin@realfitness.com"
        }

    fun loadGlobalBenchmarks() {
        if (!isSuperAdmin) {
            _loadingState.value = LoadingState.Error("Acceso denegado: Solo Super Admin")
            return
        }

        _loadingState.value = LoadingState.Loading

        viewModelScope.launch {
            try {
                val benchmarksSnapshot = db.collection("benchmark_wods")
                    .whereEqualTo("gym_id", "")
                    .get().await()

                val challengesSnapshot = db.collection("challenges")
                    .whereEqualTo("isGlobal", true)
                    .get().await()

                val benchmarks = benchmarksSnapshot.documents.mapNotNull { it.toObject(BenchmarkWod::class.java)?.copy(id = it.id) }
                val challenges = challengesSnapshot.documents.mapNotNull { it.toObject(BenchmarkWod::class.java)?.copy(id = it.id) }

                _globalBenchmarks.value = (benchmarks + challenges).sortedBy { it.name }
                _loadingState.value = LoadingState.Success

            } catch (e: Exception) {
                _loadingState.value = LoadingState.Error("Error cargando: \${e.localizedMessage}")
            }
        }
    }

    fun loadAllGyms() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("gyms").orderBy("name").get().await()
                val gyms = snapshot.documents.mapNotNull { it.toObject(Gym::class.java)?.copy(id = it.id) }
                _allGyms.value = gyms
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveBenchmark(
        id: String? = null,
        name: String,
        description: String,
        scoreType: String,
        strategy: String,
        isDesafio: Boolean,
        startDate: Timestamp?,
        endDate: Timestamp?,
        selectedGymIds: List<String>
    ) {
        if (!isSuperAdmin) {
            _saveState.value = SaveState.Error("Acceso denegado")
            return
        }

        if (name.isBlank()) {
            _saveState.value = SaveState.Error("El nombre es obligatorio")
            return
        }

        if (isDesafio && startDate != null && endDate != null) {
            if (endDate < startDate) {
                _saveState.value = SaveState.Error("La fecha de fin debe ser posterior a la fecha de inicio")
                return
            }
        }

        _saveState.value = SaveState.Saving

        viewModelScope.launch {
            try {
                val collectionName = if (isDesafio) "challenges" else "benchmark_wods"
                val docRef = if (id != null) {
                    db.collection(collectionName).document(id)
                } else {
                    db.collection(collectionName).document()
                }

                val sortOrder = if (scoreType == "TIME") "ASC" else "DESC"

                val benchmark = BenchmarkWod(
                    id = docRef.id,
                    name = name,
                    description = description,
                    strategy = strategy,
                    scoreType = scoreType,
                    measurementUnit = scoreType,
                    sortOrder = sortOrder,
                    gym_id = "",
                    allowedGymIds = if (selectedGymIds.isEmpty()) null else selectedGymIds,
                    isDesafio = isDesafio,
                    startDate = startDate,
                    endDate = endDate,
                    isGlobal = true
                )

                docRef.set(benchmark).await()
                _saveState.value = SaveState.Success
                loadGlobalBenchmarks() // Recargar lista

            } catch (e: Exception) {
                _saveState.value = SaveState.Error("Error guardando: \${e.localizedMessage}")
            }
        }
    }

    fun deleteBenchmark(benchmark: BenchmarkWod) {
        if (!isSuperAdmin || benchmark.id.isBlank()) return

        viewModelScope.launch {
            try {
                val collectionName = if (benchmark.isDesafio == true) "challenges" else "benchmark_wods"
                db.collection(collectionName).document(benchmark.id).delete().await()
                loadGlobalBenchmarks()
            } catch (e: Exception) {
                _errorMessage.value = "Error eliminando: \${e.localizedMessage}"
            }
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}
