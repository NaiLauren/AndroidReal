// RUTA: app/src/main/java/com/aquiles/crosschapp/MyApplication.kt
// VERSIÓN FINAL Y LIMPIA

package com.aquiles.crosschapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build

class MyApplication : Application() {

    companion object {
        const val DEFAULT_CHANNEL_ID = "default_channel_id"
    }

    override fun onCreate() {
        super.onCreate()

        // La única responsabilidad de esta clase es crear el canal de notificación al iniciar la app.
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