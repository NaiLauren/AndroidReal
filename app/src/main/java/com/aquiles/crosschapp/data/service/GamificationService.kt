package com.aquiles.crosschapp.data.service

import com.aquiles.crosschapp.data.model.AchievementDefinition
import com.aquiles.crosschapp.data.model.AchievementSystem
import com.aquiles.crosschapp.data.model.LevelSystem
import com.aquiles.crosschapp.data.model.XpLog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

/**
 * Service dedicated to Gamification logic to replicate iOS architecture.
 * Decouples logic from ViewModels.
 */
object GamificationService {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun processAttendanceGamification(userId: String, gymId: String, classId: String, classDate: Date) {
        val xpLogId = "attendance_${userId}_${classId}"
        val xpLogRef = firestore.collection("xp_logs").document(xpLogId)

        try {
            // 1. Check Idempotency (if log exists, skip)
            val logDoc = xpLogRef.get().await()
            if (logDoc.exists()) {
                return
            }

            // 2. Fetch attendance history once for all checks
            val historySnapshot = firestore.collection("attendance_history")
                .whereEqualTo("userId", userId)
                .whereEqualTo("gym_id", gymId)
                .get().await()

            val historyDates = historySnapshot.documents.mapNotNull { doc ->
                doc.getTimestamp("classDate")?.toDate()
            }

            // 3. Calculate stuff in memory
            val earnedDefinitions = checkSmartAchievements(userId, gymId, classDate, historyDates)
            val recurringBonuses = checkRecurringBonuses(classDate, historyDates)
            
            // Base XP + Achievements + Recurring Bonuses
            var totalXpGained = LevelSystem.XP_ATTENDANCE
            earnedDefinitions.forEach { totalXpGained += it.xpReward }
            recurringBonuses.forEach { totalXpGained += it.amount }

            // 3. Queue Batch Operations
            val batch = firestore.batch()

            // a) Create Attendance XP Log
            val attendanceLog = hashMapOf(
                "userId" to userId,
                "gym_id" to gymId,
                "amount" to LevelSystem.XP_ATTENDANCE,
                "type" to "ATTENDANCE",
                "title" to "Asistencia a Clase",
                "description" to "Ganaste XP por asistir a una clase",
                "icon" to "💪",
                "timestamp" to FieldValue.serverTimestamp(),
                "relatedId" to classId
            )
            batch.set(xpLogRef, attendanceLog)

            // b) Update User (XP and Level) - Increment Atomic
            val userRef = firestore.collection("users").document(userId)
            batch.update(userRef, "xp", FieldValue.increment(totalXpGained.toLong()))
            
            // Sync Level update
            val userSnap = userRef.get().await()
            val currentXp = (userSnap.getLong("xp") ?: 0L).toInt()
            val newTotalXp = currentXp + totalXpGained
            val newLevel = LevelSystem.getLevelName(newTotalXp)
            batch.update(userRef, "level", newLevel)

            // c) Save Unlocked Achievements
            earnedDefinitions.forEach { def ->
                // Save Achievement to user's collection
                val achRef = firestore.collection("achievements").document("${userId}_${def.id}")
                val achievementData = hashMapOf(
                    "id" to def.id,
                    "title" to def.title,
                    "description" to def.description,
                    "iconName" to def.iconName,
                    "type" to "smart",
                    "userId" to userId,
                    "gym_id" to gymId,
                    "unlockedAt" to FieldValue.serverTimestamp(),
                    "xpReward" to def.xpReward
                )
                batch.set(achRef, achievementData)

                // Log XP for Achievement
                val achLogRef = firestore.collection("xp_logs").document()
                val achLog = hashMapOf(
                    "userId" to userId,
                    "gym_id" to gymId,
                    "amount" to def.xpReward,
                    "type" to "ACHIEVEMENT",
                    "title" to "Logro Desbloqueado",
                    "description" to "Obtuviste: ${def.title}",
                    "icon" to "🏆",
                    "timestamp" to FieldValue.serverTimestamp(),
                    "relatedId" to def.id
                )
                batch.set(achLogRef, achLog)
            }

            // d) Save Recurring Bonus Logs
            recurringBonuses.forEach { bonus ->
                val bonusLogRef = firestore.collection("xp_logs").document()
                val bonusLog = hashMapOf(
                    "userId" to userId,
                    "gym_id" to gymId,
                    "amount" to bonus.amount,
                    "type" to "BONUS",
                    "title" to bonus.title,
                    "description" to "Bono extra por: ${bonus.reason}",
                    "icon" to bonus.icon,
                    "timestamp" to FieldValue.serverTimestamp(),
                    "relatedId" to "bonus_${System.currentTimeMillis()}"
                )
                batch.set(bonusLogRef, bonusLog)
            }

            // 4. Commit
            batch.commit().await()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun checkSmartAchievements(userId: String, gymId: String, classDate: Date, historyDates: List<Date>): List<AchievementDefinition> {
        val earnedDefinitions = mutableListOf<AchievementDefinition>()
        
        // Fetch existing achievements to avoid duplicates
        // Note: In a real robust system we might want to check this inside the transaction or rely on security rules,
        // but for now we mirror `AdminViewModel` logic / iOS logic.
        val existingSnapshot = firestore.collection("achievements")
            .whereEqualTo("userId", userId)
            .get().await()
        
        val existingIds = existingSnapshot.documents.mapNotNull { it.getString("id") }.toSet()

        // Helper to check if user already has it
        fun hasAchievement(id: String) = existingIds.contains(id)

        val cal = Calendar.getInstance()
        cal.time = classDate
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Dom=1...Sat=7

        // 1. MADRUGADOR (< 9 AM)
        if (currentHour < 9 && !hasAchievement("early_bird")) {
            AchievementSystem.getById("early_bird")?.let { earnedDefinitions.add(it) }
        }

        // 2. AVE NOCTURNA (>= 20 PM)
        if (currentHour >= 20 && !hasAchievement("night_owl")) {
            AchievementSystem.getById("night_owl")?.let { earnedDefinitions.add(it) }
        }

        // 3. GUERRERO DE FINDE (Sat, Sun)
        if ((dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) && !hasAchievement("weekend_warrior")) {
            AchievementSystem.getById("weekend_warrior")?.let { earnedDefinitions.add(it) }
        }

        // 4. DOMINGO SANTO
        if (dayOfWeek == Calendar.SUNDAY && !hasAchievement("clean_sunday")) {
            AchievementSystem.getById("clean_sunday")?.let { earnedDefinitions.add(it) }
        }

        // 5. LUNES SAGRADO
        if (dayOfWeek == Calendar.MONDAY && !hasAchievement("never_skip_monday")) {
            AchievementSystem.getById("never_skip_monday")?.let { earnedDefinitions.add(it) }
        }

        // 6. HORA DEL ALMUERZO
        if (currentHour in 12..14 && !hasAchievement("lunch_crew")) {
            AchievementSystem.getById("lunch_crew")?.let { earnedDefinitions.add(it) }
        }


        // Helper to check standard matches
        fun isSameDay(d1: Date, d2: Date): Boolean {
            val c1 = Calendar.getInstance().apply { time = d1 }
            val c2 = Calendar.getInstance().apply { time = d2 }
            return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                   c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
        }

        // 7. DOBLE TURNO (2 classes same day)
        // Count history logs for TODAY (excluding current if not yet logged, but we are processing current attendance)
        // Note: The current class `classDate` is NOT yet in `historySnapshot` because we typically write log after check.
        // So allow count >= 1 from history for today (meaning 1 previous + current = 2).
        val classesToday = historyDates.count { isSameDay(it, classDate) }
        if (classesToday >= 1 && !hasAchievement("double_trouble")) {
             AchievementSystem.getById("double_trouble")?.let { earnedDefinitions.add(it) }
        }

        // 8. HAT TRICK (3 days in a row)
        // Check if attended yesterday AND day before yesterday
        val yesterday = Calendar.getInstance().apply { time = classDate; add(Calendar.DAY_OF_YEAR, -1) }.time
        val dayBefore = Calendar.getInstance().apply { time = classDate; add(Calendar.DAY_OF_YEAR, -2) }.time
        
        val attendedYesterday = historyDates.any { isSameDay(it, yesterday) }
        val attendedDayBefore = historyDates.any { isSameDay(it, dayBefore) }

        if (attendedYesterday && attendedDayBefore && !hasAchievement("hat_trick")) {
            AchievementSystem.getById("hat_trick")?.let { earnedDefinitions.add(it) }
        }

        // 9. WEEK FIRE (5 classes in last 7 days)
        val oneWeekAgo = Calendar.getInstance().apply { time = classDate; add(Calendar.DAY_OF_YEAR, -7) }.time
        val classesLastWeek = historyDates.count { it.after(oneWeekAgo) && !it.after(classDate) }
        // We need 5 total. If history has 4 + current = 5.
        if ((classesLastWeek + 1) >= 5 && !hasAchievement("week_fire")) {
             AchievementSystem.getById("week_fire")?.let { earnedDefinitions.add(it) }
        }

        return earnedDefinitions
    }

    private fun checkRecurringBonuses(classDate: Date, history: List<Date>): List<BonusEntry> {
        val bonuses = mutableListOf<BonusEntry>()
        val cal = Calendar.getInstance().apply { time = classDate }
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        // 1. Weekend Bonus (+75 XP)
        if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
            bonuses.add(BonusEntry("Bono Fin de Semana", 75, "🔥", "Entrenar Sábado o Domingo"))
        }

        // 2. Strong Start (Monday) (+50 XP)
        if (dayOfWeek == Calendar.MONDAY) {
            bonuses.add(BonusEntry("Comienzo Fuerte", 50, "🚀", "Entrenar un Lunes"))
        }

        // 3. Early Bird Bonus (< 8 AM) (+30 XP)
        if (hour < 8) {
            bonuses.add(BonusEntry("Madrugador", 30, "🌅", "Entrenar antes de las 8 AM"))
        }

        // 4. Night Owl Bonus (>= 20 PM) (+30 XP)
        if (hour >= 20) {
            bonuses.add(BonusEntry("Cierre del Día", 30, "🌙", "Entrenar tarde"))
        }

        // 5. Double Turn (+100 XP)
        val classesToday = history.count { isSameDay(it, classDate) }
        if (classesToday >= 1) { // 1 existing + current = 2
            bonuses.add(BonusEntry("Doble Turno", 100, "💪", "Segunda clase del día"))
        }

        // 6. Hat-Trick (3 days in a row) (+150 XP)
        val yesterday = Calendar.getInstance().apply { time = classDate; add(Calendar.DAY_OF_YEAR, -1) }.time
        val dayBefore = Calendar.getInstance().apply { time = classDate; add(Calendar.DAY_OF_YEAR, -2) }.time
        val attendedYesterday = history.any { isSameDay(it, yesterday) }
        val attendedDayBefore = history.any { isSameDay(it, dayBefore) }
        if (attendedYesterday && attendedDayBefore) {
            bonuses.add(BonusEntry("Hat-Trick", 150, "🎩", "3 días consecutivos entrenando"))
        }

        // 7. Perfect Week (Mon-Fri) (+200 XP)
        if (dayOfWeek == Calendar.FRIDAY) {
            var allWeekAttended = true
            for (i in 1..4) {
                val target = Calendar.getInstance().apply { time = classDate; add(Calendar.DAY_OF_YEAR, -i) }.time
                if (!history.any { isSameDay(it, target) }) {
                    allWeekAttended = false
                    break
                }
            }
            if (allWeekAttended) {
                bonuses.add(BonusEntry("Semana Perfecta", 200, "🌟", "Asistencia completa Lun-Vie"))
            }
        }

        return bonuses
    }

