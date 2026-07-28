package com.alcodb.optimizer.feature_scan.data.datasource

import com.alcodb.optimizer.common.data.datasource.FirestoreDataSource
import com.alcodb.optimizer.common.data.models.CalendarData
import com.alcodb.optimizer.common.data.models.DataDayData

class ScanDataSourceImpl(
    private val firestoreDataSource: FirestoreDataSource,
) : ScanDataSource {

    override suspend fun getAllCollections(): List<String> {
        return firestoreDataSource.listCollections()
    }

    override suspend fun getUserDates(userId: String): CalendarData {
        val doc = firestoreDataSource.getDocument(userId, DOCUMENT_DATES)
            ?: return emptyMap()

        val result = mutableMapOf<String, DataDayData>()
        for ((key, value) in doc) {
            if (value is Map<*, *>) {
                try {
                    @Suppress("UNCHECKED_CAST")
                    val rawDate = (value as Map<String, Any?>)["date"]
                    val date = when (rawDate) {
                        is Long -> rawDate
                        is Int -> rawDate.toLong()
                        is Number -> rawDate.toLong()
                        else -> 0L
                    }
                    @Suppress("UNCHECKED_CAST")
                    val drinkType = (value as Map<String, Any?>)["drinkType"] as? String
                    result[key] = DataDayData(date = date, drinkType = drinkType)
                } catch (_: Exception) {
                }
            }
        }
        return result
    }

    override suspend fun deleteUserCollection(userId: String) {
        firestoreDataSource.deleteCollectionRecursive(userId)
    }

    override suspend fun removeUserFromGlobalRegistry(userEmail: String) {
        firestoreDataSource.deleteRegistryEmail(userEmail)
    }

    override suspend fun deleteAuthUser(userId: String) {
        firestoreDataSource.deleteAuthUser(userId)
    }

    override suspend fun deleteAuthUserByEmail(email: String) {
        firestoreDataSource.deleteAuthUserByEmail(email)
    }

    override suspend fun restoreUserDates(userId: String, dates: Map<String, Any?>) {
        firestoreDataSource.setDocumentMerge(userId, DOCUMENT_DATES, dates)
    }

    override suspend fun restoreUserRegistryData(email: String, data: Map<String, Any?>) {
        firestoreDataSource.setRegistryUserData(email, data)
    }

    override suspend fun getRegistryUserIds(): List<String> {
        return firestoreDataSource.getRegistryEmails()
    }

    override suspend fun getRegistryUserData(email: String): Map<String, Any>? {
        return firestoreDataSource.getRegistryUserData(email)
    }

    override fun getReadCount(): Long = firestoreDataSource.getReadCount()

    companion object {
        private const val DOCUMENT_DATES = "Dates"
    }
}
