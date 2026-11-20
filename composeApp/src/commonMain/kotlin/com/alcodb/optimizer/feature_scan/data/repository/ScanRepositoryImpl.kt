package com.alcodb.optimizer.feature_scan.data.repository

import com.alcodb.optimizer.common.data.datasource.FirestoreDataSource
import com.alcodb.optimizer.common.data.datasource.LocalStorageDataSource
import com.alcodb.optimizer.common.domain.utils.Dispatchers
import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.data.datasource.ScanDataSource
import com.alcodb.optimizer.feature_scan.domain.models.BackupEntry
import com.alcodb.optimizer.feature_scan.domain.models.DeleteAllResult
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import com.alcodb.optimizer.feature_scan.domain.models.LostDataSearchCriteria
import com.alcodb.optimizer.feature_scan.domain.models.LostDataSearchResult
import com.alcodb.optimizer.feature_scan.domain.models.OrphanedRegistryUser
import com.alcodb.optimizer.feature_scan.domain.repository.ScanProgressState
import com.alcodb.optimizer.feature_scan.domain.repository.ScanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Фасад фичи сканирования: держит роли и делегирует им вызовы.
 * Собственной логики не содержит.
 */
class ScanRepositoryImpl(
    scanDataSource: ScanDataSource,
    firestoreDataSource: FirestoreDataSource,
    localStorageDataSource: LocalStorageDataSource,
    dispatchers: Dispatchers,
) : ScanRepository {
    private val state = ScanStateStore(localStorageDataSource)
    private val backups = BackupService(scanDataSource, dispatchers, state.json)
    private val collections = CollectionsProvider(scanDataSource, localStorageDataSource, state.json)
    private val runner = ScanRunner(
        scanDataSource = scanDataSource,
        firestoreDataSource = firestoreDataSource,
        localStorageDataSource = localStorageDataSource,
        dispatchers = dispatchers,
        state = state,
        collections = collections,
        detector = InactiveUserDetector(),
    )
    private val deletion = UserDeletionService(
        scanDataSource = scanDataSource,
        dispatchers = dispatchers,
        state = state,
        backups = backups,
        json = state.json,
    )
    private val registryCleanup = RegistryCleanupService(
        scanDataSource = scanDataSource,
        dispatchers = dispatchers,
        collections = collections,
        backups = backups,
        json = state.json,
    )
    private val authCleanup = AuthCleanupService(
        scanDataSource = scanDataSource,
        dispatchers = dispatchers,
        collections = collections,
    )
    private val lostDataSearch = LostDataSearchService(
        scanDataSource = scanDataSource,
        firestoreDataSource = firestoreDataSource,
        dispatchers = dispatchers,
        collections = collections,
    )

    override fun getDeletionProgress(): Flow<Pair<Int, Int>> = state.deletionProgressFlow

    override fun getInactiveUsers(): Flow<Result<List<InactiveUser>, Error>> = flow {
        val savedUsers = state.loadSavedUsers()
        if (savedUsers.isNotEmpty()) {
            state.inactiveUsers = savedUsers
            emit(Result.Success(savedUsers))
        }
        state.inactiveUsersFlow.collect { users ->
            emit(Result.Success(users))
        }
    }

    override fun getScanProgress(): Flow<ScanProgressState> = state.progressFlow

    override suspend fun startScan(maxReads: Int): EmptyResult<Error> = runner.startScan(maxReads)

    override suspend fun stopScan(): EmptyResult<Error> = runner.stopScan()

    override suspend fun deleteAllInactiveUsers(): Result<DeleteAllResult, Error> =
        deletion.deleteAllInactiveUsers()

    override suspend fun deleteUser(userId: String, isAnonymous: Boolean): EmptyResult<Error> =
        deletion.deleteUser(userId, isAnonymous)

    override suspend fun exportFailedUsers(users: List<InactiveUser>): Result<String, Error> =
        deletion.exportFailedUsers(users)

    override suspend fun refreshInactiveUsers(): EmptyResult<Error> = runner.refreshInactiveUsers()

    override suspend fun resetCounter(): EmptyResult<Error> = runner.resetCounter()

    override suspend fun resetAll(): EmptyResult<Error> = runner.resetAll()

    override suspend fun getBackupEntries(): Result<List<BackupEntry>, Error> =
        backups.getBackupEntries()

    override suspend fun restoreUser(entry: BackupEntry): EmptyResult<Error> = backups.restoreUser(entry)

    override suspend fun findOrphanedRegistryUsers(): Result<Pair<Int, List<OrphanedRegistryUser>>, Error> =
        registryCleanup.findOrphanedRegistryUsers()

    override suspend fun removeRegistryUser(email: String): EmptyResult<Error> =
        registryCleanup.removeRegistryUser(email)

    override suspend fun removeAllOrphanedRegistryUsers(
        users: List<OrphanedRegistryUser>,
    ): EmptyResult<Error> = registryCleanup.removeAllOrphanedRegistryUsers(users)

    override suspend fun findOrphanedAuthUsers(): Result<List<String>, Error> =
        authCleanup.findOrphanedAuthUsers()

    override suspend fun deleteAuthUserByEmail(email: String): EmptyResult<Error> =
        authCleanup.deleteAuthUserByEmail(email)

    override suspend fun searchLostData(
        criteria: LostDataSearchCriteria,
        maxReads: Int,
    ): Result<LostDataSearchResult, Error> = lostDataSearch.searchLostData(criteria, maxReads)
}
