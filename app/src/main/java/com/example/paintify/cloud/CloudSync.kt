package com.example.paintify.cloud

import android.util.Log
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.example.paintify.cloud.CloudDrawing
import kotlinx.coroutines.tasks.await
import java.io.File

object CloudSync {

    private const val COLLECTION_NAME = "user_drawings"

    /**
     * Uploads the PNG file at [filePath] to Firebase Storage under
     * drawings/{userId}/{timestamp}.png and saves a Firestore metadata doc.
     */
    suspend fun uploadDrawingFileAndSaveMetadata(
        userId: String,
        title: String,
        filePath: String
    ): CloudDrawing {
        val file = File(filePath)
        require(file.exists()) { "Drawing file does not exist: $filePath" }

        val storage = Firebase.storage
        val db = Firebase.firestore

        val timestamp = System.currentTimeMillis()
        val fileName = "$timestamp.png"

        val bytes = file.readBytes()

        Log.d("CloudSync", "Uploading file ${filePath}")
        Log.d("CloudSync", "Storage bucket: ${storage.reference.bucket}")
        Log.d("CloudSync", "Full path: drawings/$userId/$fileName")

        val storageRef = storage.reference
            .child("drawings")
            .child(userId)
            .child(fileName)

        storageRef.putBytes(bytes).await()

        val downloadUrl = storageRef.downloadUrl.await().toString()

        val docRef = db.collection(COLLECTION_NAME).document()

        val cloudDrawing = CloudDrawing(
            id = docRef.id,
            userId = userId,
            imageUrl = downloadUrl,
            timestamp = timestamp,
            title = title
        )

        docRef.set(
            mapOf(
                "userId" to cloudDrawing.userId,
                "imageUrl" to cloudDrawing.imageUrl,
                "timestamp" to cloudDrawing.timestamp,
                "title" to cloudDrawing.title
            )
        ).await()

        return cloudDrawing
    }

    /**
     * Load all cloud drawings for a given userId, ordered by timestamp.
     */
    suspend fun loadDrawingsForUser(userId: String): List<CloudDrawing> {
        val db = Firebase.firestore

        val snapshot = db.collection(COLLECTION_NAME)
            .whereEqualTo("userId", userId)
            .get()
            .await()

        return snapshot.documents.map { doc ->
            CloudDrawing(
                id = doc.id,
                userId = doc.getString("userId") ?: "",
                imageUrl = doc.getString("imageUrl") ?: "",
                timestamp = doc.getLong("timestamp") ?: 0L,
                title = doc.getString("title") ?: ""
            )
        }.sortedByDescending { it.timestamp }
    }
}
