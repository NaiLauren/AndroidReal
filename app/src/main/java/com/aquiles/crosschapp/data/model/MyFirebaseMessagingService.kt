package com.aquiles.crosschapp.data.model

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aquiles.crosschapp.MainActivity
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

        // 1. Prioridad a la notificación automática si la app está en segundo plano
        // Si viene payload 'notification', el sistema la maneja solo (si la app está cerrada).
        // Si viene payload 'data' y la app está abierta, o si es solo 'data', lo manejamos nosotros.

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "CrossChApp"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "Tienes una nueva notificación"

        // Mostrar la notificación manual para asegurar sonido y vibración
        showNotification(title, body)
    }

    private fun showNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // ID del canal debe ser consistente
        val channelId = "crosschapp_default_channel"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification) // Asegúrate que este icono sea blanco con fondo transparente
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri) // Sonido
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000)) // Vibración
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // --- CORRECCIÓN CLAVE: CREAR EL CANAL SIEMPRE ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Si el canal ya existe, esto no hace nada, así que es seguro llamarlo siempre
            val channel = NotificationChannel(
                channelId,
                "Notificaciones Generales", // Nombre visible para el usuario
                NotificationManager.IMPORTANCE_HIGH // IMPORTANCE_HIGH = Sonido + Popup
            )
            channel.description = "Avisos importantes de la app"
            channel.enableVibration(true)
            channel.enableLights(true)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
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