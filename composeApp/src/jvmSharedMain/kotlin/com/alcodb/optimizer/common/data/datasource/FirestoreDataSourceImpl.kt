package com.alcodb.optimizer.common.data.datasource

import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.FirestoreOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong

class FirestoreDataSourceImpl : FirestoreDataSource {
    private var db: com.google.cloud.firestore.Firestore? = null
    private var initialized = false
    private val readCount = AtomicLong(0)

    override fun initialize(firebaseConfigPath: String) {
        if (initialized) return

        try {
            val serviceAccount = FileInputStream(firebaseConfigPath)

            val options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build()

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options)
            }

            val firestoreOptions = FirestoreOptions.newBuilder()
                .setProjectId(FirebaseApp.getInstance().options.projectId)
                .setCredentials(GoogleCredentials.fromStream(FileInputStream(firebaseConfigPath)))
                .build()

            db = firestoreOptions.service
            initialized = true
            readCount.set(0)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to initialize Firebase: ${e.message}", e)
        }
    }

    override fun isInitialized(): Boolean = initialized

    override fun getReadCount(): Long = readCount.get()

    override fun resetReadCount() {
        readCount.set(0)
    }

    override suspend fun listCollections(): List<String> = withContext(Dispatchers.IO) {
        val firestore = db ?: throw IllegalStateException("Firestore not initialized")
        firestore.listCollections().map { it.id }
    }

    override suspend fun listDocuments(collection: String): List<String> = withContext(Dispatchers.IO) {
        val firestore = db ?: throw IllegalStateException("Firestore not initialized")
        firestore.collection(collection).listDocuments().map { it.id }
    }

    override suspend fun getDocument(collection: String, document: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        val firestore = db ?: throw IllegalStateException("Firestore not initialized")
        try {
            val snapshot = firestore.collection(collection).document(document)
                .get().get(30, TimeUnit.SECONDS)
            readCount.incrementAndGet()
            if (snapshot.exists()) snapshot.data else null
        } catch (e: TimeoutException) {
            null
        }
    }

    override suspend fun deleteDocument(collection: String, document: String) = withContext(Dispatchers.IO) {
        val firestore = db ?: throw IllegalStateException("Firestore not initialized")
        try {
            firestore.collection(collection).document(document)
                .delete().get(30, TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
        }
        Unit
    }

    override suspend fun deleteCollectionRecursive(collection: String) = withContext(Dispatchers.IO) {
        val firestore = db ?: throw IllegalStateException("Firestore not initialized")
        deleteCollectionRecursiveInternal(firestore, firestore.collection(collection))
    }

    override suspend fun setDocumentMerge(collection: String, document: String, data: Map<String, Any?>) = withContext(Dispatchers.IO) {
        val firestore = db ?: throw IllegalStateException("Firestore not initialized")
        firestore.collection(collection).document(document)
            .set(data, com.google.cloud.firestore.SetOptions.merge())
            .get(30, TimeUnit.SECONDS)
        Unit
    }

    override suspend fun deleteAuthUser(uid: String) = withContext(Dispatchers.IO) {
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            auth.deleteUser(uid)
            println("[FirebaseAuth] Deleted auth user: $uid")
        } catch (e: Exception) {
            if (e.message?.contains("USER_NOT_FOUND") == true) {
                println("[FirebaseAuth] Auth user $uid already deleted (orphaned collection)")
            } else {
                println("[FirebaseAuth] Failed to delete auth user $uid: ${e.message}")
            }
        }
    }

    override suspend fun deleteAuthUserByEmail(email: String) = withContext(Dispatchers.IO) {
        try {
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val userRecord = auth.getUserByEmail(email)
            auth.deleteUser(userRecord.uid)
            println("[FirebaseAuth] Deleted auth user: $email (uid=${userRecord.uid})")
        } catch (e: Exception) {
            if (e.message?.contains("USER_NOT_FOUND") == true) {
                println("[FirebaseAuth] Auth user $email already deleted (orphaned collection)")
            } else {
                println("[FirebaseAuth] Failed to delete auth user $email: ${e.message}")
            }
        }
    }

    override suspend fun getRegistryEmails(): List<String> = withContext(Dispatchers.IO) {
        val firestore = db ?: throw IllegalStateException("Firestore not initialized")
        try {
            val snapshot = firestore.collection(COLLECTION_USERS).document(DOCUMENT_USERS)
                .get().get(30, TimeUnit.SECONDS)
            readCount.incrementAndGet()
            if (snapshot.exists()) {
                snapshot.data?.keys?.toList() ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: TimeoutException) {
            println("[Registry] Timeout reading registry emails")
            emptyList()
        }
    }

    override suspend fun getRegistryUserData(email: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        val firestore = db ?: throw IllegalStateException("Firestore not initialized")
        try {
            val snapshot = firestore.collection(COLLECTION_USERS).document(DOCUMENT_USERS)
                .get().get(30, TimeUnit.SECONDS)
            readCount.incrementAndGet()
            if (snapshot.exists()) {
                @Suppress("UNCHECKED_CAST")
                snapshot.data?.get(email) as? Map<String, Any>
            } else {
                null
            }
        } catch (e: TimeoutException) {
            println("[Registry] Timeout reading registry data for $email")
            null
        }
    }

    override suspend fun setRegistryUserData(email: String, data: Map<String, Any?>) = withContext(Dispatchers.IO) {
        val firestore = db ?: throw IllegalStateException("Firestore not initialized")
        try {
            firestore.collection(COLLECTION_USERS).document(DOCUMENT_USERS)
                .set(mapOf(email to data), com.google.cloud.firestore.SetOptions.merge())
                .get(30, TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
        }
        Unit
    }

    override suspend fun deleteRegistryEmail(email: String) = withContext(Dispatchers.IO) {
        val firestore = db ?: throw IllegalStateException("Firestore not initialized")
        try {
            val docRef = firestore.collection(COLLECTION_USERS).document(DOCUMENT_USERS)
            docRef.set(
                mapOf(email to com.google.cloud.firestore.FieldValue.delete()),
                com.google.cloud.firestore.SetOptions.merge(),
            ).get(30, TimeUnit.SECONDS)
        } catch (_: TimeoutException) {
        }
        Unit
    }

    companion object {
        private const val COLLECTION_USERS = "Collection_of_all_users"
        private const val DOCUMENT_USERS = "Users"
    }

    private fun deleteCollectionRecursiveInternal(
        firestore: com.google.cloud.firestore.Firestore,
        collectionRef: com.google.cloud.firestore.CollectionReference,
    ) {
        var batch = firestore.batch()
        var batchCount = 0

        for (doc in collectionRef.listDocuments()) {
            val subcollections = doc.listCollections()
            for (subcollection in subcollections) {
                deleteCollectionRecursiveInternal(firestore, subcollection)
            }

            batch.delete(doc)
            batchCount++

            if (batchCount % 500 == 0) {
                batch.commit()
                batch = firestore.batch()
            }
        }

        if (batchCount % 500 != 0) {
            batch.commit()
        }
    }
}
