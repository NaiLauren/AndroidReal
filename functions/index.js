// RUTA: functions/index.js
// VERSIÓN ACTUALIZADA: API FCM v1 + Limpieza de Tokens

const { onDocumentUpdated, onDocumentCreated, onDocumentWritten } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

/**
 * Función auxiliar para enviar notificaciones multicast y limpiar tokens inválidos.
 */
async function sendMulticastAndCleanup(tokens, title, body, data, userId) {
    if (!tokens || tokens.length === 0) return;

    // Estructura para enviarlo con sendEachForMulticast
    // Nota: 'data' debe contener solo strings.
    const message = {
        tokens: tokens,
        notification: {
            title: title,
            body: body,
        },
        data: data || {},
        // Configuración específica para Android (opcional pero recomendada)
        android: {
            notification: {
                sound: "default",
                priority: "high"
            }
        }
    };

    try {
        const response = await admin.messaging().sendEachForMulticast(message);
        console.log(`FCM Multicast enviado. Éxitos: ${response.successCount}, Fallos: ${response.failureCount}`);

        if (response.failureCount > 0) {
            const tokensToRemove = [];
            response.responses.forEach((resp, idx) => {
                if (!resp.success) {
                    const error = resp.error;
                    // Chequear si el error indica que el token ya no es válido
                    if (error.code === 'messaging/registration-token-not-registered' ||
                        error.code === 'messaging/invalid-argument') { // invalid-argument a veces sale con tokens corruptos
                        tokensToRemove.push(tokens[idx]);
                    }
                }
            });

            if (tokensToRemove.length > 0 && userId) {
                console.log(`Eliminando ${tokensToRemove.length} tokens inválidos para el usuario ${userId}`);
                await db.collection("users").doc(userId).update({
                    fcmTokens: admin.firestore.FieldValue.arrayRemove(...tokensToRemove)
                });
            }
        }
    } catch (error) {
        console.error("Error crítico enviando FCM multicast:", error);
    }
}

/**
 * Crea una notificación en la colección de nivel superior 'notifications'.
 */
async function createInAppNotification(userId, gymId, title, message, type) {
    if (!userId || !gymId || !title || !message) {
        console.error("Faltan datos para crear la notificación en la app.", { userId, gymId, title, message, type });
        return;
    }
    const notificationData = {
        userId: userId,
        gym_id: gymId,
        title: title,
        body: message, 
        isRead: false,
        type: type,
        timestamp: admin.firestore.FieldValue.serverTimestamp(),
    };
    try {
        await db.collection("notifications").add(notificationData);
        console.log(`Notificación en la app creada para el usuario ${userId} en el gym ${gymId}`);
    } catch (error) {
        console.error(`Error al crear notificación en la app para ${userId}:`, error);
    }
}

/**
 * Se activa cuando se actualiza una solicitud de crédito (para aprobaciones o rechazos).
 */
exports.onCreditRequestUpdate = onDocumentUpdated({
    document: "creditRequests/{requestId}",
    region: "southamerica-east1"
}, async (event) => {
    const beforeData = event.data.before.data();
    const afterData = event.data.after.data();
    const isApproved = beforeData.status !== "APPROVED" && afterData.status === "APPROVED";
    const isRejected = beforeData.status !== "REJECTED" && afterData.status === "REJECTED";
    
    if (!isApproved && !isRejected) return null;

    const userId = afterData.userId;
    const gymId = afterData.gym_id;
    const title = isApproved ? "¡Créditos Aprobados! ✅" : "Solicitud Rechazada ❌";
    const body = isApproved ?
        `Tu solicitud del pack '${afterData.comboName}' fue aprobada. ¡Ya puedes entrenar!` :
        `Tu solicitud del pack '${afterData.comboName}' fue rechazada. Contacta con un administrador.`;
    const type = isApproved ? "CREDIT_APPROVED" : "CREDIT_REJECTED";
    
    await createInAppNotification(userId, gymId, title, body, type);
    
    const userDoc = await db.collection("users").doc(userId).get();
    if (!userDoc.exists) return null;

    const fcmTokens = userDoc.data().fcmTokens;
    if (fcmTokens && Array.isArray(fcmTokens) && fcmTokens.length > 0) {
        await sendMulticastAndCleanup(fcmTokens, title, body, { type: type }, userId);
    }
    return null;
});

/**
 * Se activa cuando un usuario crea una nueva solicitud de crédito para notificar a los admins DE ESE GIMNASIO.
 */
