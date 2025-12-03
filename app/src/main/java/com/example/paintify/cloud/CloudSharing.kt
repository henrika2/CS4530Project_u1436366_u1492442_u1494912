package com.example.paintify.cloud

import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

data class SharedDrawing(
    val id: String = "",
    val imageUrl: String = "",
    val senderId: String = "",
    val receiverEmail: String = "",
    val timestamp: Long = 0L,
    val title: String = ""
)

object CloudSharing {

    private const val COLLECTION_NAME = "shared_drawings"

    /**
     * Share a drawing with another user by email.
     * - Uploads (or re-uploads) the file to Storage via CloudSync
     * - Creates a doc in shared_drawings with imageUrl + sender/receiver info
     */
    suspend fun shareDrawingWithUser(
        senderId: String,
        receiverEmail: String,
        localFilePath: String,
        title: String
    ): SharedDrawing {
        // Reuse your existing upload code: get a public imageUrl
        val cloud = CloudSync.uploadDrawingFileAndSaveMetadata(
            userId = senderId,
            title = title,
            filePath = localFilePath
        )

        val db = Firebase.firestore
        val docRef = db.collection(COLLECTION_NAME).document()
        val timestamp = System.currentTimeMillis()

        val shared = SharedDrawing(
            id = docRef.id,
            imageUrl = cloud.imageUrl,
            senderId = senderId,
            receiverEmail = receiverEmail,
            timestamp = timestamp,
            title = title
        )

        docRef.set(
            mapOf(
                "imageUrl" to shared.imageUrl,
                "senderId" to shared.senderId,
                "receiverEmail" to shared.receiverEmail,
                "timestamp" to shared.timestamp,
                "title" to shared.title
            )
        ).await()

        return shared
    }

    /**
     * Load drawings shared *to* this user (by email).
     */
    suspend fun loadSharedToUser(email: String): List<SharedDrawing> {
        val db = Firebase.firestore
        val snapshot = db.collection(COLLECTION_NAME)
            .whereEqualTo("receiverEmail", email)
            .get()
            .await()

        return snapshot.documents.map { doc ->
            SharedDrawing(
                id = doc.id,
                imageUrl = doc.getString("imageUrl") ?: "",
                senderId = doc.getString("senderId") ?: "",
                receiverEmail = doc.getString("receiverEmail") ?: "",
                timestamp = doc.getLong("timestamp") ?: 0L,
                title = doc.getString("title") ?: ""
            )
        }
    }

    /**
     * Unshare a drawing: delete all shared_drawings docs for this sender + imageUrl.
     * (You could refine this to use the doc id if you store it per card.)
     */
    suspend fun unshareDrawing(
        senderId: String,
        imageUrl: String
    ) {
        val db = Firebase.firestore
        val snapshot = db.collection(COLLECTION_NAME)
            .whereEqualTo("senderId", senderId)
            .whereEqualTo("imageUrl", imageUrl)
            .get()
            .await()

        snapshot.documents.forEach { doc ->
            doc.reference.delete().await()
        }
    }
}
