package com.alcodb.optimizer.common.data.datasource

import com.alcodb.optimizer.common.domain.utils.DataError
import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.EmptySuccessResult
import com.alcodb.optimizer.common.domain.utils.Result
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.firestore.CollectionReference
import com.google.cloud.firestore.FieldValue
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.FirestoreOptions
import com.google.cloud.firestore.SetOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicLong

class FirestoreDataSourceImpl : FirestoreDataSource {
    private var db: Firestore? = null
    private var initialized = false
    private val readCount = AtomicLong(0)

    override fun initialize(firebaseConfigPath: String): EmptyResult<Error> {
        if (initialized) return EmptySuccessResult

        return try {
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
            EmptySuccessResult
        } catch (e: Exception) {
            println("[Firestore] Failed to initialize Firebase: ${e.message}")
            Result.Error(DataError.InitializationError)
        }
    }

    override fun isInitialized(): Boolean = initialized

    override fun getReadCount(): Long = readCount.get()

    override fun resetReadCount() {
        readCount.set(0)
    }

    override suspend fun listCollections(): Result<List<String>, Error> =
        withContext(Dispatchers.IO) {
            val firestore = db ?: return@withContext Result.Error(DataError.NotInitialized)
            try {
                Result.Success(firestore.listCollections().map { it.id })
            } catch (_: Exception) {
                Result.Error(DataError.ReadError)
            }
        }

    override suspend fun listDocuments(collection: String): Result<List<String>, Error> =
        withContext(Dispatchers.IO) {
            val firestore = db ?: return@withContext Result.Error(DataError.NotInitialized)
            try {
                Result.Success(firestore.collection(collection).listDocuments().map { it.id })
            } catch (_: Exception) {
                Result.Error(DataError.ReadError)
            }
        }