exports.onNewCreditRequest = onDocumentCreated({
    document: "creditRequests/{requestId}",
    region: "southamerica-east1"
}, async (event) => {
    const newRequest = event.data.data();
    const gymId = newRequest.gym_id;
    const title = "Nueva Solicitud de Créditos ⚠️";
    const body = `${newRequest.userName} ha solicitado el pack '${newRequest.comboName}'.`;
    const type = "NEW_CREDIT_REQUEST";
    
    const adminQuery = await db.collection("users")
        .where("gym_id", "==", gymId)
        .where("role", "in", ["owner", "coach"])
        .get();

    if (adminQuery.empty) return null;

    const allAdminTokens = [];
    const adminUserIds = [];
    
    // Primera pasada: recolectar IDs y crear notificaciones in-app
    // NOTA: Para admins es difícil hacer cleanup grupal porque los tokens están dispersos en varios docs.
    // Haremos un best-effort enviando individualmente o agrupando, pero por simplicidad aquí 
    // enviaremos multicast a la lista plana de tokens. El cleanup NO se hará para admins en este bloque simple
    // para evitar lógica compleja de mapear token -> adminId.
    
    const promises = [];
    
    adminQuery.forEach((doc) => {
        adminUserIds.push(doc.id);
        const adminTokens = doc.data().fcmTokens;
        if (adminTokens && Array.isArray(adminTokens) && adminTokens.length > 0) {
            allAdminTokens.push(...adminTokens);
        }
        promises.push(createInAppNotification(doc.id, gymId, title, body, type));
    });

    await Promise.all(promises);
    
    if (allAdminTokens.length > 0) {
        // Pasamos null como userId para evitar intentar borrar tokens de un usuario inexistente "global"
        await sendMulticastAndCleanup(allAdminTokens, title, body, { type: type }, null);
    }
    return null;
});

/**
 * Se activa cuando se crea un nuevo mensaje personal para notificar al destinatario.
 */
exports.sendNewMessageNotification = onDocumentCreated({
    document: "personal_messages/{messageId}",
    region: "southamerica-east1"
}, async (event) => {
    const messageData = event.data.data();
    if (!messageData) return null;

    const recipientId = messageData.userId;
    const senderName = messageData.sender_name || "Un administrador";
    let messageContent = messageData.content;

    if (recipientId === messageData.sender_id) return null;

    if (!messageContent || messageContent.trim() === "") {
        if (messageData.attachmentType && messageData.attachmentType.includes("image")) {
            messageContent = "Te ha enviado una imagen.";
        } else {
            messageContent = "Te ha enviado un archivo adjunto.";
        }
    }
    
    await createInAppNotification(recipientId, messageData.gym_id, `Nuevo mensaje de ${senderName}`, messageContent, "NEW_PERSONAL_MESSAGE");

    console.log(`Preparando notificación PUSH de mensaje para ${recipientId} de ${senderName}`);

    const userDoc = await db.collection("users").doc(recipientId).get();
    if (!userDoc.exists) return null;
    
    const fcmTokens = userDoc.data().fcmTokens;
    if (fcmTokens && Array.isArray(fcmTokens) && fcmTokens.length > 0) {
        await sendMulticastAndCleanup(fcmTokens, `Nuevo mensaje de ${senderName}`, messageContent, { 
            type: "NEW_PERSONAL_MESSAGE",
            senderId: messageData.sender_id
        }, recipientId);
    }
    return null;
});

/**
 * Se activa cuando se crea o actualiza un documento de usuario.
 * Lee el campo 'role' y asigna un "custom claim" de autenticación 'isAdmin'.
 */
exports.setUserAdminClaim = onDocumentWritten({
    document: "users/{userId}",
    region: "southamerica-east1"
}, async (event) => {
    const userData = event.data.after.data();
    const userId = event.params.userId;
    if (!userData) return null;

    const role = userData.role;
    const isAdmin = role === "owner" || role === "coach";
    try {
        const user = await admin.auth().getUser(userId);
        const currentClaims = user.customClaims;
        if (currentClaims && currentClaims.isAdmin === isAdmin) return null;

        console.log(`Asignando claim 'isAdmin: ${isAdmin}' al usuario ${userId}`);
        return admin.auth().setCustomUserClaims(userId, { isAdmin: isAdmin });
    } catch (error) {
        console.error(`Error al asignar claims para ${userId}:`, error);
        return null;
    }
});