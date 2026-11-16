package com.alcodb.optimizer.feature_scan.data.datasource

import com.alcodb.optimizer.common.data.datasource.FirestoreDataSource
import com.alcodb.optimizer.common.data.datasource.ServiceCollections
import com.alcodb.optimizer.common.data.models.CalendarData
import com.alcodb.optimizer.common.data.models.DataDayData
import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.EmptySuccessResult
import com.alcodb.optimizer.common.domain.utils.Result

class ScanDataSourceImpl(
    private val firestoreDataSource: FirestoreDataSource,
) : ScanDataSource {

    override suspend fun getAllCollections(): Result<List<String>, Error> =
        firestoreDataSource.listCollections()

    override suspend fun getUserDates(userId: String): Result<CalendarData, Error> =
        when (val document = firestoreDataSource.getDocument(userId, DOCUMENT_DATES)) {
            is Result.Error -> document
            is Result.Success -> Result.Success(document.data.toCalendarData())
        }

    override suspend fun deleteUserCollection(userId: String): EmptyResult<Error> {
        if (ServiceCollections.isService(userId)) {
            println("[Scan] Refusing to delete service collection: $userId")
            return EmptySuccessResult
        }
        return firestoreDataSource.deleteCollectionRecursive(userId)
    }

    override suspend fun removeUserFromGlobalRegistry(userEmail: String): EmptyResult<Error> =
        firestoreDataSource.deleteRegistryEmail(userEmail)

    override suspend fun deleteAuthUser(userId: String): EmptyResult<Error> =
        firestoreDataSource.deleteAuthUser(userId)

    override suspend fun deleteAuthUserByEmail(email: String): EmptyResult<Error> =
        firestoreDataSource.deleteAuthUserByEmail(email)

    override suspend fun restoreUserDates(
        userId: String,
        dates: Map<String, Any?>,
    ): EmptyResult<Error> = firestoreDataSource.setDocumentMerge(userId, DOCUMENT_DATES, dates)

    override suspend fun restoreUserRegistryData(
        email: String,
        data: Map<String, Any?>,
    ): EmptyResult<Error> = firestoreDataSource.setRegistryUserData(email, data)

    override suspend fun getRegistryUserIds(): Result<List<String>, Error> =
        firestoreDataSource.getRegistryEmails()

    override suspend fun getRegistryUserData(email: String): Result<Map<String, Any>?, Error> =
        firestoreDataSource.getRegistryUserData(email)

    override suspend fun listAllAuthUsers(): Result<List<String>, Error> =
        firestoreDataSource.listAllAuthUsers()

    override fun getReadCount(): Long = firestoreDataSource.getReadCount()

    private fun Map<String, Any>?.toCalendarData(): CalendarData {
        if (this == null) return emptyMap()
        val result = mutableMapOf<String, DataDayData>()
        for ((key, value) in this) {
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
                    val drinkType = value["drinkType"] as? String
                    result[key] = DataDayData(date = date, drinkType = drinkType)
                } catch (_: Exception) {
                }
            }
        }
        return result
    }

    companion object {
        private const val DOCUMENT_DATES = "Dates"
    }
}