    override suspend fun getDocument(
        collection: String,
        document: String,
    ): Result<Map<String, Any>?, Error> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext Result.Error(DataError.NotInitialized)
        try {
            val snapshot = firestore.collection(collection).document(document)
                .get().get(30, TimeUnit.SECONDS)
            readCount.incrementAndGet()
            Result.Success(if (snapshot.exists()) snapshot.data else null)
        } catch (_: TimeoutException) {
            Result.Success(null)
        } catch (_: Exception) {
            Result.Error(DataError.ReadError)
        }
    }

    override suspend fun deleteDocument(
        collection: String,
        document: String,
    ): EmptyResult<Error> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext Result.Error(DataError.NotInitialized)
        try {
            firestore.collection(collection).document(document)
                .delete().get(30, TimeUnit.SECONDS)
            EmptySuccessResult
        } catch (_: TimeoutException) {
            EmptySuccessResult
        } catch (_: Exception) {
            Result.Error(DataError.DeleteError)
        }
    }

    override suspend fun deleteCollectionRecursive(collection: String): EmptyResult<Error> =
        withContext(Dispatchers.IO) {
            if (ServiceCollections.isService(collection)) {
                println("[Firestore] Refusing to delete service collection: $collection")
                return@withContext EmptySuccessResult
            }
            val firestore = db ?: return@withContext Result.Error(DataError.NotInitialized)
            try {
                deleteCollectionRecursiveInternal(firestore, firestore.collection(collection))
                EmptySuccessResult
            } catch (_: Exception) {
                Result.Error(DataError.DeleteError)
            }
        }

    override suspend fun setDocumentMerge(
        collection: String,
        document: String,
        data: Map<String, Any?>,
    ): EmptyResult<Error> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext Result.Error(DataError.NotInitialized)
        try {
            firestore.collection(collection).document(document)
                .set(data, SetOptions.merge())
                .get(30, TimeUnit.SECONDS)
            EmptySuccessResult
        } catch (_: TimeoutException) {
            EmptySuccessResult
        } catch (_: Exception) {
            Result.Error(DataError.WriteError)
        }
    }

    override suspend fun deleteAuthUser(uid: String): EmptyResult<Error> =
        withContext(Dispatchers.IO) {
            try {
                FirebaseAuth.getInstance().deleteUser(uid)
                println("[FirebaseAuth] Deleted auth user: $uid")
                EmptySuccessResult
            } catch (e: Exception) {
                if (e.message?.contains("USER_NOT_FOUND") == true) {
                    println("[FirebaseAuth] Auth user $uid already deleted (orphaned collection)")
                    EmptySuccessResult
                } else {
                    println("[FirebaseAuth] Failed to delete auth user $uid: ${e.message}")
                    Result.Error(DataError.DeleteError)
                }
            }
        }

    override suspend fun deleteAuthUserByEmail(email: String): EmptyResult<Error> =
        withContext(Dispatchers.IO) {
            try {
                val auth = FirebaseAuth.getInstance()
                val userRecord = auth.getUserByEmail(email)
                auth.deleteUser(userRecord.uid)
                println("[FirebaseAuth] Deleted auth user: $email (uid=${userRecord.uid})")
                EmptySuccessResult
            } catch (e: Exception) {
                if (e.message?.contains("USER_NOT_FOUND") == true) {
                    println("[FirebaseAuth] Auth user $email already deleted (orphaned collection)")
                    EmptySuccessResult
                } else {
                    println("[FirebaseAuth] Failed to delete auth user $email: ${e.message}")
                    Result.Error(DataError.DeleteError)
                }
            }
        }

    override suspend fun getRegistryEmails(): Result<List<String>, Error> =
        withContext(Dispatchers.IO) {
            val firestore = db ?: return@withContext Result.Error(DataError.NotInitialized)
            try {
                val snapshot = firestore.collection(COLLECTION_USERS).document(DOCUMENT_USERS)
                    .get().get(30, TimeUnit.SECONDS)
                readCount.incrementAndGet()
                Result.Success(
                    if (snapshot.exists()) snapshot.data?.keys?.toList() ?: emptyList() else emptyList(),
                )
            } catch (_: TimeoutException) {
                println("[Registry] Timeout reading registry emails")
                Result.Success(emptyList())
            } catch (_: Exception) {
                Result.Error(DataError.ReadError)
            }
        }

    override suspend fun getRegistryUserData(email: String): Result<Map<String, Any>?, Error> =
        withContext(Dispatchers.IO) {
            val firestore = db ?: return@withContext Result.Error(DataError.NotInitialized)
            try {
                val snapshot = firestore.collection(COLLECTION_USERS).document(DOCUMENT_USERS)
                    .get().get(30, TimeUnit.SECONDS)
                readCount.incrementAndGet()
                @Suppress("UNCHECKED_CAST")
                val data = if (snapshot.exists()) {
                    snapshot.data?.get(email) as? Map<String, Any>
                } else {
                    null
                }
                Result.Success(data)
            } catch (_: TimeoutException) {
                println("[Registry] Timeout reading registry data for $email")
                Result.Success(null)
            } catch (_: Exception) {
                Result.Error(DataError.ReadError)
            }
        }

    override suspend fun setRegistryUserData(
        email: String,
        data: Map<String, Any?>,
    ): EmptyResult<Error> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext Result.Error(DataError.NotInitialized)
        try {
            firestore.collection(COLLECTION_USERS).document(DOCUMENT_USERS)
                .set(mapOf(email to data), SetOptions.merge())
                .get(30, TimeUnit.SECONDS)
            EmptySuccessResult
        } catch (_: TimeoutException) {
            EmptySuccessResult
        } catch (_: Exception) {
            Result.Error(DataError.WriteError)
        }
    }

    override suspend fun deleteRegistryEmail(email: String): EmptyResult<Error> =
        withContext(Dispatchers.IO) {
            val firestore = db ?: return@withContext Result.Error(DataError.NotInitialized)
            try {
                firestore.collection(COLLECTION_USERS).document(DOCUMENT_USERS)
                    .set(mapOf(email to FieldValue.delete()), SetOptions.merge())
                    .get(30, TimeUnit.SECONDS)
                EmptySuccessResult
            } catch (_: TimeoutException) {
                EmptySuccessResult
            } catch (_: Exception) {
                Result.Error(DataError.DeleteError)
            }
        }

    override suspend fun listAllAuthUsers(): Result<List<String>, Error> =
        withContext(Dispatchers.IO) {
            try {
                val auth = FirebaseAuth.getInstance()
                val users = mutableListOf<String>()
                var page = auth.listUsers(null)
                while (true) {
                    for (user in page.values) {
                        user.email?.let(users::add)
                    }
                    if (page.hasNextPage()) {
                        page = auth.listUsers(page.nextPageToken)
                    } else {
                        break
                    }
                }
                println("[FirebaseAuth] Listed auth users: ${users.size}")
                Result.Success(users)
            } catch (e: Exception) {
                println("[FirebaseAuth] Failed to list auth users: ${e.message}")
                Result.Error(DataError.ReadError)
            }
        }

    companion object {
        private const val COLLECTION_USERS = "Collection_of_all_users"
        private const val DOCUMENT_USERS = "Users"
    }

    private fun deleteCollectionRecursiveInternal(
        firestore: Firestore,
        collectionRef: CollectionReference,
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
