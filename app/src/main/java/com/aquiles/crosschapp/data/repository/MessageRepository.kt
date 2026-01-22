package com.aquiles.crosschapp.data.repository

import com.aquiles.crosschapp.data.common.Resource
import com.aquiles.crosschapp.data.model.PersonalMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MessageRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun getUnreadPersonalMessage(userId: String, gymId: String): Flow<Resource<PersonalMessage?>> = callbackFlow {
        trySend(Resource.Loading())

        val listener = firestore.collection("personal_messages")
            .whereEqualTo("gym_id", gymId)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(1)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    trySend(Resource.Error(e.localizedMessage ?: "Error al cargar mensaje"))
                    return@addSnapshotListener
                }

                if (snapshots != null && !snapshots.isEmpty) {
                    val message = snapshots.documents.first().toObject(PersonalMessage::class.java)
                    trySend(Resource.Success(message))
                } else {
                    trySend(Resource.Success(null)) // null representa 'Empty'
                }
            }

        awaitClose { listener.remove() }
    }

    suspend fun markAsRead(messageId: String) {
        try {
            firestore.collection("personal_messages").document(messageId)
                .update("isRead", true).await()
        } catch (e: Exception) {
            // Manejar error
        }
    }
}
