// RUTA: presentation/viewmodel/CreditsViewModel.kt
// VERSIÓN CON CORRECCIÓN DE BLOQUEO DE HILO PRINCIPAL (UI THREAD)

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

    fun loadAvailablePacks() {
        viewModelScope.launch {
            _offeringsState.value = OfferingsState.Loading
            val gymId = currentUserGymId
            if (gymId.isNullOrBlank()) {
                _offeringsState.value = OfferingsState.Error("No se pudo identificar tu gimnasio.")
                return@launch
            }

            try {
                val rulesSnapshot = firestore.collection("settings").document("billing_rules").get().await()
                val rules = rulesSnapshot.toObject(BillingRules::class.java) ?: BillingRules()

                val packsSnapshot = firestore.collection("creditPacks")
                    .whereEqualTo("gym_id", gymId)
                    .whereEqualTo("active", true)
                    .orderBy("order", Query.Direction.ASCENDING)
                    .get().await()

                val activePacks = packsSnapshot.documents.mapNotNull { it.toObject(CreditPack::class.java)?.copy(id = it.id) }
                val finalPacks = if (rules.isSurchargeActive) activePacks.map { it.copy(price = it.surchargePrice) } else activePacks
                _offeringsState.value = OfferingsState.Success(finalPacks, rules.isSurchargeActive)
            } catch (e: Exception) {
                _offeringsState.value = OfferingsState.Error("No se pudieron cargar los packs.")
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
                _creditRequestOperationState.value = CreditRequestOperationState.Error("Sesión inválida. Vuelve a iniciar sesión.")
                return@launch
            }

            try {
                // --- CAMBIO CLAVE: Mover toda la operación de red a un hilo de I/O ---
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

                // Al volver al hilo principal, actualizamos la UI con el éxito
                _creditRequestOperationState.value = CreditRequestOperationState.Success("¡Solicitud enviada con éxito!")

            } catch (e: Exception) {
                Log.e("CreditsViewModel", "Error en requestCredit", e)
                _creditRequestOperationState.value = CreditRequestOperationState.Error(e.localizedMessage ?: "Error al enviar la solicitud.")
            }
        }
    }

    private suspend fun uploadImageAndGetUrl(gymId: String, userId: String, fileName: String, imageUri: Uri): String {
        val storagePath = "payment_proofs/$gymId/$userId/$fileName"
        val storageRef = storage.reference.child(storagePath)

        Log.d("STORAGE_UPLOAD", "Iniciando subida a: $storagePath")

        return suspendCancellableCoroutine { continuation ->
            val uploadTask = storageRef.putFile(imageUri)

            uploadTask
                .addOnProgressListener { taskSnapshot ->
                    val progress = (100.0 * taskSnapshot.bytesTransferred) / taskSnapshot.totalByteCount
                    Log.d("STORAGE_UPLOAD", "Progreso: ${progress.toInt()}%")
                }
                .addOnSuccessListener {
                    Log.d("STORAGE_UPLOAD", "¡Subida exitosa! Obteniendo URL de descarga...")
                    storageRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            Log.d("STORAGE_UPLOAD", "URL de descarga obtenida: $uri")
                            if (continuation.isActive) {
                                continuation.resume(uri.toString())
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e("STORAGE_UPLOAD", "Error al obtener la URL de descarga", exception)
                            if (continuation.isActive) {
                                continuation.resumeWithException(exception)
                            }
                        }
                }
                .addOnFailureListener { exception ->
                    Log.e("STORAGE_UPLOAD", "Error durante la subida del archivo", exception)
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }
                .addOnCanceledListener {
                    Log.w("STORAGE_UPLOAD", "La subida fue cancelada")
                    continuation.cancel()
                }

            continuation.invokeOnCancellation {
                uploadTask.cancel()
            }
        }
    }

    private suspend fun createUserConfirmationNotification(userId: String, comboName: String, gymId: String) {
        val notification = Notification(
            userId = userId,
            gym_id = gymId,
            title = "Solicitud Enviada",
            message = "Tu pedido del combo \"$comboName\" está pendiente de aprobación.",
            type = "CREDIT_PENDING"
        )
        try {
            firestore.collection("notifications").add(notification).await()
        } catch (e: Exception) {
            Log.e("CreditsViewModel", "Error creating user notification", e)
        }
    }

    fun resetCreditRequestOperationState() {
        _creditRequestOperationState.value = CreditRequestOperationState.Idle
    }

    fun loadPaymentDetails() {
        viewModelScope.launch {
            _paymentDetailsState.value = PaymentDetailsState.Loading
            try {
                val doc = firestore.collection("settings").document("payment_info").get().await()
                if (doc.exists()) {
                    val bankInfo = doc.getString("bankTransferInfo") ?: "No disponible."
                    val mpInfo = doc.getString("mercadoPagoInfo") ?: "No disponible."
                    _paymentDetailsState.value = PaymentDetailsState.Success(PaymentDetails(bankInfo, mpInfo))
                } else {
                    _paymentDetailsState.value = PaymentDetailsState.Error("Información de pago no configurada.")
                }
            } catch (e: Exception) {
                _paymentDetailsState.value = PaymentDetailsState.Error(e.localizedMessage ?: "Error al cargar información.")
            }
        }
    }
}