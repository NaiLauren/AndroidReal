package com.aquiles.crosschapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.BenchmarkResult
import com.aquiles.crosschapp.data.model.BenchmarkWod
import com.aquiles.crosschapp.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class LeaderboardState {
    object Idle : LeaderboardState()
    object Loading : LeaderboardState()
    data class Success(val rankings: List<BenchmarkResult>) : LeaderboardState()
    data class Error(val message: String) : LeaderboardState()
}

enum class LeaderboardTab { BENCHMARK, XP }

class LeaderboardViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    // --- State ---
    private val _leaderboardState = MutableStateFlow<LeaderboardState>(LeaderboardState.Idle)
    val leaderboardState: StateFlow<LeaderboardState> = _leaderboardState.asStateFlow()

    private val _availableBenchmarks = MutableStateFlow<List<BenchmarkWod>>(emptyList())
    val availableBenchmarks = _availableBenchmarks.asStateFlow()

    // --- Filters ---
    private val _selectedBenchmark = MutableStateFlow<BenchmarkWod?>(null)
    val selectedBenchmark = _selectedBenchmark.asStateFlow()

    private val _selectedGenderFilter = MutableStateFlow<String>("ALL") // ALL, Male, Female
    val selectedGenderFilter = _selectedGenderFilter.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String>("ALL") // ALL, RX, SCALED
    val selectedCategoryFilter = _selectedCategoryFilter.asStateFlow()

    private val _currentTab = MutableStateFlow(LeaderboardTab.BENCHMARK)
    val currentTab = _currentTab.asStateFlow()

    init {
        loadAvailableBenchmarks()
    }

    private fun loadAvailableBenchmarks() {
        val user = UserSession.currentUser.value ?: return
        if (user.gym_id.isBlank()) return

        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("benchmarks")
                    .whereEqualTo("gym_id", user.gym_id)
                    .orderBy("name", Query.Direction.ASCENDING)
                    .get().await()
                
                val list = snapshot.toObjects(BenchmarkWod::class.java).map { it.copy(id = snapshot.documents[snapshot.documents.indexOfFirst { doc -> doc.id == it.id } ?: 0].id) }
                 // Fix ID mapping if needed or rely on @DocumentId if annotated correctly in model (which it is)
                 // Actually toObject with DocumentId annotation works, but sometimes local list needs explicit ID if not serialized
                
                _availableBenchmarks.value = list
                
                // Select first by default if available
                if (list.isNotEmpty() && _selectedBenchmark.value == null) {
                    selectBenchmark(list.first())
                }
            } catch (e: Exception) {
                Log.e("LeaderboardViewModel", "Error loading benchmarks", e)
            }
        }
    }

    fun selectBenchmark(benchmark: BenchmarkWod) {
        _selectedBenchmark.value = benchmark
        loadLeaderboardData()
    }

    fun setGenderFilter(gender: String) {
        _selectedGenderFilter.value = gender
        loadLeaderboardData()
    }

    fun setCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
        loadLeaderboardData()
    }

    fun setTab(tab: LeaderboardTab) {
        _currentTab.value = tab
        if (tab == LeaderboardTab.XP) {
            loadXpLeaderboard()
        } else {
            loadLeaderboardData()
        }
    }

    private fun loadLeaderboardData() {
        if (_currentTab.value == LeaderboardTab.XP) return
        
        val benchmark = _selectedBenchmark.value ?: return
        val user = UserSession.currentUser.value ?: return
        val gender = _selectedGenderFilter.value
        val category = _selectedCategoryFilter.value

        _leaderboardState.value = LeaderboardState.Loading
        
        viewModelScope.launch {
            try {
                // Base Query
                var query: Query = firestore.collection("benchmark_results")
                    .whereEqualTo("gym_id", user.gym_id)
                    .whereEqualTo("benchmarkId", benchmark.id)

                // Apply Filters
                if (gender != "ALL") {
                    query = query.whereEqualTo("userGender", gender)
                }

                if (category != "ALL") {
                    // Legacy support: map category string to boolean isRx if needed, 
                    // or if we strictly use new 'category' field.
                    // Current Implementation Plan kept 'isRx' boolean and added 'category' vaguely.
                    // Let's stick to 'isRx' for now as mapped in PerformanceViewModel.
                    val isRx = category == "RX"
                    query = query.whereEqualTo("isRx", isRx)
                }

                // Execute Query
                // Note: Sorting by numericScore requires an index if mixed with other filters.
                // We will sort in-memory for MVP to avoid index explosion, unless list is huge (limit 100).
                val snapshot = query.limit(100).get().await()
                var results = snapshot.toObjects(BenchmarkResult::class.java)

                // In-Memory Sorting (Smart Sort based on benchmark config)
                results = if (benchmark.sortOrder == "ASC") {
                    results.sortedBy { it.numericScore } // Menor es mejor (Tiempo)
                } else {
                    results.sortedByDescending { it.numericScore } // Mayor es mejor (Peso)
                }

                // Filter out zero scores if necessary (optional)
                results = results.filter { it.numericScore > 0 }

                _leaderboardState.value = LeaderboardState.Success(results)
 
            } catch (e: Exception) {
                Log.e("LeaderboardViewModel", "Error loading leaderboard", e)
                _leaderboardState.value = LeaderboardState.Error(e.localizedMessage ?: "Error desconocido")
            }
        }
    }

    private fun loadXpLeaderboard() {
        val user = UserSession.currentUser.value ?: return
        if (user.gym_id.isBlank()) return

        _leaderboardState.value = LeaderboardState.Loading

        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users")
                    .whereEqualTo("gym_id", user.gym_id)
                    .orderBy("xp", Query.Direction.DESCENDING)
                    .limit(50)
                    .get().await()

                val users = snapshot.toObjects(User::class.java)

                // Map Users to BenchmarkResult structure for UI reuse (or create new model if needed, reusing for now as it fits)
                val rankings = users.map { u ->
                    BenchmarkResult(
                        id = u.id,
                        userId = u.id,
                        userName = u.name,
                        userLastName = u.lastName,
                        userProfileImageUrl = u.profileImageUrl ?: "",
                        userLevel = u.level,
                        userGender = u.gender,
                        numericScore = u.xp.toDouble(),
                        score = "${u.xp} XP", // Display String
                        isRx = false, // Not applicable
                        date = null,
                        benchmarkName = "XP Global"
                    )
                }
                
                _leaderboardState.value = LeaderboardState.Success(rankings)

            } catch (e: Exception) {
                 Log.e("LeaderboardViewModel", "Error loading XP leaderboard", e)
                _leaderboardState.value = LeaderboardState.Error("Error al cargar ranking de XP")
            }
        }
    }
}
