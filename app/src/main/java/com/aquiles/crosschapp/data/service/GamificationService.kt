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
        val xpLogRef = firestore.collection("users").document(userId).collection("xp_logs").document(xpLogId)

        try {
            // 1. Check Idempotency (if log exists, skip)
            val logDoc = xpLogRef.get().await()
            if (logDoc.exists()) {
                return
            }

            // 2. Calculate stuff in memory
            val earnedDefinitions = checkSmartAchievements(userId, gymId, classDate)
            
            // Base XP + Bonus XP
            var totalXpGained = LevelSystem.XP_ATTENDANCE
            earnedDefinitions.forEach { totalXpGained += it.xpReward }

            // 3. Queue Batch Operations
            val batch = firestore.batch()

            // a) Create Attendance XP Log
            val attendanceLog = hashMapOf(
                "amount" to LevelSystem.XP_ATTENDANCE,
                "type" to "ATTENDANCE",
                "title" to "Asistencia a Clase",
                "description" to "Ganaste XP por asistir a una clase",
                "icon" to "💪",
                "timestamp" to FieldValue.serverTimestamp(),
                "relatedId" to classId
            )
            batch.set(xpLogRef, attendanceLog)

            // b) Update User (XP) - Increment Atomic
            val userRef = firestore.collection("users").document(userId)
            batch.update(userRef, "xp", FieldValue.increment(totalXpGained.toLong()))
            // Note: Level update is best done via a Cloud Function trigger or by reading back, 
            // but for parity with current logic, let's leave level check for UI or separate call if needed.
            // iOS implementation essentially trusts local calculation or separate read. 
            // Here we just increment XP.

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
                val achLogRef = firestore.collection("users").document(userId).collection("xp_logs").document()
                val achLog = hashMapOf(
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

            // 4. Commit
            batch.commit().await()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun checkSmartAchievements(userId: String, gymId: String, classDate: Date): List<AchievementDefinition> {
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

        // --- HISTORY BASED CHECKS ---
        
        // Fetch recent attendance history for the user
        // We query the 'attendance_history' collection if it exists, mirroring iOS pattern.
        // Assuming 'attendance_history' contains documents with 'classDate' timestamp.
        val historySnapshot = firestore.collection("attendance_history")
            .whereEqualTo("userId", userId)
            .whereEqualTo("gym_id", gymId)
            // Limit to last 8 days (enough for Week Fire and Hat Trick)
            // Optimization: In a real app we might want an index on classDate DESC or query by range.
            // For now, let's just fetch all (or limit 50 if history is huge) and filter in memory as MVP.
            .get().await()

        val historyDates = historySnapshot.documents.mapNotNull { doc ->
            doc.getTimestamp("classDate")?.toDate()
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
    
    // Helper to be called from PerformanceViewModel too
    suspend fun addXp(userId: String, amount: Int, type: String, title: String, description: String, relatedId: String? = null) {
        val batch = firestore.batch()
        val userRef = firestore.collection("users").document(userId)
        
        batch.update(userRef, "xp", FieldValue.increment(amount.toLong()))
        
        val logRef = firestore.collection("users").document(userId).collection("xp_logs").document()
        val logData = hashMapOf(
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
    
    suspend fun awardSchedulingXP(userId: String, gymId: String, intentId: String): Int {
        val reward = 10
        val xpLogId = "schedule_intent_${userId}_${intentId}"
        val xpLogRef = firestore.collection("users").document(userId).collection("xp_logs").document(xpLogId)
        
        return try {
            val logDoc = xpLogRef.get().await()
            if (logDoc.exists()) {
                return 0 // Already rewarded for this intent
            }
            
            val batch = firestore.batch()
            
            val logData = hashMapOf(
                "amount" to reward,
                "type" to "SCHEDULE_INTENT",
                "title" to "Planificador Pro",
                "description" to "Ganaste XP por avisar tu horario de asistencia",
                "icon" to "📅",
                "timestamp" to FieldValue.serverTimestamp(),
                "relatedId" to intentId
            )
            batch.set(xpLogRef, logData)
            
            val userRef = firestore.collection("users").document(userId)
            batch.update(userRef, "xp", FieldValue.increment(reward.toLong()))
            
            // Level resolution can be triggered later, just return reward
            batch.commit().await()
            reward
            
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
}
