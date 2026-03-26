package com.aquiles.crosschapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.CompetitionPodium
import com.aquiles.crosschapp.data.model.GlobalRecord
import com.aquiles.crosschapp.data.model.PodiumEntry
import com.aquiles.crosschapp.data.model.Trophy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class HallOfFameViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _trophies = MutableStateFlow<List<Trophy>>(emptyList())
    val trophies: StateFlow<List<Trophy>> = _trophies.asStateFlow()

    private val _podiums = MutableStateFlow<List<CompetitionPodium>>(emptyList())
    val podiums: StateFlow<List<CompetitionPodium>> = _podiums.asStateFlow()

    private val _recentRecords = MutableStateFlow<List<GlobalRecord>>(emptyList())
    val recentRecords: StateFlow<List<GlobalRecord>> = _recentRecords.asStateFlow()

    private val _isLoadingTrophies = MutableStateFlow(false)
    val isLoadingTrophies: StateFlow<Boolean> = _isLoadingTrophies.asStateFlow()

    private val _isLoadingPodiums = MutableStateFlow(false)
    val isLoadingPodiums: StateFlow<Boolean> = _isLoadingPodiums.asStateFlow()

    private val _isLoadingRecords = MutableStateFlow(false)
    val isLoadingRecords: StateFlow<Boolean> = _isLoadingRecords.asStateFlow()

    private val userCache = mutableMapOf<String, Pair<String, String?>>()

    fun loadAll(gymId: String) {
        loadTrophies(gymId)
        loadPodiums(gymId)
        loadRecentRecords(gymId)
    }

    fun loadTrophies(gymId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            _isLoadingTrophies.value = true
            try {
                val snap = db.collection("wod_results")
                    .whereEqualTo("gym_id", gymId)
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("validationStatus", "approved")
                    .get().await()

                val resultsByComp = snap.documents.mapNotNull { doc ->
                    val compId = doc.getString("competitionId")
                    if (compId.isNullOrEmpty()) return@mapNotNull null
                    val numScore = doc.getDouble("numericScore") ?: 0.0
                    val scoreStr = doc.getString("score") ?: ""
                    val isRx = doc.getBoolean("isRx") ?: false
                    Triple(compId, Triple(numScore, scoreStr, isRx), doc.getString("wodName"))
                }

                val compIds = resultsByComp.map { it.first }.toSet()
                if (compIds.isEmpty()) {
                    _trophies.value = emptyList()
                    _isLoadingTrophies.value = false
                    return@launch
                }

                val earned = mutableListOf<Trophy>()

                for (compId in compIds) {
                    val compDoc = db.collection("competitions").document(compId).get().await()
                    val title = compDoc.getString("title") ?: continue
                    val isActive = compDoc.getBoolean("isActive") ?: true
                    if (isActive) continue

                    val endDateTimestamp = compDoc.getTimestamp("endDate")
                    val endDate = endDateTimestamp?.toDate() ?: Date()

                    val rankSnap = db.collection("wod_results")
                        .whereEqualTo("competitionId", compId)
                        .whereEqualTo("validationStatus", "approved")
                        .get().await()

                    val ranked = rankSnap.documents.mapNotNull { d ->
                        val uid = d.getString("userId") ?: return@mapNotNull null
                        val s = d.getDouble("numericScore") ?: 0.0
                        uid to s
                    }.sortedByDescending { it.second }

                    val position = ranked.indexOfFirst { it.first == userId }
                    if (position != -1) {
                        val rank = position + 1
                        if (rank <= 3) {
                            val myResult = resultsByComp.firstOrNull { it.first == compId }?.second
                            earned.add(
                                Trophy(
                                    id = "${compId}_${rank}",
                                    competitionId = compId,
                                    competitionName = title,
                                    rank = rank,
                                    score = myResult?.second ?: "",
                                    isRx = myResult?.third ?: false,
                                    date = endDate
                                )
                            )
                        }
                    }
                }

                _trophies.value = earned.sortedBy { it.rank }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingTrophies.value = false
            }
        }
    }

    fun loadPodiums(gymId: String) {
        viewModelScope.launch {
            _isLoadingPodiums.value = true
            try {
                val compSnap = db.collection("competitions")
                    .whereEqualTo("gym_id", gymId)
                    .whereEqualTo("isActive", false)
                    .orderBy("endDate", Query.Direction.DESCENDING)
                    .limit(10)
                    .get().await()

                val builtPodiums = mutableListOf<CompetitionPodium>()

                for (compDoc in compSnap.documents) {
                    val compId = compDoc.id
                    val title = compDoc.getString("title") ?: continue
                    val endDate = compDoc.getTimestamp("endDate")?.toDate() ?: Date()
                    val prize = compDoc.getString("prizeDescription") ?: ""

                    val rankSnap = try {
                        db.collection("wod_results")
                            .whereEqualTo("competitionId", compId)
                            .whereEqualTo("validationStatus", "approved")
                            .get().await()
                    } catch (e: Exception) {
                        continue
                    }

                    if (rankSnap.isEmpty) continue

                    val bestByUser = mutableMapOf<String, Triple<Double, String, Boolean>>()
                    for (d in rankSnap.documents) {
                        val uid = d.getString("userId") ?: continue
                        val numericScore = d.getDouble("numericScore") ?: 0.0
                        val scoreStr = d.getString("score") ?: ""
                        val isRx = d.getBoolean("isRx") ?: false

                        val currentBest = bestByUser[uid]?.first ?: 0.0
                        if (bestByUser[uid] == null || numericScore > currentBest) {
                            bestByUser[uid] = Triple(numericScore, scoreStr, isRx)
                        }
                    }

                    val top3 = bestByUser.entries.sortedByDescending { it.value.first }.take(3)
                    fetchUsersIfNeeded(top3.map { it.key })

                    val entries = top3.mapIndexed { idx, entry ->
                        val uid = entry.key
                        val result = entry.value
                        val cached = userCache[uid]
                        PodiumEntry(
                            id = uid,
                            rank = idx + 1,
                            userName = cached?.first ?: "Atleta",
                            profileImageUrl = cached?.second,
                            score = result.second,
                            isRx = result.third
                        )
                    }

                    if (entries.isNotEmpty()) {
                        builtPodiums.add(
                            CompetitionPodium(
                                id = compId,
                                competitionName = title,
                                endDate = endDate,
                                prizeDescription = prize,
                                winners = entries
                            )
                        )
                    }
                }

                _podiums.value = builtPodiums.sortedByDescending { it.endDate }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingPodiums.value = false
            }
        }
    }

    fun loadRecentRecords(gymId: String) {
        viewModelScope.launch {
            _isLoadingRecords.value = true
            try {
                val snap = db.collection("wod_results")
                    .whereEqualTo("gym_id", gymId)
                    .whereEqualTo("isPublic", true)
                    .orderBy("date", Query.Direction.DESCENDING)
                    .limit(20)
                    .get().await()

                val rawRecords = snap.documents.mapNotNull { doc ->
                    val uid = doc.getString("userId") ?: return@mapNotNull null
                    val scoreStr = doc.getString("score") ?: return@mapNotNull null
                    val date = doc.getTimestamp("date")?.toDate() ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    val reactionsMap = doc.get("reactions") as? Map<String, String> ?: emptyMap()

                    GlobalRecord(
                        id = doc.id,
                        userId = uid,
                        userName = doc.getString("userName") ?: "Atleta",
                        profileImageUrl = doc.getString("userProfileImageUrl"),
                        wodName = doc.getString("benchmarkName") ?: "WOD",
                        score = scoreStr,
                        isRx = doc.getBoolean("isRx") ?: false,
                        date = date,
                        isChallenge = false,
                        reactions = reactionsMap
                    )
                }

                val challengeSnap = db.collection("challenge_results")
                    .whereEqualTo("gym_id", gymId)
                    .limit(20)
                    .get().await()

                val challengeRecords = challengeSnap.documents.mapNotNull { doc ->
                    val uid = doc.getString("userId") ?: return@mapNotNull null
                    val scoreStr = doc.getString("score") ?: return@mapNotNull null
                    val date = doc.getTimestamp("date")?.toDate() ?: return@mapNotNull null
                    @Suppress("UNCHECKED_CAST")
                    val reactionsMap = doc.get("reactions") as? Map<String, String> ?: emptyMap()

                    GlobalRecord(
                        id = doc.id,
                        userId = uid,
                        userName = doc.getString("userName") ?: "Atleta",
                        profileImageUrl = doc.getString("userProfileImage"),
                        wodName = doc.getString("challengeName") ?: "Desafío",
                        score = scoreStr,
                        isRx = doc.getBoolean("isRx") ?: false,
                        date = date,
                        isChallenge = true,
                        reactions = reactionsMap
                    )
                }

                val allRaw = (rawRecords + challengeRecords).sortedByDescending { it.date }.take(20)
                fetchUsersIfNeeded(allRaw.map { it.userId }.distinct())

                val enriched = allRaw.map { record ->
                    val cached = userCache[record.userId]
                    record.userName = cached?.first ?: record.userName
                    record.profileImageUrl = cached?.second ?: record.profileImageUrl
                    record
                }

                _recentRecords.value = enriched
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingRecords.value = false
            }
        }
    }

    fun toggleReaction(recordId: String, emoji: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val idx = _recentRecords.value.indexOfFirst { it.id == recordId }
            if (idx == -1) return@launch
            val updatedRecords = _recentRecords.value.toMutableList()
            val record = updatedRecords[idx].copy()
            val mutableReactions = record.reactions.toMutableMap()
            
            if (mutableReactions[userId] == emoji) {
                mutableReactions.remove(userId)
            } else {
                mutableReactions[userId] = emoji
            }
            record.reactions = mutableReactions
            updatedRecords[idx] = record
            _recentRecords.value = updatedRecords

            try {
                val collectionName = if (record.isChallenge) "challenge_results" else "wod_results"
                val docRef = db.collection(collectionName).document(recordId)
                if (mutableReactions[userId] != null) {
                    docRef.update("reactions.${userId}", emoji).await()
                } else {
                    docRef.update("reactions.${userId}", com.google.firebase.firestore.FieldValue.delete()).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun fetchUsersIfNeeded(userIds: List<String>) {
        val missing = userIds.filter { userCache[it] == null }
        if (missing.isEmpty()) return

        missing.chunked(10).forEach { chunk ->
            try {
                val snap = db.collection("users")
                    .whereIn(FieldPath.documentId(), chunk)
                    .get().await()

                for (doc in snap.documents) {
                    val name = "${doc.getString("name") ?: ""} ${doc.getString("lastName") ?: ""}".trim()
                    val avatar = doc.getString("profileImageUrl")
                    userCache[doc.id] = Pair(name, avatar)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
