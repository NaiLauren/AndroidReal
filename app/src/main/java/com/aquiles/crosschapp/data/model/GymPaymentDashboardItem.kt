package com.aquiles.crosschapp.data.model

data class GymPaymentDashboardItem(
    val id: String,
    val gymName: String,
    val paymentDocId: String,
    val status: PaymentStatus,
    val proofUrl: String?,
    val studentCount: Int,
    val estimatedDebt: Double,
    val lastPaymentDateStr: String
)

enum class PaymentStatus {
    UNPAID, PENDING_REVIEW, PAID, REJECTED
}
