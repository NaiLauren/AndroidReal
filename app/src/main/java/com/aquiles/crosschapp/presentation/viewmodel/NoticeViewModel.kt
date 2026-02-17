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

    fun loadNotices() {
        val user = UserSession.currentUser.value ?: return

        android.util.Log.d("NoticeViewModel", "Loading notices for gym: ${user.gym_id}")

            // iOS path: /gyms/{gymId}/news
        firestore.collection("gyms")
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
                    android.util.Log.d("NoticeViewModel", "Notice: id=${notice.id}, title=${notice.title}, imageUrl='${notice.imageUrl}', legacy='${notice.imageUrlLegacy}', ACTUAL='${notice.actualImageUrl}'")
                }
                
                // Filtrar expirados localmente para asegurar (si aplica)
                val now = Timestamp.now()
                val filtered = items.filter { notice ->
                    notice.expiresAt == null || notice.expiresAt > now
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
                isActive = true,
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
        
        firestore.collection("gyms")
            .document(user.gym_id)
            .collection("news")
            .document(noticeId)
            .update("isActive", false)
    }
}
