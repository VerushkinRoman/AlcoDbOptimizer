package com.alcodb.optimizer.common.data.datasource

import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result

interface FirestoreDocumentDataSource {
    suspend fun listCollections(): Result<List<String>, Error>
    suspend fun listDocuments(collection: String): Result<List<String>, Error>
    suspend fun getDocument(collection: String, document: String): Result<Map<String, Any>?, Error>
    suspend fun deleteDocument(collection: String, document: String): EmptyResult<Error>
    suspend fun deleteCollectionRecursive(collection: String): EmptyResult<Error>
    suspend fun setDocumentMerge(
        collection: String,
        document: String,
        data: Map<String, Any?>,
    ): EmptyResult<Error>
    fun initialize(firebaseConfigPath: String): EmptyResult<Error>
    fun isInitialized(): Boolean
    fun getReadCount(): Long
    fun resetReadCount()
}
