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
import com.google.firebase.firestore.FieldPath
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import android.util.Log

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

    private val _enrolledUsers = MutableStateFlow<List<User>>(emptyList())
    val enrolledUsers: StateFlow<List<User>> = _enrolledUsers.asStateFlow()

    private val _myEntry = MutableStateFlow<RankingEntry?>(null)
    val myEntry: StateFlow<RankingEntry?> = _myEntry.asStateFlow()

    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

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
        val criteria = competition.resolveCriteriaEnum()
        listenerRegistration?.remove()

        // Cargar usuarios inscritos
        viewModelScope.launch {
            try {
                if (competition.linkedClassIds.isNotEmpty()) {
                    val classesSnap = db.collection("gymClasses")
                        .whereIn(FieldPath.documentId(), competition.linkedClassIds.take(10))
                        .get().await()
                    
                    val enrolledIds = classesSnap.documents.flatMap { 
                        @Suppress("UNCHECKED_CAST")
                        (it.get("enrolledUserIds") as? List<String>) ?: emptyList() 
                    }.toSet().toList()

                    if (enrolledIds.isNotEmpty()) {
                        val usersSnap = db.collection("users")
                            .whereIn(FieldPath.documentId(), enrolledIds.take(10))
                            .get().await()
                            
                        _enrolledUsers.value = usersSnap.toObjects(User::class.java)
                    }
                }
            } catch (e: Exception) {
                Log.e("StudentCompetitionVM", "Error loading enrolled users", e)
            }
        }
        
        try {
            // Utilizamos SnapshotListener para reflejar las Emojis (reactions) en tiempo real
            listenerRegistration = db.collection("wod_results")
                .whereEqualTo("competitionId", competition.id)
                .whereEqualTo("validationStatus", "approved") // Solo validados
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        _errorMessage.value = "Error calculando ranking: ${error.message}"
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        val results = snapshot.toObjects(BenchmarkResult::class.java)

                        val resultsByUser = results.groupBy { it.userId }
                        
                        val rankingEntries = resultsByUser.map { (userId, userResults) ->
                            val bestResult = getBestResult(userResults, criteria)
                            
                            RankingEntry(
                                userId = userId,
                                userName = bestResult.userName.ifBlank { "Atleta" },
                                userProfileImageUrl = bestResult.userProfileImageUrl,
                                userLevel = bestResult.userLevel,
                                score = bestResult.numericScore,
                                scoreDisplay = bestResult.score,
                                rank = 0, // Se asigna después de ordenar
                                resultId = bestResult.id,
                                validationStatus = bestResult.validationStatus,
                                reactions = bestResult.reactions
                            )
                        }
                        
                        // Ordenar
                        val sortedEntries = sortRanking(rankingEntries, criteria)
                        
                        // Asignar Rank
                        val finalRanking = sortedEntries.mapIndexed { index, entry ->
                            entry.copy(rank = index + 1)
                        }
                        
                        _ranking.value = finalRanking
                        
                        // Asignar Ranking de Usuario Actual
                        val currentUserId = UserSession.getCurrentUserId()
                        _myEntry.value = finalRanking.find { it.userId == currentUserId }
                    }
                }
            
        } catch (e: Exception) {
            _errorMessage.value = "Error inicializando ranking: ${e.message}"
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

    // MANDATO: Actualziar solo elements especificos `affectedKeys().hasOnly(['reactions'])`
    fun toggleReaction(resultId: String, emotion: String) {
        val currentUser = UserSession.currentUser.value ?: return
        val currentUserId = currentUser.id
        
        viewModelScope.launch {
            try {
                val docRef = db.collection("wod_results").document(resultId)
                db.runTransaction { transaction ->
                    val snapshot = transaction.get(docRef)
                    @Suppress("UNCHECKED_CAST")
                    val currentReactions = snapshot.get("reactions") as? Map<String, String> ?: emptyMap()
                    
                    val newReactions = currentReactions.toMutableMap()
                    if (newReactions[currentUserId] == emotion) {
                        newReactions.remove(currentUserId)
                    } else {
                        newReactions[currentUserId] = emotion
                    }
                    
                    transaction.update(docRef, "reactions", newReactions)
                    null
                }.await()
            } catch (e: Exception) {
                Log.e("StudentCompetitionVM", "Error updating reaction", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
