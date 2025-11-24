// RUTA: functions/index.js
// VERSIÓN CON NOTIFICACIONES DE CRÉDITO Y MENSAJES PERSONALES

const { onDocumentUpdated, onDocumentCreated, onDocumentWritten } = require("firebase-functions/v2/firestore");
const admin = require("firebase-admin");

admin.initializeApp();
const db = admin.firestore();

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
        body: message, // Cambiado 'message' por 'body' para consistencia con el resto de la app
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
    if (!isApproved && !isRejected) {
        console.log("Actualización de estado no relevante para notificación. Saliendo.");
        return null;
    }
    const userId = afterData.userId;
    const gymId = afterData.gym_id;
    const title = isApproved ? "¡Créditos Aprobados! ✅" : "Solicitud Rechazada ❌";
    const body = isApproved ?
        `Tu solicitud del pack '${afterData.comboName}' fue aprobada. ¡Ya puedes entrenar!` :
        `Tu solicitud del pack '${afterData.comboName}' fue rechazada. Contacta con un administrador.`;
    const type = isApproved ? "CREDIT_APPROVED" : "CREDIT_REJECTED";
    
    // La creación de la notificación interna sigue igual
    await createInAppNotification(userId, gymId, title, body, type);
    
    const userDoc = await db.collection("users").doc(userId).get();
    if (!userDoc.exists) {
        console.log(`Usuario ${userId} no encontrado para enviar push.`);
        return null;
    }
    const fcmTokens = userDoc.data().fcmTokens;
    if (fcmTokens && Array.isArray(fcmTokens) && fcmTokens.length > 0) {
        
        const payload = {
            notification: {
                title: title,
                body: body,
                sound: "default",
                badge: "1"
            },
            data: { 
                type: type 
            }
        };
        console.log("Enviando payload COMPLETO (notification + data) a FCM:", payload);
        return admin.messaging().sendToDevice(fcmTokens, payload);
    }
    console.log(`Usuario ${userId} no tiene tokens FCM para notificar.`);
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
    if (adminQuery.empty) {
        console.log(`No se encontraron administradores para el gym ${gymId}.`);
        return null;
    }
    const allAdminTokens = [];
    const adminUserIds = [];
    adminQuery.forEach((doc) => {
        adminUserIds.push(doc.id);
        const adminTokens = doc.data().fcmTokens;
        if (adminTokens && Array.isArray(adminTokens) && adminTokens.length > 0) {
            allAdminTokens.push(...adminTokens);
        }
    });
    const inAppNotificationsPromises = adminUserIds.map(adminId =>
        createInAppNotification(adminId, gymId, title, body, type)
    );
    await Promise.all(inAppNotificationsPromises);
    
    if (allAdminTokens.length > 0) {
        const payload = {
            notification: {
                title: title,
                body: body,
                sound: "default",
                badge: "1",
            },
            data: { 
                type: type 
            }
        };
        console.log(`Enviando payload COMPLETO a ${allAdminTokens.length} tokens de admin del gym ${gymId}:`, payload);
        return admin.messaging().sendToDevice(allAdminTokens, payload);
    }
    console.log(`No se encontraron tokens FCM en ninguna cuenta de admin para el gym ${gymId}.`);
    return null;
});


// --- INICIO DE LA NUEVA FUNCIÓN AÑADIDA ---

/**
 * Se activa cuando se crea un nuevo mensaje personal para notificar al destinatario.
 */
exports.sendNewMessageNotification = onDocumentCreated({
    document: "personal_messages/{messageId}",
    region: "southamerica-east1"
}, async (event) => {
    const messageData = event.data.data();
    if (!messageData) {
        console.log("No se encontraron datos en el nuevo mensaje.");
        return null;
    }

    const recipientId = messageData.userId;
    const senderName = messageData.sender_name || "Un administrador";
    let messageContent = messageData.content;

    // Evita enviar una notificación push si te envías un mensaje a ti mismo.
    if (recipientId === messageData.sender_id) {
        console.log(`Auto-mensaje detectado para ${recipientId}. No se enviará notificación PUSH.`);
        // Creamos la notificación interna para que aparezca en la bandeja de entrada, pero no enviamos el PUSH.
        await createInAppNotification(recipientId, messageData.gym_id, `Nuevo mensaje de ${senderName}`, messageContent, "NEW_PERSONAL_MESSAGE");
        return null;
    }

    // Crea un texto genérico si el mensaje solo tiene un adjunto.
    if (!messageContent || messageContent.trim() === "") {
        if (messageData.attachmentType && messageData.attachmentType.includes("image")) {
            messageContent = "Te ha enviado una imagen.";
        } else {
            messageContent = "Te ha enviado un archivo adjunto.";
        }
    }
    
    // Creamos la notificación in-app para la bandeja de entrada
    await createInAppNotification(recipientId, messageData.gym_id, `Nuevo mensaje de ${senderName}`, messageContent, "NEW_PERSONAL_MESSAGE");

    console.log(`Preparando notificación PUSH de mensaje para ${recipientId} de ${senderName}`);

    const userDoc = await db.collection("users").doc(recipientId).get();
    if (!userDoc.exists) {
        console.log(`Destinatario ${recipientId} no encontrado.`);
        return null;
    }
    
    const fcmTokens = userDoc.data().fcmTokens;
    if (fcmTokens && Array.isArray(fcmTokens) && fcmTokens.length > 0) {
        const payload = {
            notification: {
                title: `Nuevo mensaje de ${senderName}`,
                body: messageContent,
                sound: "default",
                badge: "1"
            },
            data: { 
                type: "NEW_PERSONAL_MESSAGE",
                senderId: messageData.sender_id
            }
        };
        console.log("Enviando payload de nuevo mensaje a FCM:", payload);
        return admin.messaging().sendToDevice(fcmTokens, payload);
    }
    console.log(`Usuario ${recipientId} no tiene tokens FCM para notificar.`);
    return null;
});

// --- FIN DE LA NUEVA FUNCIÓN AÑADIDA ---


/**
 * Se activa cuando se crea o actualiza un documento de usuario.
 * Lee el campo 'role' y asigna un "custom claim" de autenticación 'isAdmin'.
 * Este claim se usa en las reglas de seguridad de Storage para dar permisos de admin.
 */
exports.setUserAdminClaim = onDocumentWritten({
    document: "users/{userId}",
    region: "southamerica-east1"
}, async (event) => {
    // ... esta función está bien y no necesita cambios ...
    const userData = event.data.after.data();
    const userId = event.params.userId;
    if (!userData) {
        console.log(`Usuario ${userId} borrado, no se actualizan claims.`);
        return null;
    }
    const role = userData.role;
    const isAdmin = role === "owner" || role === "coach";
    try {
        const user = await admin.auth().getUser(userId);
        const currentClaims = user.customClaims;
        if (currentClaims && currentClaims.isAdmin === isAdmin) {
            console.log(`Claim 'isAdmin' para ${userId} ya está correcto (${isAdmin}). Sin cambios.`);
            return null;
        }
        console.log(`Asignando claim 'isAdmin: ${isAdmin}' al usuario ${userId}`);
        return admin.auth().setCustomUserClaims(userId, { isAdmin: isAdmin });
    } catch (error) {
        console.error(`Error al obtener usuario o asignar claims para ${userId}:`, error);
        return null;
    }
});