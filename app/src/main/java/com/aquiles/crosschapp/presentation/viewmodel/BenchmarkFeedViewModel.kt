package com.aquiles.crosschapp.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.BenchmarkResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class FeedState {
    object Idle : FeedState()
    object Loading : FeedState()
    data class Success(val items: List<BenchmarkResult>) : FeedState()
    data class Error(val message: String) : FeedState()
}

class BenchmarkFeedViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _feedState = MutableStateFlow<FeedState>(FeedState.Idle)
    val feedState = _feedState.asStateFlow()

    private var listenerRegistration: com.google.firebase.firestore.ListenerRegistration? = null

    fun loadFeed() {
        val currentUser = UserSession.currentUser.value ?: return
        if (currentUser.gym_id.isBlank()) return

        // Evitar duplicar listeners
        listenerRegistration?.remove()
        
        _feedState.value = FeedState.Loading

        listenerRegistration = firestore.collection("benchmark_results")
            .whereEqualTo("gym_id", currentUser.gym_id)
            .orderBy("date", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("BenchmarkFeedVM", "Error loading feed", error)
                    _feedState.value = FeedState.Error("Error al cargar el muro: ${error.localizedMessage}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val items = snapshot.toObjects(BenchmarkResult::class.java)
                    _feedState.value = FeedState.Success(items)
                }
            }
    }

    fun toggleVerification(result: BenchmarkResult) {
        val currentUser = UserSession.currentUser.value
        // Security Check: Only admins (Client-side check, enforced by Security Rules)
        if (currentUser?.isAdmin != true && currentUser?.role != "owner") return

        viewModelScope.launch {
            try {
                val newStatus = !result.isVerified
                firestore.collection("benchmark_results")
                    .document(result.resultId)
                    .update("isVerified", newStatus)
                    .await()
                
                // No necesitamos recargar manual (loadFeed), el listener lo hará solo.
            } catch (e: Exception) {
                Log.e("BenchmarkFeedVM", "Error verifying result", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}
