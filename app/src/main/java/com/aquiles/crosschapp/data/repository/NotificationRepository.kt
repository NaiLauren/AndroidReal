package com.aquiles.crosschapp.data.repository

import com.aquiles.crosschapp.data.common.Resource
import com.aquiles.crosschapp.data.model.Notification
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class NotificationRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun getUnreadNotifications(userId: String, gymId: String): Flow<Resource<List<Notification>>> = callbackFlow {
        trySend(Resource.Loading())
        
        val listener = firestore.collection("notifications")
            .whereEqualTo("gym_id", gymId)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    trySend(Resource.Error(e.localizedMessage ?: "Error desconocido"))
                    return@addSnapshotListener
                }
                
                val notifications = snapshots?.toObjects(Notification::class.java) ?: emptyList()
                trySend(Resource.Success(notifications))
            }

        awaitClose { listener.remove() }
    }

    suspend fun markAsRead(notificationId: String) {
        try {
            firestore.collection("notifications").document(notificationId)
                .update("isRead", true).await()
        } catch (e: Exception) {
            // Manejar error o loggear si es necesario
        }
    }
}
