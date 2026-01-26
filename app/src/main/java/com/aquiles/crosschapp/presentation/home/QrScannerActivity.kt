package com.aquiles.crosschapp.presentation.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aquiles.crosschapp.R
import com.aquiles.crosschapp.data.model.Achievement
import com.aquiles.crosschapp.data.model.AchievementSystem
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Date

class QrScannerActivity : AppCompatActivity() {

    private lateinit var codeScanner: CodeScanner
    private val db = FirebaseFirestore.getInstance()
    private var realClassId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        realClassId = intent.getStringExtra("CLASS_ID_PARAM")

        if (realClassId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: No se recibió el ID de la clase.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val scannerView = findViewById<CodeScannerView>(R.id.scanner_view)
        codeScanner = CodeScanner(this, scannerView)

        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = CodeScanner.ALL_FORMATS
        codeScanner.autoFocusMode = AutoFocusMode.SAFE
        codeScanner.scanMode = ScanMode.SINGLE
        codeScanner.isAutoFocusEnabled = true
        codeScanner.isFlashEnabled = false

        codeScanner.decodeCallback = DecodeCallback {
            runOnUiThread {
                val gymIdFromQr = it.text
                processAttendance(gymIdFromQr, realClassId!!)
            }
        }

        codeScanner.errorCallback = ErrorCallback {
            runOnUiThread {
                Toast.makeText(this, "Error de cámara: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }

        checkPermissions()
    }

    private fun processAttendance(scannedGymId: String, classIdToSave: String) {
        val currentUser = UserSession.currentUser.value ?: return

        if (scannedGymId != currentUser.gym_id) {
            Toast.makeText(this, "❌ QR Incorrecto. Ese código no es de tu gimnasio.", Toast.LENGTH_LONG).show()
            codeScanner.startPreview()
            return
        }

        val attendanceRef = db.collection("attendance_history")

        attendanceRef
            .whereEqualTo("gym_id", currentUser.gym_id)
            .whereEqualTo("userId", currentUser.id)
            .whereEqualTo("classId", classIdToSave)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    Toast.makeText(this, "⚠️ Ya tenías el presente registrado.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    saveToFirebase(currentUser.id, currentUser.gym_id, currentUser.name, currentUser.lastName, classIdToSave)
                }
            }
            .addOnFailureListener { e ->
                Log.e("QrScanner", "Error al consultar: ", e)
                Toast.makeText(this, "Error de conexión", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun saveToFirebase(userId: String, gymId: String, name: String, lastName: String, classId: String) {
        val data = hashMapOf(
            "userId" to userId,
            "gym_id" to gymId,
            "classId" to classId,
            "classDate" to Date(),
            "status" to "PRESENT",
            "userName" to name,
            "userLastName" to lastName
        )

        db.collection("attendance_history")
            .add(data)
            .addOnSuccessListener {
                // AQUÍ OCURRE EL CAMBIO: En vez de cerrar directo, verificamos logros
                checkSmartAchievements(userId, gymId)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // --- LÓGICA DE LOGROS INTELIGENTES ---
    private fun checkSmartAchievements(userId: String, gymId: String) {
        // Obtenemos todo el historial para calcular rachas
        db.collection("attendance_history")
            .whereEqualTo("gym_id", gymId)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->

                val earnedDefinitions = mutableListOf<com.aquiles.crosschapp.data.model.AchievementDefinition>()

                // Análisis de Contexto
                val now = Calendar.getInstance()
                val currentHour = now.get(Calendar.HOUR_OF_DAY)
                val dayOfWeek = now.get(Calendar.DAY_OF_WEEK)

                // - MADRUGADOR (< 9 AM)
                if (currentHour < 9) AchievementSystem.getById("early_bird")?.let { earnedDefinitions.add(it) }

                // - AVE NOCTURNA (>= 20 PM)
                if (currentHour >= 20) AchievementSystem.getById("night_owl")?.let { earnedDefinitions.add(it) }
                
                // - GUERRERO DE FINDE (Sab=7, Dom=1)
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    AchievementSystem.getById("weekend_warrior")?.let { earnedDefinitions.add(it) }
                }
                
                // - LUNES SAGRADO (Lun=2)
                if (dayOfWeek == Calendar.MONDAY) {
                    AchievementSystem.getById("never_skip_monday")?.let { earnedDefinitions.add(it) }
                }
                
                // - HORA DEL ALMUERZO (12 a 14)
                if (currentHour in 12..14) {
                    AchievementSystem.getById("lunch_crew")?.let { earnedDefinitions.add(it) }
                }

                // ANÁLISIS HISTÓRICO
                // - DOBLE TURNO
                val todayStart = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }
                val classesToday = documents.count { doc ->
                    val timestamp = doc.getTimestamp("classDate")
                    timestamp != null && timestamp.toDate().after(todayStart.time)
                }
                // (Nota: Incluye la actual porque ya guardamos antes)
                if (classesToday >= 2) {
                     AchievementSystem.getById("double_trouble")?.let { earnedDefinitions.add(it) }
                }
                
                // - HAT TRICK (3 días seguidos)
                val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }
                val dayBeforeYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -2); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0) }
                val endOfYesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1); set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59) }
                
                val hasClassYesterday = documents.any { doc ->
                     val d = doc.getTimestamp("classDate")?.toDate()
                     d != null && d.after(yesterday.time) && d.before(endOfYesterday.time)
                }
                
                val hasClassDayBefore = documents.any { doc ->
                     val d = doc.getTimestamp("classDate")?.toDate()
                     d != null && d.after(dayBeforeYesterday.time) && d.before(yesterday.time)
                }
                
                if (hasClassYesterday && hasClassDayBefore) {
                    AchievementSystem.getById("hat_trick")?.let { earnedDefinitions.add(it) }
                }

                // - SEMANA DE FUEGO (5 clases en últimos 7 días)
                val oneWeekAgo = Calendar.getInstance()
                oneWeekAgo.add(Calendar.DAY_OF_YEAR, -7)

                // Contamos documentos recientes (incluyendo el que acabamos de guardar)
                val classesThisWeek = documents.count { doc ->
                    val timestamp = doc.getTimestamp("classDate")
                    timestamp != null && timestamp.toDate().after(oneWeekAgo.time)
                }

                if (classesThisWeek >= 5) {
                    AchievementSystem.getById("week_fire")?.let { earnedDefinitions.add(it) }
                }

                // Si ganó algo, lo guardamos (y sumamos su XP)
                if (earnedDefinitions.isNotEmpty()) {
                    saveUnlockedAchievements(userId, gymId, earnedDefinitions)
                } else {
                    // Si no ganó logros, igual sumamos la XP de asistencia (+10)
                    updateUserXpOnly(userId)
                }
            }
            .addOnFailureListener {
                // Si falla el chequeo de logros, cerramos igual porque el presente ya se guardó
                finishWithSuccess("¡Presente Registrado! ✅")
            }
    }
    
    private fun updateUserXpOnly(userId: String) {
        val userRef = db.collection("users").document(userId)
        val xpGain = com.aquiles.crosschapp.data.model.LevelSystem.XP_ATTENDANCE
        
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentXp = snapshot.getLong("xp")?.toInt() ?: 0
            val newTotalXp = currentXp + xpGain
            val newLevel = com.aquiles.crosschapp.data.model.LevelSystem.getLevelName(newTotalXp)
            
            transaction.update(userRef, "xp", newTotalXp)
            transaction.update(userRef, "level", newLevel)
            // Fix: Increment totalClassesAttended for achievements
            transaction.update(userRef, "totalClassesAttended", com.google.firebase.firestore.FieldValue.increment(1))
            
            return@runTransaction newLevel
        }.addOnSuccessListener { newLevel ->
             // Podríamos chequear si subió de nivel comparando con UserSession local, 
             // pero por simplicidad mostramos solo XP ganada
             finishWithSuccess("¡Presente (+${xpGain} XP)! ✅")
        }.addOnFailureListener {
             finishWithSuccess("¡Presente Registrado! ✅")
        }
    }

    private fun saveUnlockedAchievements(
        userId: String,
        gymId: String,
        achievements: List<com.aquiles.crosschapp.data.model.AchievementDefinition>
    ) {
        val batch = db.batch()
        var newUnlockTitle = ""
        var totalXpGained = 0
        
        // 1. Sumar XP de logros
        achievements.forEach { def ->
            val docRef = db.collection("achievements").document("${userId}_${def.id}")
            val newAchievement = Achievement(
                id = def.id,
                title = def.title,
                description = def.description,
                iconName = def.iconName,
                type = "smart",
                userId = userId,
                gym_id = gymId,
                unlockedAt = Date()
            )
            batch.set(docRef, newAchievement)
            newUnlockTitle = def.title
            
            // Sumar XP del logro
            totalXpGained += def.xpReward
        }

        // 2. Sumar XP de Asistencia (que ya se guardó)
        // Nota: saveToFirebase guardó la asistencia, aquí sumamos su XP al usuario
        totalXpGained += com.aquiles.crosschapp.data.model.LevelSystem.XP_ATTENDANCE

        // 3. Actualizar Usuario (XP y Nivel)
        val userRef = db.collection("users").document(userId)
        
        // Leemos el usuario actual para saber su XP base
        // (Podríamos usar FieldValue.increment pero necesitamos calcular el nuevo nivel)
        userRef.get().addOnSuccessListener { snapshot ->
            val currentXp = snapshot.getLong("xp")?.toInt() ?: 0
            val newTotalXp = currentXp + totalXpGained
            val newLevel = com.aquiles.crosschapp.data.model.LevelSystem.getLevelName(newTotalXp)
            val oldLevel = snapshot.getString("level") ?: "Novato"
            
            batch.update(userRef, "xp", newTotalXp)
            batch.update(userRef, "level", newLevel)
            // Fix: Increment totalClassesAttended for achievements
            batch.update(userRef, "totalClassesAttended", com.google.firebase.firestore.FieldValue.increment(1))
            
            batch.commit().addOnSuccessListener {
                var message = "¡Presente (+10 XP)!"
                if (achievements.isNotEmpty()) {
                    message = if (achievements.size > 1) "¡Múltiples Logros (+${totalXpGained} XP)!" else "¡Logro: $newUnlockTitle (+${totalXpGained} XP)!"
                }
                
                if (newLevel != oldLevel) {
                    message += "\n🚀 ¡SUBISTE A NIVEL $newLevel! 🚀"
                }
                
                finishWithSuccess("🎉 $message")
            }.addOnFailureListener {
                finishWithSuccess("¡Presente Registrado! ✅")
            }
        }.addOnFailureListener {
            // Si falla leer el usuario, al menos comiteamos los logros
            batch.commit()
            finishWithSuccess("¡Presente Registrado! ✅")
        }
    }

    private fun finishWithSuccess(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        setResult(RESULT_OK)
        finish()
    }

    // --- Permisos (Sin cambios) ---
    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            codeScanner.startPreview()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 101)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            codeScanner.startPreview()
        } else {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            codeScanner.startPreview()
        }
    }

    override fun onPause() {
        codeScanner.releaseResources()
        super.onPause()
    }
}