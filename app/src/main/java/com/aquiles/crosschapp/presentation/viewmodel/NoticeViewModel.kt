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
            .whereEqualTo("isActive", true)
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
                    android.util.Log.d("NoticeViewModel", "Notice: id=${notice.id}, title=${notice.title}, imageUrl=${notice.imageUrl}, type=${notice.type}, priority=${notice.priority}")
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

    fun createNotice(
        title: String?,
        imageUrl: String,
        message: String = "" // Optional message, defaults to empty for iOS compatibility
    ) {
        val user = UserSession.currentUser.value ?: return

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
            isActive = true
            // type, priority, expiresAt remain null (iOS-style)
        )

        newDocRef.set(notice)
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
