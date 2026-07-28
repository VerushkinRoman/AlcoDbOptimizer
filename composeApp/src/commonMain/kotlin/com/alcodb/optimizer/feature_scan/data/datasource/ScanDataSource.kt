package com.alcodb.optimizer.feature_scan.data.datasource

import com.alcodb.optimizer.common.data.models.CalendarData

interface ScanDataSource {
    suspend fun getAllCollections(): List<String>
    suspend fun getUserDates(userId: String): CalendarData
    suspend fun deleteUserCollection(userId: String)
    suspend fun removeUserFromGlobalRegistry(userEmail: String)
    suspend fun deleteAuthUser(userId: String)
    suspend fun deleteAuthUserByEmail(email: String)
    suspend fun restoreUserDates(userId: String, dates: Map<String, Any?>)
    suspend fun restoreUserRegistryData(email: String, data: Map<String, Any?>)
    suspend fun getRegistryUserIds(): List<String>
    suspend fun getRegistryUserData(email: String): Map<String, Any>?
    fun getReadCount(): Long
}
