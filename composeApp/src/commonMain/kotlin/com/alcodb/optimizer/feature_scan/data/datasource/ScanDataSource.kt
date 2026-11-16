package com.alcodb.optimizer.feature_scan.data.datasource

import com.alcodb.optimizer.common.data.models.CalendarData
import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result

interface ScanDataSource {
    suspend fun getAllCollections(): Result<List<String>, Error>
    suspend fun getUserDates(userId: String): Result<CalendarData, Error>
    suspend fun deleteUserCollection(userId: String): EmptyResult<Error>
    suspend fun removeUserFromGlobalRegistry(userEmail: String): EmptyResult<Error>
    suspend fun deleteAuthUser(userId: String): EmptyResult<Error>
    suspend fun deleteAuthUserByEmail(email: String): EmptyResult<Error>
    suspend fun restoreUserDates(userId: String, dates: Map<String, Any?>): EmptyResult<Error>
    suspend fun restoreUserRegistryData(email: String, data: Map<String, Any?>): EmptyResult<Error>
    suspend fun getRegistryUserIds(): Result<List<String>, Error>
    suspend fun getRegistryUserData(email: String): Result<Map<String, Any>?, Error>
    suspend fun listAllAuthUsers(): Result<List<String>, Error>
    fun getReadCount(): Long
}
