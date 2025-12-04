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
    /**
     * Share an already-uploaded cloud drawing with another user by email.
     *
     * The receiver can later query Firestore for documents where
     * receiverEmail == their email to see drawings shared with them.
     */
    suspend fun shareCloudDrawingWithUser(
        senderId: String,
        receiverEmail: String,
        imageUrl: String,
        title: String
    ) {
        val db = Firebase.firestore
        val timestamp = System.currentTimeMillis()

        val data = mapOf(
            "senderId" to senderId,
            "receiverEmail" to receiverEmail,
            "imageUrl" to imageUrl,
            "title" to title,
            "timestamp" to timestamp,
            "type" to "cloud"  // optional, but handy if you also store local shares here
        )

        // Fire-and-forget-ish, but suspend until write completes
        db.collection(COLLECTION_NAME)
            .add(data)
            .await()
    }
}
