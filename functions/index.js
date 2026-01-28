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
                priority: "high",
                channelId: "crosschapp_default_channel"
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
        message: message, // <-- FIX: iOS espera 'message', Android 'body' (o ambos)
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

    // ... (código existente)

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
        return null; // Return null on error to match function signature
    }
});

// ========================================================================
// NUEVA FUNCIÓN: REGISTRO UNIFICADO DE ASISTENCIA (QR)
// ========================================================================
const { onCall, HttpsError } = require("firebase-functions/v2/https");

exports.registerAttendance = onCall({ region: "southamerica-east1" }, async (request) => {
    // 1. Validar Autenticación
    if (!request.auth) {
        throw new HttpsError('unauthenticated', 'Debes iniciar sesión para dar el presente.');
    }

    const { classId, scannedCode } = request.data;
    const userId = request.auth.uid;

    if (!classId || !scannedCode) {
        throw new HttpsError('invalid-argument', 'Faltan datos (classId o código QR).');
    }

    // 2. Obtener Usuario y Clase
    const userDocRef = db.collection("users").doc(userId);
    const classDocRef = db.collection("gymClasses").doc(classId);

    const [userSnap, classSnap] = await Promise.all([
        userDocRef.get(),
        classDocRef.get()
    ]);

    if (!userSnap.exists) throw new HttpsError('not-found', 'Usuario no encontrado.');
    if (!classSnap.exists) throw new HttpsError('not-found', 'Clase no encontrada.');

    const userData = userSnap.data();
    const classData = classSnap.data();

    // 3. Validar Gimnasio (Código QR)
    // El 'scannedCode' debe ser igual al gym_id del usuario (o alguna lógica de validación QR que uses)
    if (scannedCode !== userData.gym_id) {
        throw new HttpsError('permission-denied', 'Este código QR no pertenece a tu gimnasio.');
    }

    // Validación adicional: La clase debe ser del mismo gimnasio que el usuario
    if (classData.gym_id !== userData.gym_id) {
        throw new HttpsError('permission-denied', 'Esta clase no pertenece a tu gimnasio.');
    }

    // 4. Validar Inscripción
    const enrolledUserIds = classData.enrolledUserIds || [];
    if (!enrolledUserIds.includes(userId)) {
        throw new HttpsError('failed-precondition', 'No estás inscrito en esta clase.');
    }

    // 5. Validar Horario (+/- 30 minutos)
    const now = new Date();
    const classDate = classData.dateTime.toDate(); // Firestore Timestamp a JS Date
    const diffMinutes = (now - classDate) / 1000 / 60;

    // Permitir check-in desde 30 min antes hasta 60 min después (para no ser tan estrictos si llegan tarde)
    if (diffMinutes < -30) {
        throw new HttpsError('failed-precondition', 'Es muy temprano para dar el presente (máx 30 min antes).');
    }
    if (diffMinutes > 60) {
        throw new HttpsError('failed-precondition', 'La clase ya ha terminado o pasado excesivamente.');
    }

    // 6. Verificar si YA dio presente (Idempotencia)
    const attendedUserIds = classData.attendedUserIds || [];
    // También chequeamos checkedInUserIds por compatibilidad con el sistema viejo
    const checkedInUserIds = classData.checkedInUserIds || [];

    if (attendedUserIds.includes(userId) || checkedInUserIds.includes(userId)) {
        return { success: true, message: "¡Ya habías dado el presente!", xpGained: 0 };
    }

    // 7. EJECUTAR TRANSACCIÓN ATÓMICA
    let xpGained = 10; // Base XP por asistencia
    let newLevel = userData.level || "Novato";

    try {
        await db.runTransaction(async (transaction) => {
            // Re-leer para consistencia dentro de TX
            const tUserSnap = await transaction.get(userDocRef);
            const tClassSnap = await transaction.get(classDocRef);

            // Lógica de Gamificación (Simplificada en Backend por ahora)
            // A futuro: Mover lógica compleja de 'GamificationService' aquí.
            const currentXp = tUserSnap.data().xp || 0;
            const newTotalXp = currentXp + xpGained;

            // Calcular nivel (Hardcoded simple logic por ahora, idealmente traer de config)
            // Esto es solo un ejemplo, deberías duplicar tu lógica de niveles aquí o guardarla en DB
            // newLevel = calculateLevel(newTotalXp); 

            // A. Registrar en Historial
            const historyRef = db.collection("attendance_history").doc();
            transaction.set(historyRef, {
                userId: userId,
                gym_id: userData.gym_id,
                classId: classId,
                classDate: classData.dateTime,
                status: "PRESENT",
                checkInTime: admin.firestore.FieldValue.serverTimestamp(),
                checkInMethod: "QR_APP"
            });

            // B. Actualizar Clase (Lista de presentes oficial)
            transaction.update(classDocRef, {
                attendedUserIds: admin.firestore.FieldValue.arrayUnion(userId),
                checkedInUserIds: admin.firestore.FieldValue.arrayUnion(userId) // Mantenemos ambos por legacy
            });

            // C. Actualizar Usuario (Contador + XP)
            transaction.update(userDocRef, {
                totalClassesAttended: admin.firestore.FieldValue.increment(1),
                xp: admin.firestore.FieldValue.increment(xpGained)
                // level: newLevel // Descomentar si implementamos calculo de nivel en Backend
            });

            // D. Registrar Log de XP (Historial Visual)
            // Lógica de mensaje motivacional
            // Si la clase aún no empezó (diffMinutes < 0), es "A darlo todo".
            // Si ya empezó o terminó, asumimos que ya entrenó o está en eso: "Entrenaste duro".
            let logDescription = "¡Entrenaste duro!";
            if (diffMinutes < 0) {
                logDescription = "¡A darlo todo!";
            }

            const xpLogRef = db.collection("xp_logs").doc();
            transaction.set(xpLogRef, {
                userId: userId,
                gym_id: userData.gym_id,
                amount: xpGained,
                type: "ATTENDANCE",
                title: "Asistencia de Clase",
                description: logDescription,
                icon: "💪",
                timestamp: admin.firestore.FieldValue.serverTimestamp(),
                relatedId: classId
            });
        });

        console.log(`Asistencia registrada exitosamente para ${userId} en clase ${classId}`);
        return {
            success: true,
            message: `¡Presente registrado! (+${xpGained} XP)`,
            xpGained: xpGained
        };

    } catch (error) {
        console.error("Error en transacción de asistencia:", error);
        throw new HttpsError('internal', 'Error al procesar asistencia: ' + error.message);
    }
});