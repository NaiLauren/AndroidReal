package com.aquiles.crosschapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.BenchmarkResult
import com.aquiles.crosschapp.data.model.WodResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class FeedState {
    object Idle : FeedState()
    object Loading : FeedState()
    data class Success(val items: List<BenchmarkResult>) : FeedState()
    data class Error(val message: String) : FeedState()
}

enum class FeedTab { TODAY, RECORDS }

// Wrapper for UI Items (Unifying WodResult and BenchmarkResult)
data class FeedUiItem(
    val id: String,
    val title: String, // Wod Name or Benchmark Name
    val userName: String,
    val userProfileImageUrl: String?,
    val userLevel: String,
    val date: java.util.Date?,
    val score: String,
    val isRx: Boolean,
    val isVerified: Boolean,
    val type: FeedTab
)


class BenchmarkFeedViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _feedState = MutableStateFlow<FeedState>(FeedState.Idle)
    val feedState = _feedState.asStateFlow()

    private val _filteredItems = MutableStateFlow<List<BenchmarkResult>>(emptyList())
    val filteredItems = _filteredItems.asStateFlow()

    private val _allFeedItems = MutableStateFlow<List<BenchmarkResult>>(emptyList())
    private val _selectedGenderFilter = MutableStateFlow<String?>(null)
    val selectedGenderFilter = _selectedGenderFilter.asStateFlow()

    private val _selectedWodFilter = MutableStateFlow<String?>(null)
    val selectedWodFilter = _selectedWodFilter.asStateFlow()
    
    private val _sortCriteria = MutableStateFlow<String?>(null)
    val sortCriteria = _sortCriteria.asStateFlow()
    
    private val _isSortAscending = MutableStateFlow(true) // true = best first
    val isSortAscending = _isSortAscending.asStateFlow()
    
    private val _availableBenchmarks = MutableStateFlow<List<String>>(emptyList())
    val availableBenchmarks = _availableBenchmarks.asStateFlow()

    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var dailyListenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null // [NEW]

    // [NEW] Tab & Daily Feed
    private val _currentTab = MutableStateFlow(FeedTab.TODAY)
    val currentTab = _currentTab.asStateFlow()

    private val _dailyFeedItems = MutableStateFlow<List<FeedUiItem>>(emptyList())
    val dailyFeedItems = _dailyFeedItems.asStateFlow()

    // Unified Output
    private val _feedItems = MutableStateFlow<List<FeedUiItem>>(emptyList())
    val feedItems = _feedItems.asStateFlow()

    fun setTab(tab: FeedTab) {
        _currentTab.value = tab
        applyFilters() // Trigger re-emission
        
        if (tab == FeedTab.TODAY && _dailyFeedItems.value.isEmpty()) {
            loadDailyFeed()
        }
        if (tab == FeedTab.RECORDS && _allFeedItems.value.isEmpty()) {
            loadFeed()
        }
    }

    // Call this when underlying data changes or tab changes
    private fun emitUnifiedFeed() {
        if (_currentTab.value == FeedTab.TODAY) {
            _feedItems.value = _dailyFeedItems.value
        } else {
             // Map Filtered Benchmarks to FeedUiItem
             _feedItems.value = _filteredItems.value.mapIndexed { index, raw ->
                FeedUiItem(
                    id = raw.id,
                    title = raw.benchmarkName,
                    userName = "${raw.userName} ${raw.userLastName}".trim(), // Combine names
                    userProfileImageUrl = raw.userProfileImageUrl,
                    userLevel = raw.userLevel,
                    date = raw.date,
                    score = raw.score,
                    isRx = raw.isRx,
                    isVerified = raw.isVerified,
                    type = FeedTab.RECORDS
                )
             }
        }
    }

    private fun loadDailyFeed() {
         val currentUser = UserSession.currentUser.value ?: return
         if (currentUser.gym_id.isBlank()) return
         
         dailyListenerRegistration?.remove()
         
         // Start of Today
         val calendar = java.util.Calendar.getInstance()
         calendar.set(java.util.Calendar.HOUR_OF_DAY, 0); calendar.set(java.util.Calendar.MINUTE, 0); calendar.set(java.util.Calendar.SECOND, 0)
         val startToday = calendar.time
         
         dailyListenerRegistration = firestore.collection("wod_results")
            .whereEqualTo("gym_id", currentUser.gym_id)
            .whereGreaterThanOrEqualTo("date", startToday)
            .limit(100)
            .addSnapshotListener { snapshot, error ->
                if (error != null) { return@addSnapshotListener }
                if (snapshot != null) {
                    val rawItems = snapshot.toObjects(WodResult::class.java)
                    // Map to UI Item
                    val uiItems = rawItems.map { raw ->
                        FeedUiItem(
                            id = raw.id,
                            title = raw.wodName ?: "WOD del Día",
                            userName = if (raw.userName.isNotBlank()) raw.userName else "Atleta",
                            userProfileImageUrl = raw.userProfileImageUrl,
                            userLevel = if (raw.userLevel.isNotBlank()) raw.userLevel else "Novato",
                            date = raw.date,
                            score = raw.score,
                            isRx = raw.isRx,
                            isVerified = false,
                            type = FeedTab.TODAY
                        )
                    }.sortedByDescending { it.date }
                    _dailyFeedItems.value = uiItems
                    if (_currentTab.value == FeedTab.TODAY) emitUnifiedFeed()
                }
            }
    }



    fun loadFeed() {
        val currentUser = UserSession.currentUser.value ?: return
        if (currentUser.gym_id.isBlank()) return

        // Evitar duplicar listeners
        listenerRegistration?.remove()
        
        _feedState.value = FeedState.Loading

        listenerRegistration = firestore.collection("benchmark_results")
            .whereEqualTo("gym_id", currentUser.gym_id)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BenchmarkFeedVM", "Error loading feed", error)
                    _feedState.value = FeedState.Error("Error al cargar el muro: ${error.localizedMessage}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.toObjects(BenchmarkResult::class.java)
                    _allFeedItems.value = items
                    
                    // Actualizar lista de benchmarks disponibles
                    val uniqueBenchmarks = items.map { it.benchmarkName }.distinct().sorted()
                    _availableBenchmarks.value = uniqueBenchmarks
                    
                    applyFilters()
                    _feedState.value = FeedState.Success(items) // Estado base (éxito)
                }
            }
    }

    fun setGenderFilter(gender: String?) {
        _selectedGenderFilter.value = gender
        applyFilters()
    }


    fun setWodFilter(wodName: String?) {
        _selectedWodFilter.value = wodName
        
        // Si estamos en Records y se seleccionó un WOD específico, cargar RANKING REAL
        if (_currentTab.value == FeedTab.RECORDS && wodName != null) {
            loadRankingForBenchmark(wodName)
        } else if (_currentTab.value == FeedTab.RECORDS && wodName == null) {
            // Si se deselecciona, volver al feed general (actividad reciente)
            loadFeed()
        } else {
            applyFilters()
        }
    }
    
    fun toggleSortOrder() {
        _isSortAscending.value = !_isSortAscending.value
        applyFilters()
    }
    
    private fun loadRankingForBenchmark(benchmarkName: String) {
        val currentUser = UserSession.currentUser.value ?: return
        if (currentUser.gym_id.isBlank()) return

        listenerRegistration?.remove()
        _feedState.value = FeedState.Loading
        
        // Para un Ranking real, necesitamos TRAER TODOS (o muchos) resultados de ese benchmark
        // No podemos limitar por fecha porque el mejor timestamp puede ser antiguo.
        // Limitamos a 200 por seguridad de rendimiento, idealmente paginado o query ordenado por score.
        // Nota: Ordenar por 'numericScore' en Firestore requiere saber si es ASC o DESC de antemano.
        // Por simplicidad y robustez híbrida: Traemos los últimos 200 (o mejores si pudiéramos) y ordenamos en memoria.
        
        listenerRegistration = firestore.collection("benchmark_results")
            .whereEqualTo("gym_id", currentUser.gym_id)
            .whereEqualTo("benchmarkName", benchmarkName)
            .limit(200) 
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _feedState.value = FeedState.Error("Error cargando ranking")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.toObjects(BenchmarkResult::class.java)
                    _allFeedItems.value = items
                    
                    // Asegurar que el filtro WOD esté aplicado (redundante pero seguro)
                    // _selectedWodFilter.value = benchmarkName 
                    
                    // Aplicar filtros (que hará el sort correcto en memoria)
                    applyFilters()
                    
                    // Emitir estado
                    _feedState.value = FeedState.Success(items)
                }
            }
    }

    private fun applyFilters() {
        var currentList = _allFeedItems.value
        
        // 1. Filtro por WOD
        _selectedWodFilter.value?.let { wod ->
            currentList = currentList.filter { it.benchmarkName == wod }
        }

        // 2. Filtro por Género
        _selectedGenderFilter.value?.let { gender ->
            // Normalizar a lowercase para comparar ("male", "female")
            currentList = currentList.filter { it.userGender.equals(gender, ignoreCase = true) }
        }

        // 3. Ordenamiento
        if (currentList.isNotEmpty()) {
            // Detectar si es tiempo o reps (buscar : o ')
            val sample = currentList.firstOrNull()?.score ?: ""
            val isTime = sample.contains(":") || sample.contains("'")
            
            val showBestFirst = _isSortAscending.value
            
            // Actualizar criterio de ordenamiento para la UI
            _sortCriteria.value = when {
                isTime && showBestFirst -> "🏆 Menor tiempo (Mejor primero)"
                isTime && !showBestFirst -> "⏱️ Mayor tiempo"
                !isTime && showBestFirst -> "🏆 Mayor cantidad (Mejor primero)"
                else -> "📊 Menor cantidad"
            }
            
            Log.d("BenchmarkFeedVM", "=== SORTING DEBUG ===")
            Log.d("BenchmarkFeedVM", "Total items: ${currentList.size}")
            Log.d("BenchmarkFeedVM", "Sample score: $sample, isTime: $isTime")
            
            // Ordenar: primero RX, luego por score
            currentList = currentList.sortedWith(compareBy<BenchmarkResult> {
                // 1. Prioridad: RX primero (RX=0, Scaled=1)
                if (it.isRx) 0 else 1
            }.thenBy {
                // 2. Score: calcular numericScore on-the-fly si es necesario
                val calculatedScore = if (it.numericScore == 0.0 && it.score.isNotEmpty()) {
                    // Calcular si no está poblado
                    if (isTime) parseTimeToSeconds(it.score) else parseRepsOrWeight(it.score)
                } else {
                    it.numericScore
                }
                
                Log.d("BenchmarkFeedVM", "  ${it.userName}: score='${it.score}', numericScore=${it.numericScore}, calculated=$calculatedScore, isRx=${it.isRx}")
                
                // Aplicar ordenamiento basado en el toggle
                if (isTime) {
                    // Tiempo: si showBestFirst=true, ascendente (menor primero)
                    if (showBestFirst) calculatedScore else -calculatedScore
                } else {
                    // Reps: si showBestFirst=true, descendente (mayor primero)
                    if (showBestFirst) -calculatedScore else calculatedScore
                }
            })
            
            Log.d("BenchmarkFeedVM", "After sort:")
            currentList.take(5).forEach {
                Log.d("BenchmarkFeedVM", "  ${it.userName}: ${it.score}")
            }
        }

        _filteredItems.value = currentList
        if (_currentTab.value == FeedTab.RECORDS) emitUnifiedFeed()
    }
    
    private fun parseTimeToSeconds(timeString: String): Double {
        // Soportar ambos formatos:
        // Formato 1: mm:ss o hh:mm:ss (ej: "00:10", "1:25:30")
        // Formato 2: mm'ss" o hh'mm"ss' (ej: "14'56"", "1'25"30'")
        
        val normalized = timeString
            .replace("'", ":")  // 14'56" -> 14:56"
            .replace("\"", "")  // 14:56" -> 14:56
            .replace("'", "")   // Por si acaso hay comillas simples residuales
        
        val parts = normalized.split(":").mapNotNull { it.toDoubleOrNull() }
        return when (parts.size) {
            3 -> (parts[0] * 3600) + (parts[1] * 60) + parts[2] // hh:mm:ss
            2 -> (parts[0] * 60) + parts[1] // mm:ss
            1 -> parts[0] // ss
            else -> 0.0
        }
    }
    
    private fun parseRepsOrWeight(scoreString: String): Double {
        val numericString = scoreString.filter { it.isDigit() || it == '.' }
        return numericString.toDoubleOrNull() ?: 0.0
    }


    fun toggleVerification(itemId: String, currentStatus: Boolean) {
        val currentUser = UserSession.currentUser.value
        // Security Check: Only admins (Client-side check, enforced by Security Rules)
        if (currentUser?.isAdmin != true && currentUser?.role != "owner") return

        viewModelScope.launch {
            try {
                val newStatus = !currentStatus
                firestore.collection("benchmark_results")
                    .document(itemId)
                    .update("isVerified", newStatus)
                    .await()
                
                // No necesitamos recargar manual (loadFeed), el listener lo hará solo.
            } catch (e: Exception) {
                Log.e("BenchmarkFeedVM", "Error verifying result", e)
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
