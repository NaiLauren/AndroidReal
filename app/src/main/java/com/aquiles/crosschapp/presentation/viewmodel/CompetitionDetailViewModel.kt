package com.aquiles.crosschapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.Competition
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

// Modelo simple para resultados en ranking
data class CompetitionResultEntry(
    val resultId: String = "",
    val userId: String = "",
    val userName: String = "Atleta",
    val userProfileUrl: String? = null,
    val score: String = "",
    val numericScore: Double = 0.0,
    val isRx: Boolean = false,
    val validationStatus: String = "pending" // "approved", "pending", "rejected"
) {
    val isPending get() = validationStatus == "pending"
    val isApproved get() = validationStatus == "approved"
}

class CompetitionDetailViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _competition = MutableStateFlow<Competition?>(null)
    val competition: StateFlow<Competition?> = _competition.asStateFlow()

    private val _linkedClasses = MutableStateFlow<List<GymClass>>(emptyList())
    val linkedClasses: StateFlow<List<GymClass>> = _linkedClasses.asStateFlow()

    private val _isLoadingClasses = MutableStateFlow(false)
    val isLoadingClasses: StateFlow<Boolean> = _isLoadingClasses.asStateFlow()

    // For Class Selector
    private val _classesForDate = MutableStateFlow<List<GymClass>>(emptyList())
    val classesForDate: StateFlow<List<GymClass>> = _classesForDate.asStateFlow()
    
    private val _isLoadingSelector = MutableStateFlow(false)
    val isLoadingSelector: StateFlow<Boolean> = _isLoadingSelector.asStateFlow()

    // --- RANKING ---
    private val _competitionResults = MutableStateFlow<List<CompetitionResultEntry>>(emptyList())
    val competitionResults: StateFlow<List<CompetitionResultEntry>> = _competitionResults.asStateFlow()

    private val _isLoadingRanking = MutableStateFlow(false)
    val isLoadingRanking: StateFlow<Boolean> = _isLoadingRanking.asStateFlow()

    private val _isSubmittingScore = MutableStateFlow(false)
    val isSubmittingScore: StateFlow<Boolean> = _isSubmittingScore.asStateFlow()

    // Participantes (usuarios inscritos en las clases vinculadas)
    private val _enrolledUsers = MutableStateFlow<List<User>>(emptyList())
    val enrolledUsers: StateFlow<List<User>> = _enrolledUsers.asStateFlow()

    fun loadCompetition(competitionId: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("competitions").document(competitionId).get().await()
                val comp = snapshot.toObject(Competition::class.java)
                if (comp != null) {
                    setCompetition(comp)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setCompetition(comp: Competition) {
        _competition.value = comp
        loadLinkedClasses(comp)
    }

    private fun loadLinkedClasses(comp: Competition) {
        val ids = comp.linkedClassIds
        if (ids.isEmpty()) {
            _linkedClasses.value = emptyList()
            return
        }

        _isLoadingClasses.value = true
        viewModelScope.launch {
            try {
                val chunks = ids.chunked(10)
                val allFetchedClasses = mutableListOf<GymClass>()

                for (chunk in chunks) {
                    val snapshot = db.collection("gymClasses")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .await()
                    allFetchedClasses.addAll(snapshot.toObjects(GymClass::class.java))
                }
                
                _linkedClasses.value = allFetchedClasses.sortedBy { it.dateTime }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingClasses.value = false
            }
        }
    }

    fun loadClassesForDate(date: Date, gymId: String) {
        _isLoadingSelector.value = true
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                calendar.time = date
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.time

                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = calendar.time

                val snapshot = db.collection("gymClasses")
                    .whereEqualTo("gym_id", gymId)
                    .whereGreaterThanOrEqualTo("dateTime", startOfDay)
                    .whereLessThan("dateTime", endOfDay)
                    .orderBy("dateTime")
                    .get()
                    .await()

                val classes = snapshot.toObjects(GymClass::class.java)
                _classesForDate.value = classes.filter { !it.isCancelled }

            } catch (e: Exception) {
                e.printStackTrace()
                _classesForDate.value = emptyList()
            } finally {
                _isLoadingSelector.value = false
            }
        }
    }

    fun linkClasses(newClasses: List<GymClass>) {
        val currentComp = _competition.value ?: return
        val compId = currentComp.id
        if (compId.isBlank()) return

        val newIds = newClasses.map { it.documentId }
        val updatedIds = (currentComp.linkedClassIds + newIds).distinct()

        viewModelScope.launch {
            try {
                db.collection("competitions").document(compId)
                    .update("linked_class_ids", updatedIds)
                    .await()
                
                for (classId in newIds) {
                    db.collection("gymClasses").document(classId).update(
                        mapOf(
                            "classType" to "COMPETITION",
                            "competitionId" to compId
                        )
                    ).await()
                }

                val updatedComp = currentComp.copy(linkedClassIds = updatedIds)
                setCompetition(updatedComp) 
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun createAndLinkCompetitionClass(gymId: String, dateTime: Date, durationMinutes: Int, maxCapacity: Int) {
        val currentComp = _competition.value ?: return
        val compId = currentComp.id
        if (compId.isBlank()) return

        _isLoadingClasses.value = true
        viewModelScope.launch {
            try {
                val newClassRef = db.collection("gymClasses").document()
                val newClass = GymClass(
                    documentId = newClassRef.id,
                    gym_id = gymId,
                    name = currentComp.title,
                    dateTime = dateTime,
                    durationMinutes = durationMinutes,
                    maxCapacity = maxCapacity,
                    classType = "COMPETITION",
                    competitionId = compId,
                    isOpenGym = false,
                    isCancelled = false,
                    coachName = "Organización",
                    enrolledUserIds = emptyList(),
                    waitingList = emptyList(),
                    hexColor = "#FFD700"
                )

                newClassRef.set(newClass).await()

                val updatedIds = currentComp.linkedClassIds + newClassRef.id
                db.collection("competitions").document(compId)
                    .update("linked_class_ids", updatedIds)
                    .await()
                
                val updatedComp = currentComp.copy(linkedClassIds = updatedIds)
                setCompetition(updatedComp)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                loadLinkedClasses(_competition.value!!)
            }
        }
    }

    fun unlinkClass(classId: String) {
        val currentComp = _competition.value ?: return
        val compId = currentComp.id
        if (compId.isBlank()) return

        val updatedIds = currentComp.linkedClassIds.filter { it != classId }

        viewModelScope.launch {
            try {
                 db.collection("competitions").document(compId)
                    .update("linked_class_ids", updatedIds)
                    .await()
                
                db.collection("gymClasses").document(classId).update(
                    mapOf(
                        "classType" to "WOD",
                        "competitionId" to FieldValue.delete()
                    )
                ).await()

                val updatedComp = currentComp.copy(linkedClassIds = updatedIds)
                setCompetition(updatedComp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun finishCompetition() {
         val currentComp = _competition.value ?: return
         val compId = currentComp.id

         viewModelScope.launch {
             try {
                 db.collection("competitions").document(compId)
                    .update("isActive", false)
                    .await()
                 
                 val updatedComp = currentComp.copy(isActive = false)
                 _competition.value = updatedComp
             } catch (e: Exception) {
                 e.printStackTrace()
             }
         }
    }
    
    fun editCompetition(title: String, description: String, prizeDescription: String?) {
        val currentComp = _competition.value ?: return
        val compId = currentComp.id
        if (compId.isBlank()) return

        viewModelScope.launch {
            try {
                val compRef = db.collection("competitions").document(compId)
                val compUpdates = mutableMapOf<String, Any>(
                    "title" to title,
                    "description" to description
                )
                if (prizeDescription.isNullOrBlank()) {
                    compUpdates["prizeDescription"] = FieldValue.delete()
                } else {
                    compUpdates["prizeDescription"] = prizeDescription
                }
                
                db.runBatch { batch ->
                    batch.update(compRef, compUpdates)
                    
                    _linkedClasses.value.forEach { gymClass ->
                        val classRef = db.collection("gymClasses").document(gymClass.documentId)
                        batch.update(classRef, mapOf(
                            "name" to title,
                            "description" to description
                        ))
                        
                        gymClass.wodId?.let { wodId ->
                            val wodRef = db.collection("wods").document(wodId)
                            batch.update(wodRef, mapOf(
                                "title" to title,
                                "description" to description
                            ))
                        }
                    }
                }.await()

                val updatedComp = currentComp.copy(
                    title = title,
                    description = description,
                    prizeDescription = if (prizeDescription.isNullOrBlank()) null else prizeDescription
                )
                setCompetition(updatedComp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // -------------------------------------------------------
    // RANKING DE COMPETENCIA
    // -------------------------------------------------------

    /** Carga todos los resultados de la competencia (pendientes + aprobados, excluye rechazados) */
    fun loadCompetitionRanking() {
        val compId = _competition.value?.id ?: return
        val gymId = _competition.value?.gymId ?: return
        _isLoadingRanking.value = true

        viewModelScope.launch {
            try {
                // 1. Fetch todos los resultados de la competencia
                val snapshot = db.collection("wod_results")
                    .whereEqualTo("gym_id", gymId)
                    .whereEqualTo("competitionId", compId)
                    .get().await()

                val rawResults = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val status = data["validationStatus"] as? String ?: "pending"
                    if (status == "rejected") return@mapNotNull null // excluir rechazados

                    val userId = data["userId"] as? String ?: return@mapNotNull null
                    val score = data["score"] as? String ?: ""
                    val numericScore = (data["numericScore"] as? Number)?.toDouble()
                        ?: score.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                    val isRx = data["isRx"] as? Boolean ?: false

                    CompetitionResultEntry(
                        resultId = doc.id,
                        userId = userId,
                        userName = data["userName"] as? String ?: "Atleta",
                        score = score,
                        numericScore = numericScore,
                        isRx = isRx,
                        validationStatus = status
                    )
                }

                // 2. Fetch nombres de usuarios
                val userIds = rawResults.map { it.userId }.toSet().toList()
                val userNames = mutableMapOf<String, Pair<String, String?>>() // userId -> (name, profileUrl)
                userIds.chunked(10).forEach { chunk ->
                    val usersSnap = db.collection("users")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get().await()
                    usersSnap.documents.forEach { uDoc ->
                        val uData = uDoc.data ?: return@forEach
                        val name = "${uData["name"] ?: ""} ${uData["lastName"] ?: ""}".trim()
                        val profileUrl = uData["profileImageUrl"] as? String
                        userNames[uDoc.id] = Pair(name.ifBlank { "Atleta" }, profileUrl)
                    }
                }

                // 3. Merge y enriquecer
                val enriched = rawResults.map { entry ->
                    val (name, profileUrl) = userNames[entry.userId] ?: Pair(entry.userName, null)
                    entry.copy(userName = name, userProfileUrl = profileUrl)
                }

                // 4. Ordenar: aprobados primero, luego por score desc
                val sorted = enriched.sortedWith(compareByDescending<CompetitionResultEntry> { it.isApproved }
                    .thenByDescending { it.numericScore })

                _competitionResults.value = sorted

                // 5. Cargar participantes inscritos (desde las clases vinculadas)
                val allAttendeeIds = _linkedClasses.value.flatMap { it.enrolledUserIds }.toSet().toList()
                if (allAttendeeIds.isNotEmpty()) {
                    val users = mutableListOf<User>()
                    allAttendeeIds.chunked(10).forEach { chunk ->
                        val usSnap = db.collection("users")
                            .whereIn(FieldPath.documentId(), chunk)
                            .get().await()
                        users.addAll(usSnap.toObjects(User::class.java))
                    }
                    _enrolledUsers.value = users
                } else {
                    // Fallback: usar los usuarios de los resultados
                    val users = mutableListOf<User>()
                    userIds.chunked(10).forEach { chunk ->
                        val usSnap = db.collection("users")
                            .whereIn(FieldPath.documentId(), chunk)
                            .get().await()
                        users.addAll(usSnap.toObjects(User::class.java))
                    }
                    _enrolledUsers.value = users
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoadingRanking.value = false
            }
        }
    }

    /** Admin inyecta un resultado para un participante (queda directamente aprobado) */
    fun submitScoreForUser(userId: String, userName: String, score: String, notes: String, isRx: Boolean) {
        val comp = _competition.value ?: return
        val compId = comp.id
        if (compId.isBlank() || score.isBlank()) return

        val wodId = _linkedClasses.value.firstOrNull()?.wodId ?: compId
        val numericScore = score.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0

        _isSubmittingScore.value = true
        viewModelScope.launch {
            try {
                val result = hashMapOf(
                    "userId" to userId,
                    "wodId" to wodId,
                    "score" to score,
                    "notes" to notes,
                    "isRx" to isRx,
                    "gym_id" to (comp.gymId),
                    "classSessionId" to (_linkedClasses.value.firstOrNull()?.documentId ?: ""),
                    "isPublic" to true,
                    "validationStatus" to "approved", // Admin = auto aprobado
                    "competitionId" to compId,
                    "numericScore" to numericScore,
                    "wodName" to comp.title,
                    "userName" to userName,
                    "date" to Timestamp.now()
                )

                db.collection("wod_results").add(result).await()

                // Refrescar ranking
                loadCompetitionRanking()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSubmittingScore.value = false
            }
        }
    }
}


