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
import com.aquiles.crosschapp.presentation.viewmodel.UserSession
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.CodeScannerView
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class QrScannerActivity : AppCompatActivity() {

    private lateinit var codeScanner: CodeScanner
    private val db = FirebaseFirestore.getInstance()

    // Esta variable guardará el ID "RnCC..." que viene de la pantalla de detalles
    private var realClassId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_scanner)

        // 1. RECUPERAMOS EL ID REAL DE LA CLASE
        realClassId = intent.getStringExtra("CLASS_ID_PARAM")

        // Verificación de seguridad
        if (realClassId.isNullOrEmpty()) {
            Toast.makeText(this, "Error: No se recibió el ID de la clase.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val scannerView = findViewById<CodeScannerView>(R.id.scanner_view)
        codeScanner = CodeScanner(this, scannerView)

        // Configuración de cámara
        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = CodeScanner.ALL_FORMATS
        codeScanner.autoFocusMode = AutoFocusMode.SAFE
        codeScanner.scanMode = ScanMode.SINGLE
        codeScanner.isAutoFocusEnabled = true
        codeScanner.isFlashEnabled = false

        // AL ESCANEAR EL QR
        codeScanner.decodeCallback = DecodeCallback {
            runOnUiThread {
                val gymIdFromQr = it.text // Esto será "IC46..." (El QR de la pared)

                // Llamamos a la función pasando AMBOS datos
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

        // 1. VALIDACIÓN: ¿El QR de la pared coincide con el gimnasio del usuario?
        if (scannedGymId != currentUser.gym_id) {
            Toast.makeText(this, "❌ QR Incorrecto. Ese código no es de tu gimnasio.", Toast.LENGTH_LONG).show()
            codeScanner.startPreview() // Reiniciar para intentar de nuevo
            return
        }

        val attendanceRef = db.collection("attendance_history")

        // 2. CONSULTA CORREGIDA: Agregamos el filtro de gym_id para cumplir con las reglas
        attendanceRef
            .whereEqualTo("gym_id", currentUser.gym_id) // <--- ESTA LÍNEA ES OBLIGATORIA
            .whereEqualTo("userId", currentUser.id)
            .whereEqualTo("classId", classIdToSave)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    Toast.makeText(this, "⚠️ Ya tenías el presente registrado.", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    // Si no existe, lo creamos
                    saveToFirebase(currentUser.id, currentUser.gym_id, currentUser.name, currentUser.lastName, classIdToSave)
                }
            }
            .addOnFailureListener { e ->
                // Ahora mostramos el error real por si acaso
                Log.e("QrScanner", "Error al consultar: ", e)
                Toast.makeText(this, "Error de permisos o red: ${e.message}", Toast.LENGTH_LONG).show()
                finish()
            }
    }

    private fun saveToFirebase(userId: String, gymId: String, name: String, lastName: String, classId: String) {
        val data = hashMapOf(
            "userId" to userId,
            "gym_id" to gymId,
            "classId" to classId, // Guardamos "RnCC..."
            "classDate" to Date(),
            "status" to "PRESENT",
            "userName" to name,
            "userLastName" to lastName
        )

        db.collection("attendance_history")
            .add(data)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Presente Registrado! ✅", Toast.LENGTH_LONG).show()
                // IMPORTANTE: Devolvemos RESULT_OK para que la pantalla anterior se actualice
                setResult(RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    // --- Permisos de Cámara (Igual que antes) ---
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