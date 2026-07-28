package com.alcodb.optimizer.common.data.datasource

interface FirestoreDataSource {
    suspend fun listCollections(): List<String>
    suspend fun listDocuments(collection: String): List<String>
    suspend fun getDocument(collection: String, document: String): Map<String, Any>?
    suspend fun deleteDocument(collection: String, document: String)
    suspend fun deleteCollectionRecursive(collection: String)
    suspend fun setDocumentMerge(collection: String, document: String, data: Map<String, Any?>)
    suspend fun deleteAuthUser(uid: String)
    suspend fun deleteAuthUserByEmail(email: String)
    suspend fun getRegistryEmails(): List<String>
    suspend fun getRegistryUserData(email: String): Map<String, Any>?
    suspend fun setRegistryUserData(email: String, data: Map<String, Any?>)
    suspend fun deleteRegistryEmail(email: String)
    fun initialize(firebaseConfigPath: String)
    fun isInitialized(): Boolean
    fun getReadCount(): Long
    fun resetReadCount()
}
