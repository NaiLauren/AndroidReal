// RUTA: app/src/main/java/com/aquiles/crosschapp/MyApplication.kt

package com.aquiles.crosschapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
// Imports nuevos para Firebase App Check
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class MyApplication : Application() {

    companion object {
        const val DEFAULT_CHANNEL_ID = "default_channel_id"
    }

    override fun onCreate() {
        super.onCreate()

        // 1. Inicializamos Firebase (asegura el contexto)
        FirebaseApp.initializeApp(this)

        // 2. Activamos App Check usando el proveedor Play Integrity
        // Esto validará que la app sea legítima usando el SHA-256 que registraste.
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        firebaseAppCheck.installAppCheckProviderFactory(
            PlayIntegrityAppCheckProviderFactory.getInstance()
        )

        // 3. Crear el canal de notificación (Tu código original)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = "Notificaciones Generales"
            val channelDescription = "Notificaciones para reservas, créditos y mensajes importantes."
            val importance = NotificationManager.IMPORTANCE_HIGH

            val soundUri = Uri.parse("${ContentResolver.SCHEME_ANDROID_RESOURCE}://${packageName}/${R.raw.notification_sound}")

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val channel = NotificationChannel(DEFAULT_CHANNEL_ID, channelName, importance).apply {
                description = channelDescription
                enableVibration(true)
                setSound(soundUri, audioAttributes)
            }

            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}