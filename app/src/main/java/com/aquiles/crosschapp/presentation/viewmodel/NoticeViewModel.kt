package com.aquiles.crosschapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.GymNotice
import com.aquiles.crosschapp.data.model.NoticePriority
import com.aquiles.crosschapp.data.model.NoticeType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class NoticeViewModel : ViewModel() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _notices = MutableStateFlow<List<GymNotice>>(emptyList())
    val notices = _notices.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    init {
        loadNotices()
    }

    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null
    private var currentGymId: String? = null

    // Called automatically in init, but also safe to call manually to refresh/retry
    fun loadNotices() {
        val user = UserSession.currentUser.value ?: return
        
        // If we represent the same user/gym and have an active listener, don't reload
        if (listenerRegistration != null && currentGymId == user.gym_id) {
            android.util.Log.d("NoticeViewModel", "Listener already active for gym ${user.gym_id}, skipping reload")
            return
        }

        // Prevent duplicate listeners
        listenerRegistration?.remove()
        currentGymId = user.gym_id

        android.util.Log.d("NoticeViewModel", "Loading notices for gym: ${user.gym_id} (New Listener)")

            // iOS path: /gyms/{gymId}/news
        listenerRegistration = firestore.collection("gyms")
            .document(user.gym_id)
            .collection("news")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("NoticeViewModel", "Error loading notices", error)
                    return@addSnapshotListener
                }

                android.util.Log.d("NoticeViewModel", "Snapshot received: ${snapshot?.size()} documents")
                
                snapshot?.documents?.forEach { doc ->
                    android.util.Log.d("NoticeViewModel", "Document ${doc.id}: ${doc.data}")
                }

                val items = snapshot?.toObjects(GymNotice::class.java) ?: emptyList()
                
                android.util.Log.d("NoticeViewModel", "Deserialized ${items.size} notices")
                items.forEach { notice ->
                    val isActive = notice.isEffectivelyActive()
                    android.util.Log.d("NoticeViewModel", "Notice: id=${notice.id}, isActive=${notice.isActiveField}, active=${notice.active} -> EFFECTIVE=$isActive")
                }
                
                // Filtrar expirados localmente para asegurar (si aplica)
                val now = Timestamp.now()
                val filtered = items.filter { notice ->
                    (notice.expiresAt == null || notice.expiresAt > now) && notice.isEffectivelyActive() && notice.id.isNotBlank()
                }
                
                android.util.Log.d("NoticeViewModel", "After filter: ${filtered.size} notices")
                _notices.value = filtered
            }
    }

    suspend fun createNotice(
        title: String?,
        imageUrl: String,
        message: String = "" 
    ) {
        val user = UserSession.currentUser.value ?: return

        try {
            android.util.Log.d("NoticeViewModel", "Creating notice for user ${user.id} in gym ${user.gym_id}")
            
            val newDocRef = firestore.collection("gyms")
                .document(user.gym_id)
                .collection("news")
                .document()
            
            val notice = GymNotice(
                id = newDocRef.id,
                gymId = user.gym_id,
                title = title,
                message = message,
                imageUrl = imageUrl,
                authorId = user.id,
                authorName = user.name,
                isActiveField = true, // Set strictly true
                createdAt = Timestamp.now()
            )

            newDocRef.set(notice).await()
            android.util.Log.d("NoticeViewModel", "Notice created successfully: ${newDocRef.id}")
            
        } catch (e: Exception) {
            android.util.Log.e("NoticeViewModel", "Error creating notice", e)
            throw e // Re-throw to be caught by UI
        }
    }

    fun deleteNotice(noticeId: String) {
        val user = UserSession.currentUser.value ?: return
        
        if (noticeId.isBlank()) {
            android.util.Log.e("NoticeViewModel", "Cannot delete notice with empty ID")
            return
        }
        
        android.util.Log.d("NoticeViewModel", "Attempting to delete notice: $noticeId")

        // Update BOTH fields to be false to cover all bases
        val updates = mapOf(
            "isActive" to false,
            "active" to false
        )

        firestore.collection("gyms")
            .document(user.gym_id)
            .collection("news")
            .document(noticeId)
            .update(updates)
            .addOnSuccessListener {
                 android.util.Log.d("NoticeViewModel", "Notice $noticeId marked as inactive successfully")
            }
            .addOnFailureListener { e ->
                android.util.Log.e("NoticeViewModel", "Error deleting notice $noticeId", e)
            }
    }
    
    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
        listenerRegistration = null
        currentGymId = null
    }
}
