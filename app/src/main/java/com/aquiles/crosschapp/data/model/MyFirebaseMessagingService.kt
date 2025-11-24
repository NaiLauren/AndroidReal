// RUTA: app/src/main/java/com/aquiles/crosschapp/data/model/MyFirebaseMessagingService.kt

package com.aquiles.crosschapp.data.model

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aquiles.crosschapp.MainActivity
import com.aquiles.crosschapp.MyApplication.Companion.DEFAULT_CHANNEL_ID
import com.aquiles.crosschapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val TAG = "MyFCMService"

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Nos aseguramos de procesar solo mensajes de "data" para tener control total
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Mensaje de 'data' recibido: ${remoteMessage.data}")

            val title = remoteMessage.data["title"] ?: "TribeOnMove"
            val body = remoteMessage.data["body"] ?: "Has recibido una nueva notificación."

            showNotification(title, body)
        } else {
            Log.d(TAG, "Mensaje recibido sin payload de 'data'.")
        }
    }

    private fun showNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val notificationBuilder = NotificationCompat.Builder(this, DEFAULT_CHANNEL_ID)
            // --- ¡IMPORTANTE! Usa un icono de notificación blanco y transparente ---
            .setSmallIcon(R.drawable.ic_stat_notification) // Asegúrate de tener este drawable
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Prioridad alta para heads-up
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Usa la configuración (sonido, vibración) del CANAL

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Ya no creamos el canal aquí. Solo mostramos la notificación.
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
        Log.d(TAG, "Notificación mostrada usando el canal '$DEFAULT_CHANNEL_ID'.")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Nuevo token FCM: $token")
        sendTokenToFirestore(token)
    }

    companion object {
        fun sendTokenToFirestore(token: String) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("users").document(userId)
            userRef.update("fcmTokens", FieldValue.arrayUnion(token))
                .addOnSuccessListener { Log.d("FCM_Companion", "Token guardado.") }
                .addOnFailureListener { e -> Log.w("FCM_Companion", "Error al guardar token.", e) }
        }
    }
}