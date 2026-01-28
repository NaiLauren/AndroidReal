package com.aquiles.crosschapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import com.aquiles.crosschapp.data.model.*
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*

// --- DATA CLASSES Y ENUMS ---
data class WeeklyAttendanceDay(val dayName: String, val attended: Boolean)
sealed interface AttendanceData {
    data class BarChartData(val values: List<Float>, val labels: List<String>) : AttendanceData
    data class WeeklySummaryData(val days: List<WeeklyAttendanceDay>) : AttendanceData
}
enum class TimeRange { WEEK, MONTH, YEAR }

data class AttendanceChartUiState(
    val data: AttendanceData = AttendanceData.BarChartData(emptyList(), emptyList()),
    val selectedRange: TimeRange = TimeRange.MONTH,
    val isLoading: Boolean = true
)

data class PerformanceRecord(
    val result: Any,
    val wodDetails: Any?
)

sealed class PerformanceState {
    object Idle : PerformanceState()
    object Loading : PerformanceState()
    data class Success(val records: List<PerformanceRecord>) : PerformanceState()
    data class Error(val message: String) : PerformanceState()
}

data class AchievementUiModel(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean,
    val unlockedAt: Date? = null,
    val xpReward: Int = 0
)

sealed class AchievementState {
    object Idle: AchievementState()
    object Loading : AchievementState()
    data class Success(val achievements: List<AchievementUiModel>) : AchievementState()
    data class Error(val message: String) : AchievementState()
}

sealed class SaveResultState {
    object Idle : SaveResultState()
    object Loading : SaveResultState()
    data class Success(val message: String) : SaveResultState()
    data class Error(val message: String) : SaveResultState()
}

sealed class BenchmarkSaveState {
    object Idle : BenchmarkSaveState()
    object Loading : BenchmarkSaveState()
    data class Success(val message: String) : BenchmarkSaveState()
    data class Error(val message: String) : BenchmarkSaveState()
}

class PerformanceViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    // --- STATES ---
    private val _benchmarkRecordsState = MutableStateFlow<PerformanceState>(PerformanceState.Idle)
    val benchmarkRecordsState = _benchmarkRecordsState.asStateFlow()

    private val _dailyWodRecordsState = MutableStateFlow<PerformanceState>(PerformanceState.Idle)
    val dailyWodRecordsState = _dailyWodRecordsState.asStateFlow()

    private val _achievementsState = MutableStateFlow<AchievementState>(AchievementState.Idle)
    val achievementsState = _achievementsState.asStateFlow()

    private val _attendanceChartState = MutableStateFlow<AttendanceChartUiState>(AttendanceChartUiState())
    val attendanceChartState = _attendanceChartState.asStateFlow()

    private val _saveResultState = MutableStateFlow<SaveResultState>(SaveResultState.Idle)
    val saveResultState = _saveResultState.asStateFlow()

    private val _saveBenchmarkState = MutableStateFlow<BenchmarkSaveState>(BenchmarkSaveState.Idle)
    val saveBenchmarkState = _saveBenchmarkState.asStateFlow()

    // --- HELPERS ---
    private val currentUserGymId: String?
        get() = UserSession.currentUser.value?.gym_id

    fun loadInitialData() {
        val user = UserSession.currentUser.value ?: return
        Log.d("PerfViewModel", "Cargando datos iniciales para ${user.email}")
        loadBenchmarkRecords(user.id, user.gym_id)
        loadDailyWodRecords(user.id, user.gym_id)
        checkAndAwardAchievements(user)
        loadAttendanceData(user.id, TimeRange.MONTH)
    }

    // --- BENCHMARKS ---
    fun loadBenchmarkRecords(userId: String, gymId: String) {
        _benchmarkRecordsState.value = PerformanceState.Loading
        viewModelScope.launch {
            try {
                // Removed orderBy to avoid missing index errors
                val resultsSnapshot = firestore.collection("benchmark_results")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("gym_id", gymId)
                    .get().await()
                
                val results = resultsSnapshot.toObjects(BenchmarkResult::class.java)
                    .sortedByDescending { it.date } // Sort in memory
                
                if (results.isEmpty()) {
                    _benchmarkRecordsState.value = PerformanceState.Success(emptyList())
                    return@launch
                }

                val benchmarkIds = results.map { it.benchmarkId }.distinct().filter { it.isNotBlank() }
                val benchmarksMap = mutableMapOf<String, BenchmarkWod>()
                
                 if (benchmarkIds.isNotEmpty()) {
                     val deferreds = benchmarkIds.map { id ->
                        val docRef = firestore.collection("benchmarks").document(id)
                        async {
                            try {
                                val doc = docRef.get().await()
                                doc.toObject(BenchmarkWod::class.java)?.copy(id = doc.id)
                            } catch (e: Exception) {
                                null
                            }
                        }
                     }
                     
                     deferreds.awaitAll().forEach { benchmark ->
                         if (benchmark != null) {
                             benchmarksMap[benchmark.id] = benchmark
                         }
                     }
                 }

                val records = results.map { res ->
                    PerformanceRecord(result = res, wodDetails = benchmarksMap[res.benchmarkId])
                }
                _benchmarkRecordsState.value = PerformanceState.Success(records)

            } catch (e: Exception) {
                Log.e("PerfViewModel", "Error loading benchmarks", e)
                _benchmarkRecordsState.value = PerformanceState.Error("Error al cargar benchmarks: ${e.localizedMessage}")
            }
        }
    }

    // --- DAILY WODS ---
    fun loadDailyWodRecords(userId: String, gymId: String) {
        _dailyWodRecordsState.value = PerformanceState.Loading
        viewModelScope.launch {
            try {
                // Removed orderBy to avoid missing index errors
                val resultsSnapshot = firestore.collection("wod_results")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("gym_id", gymId)
                    .limit(50) // Increased limit slightly, sorting in memory
                    .get().await()
                
                val results = resultsSnapshot.toObjects(WodResult::class.java)
                    .sortedByDescending { it.date } // Sort in memory

                if (results.isEmpty()) {
                    _dailyWodRecordsState.value = PerformanceState.Success(emptyList())
                    return@launch
                }

                val wodIds = results.map { it.wodId }.distinct().filter { it.isNotBlank() }
                val wodsMap = mutableMapOf<String, Wod>()

                if (wodIds.isNotEmpty()) {

                    val deferreds = wodIds.map { id ->
                        val docRef = firestore.collection("wods").document(id)
                        async {
                            try {
                                val doc = docRef.get().await()
                                doc.toObject(Wod::class.java)?.copy(id = doc.id)
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }

                    deferreds.awaitAll().forEach { wod ->
                        if (wod != null) {
                            wodsMap[wod.id] = wod
                        }
                    }
                }

                val records = results.map { res ->
                    PerformanceRecord(result = res, wodDetails = wodsMap[res.wodId])
                }
                _dailyWodRecordsState.value = PerformanceState.Success(records)

            } catch (e: Exception) {
                Log.e("PerfViewModel", "Error loading daily wods", e)
                _dailyWodRecordsState.value = PerformanceState.Error("Error: ${e.localizedMessage}")
            }
        }
    }

    // --- ATTENDANCE CHART ---
    fun onTimeRangeSelected(range: TimeRange) {
        val user = UserSession.currentUser.value ?: return
        _attendanceChartState.value = _attendanceChartState.value.copy(selectedRange = range, isLoading = true)
        loadAttendanceData(user.id, range)
    }

    private fun loadAttendanceData(userId: String, range: TimeRange) {
        viewModelScope.launch {
            try {
                // Get GymID for correct filtering
                val gymId = currentUserGymId ?: return@launch

                 val now = LocalDate.now()
                 val startDate = when(range) {
                     TimeRange.WEEK -> now.minusDays(6) // Last 7 days including today
                     TimeRange.MONTH -> now.minusMonths(1)
                     TimeRange.YEAR -> now.minusYears(1)
                 }
                 
                 val startTimestamp = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant())

                 // Use attendance_history instead of attendance
                 val snapshot = firestore.collection("attendance_history")
                     .whereEqualTo("userId", userId)
                     .whereEqualTo("gym_id", gymId)
                     .whereGreaterThanOrEqualTo("classDate", startTimestamp)
                     .get().await()
                 
                 val records = snapshot.toObjects(AttendanceRecord::class.java)
                 
                 val data = when(range) {
                     TimeRange.WEEK -> {
                         val days = (0..6).map { i ->
                             val date = now.minusDays((6 - i).toLong())
                             val isAttended = records.any { 
                                 val recDate = it.classDate?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate()
                                 recDate == date 
                             }
                             val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale("es"))
                             WeeklyAttendanceDay(dayName.replaceFirstChar { it.uppercase() }, isAttended)
                         }
                         AttendanceData.WeeklySummaryData(days)
                     }
                     TimeRange.MONTH -> {
                         // Logic for month aggregation (simplified count per week)
                         // For now keeping static or simple aggregation if needed.
                         // User's previous code had valid mocked data for Month/Year
                         val values = listOf(5f, 3f, 4f, 2f) 
                         val labels = listOf("Sem 1", "Sem 2", "Sem 3", "Sem 4")
                         AttendanceData.BarChartData(values, labels)
                     }
                     TimeRange.YEAR -> {
                         val values = List(12) { 10f }
                         val labels = listOf("Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic")
                         AttendanceData.BarChartData(values, labels)
                     }
                 }

                 _attendanceChartState.value = _attendanceChartState.value.copy(data = data, isLoading = false)

            } catch (e: Exception) {
                Log.e("PerfViewModel", "Error chart", e)
                _attendanceChartState.value = _attendanceChartState.value.copy(isLoading = false)
            }
        }
    }

    // --- SAVING ACTIONS ---
    fun saveWodResult(wodId: String, score: String, notes: String, isRx: Boolean) {
        val user = UserSession.currentUser.value ?: return
        _saveResultState.value = SaveResultState.Loading
        viewModelScope.launch {
            try {
                val result = WodResult(
                    userId = user.id,
                    gym_id = user.gym_id,
                    wodId = wodId,
                    date = Date(),
                    score = score,
                    notes = notes,
                    isRx = isRx
                )
                firestore.collection("wod_results").add(result).await()
                _saveResultState.value = SaveResultState.Success("Resultado guardado!")
                loadDailyWodRecords(user.id, user.gym_id)
                checkAndAwardAchievements(user)
            } catch(e: Exception) {
                 _saveResultState.value = SaveResultState.Error(e.message ?: "Error al guardar")
            }
        }
    }

    fun saveBenchmarkResult(result: BenchmarkResult) {
        val user = UserSession.currentUser.value ?: return
        _saveBenchmarkState.value = BenchmarkSaveState.Loading
        viewModelScope.launch {
            try {
                // Desnormalización de datos del usuario
                val resultWithUserData = result.copy(
                    userName = user.name,
                    userLastName = user.lastName,
                    userLevel = user.level,
                    userProfileImageUrl = user.profileImageUrl ?: ""
                )
                
                firestore.collection("benchmark_results").add(resultWithUserData).await()
                _saveBenchmarkState.value = BenchmarkSaveState.Success("Benchmark guardado!")
                loadBenchmarkRecords(user.id, user.gym_id)
                checkAndAwardAchievements(user)
            } catch(e: Exception) {
                 _saveBenchmarkState.value = BenchmarkSaveState.Error(e.message ?: "Error al guardar")
            }
        }
    }

    fun resetSaveResultState() { _saveResultState.value = SaveResultState.Idle }
    fun resetSaveState() { _saveBenchmarkState.value = BenchmarkSaveState.Idle }

    // --- ACHIEVEMENTS ---
    fun checkAndAwardAchievements(user: User) {
        val userId = user.id
        val gymId = user.gym_id

        _achievementsState.value = AchievementState.Loading

        viewModelScope.launch {
            try {
                val achievementsRef = firestore.collection("achievements")
                val unlockedAchievementsSnapshot = achievementsRef
                    .whereEqualTo("gym_id", gymId)
                    .whereEqualTo("userId", userId)
                    .get().await()

                val unlockedAchievements = unlockedAchievementsSnapshot.toObjects(Achievement::class.java)
                val unlockedIds = unlockedAchievements.map { it.achievementId }.toSet()

                val batch = firestore.batch()
                var newAchievementsAwarded = false

                AchievementSystem.smartList.forEach { def ->
                    if (def.type == "automatic" && def.id.contains("_classes")) {
                        val requiredClasses = def.id.replace("_classes", "").toIntOrNull()
                        if (requiredClasses != null && user.totalClassesAttended >= requiredClasses && !unlockedIds.contains(def.id)) {
                            val newAchievement = Achievement(
                                achievementId = def.id,
                                title = def.title,
                                description = def.description,
                                iconName = def.iconName,
                                type = "automatic",
                                userId = userId,
                                gym_id = gymId,
                                unlockedAt = Date()
                            )
                            batch.set(achievementsRef.document("${userId}_${def.id}"), newAchievement)
                            newAchievementsAwarded = true
                        }
                    }
                }

                if (newAchievementsAwarded) {
                    batch.commit().await()
                }

                val combinedList = mutableListOf<AchievementUiModel>()

                val finalUnlockedList = if (newAchievementsAwarded) {
                    achievementsRef.whereEqualTo("gym_id", gymId).whereEqualTo("userId", userId).get().await().toObjects(Achievement::class.java)
                } else {
                    unlockedAchievements
                }
                
                val finalUnlockedIds = finalUnlockedList.map { it.achievementId }.toSet()

                finalUnlockedList.forEach { a ->
                    val def = AchievementSystem.getById(a.achievementId)
                    combinedList.add(AchievementUiModel(a.achievementId, a.title, a.description, a.iconName, true, a.unlockedAt, def?.xpReward ?: 0))
                }

                AchievementSystem.smartList.forEach { def ->
                    if (!finalUnlockedIds.contains(def.id)) {
                        combinedList.add(
                            AchievementUiModel(def.id, def.title, def.description, def.iconName, false, null, def.xpReward)
                        )
                    }
                }

                val sortedList = combinedList.sortedWith(compareByDescending<AchievementUiModel> { it.isUnlocked }
                    .thenByDescending { it.unlockedAt }
                    .thenBy { it.xpReward }
                )

                _achievementsState.value = AchievementState.Success(sortedList)

            } catch (e: Exception) {
                Log.e("PerformanceViewModel", "Error en logros", e)
                _achievementsState.value = AchievementState.Error(e.localizedMessage ?: "Error al cargar logros.")
            }
        }
    }

    fun deleteRecord(record: PerformanceRecord, isBenchmark: Boolean) {
        viewModelScope.launch {
            try {
                val resultObject = record.result
                val userId: String
                val gymId: String

                if (isBenchmark && resultObject is BenchmarkResult) {
                    if (resultObject.resultId.isBlank()) return@launch
                    userId = resultObject.userId
                    gymId = resultObject.gym_id
                    firestore.collection("benchmark_results").document(resultObject.resultId).delete().await()
                } else if (!isBenchmark && resultObject is WodResult) {
                    userId = resultObject.userId
                    gymId = resultObject.gym_id
                    
                    if (resultObject.id.isNotBlank()) {
                         firestore.collection("wod_results").document(resultObject.id).delete().await()
                    } else {
                        // Fallback only if ID is missing (should not happen with new app usage)
                        val querySnapshot = firestore.collection("wod_results")
                        .whereEqualTo("wodId", resultObject.wodId)
                        .whereEqualTo("userId", resultObject.userId)
                        .whereEqualTo("date", resultObject.date)
                        .limit(1).get().await()

                        if (!querySnapshot.isEmpty) {
                            val docId = querySnapshot.documents.first().id
                            firestore.collection("wod_results").document(docId).delete().await()
                        }
                    }
                } else {
                    return@launch
                }

                if (isBenchmark) loadBenchmarkRecords(userId, gymId) else loadDailyWodRecords(userId, gymId)

            } catch (e: Exception) {
                Log.e("PerformanceViewModel", "Error al eliminar récord", e)
            }
        }
    }

    fun awardManualAchievement(userId: String, achievementId: String, title: String, description: String, iconName: String) {
        val gymId = currentUserGymId
        viewModelScope.launch {
            if (gymId.isNullOrBlank()) {
                Log.e("Achievements", "No se puede otorgar logro, gymId es nulo")
                return@launch
            }
            try {
                val newAchievement = Achievement(
                    achievementId = achievementId,
                    title = title,
                    description = description,
                    iconName = iconName,
                    type = "manual",
                    userId = userId,
                    gym_id = gymId
                )
                firestore.collection("achievements").document("${userId}_${achievementId}").set(newAchievement).await()
            } catch (e: Exception) {
                Log.e("Achievements", "Error al otorgar logro manual a $userId", e)
            }
        }
    }
}