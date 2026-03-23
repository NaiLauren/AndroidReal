package com.aquiles.crosschapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.GlobalPaymentSettings
import com.aquiles.crosschapp.data.model.GymPaymentDashboardItem
import com.aquiles.crosschapp.data.model.PaymentStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SuperAdminPaymentsViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val superAdminEmail = "admin@realfitness.com"

    private val _dashboardItems = MutableStateFlow<List<GymPaymentDashboardItem>>(emptyList())
    val dashboardItems: StateFlow<List<GymPaymentDashboardItem>> = _dashboardItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _selectedDate = MutableStateFlow(Date())
    val selectedDate: StateFlow<Date> = _selectedDate.asStateFlow()

    private val _totalEstRevenue = MutableStateFlow(0.0)
    val totalEstRevenue: StateFlow<Double> = _totalEstRevenue.asStateFlow()

    private val _paymentSettings = MutableStateFlow(GlobalPaymentSettings())
    val paymentSettings: StateFlow<GlobalPaymentSettings> = _paymentSettings.asStateFlow()

    private val _isSavingSettings = MutableStateFlow(false)
    val isSavingSettings: StateFlow<Boolean> = _isSavingSettings.asStateFlow()

    fun checkPermissionAndLoad() {
        val userEmail = auth.currentUser?.email
        if (userEmail == null || !userEmail.equals(superAdminEmail, ignoreCase = true)) {
            _errorMessage.value = "Acceso denegado. No eres Super Admin."
            return
        }
        loadDashboard()
        loadPaymentSettings()
    }

    private fun loadPaymentSettings() {
        viewModelScope.launch {
            try {
                val snapshot = db.collection("settings").document("payment_info").get().await()
                if (snapshot.exists()) {
                    val settings = GlobalPaymentSettings(
                        alias = snapshot.getString("alias") ?: "",
                        cbu = snapshot.getString("cbu") ?: "",
                        bankName = snapshot.getString("bankName") ?: "",
                        accountHolder = snapshot.getString("accountHolder") ?: ""
                    )
                    _paymentSettings.value = settings
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun savePaymentSettings(alias: String, cbu: String, bankName: String, accountHolder: String) {
        viewModelScope.launch {
            _isSavingSettings.value = true
            try {
                val data = mapOf(
                    "alias" to alias,
                    "cbu" to cbu,
                    "bankName" to bankName,
                    "accountHolder" to accountHolder,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                db.collection("settings").document("payment_info").set(data).await()
                _paymentSettings.value = GlobalPaymentSettings(alias, cbu, bankName, accountHolder)
            } catch (e: Exception) {
                _errorMessage.value = "Error guardando configuración: \${e.localizedMessage}"
            } finally {
                _isSavingSettings.value = false
            }
        }
    }

    fun changeMonth(value: Int) {
        val calendar = Calendar.getInstance()
        calendar.time = _selectedDate.value
        calendar.add(Calendar.MONTH, value)
        _selectedDate.value = calendar.time
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            try {
                // 1. Obtener todos los gimnasios
                val gymsSnap = db.collection("gyms").get().await()
                val gyms = gymsSnap.documents.map { doc ->
                    Pair(doc.id, doc.getString("name") ?: "Sin Nombre")
                }

                // 2. Obtener pagos del mes actual
                val sdf = SimpleDateFormat("yyyy_MM", Locale.getDefault())
                val monthStr = sdf.format(_selectedDate.value)

                val paymentsSnap = db.collection("app_payments")
                    .whereEqualTo("month", monthStr)
                    .get().await()

                val paymentsMap = paymentsSnap.documents.associateBy { it.getString("gym_id") ?: "" }

                val items = mutableListOf<GymPaymentDashboardItem>()
                for (gym in gyms) {
                    val gymId = gym.first
                    val gymName = gym.second
                    val paymentDoc = paymentsMap[gymId]

                    var status = PaymentStatus.UNPAID
                    var proofUrl: String? = null

                    if (paymentDoc != null) {
                        proofUrl = paymentDoc.getString("proof_url")
                        val statusStr = paymentDoc.getString("status")
                        status = when (statusStr) {
                            "PAID" -> PaymentStatus.PAID
                            "PENDING_REVIEW" -> PaymentStatus.PENDING_REVIEW
                            "REJECTED" -> PaymentStatus.REJECTED
                            else -> PaymentStatus.UNPAID
                        }
                    }

                    // B. Contar alumnos activos (simples para este scope)
                    val studentsSnap = db.collection("users")
                        .whereEqualTo("gym_id", gymId)
                        .get().await() // count is not fully supported without aggregation plugin, fallback to snap size
                    val studentCount = studentsSnap.size()
                    val estDebt = studentCount * 1.13

                    // C. Último pago
                    var lastPayStr = "Nunca"
                    val lastPaySnap = db.collection("app_payments")
                        .whereEqualTo("gym_id", gymId)
                        .whereEqualTo("status", "PAID")
                        .orderBy("month", Query.Direction.DESCENDING)
                        .limit(1)
                        .get().await()

                    if (!lastPaySnap.isEmpty) {
                        val mStr = lastPaySnap.documents[0].getString("month") ?: ""
                        lastPayStr = formatMonthStr(mStr)
                    }

                    items.add(
                        GymPaymentDashboardItem(
                            id = gymId,
                            gymName = gymName,
                            paymentDocId = "\${gymId}_\${monthStr}",
                            status = status,
                            proofUrl = proofUrl,
                            studentCount = studentCount,
                            estimatedDebt = estDebt,
                            lastPaymentDateStr = lastPayStr
                        )
                    )
                }

                val sortedItems = items.sortedBy { rankForStatus(it.status) }
                val totalExpected = sortedItems.sumOf { it.estimatedDebt }

                _dashboardItems.value = sortedItems
                _totalEstRevenue.value = totalExpected

            } catch (e: Exception) {
                _errorMessage.value = "Error: \${e.localizedMessage}"
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun formatMonthStr(raw: String): String {
        val parts = raw.split("_")
        if (parts.size == 2) {
            try {
                val year = parts[0].toInt()
                val month = parts[1].toInt() - 1 // Calendar is 0-indexed
                val cal = Calendar.getInstance()
                cal.set(year, month, 1)
                val sdf = SimpleDateFormat("MMM yy", Locale("es", "ES"))
                return sdf.format(cal.time).replaceFirstChar { it.uppercase() }
            } catch (e: Exception) {
                // ignore
            }
        }
        return raw
    }

    private fun rankForStatus(status: PaymentStatus): Int {
        return when (status) {
            PaymentStatus.PENDING_REVIEW -> 0
            PaymentStatus.UNPAID -> 1
            PaymentStatus.REJECTED -> 2
            PaymentStatus.PAID -> 3
        }
    }

    fun markAsPaid(item: GymPaymentDashboardItem) { updateStatus(item, "PAID") }
    fun rejectPayment(item: GymPaymentDashboardItem) { updateStatus(item, "REJECTED") }

    private fun updateStatus(item: GymPaymentDashboardItem, newStatus: String) {
        viewModelScope.launch {
            try {
                val sdf = SimpleDateFormat("yyyy_MM", Locale.getDefault())
                val monthStr = sdf.format(_selectedDate.value)

                val data = mapOf(
                    "gym_id" to item.id,
                    "gymName" to item.gymName,
                    "month" to monthStr,
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "reviewedBy" to superAdminEmail,
                    "snapshot_studentCount" to item.studentCount,
                    "snapshot_amount" to item.estimatedDebt
                )

                db.collection("app_payments").document(item.paymentDocId).set(data, com.google.firebase.firestore.SetOptions.merge()).await()
                loadDashboard()
            } catch (e: Exception) {
                e.printStackTrace()
                _errorMessage.value = "Error al actualizar pago: \${e.localizedMessage}"
            }
        }
    }
}
