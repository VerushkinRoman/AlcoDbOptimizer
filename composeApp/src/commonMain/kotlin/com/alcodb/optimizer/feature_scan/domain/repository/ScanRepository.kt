package com.alcodb.optimizer.feature_scan.domain.repository

import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.domain.models.BackupEntry
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import com.alcodb.optimizer.feature_scan.domain.models.OrphanedRegistryUser
import kotlinx.coroutines.flow.Flow

interface ScanRepository {
    fun getInactiveUsers(): Flow<Result<List<InactiveUser>, Error>>
    fun getScanProgress(): Flow<ScanProgressState>
    fun getDeletionProgress(): Flow<Pair<Int, Int>>
    suspend fun startScan(maxReads: Int = 1000): EmptyResult<Error>
    suspend fun stopScan(): EmptyResult<Error>
    suspend fun deleteAllInactiveUsers(): EmptyResult<Error>
    suspend fun deleteUser(userId: String, isAnonymous: Boolean): EmptyResult<Error>
    suspend fun refreshInactiveUsers(): EmptyResult<Error>
    suspend fun resetCounter(): EmptyResult<Error>
    suspend fun resetAll(): EmptyResult<Error>
    suspend fun getBackupEntries(): List<BackupEntry>
    suspend fun restoreUser(entry: BackupEntry): EmptyResult<Error>
    suspend fun findOrphanedRegistryUsers(): Result<Pair<Int, List<OrphanedRegistryUser>>, Error>
    suspend fun removeRegistryUser(email: String): EmptyResult<Error>
    suspend fun removeAllOrphanedRegistryUsers(users: List<OrphanedRegistryUser>): EmptyResult<Error>
}

data class ScanProgressState(
    val isScanning: Boolean = false,
    val collectionsTotal: Int = 0,
    val collectionsProcessed: Int = 0,
    val readsUsed: Int = 0,
    val maxReads: Int = 1000,
    val lastScanDate: Long = 0L,
    val canScanToday: Boolean = true,
    val isComplete: Boolean = false,
)
