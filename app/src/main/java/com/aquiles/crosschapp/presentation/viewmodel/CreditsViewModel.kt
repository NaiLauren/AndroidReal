package com.aquiles.crosschapp.presentation.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aquiles.crosschapp.data.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

sealed class OfferingsState {
    object Idle : OfferingsState()
    object Loading : OfferingsState()
    data class Success(val packs: List<CreditPack>, val surchargeApplied: Boolean) : OfferingsState()
    data class Error(val message: String) : OfferingsState()
}

sealed class CreditRequestOperationState {
    object Idle : CreditRequestOperationState()
    object Loading : CreditRequestOperationState()
    data class Success(val message: String) : CreditRequestOperationState()
    data class Error(val message: String) : CreditRequestOperationState()
}

data class PaymentDetails(val bankTransferInfo: String = "", val mercadoPagoInfo: String = "")

sealed class PaymentDetailsState {
    object Loading : PaymentDetailsState()
    data class Success(val details: PaymentDetails) : PaymentDetailsState()
    data class Error(val message: String) : PaymentDetailsState()
}

class CreditsViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private val currentUserGymId: String?
        get() = UserSession.currentUserGymId.value

    private val _offeringsState = MutableStateFlow<OfferingsState>(OfferingsState.Idle)
    val offeringsState: StateFlow<OfferingsState> = _offeringsState.asStateFlow()

    private val _creditRequestOperationState = MutableStateFlow<CreditRequestOperationState>(CreditRequestOperationState.Idle)
    val creditRequestOperationState: StateFlow<CreditRequestOperationState> = _creditRequestOperationState.asStateFlow()

    private val _paymentDetailsState = MutableStateFlow<PaymentDetailsState>(PaymentDetailsState.Loading)
    val paymentDetailsState: StateFlow<PaymentDetailsState> = _paymentDetailsState.asStateFlow()

    init {
        loadAvailablePacks()
        loadPaymentDetails()
    }

    // Carga los packs y verifica si hay recargo en 'payment_info'
    fun loadAvailablePacks() {
        viewModelScope.launch {
            _offeringsState.value = OfferingsState.Loading
            val gymId = currentUserGymId
            if (gymId.isNullOrBlank()) {
                _offeringsState.value = OfferingsState.Error("No se pudo identificar tu gimnasio.")
                return@launch
            }

            try {
                // UNIFICADO: Todo en 'payment_info'
                val configSnapshot = firestore.collection("gyms").document(gymId)
                    .collection("settings").document("payment_info")
                    .get().await()

                val isSurchargeActive = configSnapshot.getBoolean("isSurchargeActive") ?: false

                val packsSnapshot = firestore.collection("creditPacks")
                    .whereEqualTo("gym_id", gymId)
                    .whereEqualTo("active", true)
                    .orderBy("order", Query.Direction.ASCENDING)
                    .get().await()

                val activePacks = packsSnapshot.documents.mapNotNull { it.toObject(CreditPack::class.java)?.copy(id = it.id) }

                // Aplica precio con recargo o normal
                val finalPacks = if (isSurchargeActive) activePacks.map { it.copy(price = it.surchargePrice) } else activePacks

                _offeringsState.value = OfferingsState.Success(finalPacks, isSurchargeActive)
            } catch (e: Exception) {
                _offeringsState.value = OfferingsState.Error("No se pudieron cargar los packs.")
            }
        }
    }

    // Carga los textos de CBU/Alias desde 'payment_info'
    fun loadPaymentDetails() {
        viewModelScope.launch {
            _paymentDetailsState.value = PaymentDetailsState.Loading
            val gymId = currentUserGymId
            if (gymId.isNullOrBlank()) {
                _paymentDetailsState.value = PaymentDetailsState.Error("No se identificó el gimnasio.")
                return@launch
            }

            try {
                // UNIFICADO: Todo en 'payment_info'
                val doc = firestore.collection("gyms").document(gymId)
                    .collection("settings").document("payment_info")
                    .get().await()

                if (doc.exists()) {
                    val bankInfo = doc.getString("bankTransferInfo") ?: ""
                    val mpInfo = doc.getString("mercadoPagoInfo") ?: ""
                    _paymentDetailsState.value = PaymentDetailsState.Success(PaymentDetails(bankInfo, mpInfo))
                } else {
                    _paymentDetailsState.value = PaymentDetailsState.Success(PaymentDetails("Consultar en recepción", "Consultar en recepción"))
                }
            } catch (e: Exception) {
                _paymentDetailsState.value = PaymentDetailsState.Error(e.localizedMessage ?: "Error al cargar información de pago.")
            }
        }
    }

    fun requestCredit(pack: CreditPack, paymentMethod: String, paymentProofUri: Uri?) {
        viewModelScope.launch {
            _creditRequestOperationState.value = CreditRequestOperationState.Loading

            val user = UserSession.currentUser.value
            val userId = user?.id
            val gymId = user?.gym_id

            if (user == null || userId.isNullOrBlank() || gymId.isNullOrBlank()) {
                _creditRequestOperationState.value = CreditRequestOperationState.Error("Sesión inválida.")
                return@launch
            }

            try {
                withContext(Dispatchers.IO) {
                    val newRequestRef = firestore.collection("creditRequests").document()
                    val requestId = newRequestRef.id

                    val paymentProofFullUrl: String? = if (paymentProofUri != null) {
                        uploadImageAndGetUrl(gymId, userId, "$requestId.jpg", paymentProofUri)
                    } else {
                        null
                    }

                    val newCreditRequest = CreditRequest(
                        id = requestId,
                        gym_id = gymId,
                        userId = userId,
                        userName = user.fullName,
                        contactInfo = user.phoneNumber,
                        comboName = pack.name,
                        creditsRequested = pack.credits,
                        amountPaid = pack.price,
                        paymentMethod = paymentMethod,
                        paymentProofUrl = paymentProofFullUrl,
                        fcmToken = null,
                        status = CreditRequestStatus.PENDING.name
                    )

                    newRequestRef.set(newCreditRequest).await()
                    createUserConfirmationNotification(userId, pack.name, gymId)
                }
                _creditRequestOperationState.value = CreditRequestOperationState.Success("¡Solicitud enviada con éxito!")
            } catch (e: Exception) {
                _creditRequestOperationState.value = CreditRequestOperationState.Error(e.localizedMessage ?: "Error al enviar la solicitud.")
            }
        }
    }

    private suspend fun uploadImageAndGetUrl(gymId: String, userId: String, fileName: String, imageUri: Uri): String {
        val storagePath = "payment_proofs/$gymId/$userId/$fileName"
        val storageRef = storage.reference.child(storagePath)

        return suspendCancellableCoroutine { continuation ->
            val uploadTask = storageRef.putFile(imageUri)
            uploadTask.addOnSuccessListener {
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    if (continuation.isActive) continuation.resume(uri.toString())
                }.addOnFailureListener { e -> if (continuation.isActive) continuation.resumeWithException(e) }
            }.addOnFailureListener { e -> if (continuation.isActive) continuation.resumeWithException(e) }
                .addOnCanceledListener { continuation.cancel() }
        }
    }

    private suspend fun createUserConfirmationNotification(userId: String, comboName: String, gymId: String) {
        val notification = Notification(userId = userId, gym_id = gymId, title = "Solicitud Enviada", message = "Tu pedido del combo \"$comboName\" está pendiente.", type = "CREDIT_PENDING")
        try { firestore.collection("notifications").add(notification).await() } catch (e: Exception) { e.printStackTrace() }
    }

    fun resetCreditRequestOperationState() { _creditRequestOperationState.value = CreditRequestOperationState.Idle }
}