    data class BonusEntry(val title: String, val amount: Int, val icon: String, val reason: String)

    private fun isSameDay(d1: Date, d2: Date): Boolean {
        val c1 = Calendar.getInstance().apply { time = d1 }
        val c2 = Calendar.getInstance().apply { time = d2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
               c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }
    
    // Helper to be called from PerformanceViewModel too
    suspend fun addXp(userId: String, gymId: String, amount: Int, type: String, title: String, description: String, relatedId: String? = null) {
        val batch = firestore.batch()
        val userRef = firestore.collection("users").document(userId)
        
        batch.update(userRef, "xp", FieldValue.increment(amount.toLong()))
        
        val logRef = firestore.collection("xp_logs").document()
        val logData = hashMapOf(
            "userId" to userId,
            "gym_id" to gymId,
            "amount" to amount,
            "type" to type,
            "title" to title,
            "description" to description,
            "timestamp" to FieldValue.serverTimestamp(),
            "relatedId" to relatedId
        )
        batch.set(logRef, logData)
        
        batch.commit().await()
    }
    
    // MARK: - Smart Schedule Gamification

    /** XP máximo que un alumno puede ganar mediante el Planificador Pro (14 slots × 10 XP) */
    const val SCHEDULE_XP_CAP = 140

    suspend fun awardSchedulingXP(userId: String, gymId: String, intentId: String): Int {
        val reward = 10
        // La key de idempotencia incluye el userId + intentId para evitar doble pago por el mismo slot.
        // IMPORTANTE: El intentId viene del objeto UserTrainingIntention, que tiene un UUID generado
        // en el momento de la creación. Si el alumno borra y re-agrega el mismo horario, el UUID
        // cambia → para prevenir esto, también verificamos el CAP GLOBAL antes de otorgar XP.
        val xpLogId = "schedule_intent_${userId}_${intentId}"
        val xpLogRef = firestore.collection("xp_logs").document(xpLogId)

        return try {
            // 1. Verificar idempotencia (mismo intento exacto)
            val logDoc = xpLogRef.get().await()
            if (logDoc.exists()) {
                return 0
            }

            // 2. Verificar CAP GLOBAL: Sumar todo el XP de tipo SCHEDULE_INTENT del usuario
            val previousLogsSnap = firestore.collection("xp_logs")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "SCHEDULE_INTENT")
                .get()
                .await()

            val totalScheduleXpEarned = previousLogsSnap.documents.sumOf { doc ->
                (doc.getLong("amount") ?: 0L).toInt()
            }

            if (totalScheduleXpEarned >= SCHEDULE_XP_CAP) {
                // Límite alcanzado — no otorgar más XP
                return 0
            }

            // Calcular cuánto podemos otorgar sin superar el cap
            val xpToGrant = minOf(reward, SCHEDULE_XP_CAP - totalScheduleXpEarned)

            // 3. Registrar log y actualizar usuario
            val batch = firestore.batch()

            val logData = hashMapOf(
                "userId" to userId,
                "gym_id" to gymId,
                "amount" to xpToGrant,
                "type" to "SCHEDULE_INTENT",
                "title" to "Planificador Pro",
                "description" to "Ganaste XP por avisar tu horario de asistencia",
                "icon" to "📅",
                "timestamp" to FieldValue.serverTimestamp(),
                "relatedId" to intentId
            )
            batch.set(xpLogRef, logData)

            val userRef = firestore.collection("users").document(userId)
            batch.update(userRef, "xp", FieldValue.increment(xpToGrant.toLong()))

            batch.commit().await()
            xpToGrant

        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}

