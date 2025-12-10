package com.aquiles.crosschapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.*
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.*
import kotlinx.coroutines.flow.StateFlow

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

sealed class AchievementState {
    object Idle: AchievementState()
    object Loading : AchievementState()
    data class Success(val achievements: List<Achievement>) : AchievementState()
    data class Error(val message: String) : AchievementState()
}

private data class AchievementData(
    val id: String,
    val title: String,
    val description: String,
    val iconName: String
)

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
    private val currentUserId: String? get() = UserSession.getCurrentUserId()
    private val currentUserGymId: String? get() = UserSession.currentUserGymId.value

    // Estados para la UI
    private val _benchmarkRecordsState = MutableStateFlow<PerformanceState>(PerformanceState.Idle)
    val benchmarkRecordsState = _benchmarkRecordsState.asStateFlow()

    private val _dailyWodRecordsState = MutableStateFlow<PerformanceState>(PerformanceState.Idle)
    val dailyWodRecordsState = _dailyWodRecordsState.asStateFlow()

    private val _achievementsState = MutableStateFlow<AchievementState>(AchievementState.Idle)
    val achievementsState = _achievementsState.asStateFlow()

    private val _attendanceChartState = MutableStateFlow(AttendanceChartUiState())
    val attendanceChartState = _attendanceChartState.asStateFlow()

    private val _saveResultState = MutableStateFlow<SaveResultState>(SaveResultState.Idle)
    val saveResultState = _saveResultState.asStateFlow()

    private val _saveBenchmarkState = MutableStateFlow<BenchmarkSaveState>(BenchmarkSaveState.Idle)
    val saveBenchmarkState: StateFlow<BenchmarkSaveState> = _saveBenchmarkState.asStateFlow()

    private val _availableBenchmarks = MutableStateFlow<List<BenchmarkWod>>(emptyList())
    val availableBenchmarks: StateFlow<List<BenchmarkWod>> = _availableBenchmarks.asStateFlow()

    // Variable local para caché de asistencia
    private var fullAttendanceHistory: List<AttendanceRecord> = emptyList()

    /**
     * Carga inicial de datos.
     * IMPORTANTE: Resetea los estados para evitar mostrar datos del usuario anterior.
     */
    fun loadInitialData() {
        val user = UserSession.currentUser.value
        val userId = user?.id
        val gymId = user?.gym_id

        // 1. LIMPIEZA OBLIGATORIA: Borrar datos en memoria del usuario anterior
        fullAttendanceHistory = emptyList()
        _attendanceChartState.value = AttendanceChartUiState(isLoading = true)
        _benchmarkRecordsState.value = PerformanceState.Loading
        _dailyWodRecordsState.value = PerformanceState.Loading
        _achievementsState.value = AchievementState.Loading

        if (userId.isNullOrBlank() || gymId.isNullOrBlank()) {
            val errorMsg = "Usuario no autenticado."
            _benchmarkRecordsState.value = PerformanceState.Error(errorMsg)
            _dailyWodRecordsState.value = PerformanceState.Error(errorMsg)
            _achievementsState.value = AchievementState.Error(errorMsg)
            _attendanceChartState.update { it.copy(isLoading = false) }
            return
        }

        // 2. Cargar datos frescos
        loadBenchmarkRecords(userId, gymId)
        loadDailyWodRecords(userId, gymId)
        checkAndAwardAchievements(user)
        loadAttendanceHistory(userId, gymId)
    }

    private fun loadAttendanceHistory(userId: String, gymId: String) {
        // CORRECCIÓN: Eliminado el chequeo "if (fullAttendanceHistory.isNotEmpty())"
        // para obligar a recargar los datos correctos del usuario actual.

        viewModelScope.launch {
            _attendanceChartState.update { it.copy(isLoading = true) }
            try {
                val snapshot = firestore.collection("attendance_history")
                    .whereEqualTo("gym_id", gymId)
                    .whereEqualTo("userId", userId)
                    .get().await()

                // Filtramos solo los que tienen fecha válida
                fullAttendanceHistory = snapshot.toObjects(AttendanceRecord::class.java).filter { it.classDate != null }

                // Procesamos con el rango seleccionado actualmente
                processAttendanceData(_attendanceChartState.value.selectedRange)
            } catch (e: Exception) {
                Log.e("PerformanceViewModel", "Error cargando historial de asistencia", e)
                _attendanceChartState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onTimeRangeSelected(range: TimeRange) {
        _attendanceChartState.update { it.copy(selectedRange = range) }
        // Si ya tenemos historial cargado, solo reprocesamos localmente
        if (fullAttendanceHistory.isNotEmpty()) {
            processAttendanceData(range)
        } else {
            // Si por alguna razón está vacío, intentamos recargar (fail-safe)
            val uid = currentUserId
            val gid = currentUserGymId
            if (uid != null && gid != null) loadAttendanceHistory(uid, gid)
        }
    }

    private fun processAttendanceData(range: TimeRange) {
        val data = when (range) {
            TimeRange.WEEK -> processForLast7Days(fullAttendanceHistory)
            TimeRange.MONTH -> processForLast30Days(fullAttendanceHistory)
            TimeRange.YEAR -> processForLast12Months(fullAttendanceHistory)
        }
        _attendanceChartState.update { it.copy(data = data, isLoading = false) }
    }

    private fun processForLast7Days(records: List<AttendanceRecord>): AttendanceData.WeeklySummaryData {
        val today = LocalDate.now()
        val days = mutableListOf<WeeklyAttendanceDay>()

        // Mapeamos las fechas a conteos
        val recordsByDate = records.groupingBy {
            it.classDate!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        }.eachCount()

        for (i in 6 downTo 0) {
            val date = today.minusDays(i.toLong())
            val attended = recordsByDate[date] != null && recordsByDate[date]!! > 0
            // Locale seguro
            val dayName = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-ES")).uppercase()
            days.add(WeeklyAttendanceDay(dayName = dayName, attended = attended))
        }
        return AttendanceData.WeeklySummaryData(days)
    }

    private fun processForLast30Days(records: List<AttendanceRecord>): AttendanceData.BarChartData {
        val today = LocalDate.now()
        val data = mutableListOf<Float>()
        val labels = mutableListOf<String>()
        val recordsByDate = records.groupingBy {
            it.classDate!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
        }.eachCount()

        for (i in 29 downTo 0) {
            val date = today.minusDays(i.toLong())
            data.add(recordsByDate[date]?.toFloat() ?: 0f)
            val label = when {
                i == 29 || i == 0 || (29 - i + 1) % 5 == 0 -> date.dayOfMonth.toString()
                else -> ""
            }
            labels.add(label)
        }
        return AttendanceData.BarChartData(data, labels)
    }

    private fun processForLast12Months(records: List<AttendanceRecord>): AttendanceData.BarChartData {
        val today = LocalDate.now()
        val data = mutableListOf<Float>()
        val labels = mutableListOf<String>()
        val recordsByMonth = records.groupingBy {
            val date = it.classDate!!.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
            Pair(date.year, date.month)
        }.eachCount()

        for (i in 11 downTo 0) {
            val monthDate = today.minusMonths(i.toLong())
            val key = Pair(monthDate.year, monthDate.month)
            data.add(recordsByMonth[key]?.toFloat() ?: 0f)
            val label = monthDate.month.getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-ES")).replace(".","").uppercase()
            labels.add(label)
        }
        return AttendanceData.BarChartData(data, labels)
    }

    fun saveWodResult(wodId: String, score: String, notes: String, isRx: Boolean) {
        val userId = currentUserId
        val gymId = currentUserGymId
        if (userId == null || gymId == null || score.isBlank()) {
            _saveResultState.value = SaveResultState.Error("Faltan datos para guardar.")
            return
        }
        viewModelScope.launch {
            _saveResultState.value = SaveResultState.Loading
            val resultData = WodResult(userId = userId, wodId = wodId, score = score.trim(), notes = notes.trim(), isRx = isRx, date = Date(), gym_id = gymId)
            try {
                firestore.collection("wod_results").add(resultData).await()
                _saveResultState.value = SaveResultState.Success("¡Marca guardada con éxito!")
                loadDailyWodRecords(userId, gymId)
            } catch (e: Exception) {
                _saveResultState.value = SaveResultState.Error(e.localizedMessage ?: "Error al guardar.")
            }
        }
    }

    fun saveBenchmarkResult(result: BenchmarkResult) {
        viewModelScope.launch {
            _saveBenchmarkState.value = BenchmarkSaveState.Loading
            try {
                firestore.collection("benchmark_results").document().set(result).await()
                _saveBenchmarkState.value = BenchmarkSaveState.Success("¡Récord guardado con éxito!")
                loadBenchmarkRecords(result.userId, result.gym_id)
            } catch (e: Exception) {
                _saveBenchmarkState.value = BenchmarkSaveState.Error(e.localizedMessage ?: "Error al guardar la marca")
                Log.e("PerformanceViewModel", "Error saving benchmark result", e)
            }
        }
    }

    fun resetSaveResultState() { _saveResultState.value = SaveResultState.Idle }
    fun resetSaveState() { _saveBenchmarkState.value = BenchmarkSaveState.Idle }

    fun loadAvailableBenchmarks(gymId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("benchmark_wods")
                    .whereEqualTo("gym_id", gymId)
                    .orderBy("name")
                    .get().await()
                _availableBenchmarks.value = snapshot.toObjects(BenchmarkWod::class.java)
            } catch (e: Exception) {
                Log.e("PerformanceViewModel", "Error loading available benchmarks", e)
            }
        }
    }

    fun loadBenchmarkRecords(userId: String, gymId: String) {
        viewModelScope.launch {
            _benchmarkRecordsState.value = PerformanceState.Loading
            try {
                val resultsSnapshot = firestore.collection("benchmark_results")
                    .whereEqualTo("gym_id", gymId)
                    .whereEqualTo("userId", userId)
                    .orderBy("date", Query.Direction.DESCENDING)
                    .get().await()

                val results = resultsSnapshot.toObjects(BenchmarkResult::class.java)
                if (results.isEmpty()) {
                    _benchmarkRecordsState.value = PerformanceState.Success(emptyList())
                    return@launch
                }
                val wodIds = results.map { it.benchmarkId }.distinct().filter { it.isNotBlank() }
                if (wodIds.isEmpty()) {
                    _benchmarkRecordsState.value = PerformanceState.Success(results.map { PerformanceRecord(it, null) })
                    return@launch
                }
                val wodsSnapshot = firestore.collection("benchmark_wods")
                    .whereEqualTo("gym_id", gymId)
                    .whereIn(FieldPath.documentId(), wodIds).get().await()
                val wodsMap = wodsSnapshot.documents.mapNotNull { it.toObject(BenchmarkWod::class.java) }.associateBy { it.id }

                val performanceRecords = results.map { PerformanceRecord(it, wodsMap[it.benchmarkId]) }
                _benchmarkRecordsState.value = PerformanceState.Success(performanceRecords)
            } catch (e: Exception) {
                Log.e("PerfViewModel", "Error en Benchmarks", e)
                _benchmarkRecordsState.value = PerformanceState.Error(e.localizedMessage ?: "Error al cargar récords.")
            }
        }
    }

    fun loadDailyWodRecords(userId: String, gymId: String) {
        viewModelScope.launch {
            _dailyWodRecordsState.value = PerformanceState.Loading
            try {
                val resultsSnapshot = firestore.collection("wod_results")
                    .whereEqualTo("gym_id", gymId)
                    .whereEqualTo("userId", userId)
                    .orderBy("date", Query.Direction.DESCENDING)
                    .get().await()

                val results = resultsSnapshot.toObjects(WodResult::class.java)
                if (results.isEmpty()) {
                    _dailyWodRecordsState.value = PerformanceState.Success(emptyList())
                    return@launch
                }
                val wodIds = results.map { it.wodId }.distinct().filter { it.isNotBlank() }
                if (wodIds.isEmpty()) {
                    _dailyWodRecordsState.value = PerformanceState.Success(results.map { PerformanceRecord(it, null) })
                    return@launch
                }
                val wodsSnapshot = firestore.collection("wods")
                    .whereEqualTo("gym_id", gymId)
                    .whereIn(FieldPath.documentId(), wodIds).get().await()
                val wodsMap = wodsSnapshot.documents.mapNotNull { it.toObject(Wod::class.java) }.associateBy { it.id }

                val performanceRecords = results.map { PerformanceRecord(it, wodsMap[it.wodId]) }
                _dailyWodRecordsState.value = PerformanceState.Success(performanceRecords)
            } catch (e: Exception) {
                Log.e("PerfViewModel", "Error en WODs Diarios", e)
                _dailyWodRecordsState.value = PerformanceState.Error(e.localizedMessage ?: "Error al cargar marcas.")
            }
        }
    }

    fun checkAndAwardAchievements(user: User) {
        val userId = user.id
        val gymId = user.gym_id

        // CORRECCIÓN: Reseteamos estado para que no se vea el del usuario anterior mientras carga
        _achievementsState.value = AchievementState.Loading

        viewModelScope.launch {
            try {
                val achievementsRef = firestore.collection("achievements")
                val unlockedAchievementsSnapshot = achievementsRef
                    .whereEqualTo("gym_id", gymId)
                    .whereEqualTo("userId", userId)
                    .get().await()

                val unlockedAchievementIds = unlockedAchievementsSnapshot.documents.map { it.id }.toSet()

                val classCountAchievements = mapOf(
                    1 to AchievementData("1_class", "Primer Paso", "¡Completaste tu primera clase!", "flag"),
                    5 to AchievementData("5_classes", "Calentando Motores", "¡5 clases! Ya le coges el ritmo.", "local_fire_department"),
                    20 to AchievementData("20_classes", "Parte del Mobiliario", "¡20 clases! Ya te conocemos.", "weekend"),
                    50 to AchievementData("50_classes", "Lealtad de Hierro", "50 clases. Tu constancia es tu fuerza.", "military_tech"),
                    100 to AchievementData("100_classes", "Titán del Box", "¡100 clases! Eres una leyenda.", "shield"),
                    200 to AchievementData("200_classes", "Centurión", "¡200 clases! Respeto.", "workspace_premium"),
                    300 to AchievementData("300_classes", "Deidad del Box", "300 clases. ¿Vives aquí?", "auto_awesome")
                )

                val batch = firestore.batch()
                var newAchievementsAwarded = false

                // Si el usuario es nuevo (clases=0), este bucle no hará nada, lo cual es correcto
                for ((requiredClasses, achievementData) in classCountAchievements) {
                    if (user.totalClassesAttended >= requiredClasses && achievementData.id !in unlockedAchievementIds) {
                        val newAchievement = Achievement(
                            id = achievementData.id,
                            title = achievementData.title,
                            description = achievementData.description,
                            iconName = achievementData.iconName,
                            type = "automatic",
                            userId = userId,
                            gym_id = gymId
                        )
                        // Usamos un ID que combine userId + achievementId para evitar duplicados
                        val docRef = achievementsRef.document("${userId}_${achievementData.id}")
                        batch.set(docRef, newAchievement)
                        newAchievementsAwarded = true
                    }
                }

                if (newAchievementsAwarded) {
                    batch.commit().await()
                }

                // Cargar la lista actualizada (o vacía si es nuevo)
                val allAchievementsSnapshot = achievementsRef
                    .whereEqualTo("gym_id", gymId)
                    .whereEqualTo("userId", userId)
                    .orderBy("unlockedAt", Query.Direction.DESCENDING)
                    .get().await()

                val allAchievements = allAchievementsSnapshot.toObjects(Achievement::class.java)
                _achievementsState.value = AchievementState.Success(allAchievements)
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
                    if (resultObject.id.isBlank()) return@launch
                    userId = resultObject.userId
                    gymId = resultObject.gym_id
                    firestore.collection("benchmark_results").document(resultObject.id).delete().await()
                } else if (!isBenchmark && resultObject is WodResult) {
                    userId = resultObject.userId
                    gymId = resultObject.gym_id
                    val querySnapshot = firestore.collection("wod_results")
                        .whereEqualTo("wodId", resultObject.wodId)
                        .whereEqualTo("userId", resultObject.userId)
                        .whereEqualTo("date", resultObject.date)
                        .limit(1).get().await()

                    if (!querySnapshot.isEmpty) {
                        val docId = querySnapshot.documents.first().id
                        firestore.collection("wod_results").document(docId).delete().await()
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
                    id = achievementId,
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