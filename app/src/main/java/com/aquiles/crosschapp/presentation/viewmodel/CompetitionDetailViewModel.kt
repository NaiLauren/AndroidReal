package com.aquiles.crosschapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.Competition
import com.aquiles.crosschapp.data.model.GymClass
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.Date

class CompetitionDetailViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _competition = MutableStateFlow<Competition?>(null)
    val competition: StateFlow<Competition?> = _competition.asStateFlow()

    private val _linkedClasses = MutableStateFlow<List<GymClass>>(emptyList())
    val linkedClasses: StateFlow<List<GymClass>> = _linkedClasses.asStateFlow()

    private val _isLoadingClasses = MutableStateFlow(false)
    val isLoadingClasses: StateFlow<Boolean> = _isLoadingClasses.asStateFlow()

    // For Class Selector
    private val _classesForDate = MutableStateFlow<List<GymClass>>(emptyList())
    val classesForDate: StateFlow<List<GymClass>> = _classesForDate.asStateFlow()
    
    private val _isLoadingSelector = MutableStateFlow(false)
    val isLoadingSelector: StateFlow<Boolean> = _isLoadingSelector.asStateFlow()

    fun loadCompetition(competitionId: String) {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("competitions").document(competitionId).get().await()
                val comp = snapshot.toObject(Competition::class.java)
                if (comp != null) {
                    setCompetition(comp)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun setCompetition(comp: Competition) {
        _competition.value = comp
        loadLinkedClasses(comp)
    }

    private fun loadLinkedClasses(comp: Competition) {
        val ids = comp.linkedClassIds
        if (ids.isEmpty()) {
            _linkedClasses.value = emptyList()
            return
        }

        _isLoadingClasses.value = true
        viewModelScope.launch {
            try {
                // Chunk queries by 10 IDs limit for 'in' query
                val chunks = ids.chunked(10)
                val allFetchedClasses = mutableListOf<GymClass>()

                for (chunk in chunks) {
                    val snapshot = db.collection("gymClasses")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .await()
                    allFetchedClasses.addAll(snapshot.toObjects(GymClass::class.java))
                }
                
                // Sort by date equivalent
                _linkedClasses.value = allFetchedClasses.sortedBy { it.dateTime }
            } catch (e: Exception) {
                // Handle error
                e.printStackTrace()
            } finally {
                _isLoadingClasses.value = false
            }
        }
    }

    fun loadClassesForDate(date: Date, gymId: String) {
        _isLoadingSelector.value = true
        viewModelScope.launch {
            try {
                val calendar = Calendar.getInstance()
                calendar.time = date
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.time

                calendar.add(Calendar.DAY_OF_MONTH, 1)
                val endOfDay = calendar.time

                val snapshot = db.collection("gymClasses")
                    .whereEqualTo("gym_id", gymId)
                    .whereGreaterThanOrEqualTo("dateTime", startOfDay)
                    .whereLessThan("dateTime", endOfDay)
                    .orderBy("dateTime")
                    .get()
                    .await()

                val classes = snapshot.toObjects(GymClass::class.java)
                _classesForDate.value = classes.filter { !it.isCancelled }

            } catch (e: Exception) {
                e.printStackTrace()
                _classesForDate.value = emptyList()
            } finally {
                _isLoadingSelector.value = false
            }
        }
    }

    fun linkClasses(newClasses: List<GymClass>) {
        val currentComp = _competition.value ?: return
        val compId = currentComp.id
        if (compId.isBlank()) return

        val newIds = newClasses.map { it.documentId }
        val updatedIds = (currentComp.linkedClassIds + newIds).distinct()

        viewModelScope.launch {
            try {
                db.collection("competitions").document(compId)
                    .update("linked_class_ids", updatedIds)
                    .await()
                
                // Update local state
                val updatedComp = currentComp.copy(linkedClassIds = updatedIds)
                setCompetition(updatedComp) 
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun unlinkClass(classId: String) {
        val currentComp = _competition.value ?: return
        val compId = currentComp.id
        if (compId.isBlank()) return

        val updatedIds = currentComp.linkedClassIds.filter { it != classId }

        viewModelScope.launch {
            try {
                 db.collection("competitions").document(compId)
                    .update("linked_class_ids", updatedIds)
                    .await()
                
                // Update local state
                val updatedComp = currentComp.copy(linkedClassIds = updatedIds)
                setCompetition(updatedComp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun finishCompetition() {
         val currentComp = _competition.value ?: return
         val compId = currentComp.id

         viewModelScope.launch {
             try {
                 db.collection("competitions").document(compId)
                    .update("isActive", false)
                    .await()
                 
                 val updatedComp = currentComp.copy(isActive = false)
                 _competition.value = updatedComp
             } catch (e: Exception) {
                 e.printStackTrace()
             }
         }
    }
}
