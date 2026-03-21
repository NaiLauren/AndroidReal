package com.aquiles.crosschapp.data.service

import android.net.Uri
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.*

/**
 * Service dedicated to video uploads to Cloud Storage.
 * Handles proof-of-work videos for challenges.
 */
object VideoUploadService {
    private val storage = FirebaseStorage.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    sealed class UploadResult {
        data class Success(val videoUrl: String) : UploadResult()
        data class Error(val exception: Exception) : UploadResult()
        object Loading : UploadResult()
    }

    /**
     * Upload a video to Cloud Storage for challenge proof.
     * Returns a publicly accessible URL.
     */
    suspend fun uploadChallengeVideo(
        videoUri: Uri,
        challengeResultId: String,
        userId: String
    ): UploadResult {
        return try {
            val timestamp = System.currentTimeMillis()
            val filename = "challenges/$userId/$challengeResultId/${timestamp}.mp4"
            val storageRef = storage.reference.child(filename)

            // Upload the video
            storageRef.putFile(videoUri).await()

            // Get the download URL
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Update the challenge result with the video URL
            firestore.collection("challenge_results")
                .document(challengeResultId)
                .update("videoUrl", downloadUrl)
                .await()

            UploadResult.Success(downloadUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            UploadResult.Error(e)
        }
    }

    /**
     * Validate video file before upload.
     * Returns (isValid, errorMessage)
     */
    fun validateVideoFile(fileSize: Long, fileName: String): Pair<Boolean, String?> {
        val maxSizeBytes = 500 * 1024 * 1024 // 500 MB
        
        return when {
            fileSize <= 0 -> Pair(false, "El archivo está vacío")
            fileSize > maxSizeBytes -> Pair(false, "El video es muy grande (máximo 500 MB)")
            !fileName.endsWith(".mp4") && !fileName.endsWith(".mov") && 
            !fileName.endsWith(".avi") && !fileName.endsWith(".mkv") -> 
                Pair(false, "Formato no soportado. Usa .mp4, .mov, .avi o .mkv")
            else -> Pair(true, null)
        }
    }

    /**
     * Delete a video from Cloud Storage.
     */
    suspend fun deleteVideo(videoUrl: String): Boolean {
        return try {
            val fileRef = storage.getReferenceFromUrl(videoUrl)
            fileRef.delete().await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get video info (size, metadata)
     */
    suspend fun getVideoMetadata(videoUrl: String): Map<String, Any>? {
        return try {
            val fileRef = storage.getReferenceFromUrl(videoUrl)
            val metadata = fileRef.metadata.await()
            mapOf(
                "size" to (metadata.sizeBytes ?: 0),
                "created" to (metadata.creationTimeMillis ?: 0),
                "updated" to (metadata.updatedTimeMillis ?: 0),
                "contentType" to (metadata.contentType ?: "video/mp4")
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
