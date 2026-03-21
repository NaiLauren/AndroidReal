package com.aquiles.crosschapp.presentation.viewmodel

import com.aquiles.crosschapp.data.model.CreditRequest
import com.aquiles.crosschapp.data.model.GymClass
import com.aquiles.crosschapp.data.model.User
import com.aquiles.crosschapp.data.model.Wod
import com.aquiles.crosschapp.data.model.BenchmarkWod
import com.google.firebase.firestore.PropertyName

// --- MODELOS DE DATOS ESPECÍFICOS DEL VIEWMODEL ---

data class CreditPack(

    val id: String = "",
    val name: String = "",
    val description: String = "",
    val credits: Int = 0,
    var price: Double = 0.0,
    val surchargePrice: Double = 0.0,
    @get:PropertyName("active") @set:PropertyName("active")
    var isActive: Boolean = true,
    val gym_id: String = "",
    val order: Int = 0,
    val isUnlimited: Boolean = false,
    val durationDays: Int = 30
)

data class BillingRules(
    @get:PropertyName("surchargeActive") @set:PropertyName("surchargeActive")
    var isSurchargeActive: Boolean = false
)

data class Attendee(val userId: String, val fullName: String, val profileImageUrl: String)


// --- DEFINICIONES DE ESTADOS (SEALED CLASSES) ---

sealed class CreditRequestsListState {
    object Loading : CreditRequestsListState()
    data class Success(
        val pendingRequests: List<CreditRequest>,
        val processedRequests: List<CreditRequest>
    ) : CreditRequestsListState()
    object Empty : CreditRequestsListState()
    data class Error(val message: String) : CreditRequestsListState()
    object Idle: CreditRequestsListState()
}

sealed class RequestUpdateState {
    object Idle : RequestUpdateState()
    object Loading : RequestUpdateState()
    data class Success(val message: String) : RequestUpdateState()
    data class Error(val message: String) : RequestUpdateState()
}

sealed class ClassListState {
    object Loading : ClassListState()
    data class Success(val classes: List<GymClass>) : ClassListState()
    data class Error(val message: String) : ClassListState()
    object Idle: ClassListState()
}

sealed class ClassOperationState {
    object Idle : ClassOperationState()
    object Loading : ClassOperationState()
    data class Success(val message: String) : ClassOperationState()
    data class Error(val message: String) : ClassOperationState()
}

sealed class WodOperationState {
    object Idle : WodOperationState()
    object Loading : WodOperationState()
    data class Success(val message: String) : WodOperationState()
    data class Error(val message: String) : WodOperationState()
}

sealed class WodDetailsState {
    object Loading : WodDetailsState()
    data class Success(val wod: Wod?) : WodDetailsState()
    data class Error(val message: String) : WodDetailsState()
    data object Idle : WodDetailsState()
}

sealed class AppConfigState {
    object Loading: AppConfigState()
    data class Success(val imagesByDay: Map<String, String>) : AppConfigState()
    data class Error(val message: String) : AppConfigState()
    object Idle : AppConfigState()
}

sealed class BenchmarkWodsState {
    data object Loading: BenchmarkWodsState()
    data class Success(val wods: List<BenchmarkWod>) : BenchmarkWodsState()
    data class Error(val message: String) : BenchmarkWodsState()
    object Idle : BenchmarkWodsState()
}

sealed class WodsDashboardState {
    object Loading : WodsDashboardState()
    data class Success(val todayWod: Wod?, val tomorrowWod: Wod?) : WodsDashboardState()
    data class Error(val message: String) : WodsDashboardState()
    object Idle: WodsDashboardState()
}

sealed class UserListState {
    object Loading : UserListState()
    data class Success(val users: List<User>) : UserListState()
    data class Error(val message: String) : UserListState()
}

sealed class AttendeeListState {
    data object Loading : AttendeeListState()
    data class Success(val attendees: List<User>) : AttendeeListState()
    data class Error(val message: String) : AttendeeListState()
}

sealed class UserDetailsState {
    object Loading : UserDetailsState()
    data class Success(val user: User) : UserDetailsState()
    data class Error(val message: String) : UserDetailsState()
    object Idle: UserDetailsState()
}

sealed class ClassForEditState {
    object Loading : ClassForEditState()
    data class Success(val gymClass: GymClass, val wod: Wod?) : ClassForEditState()
    data class Error(val message: String) : ClassForEditState()
    object Idle: ClassForEditState()
}

data class AppPaymentInfo(
    val alias: String = "",
    val cbu: String = "",
    val bankName: String = "",
    val holder: String = ""
)

sealed class ReportsState {
    object Loading : ReportsState()
    data class Success(
        val totalRevenue: Double,
        val monthlyActiveUsers: Int,
        val monthlyTransactions: List<CreditRequest>,
        val dollarRate: Double? = null,
        val paymentStatus: String = "PENDING", // PENDING, PENDING_REVIEW, PAID, REJECTED
        val paymentInfo: AppPaymentInfo? = null
    ) : ReportsState()
    data class Error(val message: String) : ReportsState()
    object Idle : ReportsState()
}

sealed class CreditPacksState {
    object Loading : CreditPacksState()
    data class Success(val packs: List<CreditPack>) : CreditPacksState()
    object Empty : CreditPacksState()
    data class Error(val message: String) : CreditPacksState()
    object Idle : CreditPacksState()
}

sealed class BillingRulesState {
    object Loading : BillingRulesState()
    data class Success(val rules: BillingRules) : BillingRulesState()
    data class Error(val message: String) : BillingRulesState()
}
