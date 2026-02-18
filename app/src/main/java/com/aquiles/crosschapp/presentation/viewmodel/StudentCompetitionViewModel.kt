package com.aquiles.crosschapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.BenchmarkResult
import com.aquiles.crosschapp.data.model.Competition
import com.aquiles.crosschapp.data.model.RankingCriteria
import com.aquiles.crosschapp.data.model.RankingEntry
import com.aquiles.crosschapp.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class StudentCompetitionViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _competition = MutableStateFlow<Competition?>(null)
    val competition: StateFlow<Competition?> = _competition.asStateFlow()

    private val _ranking = MutableStateFlow<List<RankingEntry>>(emptyList())
    val ranking: StateFlow<List<RankingEntry>> = _ranking.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _myEntry = MutableStateFlow<RankingEntry?>(null)
    val myEntry: StateFlow<RankingEntry?> = _myEntry.asStateFlow()

    fun loadCompetition(competitionId: String) {
        _isLoading.value = true
        viewModelScope.launch {
            try {
                val doc = db.collection("competitions").document(competitionId).get().await()
                val comp = doc.toObject(Competition::class.java)
                _competition.value = comp

                if (comp != null) {
                    loadRanking(comp)
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error al cargar competencia: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadRanking(competition: Competition) {
        val criteria = competition.getCriteriaEnum()
        
        try {
            // 1. Obtener todos los resultados (WOD Results) vinculados a esta competencia
            // Nota: En Android BenchmarkResult se usa también para WodResult mapeado, pero la colección raw es 'wod_results'
            // Asumimos que los resultados de competencia se guardan en 'wod_results' con 'competitionId' set.
            
            val snapshot = db.collection("wod_results")
                .whereEqualTo("competitionId", competition.id)
                .whereEqualTo("validationStatus", "approved") // Solo validados
                .get()
                .await()
            
            val results = snapshot.toObjects(BenchmarkResult::class.java) // Usamos BenchmarkResult como DTO compatible

            // 2. Agrupar por Usuario (Si es acumulativo o "Best score")
            // Para simplicidad inicial y paridad con lo visto en iOS SocialViewModel, 
            // asumimos "Best Score" si es una competencia de un solo evento, 
            // o "Accumulative" si es multicompetencia.
            // PERO: Si la competencia tiene 'linkedClassIds', probablemente sea un evento único o serie.
            
            // LÓGICA DE RANKING:
            // Por ahora, asumiremos que cada usuario tiene UN resultado válido principal. 
            // Si hay múltiples, tomamos el mejor o la suma según criterio.
            
            // Vamos a agrupar por usuario y tomar el MEJOR resultado
            val resultsByUser = results.groupBy { it.userId }
            
            val rankingEntries = resultsByUser.map { (userId, userResults) ->
                // Calcular Score del Usuario
                val bestResult = getBestResult(userResults, criteria)
                
                // Enriquecer con datos de usuario (esto requiere fetch adicional si no está en el result,
                // pero BenchmarkResult tiene userName/userProfileImage desnormalizados a menudo.
                // Si faltan, deberíamos hacer fetch de usuarios. Por eficiencia, usaremos lo que hay en result por ahora.)
                
                RankingEntry(
                    userId = userId,
                    userName = bestResult.userName.ifBlank { "Atleta" },
                    userProfileImageUrl = bestResult.userProfileImageUrl,
                    userLevel = bestResult.userLevel,
                    score = bestResult.numericScore,
                    scoreDisplay = bestResult.score,
                    rank = 0 // Se asigna después de ordenar
                )
            }
            
            // 3. Ordenar
            val sortedEntries = sortRanking(rankingEntries, criteria)
            
            // 4. Asignar Rank
            val finalRanking = sortedEntries.mapIndexed { index, entry ->
                entry.copy(rank = index + 1)
            }
            
            _ranking.value = finalRanking
            
            // 5. Asignar Ranking de Usuario Actual
            val currentUserId = UserSession.getCurrentUserId()
            _myEntry.value = finalRanking.find { it.userId == currentUserId }
            
        } catch (e: Exception) {
            _errorMessage.value = "Error calculando ranking: ${e.message}"
        }
    }

    private fun getBestResult(results: List<BenchmarkResult>, criteria: RankingCriteria): BenchmarkResult {
        // Si la lista está vacía, no debería pasar por el groupBy
        if (results.isEmpty()) return BenchmarkResult() 
        
        return when (criteria) {
            RankingCriteria.TIME -> results.minByOrNull { it.numericScore } ?: results.first() // Menor tiempo es mejor
            RankingCriteria.REPS, RankingCriteria.LOAD, RankingCriteria.POINTS -> results.maxByOrNull { it.numericScore } ?: results.first() // Mayor es mejor
        }
    }

    private fun sortRanking(entries: List<RankingEntry>, criteria: RankingCriteria): List<RankingEntry> {
        return when (criteria) {
            RankingCriteria.TIME -> entries.sortedBy { it.score } // Ascendente
            RankingCriteria.REPS, RankingCriteria.LOAD, RankingCriteria.POINTS -> entries.sortedByDescending { it.score } // Descendente
        }
    }
}
