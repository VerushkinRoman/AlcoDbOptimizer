package com.alcodb.optimizer.feature_scan.data.repository

import com.alcodb.optimizer.common.data.datasource.LocalStorageDataSource
import com.alcodb.optimizer.common.data.models.CalendarData
import com.alcodb.optimizer.common.domain.utils.Dispatchers
import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.EmptySuccessResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.data.datasource.ScanDataSource
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import com.alcodb.optimizer.feature_scan.domain.utils.ScanError
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserDeletionServiceTest {
    @Test
    fun partialDeletionReturnsFailedUsersAndKeepsThemInStorage() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val localStorage = FakeLocalStorage()
        val scanDataSource = FakeScanDataSource(failUserId = "bad")
        val state = ScanStateStore(localStorage)
        val stateJson = Json { ignoreUnknownKeys = true; prettyPrint = true }
        val service = UserDeletionService(
            scanDataSource = scanDataSource,
            dispatchers = TestDispatchers(dispatcher),
            state = state,
            backups = BackupService(scanDataSource, TestDispatchers(dispatcher), stateJson),
            json = stateJson,
        )
        state.inactiveUsers = listOf(user("good-1"), user("bad"), user("good-2"))

        val result = service.deleteAllInactiveUsers()

        assertTrue(result is Result.Success)
        val data = result.data
        assertEquals(2, data.deleted)
        assertEquals(3, data.total)
        assertEquals(listOf("bad"), data.failedUsers.map { it.id })
        assertEquals(listOf("good-1", "good-2"), scanDataSource.deletedCollections)
        assertEquals(listOf("bad"), state.inactiveUsers.map { it.id })
    }

    @Test
    fun serviceCollectionsAreNotDeletedByMassOperation() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scanDataSource = FakeScanDataSource()
        val state = ScanStateStore(FakeLocalStorage())
        val stateJson = Json { ignoreUnknownKeys = true; prettyPrint = true }
        val service = UserDeletionService(
            scanDataSource = scanDataSource,
            dispatchers = TestDispatchers(dispatcher),
            state = state,
            backups = BackupService(scanDataSource, TestDispatchers(dispatcher), stateJson),
            json = stateJson,
        )
        state.inactiveUsers = listOf(user("users"), user("good"))

        val result = service.deleteAllInactiveUsers()

        assertTrue(result is Result.Success)
        assertEquals(1, result.data.total)
        assertEquals(1, result.data.deleted)
        assertEquals(listOf("good"), scanDataSource.deletedCollections)
        assertEquals(listOf("users"), state.inactiveUsers.map { it.id })
    }

    @Test
    fun singleDeleteRefusesServiceCollection() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scanDataSource = FakeScanDataSource()
        val state = ScanStateStore(FakeLocalStorage())
        val stateJson = Json { ignoreUnknownKeys = true; prettyPrint = true }
        val service = UserDeletionService(
            scanDataSource = scanDataSource,
            dispatchers = TestDispatchers(dispatcher),
            state = state,
            backups = BackupService(scanDataSource, TestDispatchers(dispatcher), stateJson),
            json = stateJson,
        )
        state.inactiveUsers = listOf(user("users"))

        val result = service.deleteUser("users", isAnonymous = false)

        assertTrue(result is Result.Error)
        assertEquals(ScanError.FailedToDeleteUser, result.error)
        assertEquals(emptyList(), scanDataSource.deletedCollections)
    }

    private class FakeScanDataSource(
        private val failUserId: String? = null,
    ) : ScanDataSource {
        val deletedCollections = mutableListOf<String>()

        override suspend fun getAllCollections(): Result<List<String>, Error> =
            Result.Success(emptyList())

        override suspend fun getUserDates(userId: String): Result<CalendarData, Error> =
            Result.Success(emptyMap())

        override suspend fun deleteUserCollection(userId: String): EmptyResult<Error> {
            if (userId == failUserId) return Result.Error(ScanError.FailedToDeleteUser)
            deletedCollections += userId
            return EmptySuccessResult
        }

        override suspend fun removeUserFromGlobalRegistry(userEmail: String): EmptyResult<Error> =
            EmptySuccessResult

        override suspend fun deleteAuthUser(userId: String): EmptyResult<Error> = EmptySuccessResult

        override suspend fun deleteAuthUserByEmail(email: String): EmptyResult<Error> =
            EmptySuccessResult

        override suspend fun restoreUserDates(
            userId: String,
            dates: Map<String, Any?>,
        ): EmptyResult<Error> = EmptySuccessResult

        override suspend fun restoreUserRegistryData(
            email: String,
            data: Map<String, Any?>,
        ): EmptyResult<Error> = EmptySuccessResult

        override suspend fun getRegistryUserIds(): Result<List<String>, Error> =
            Result.Success(emptyList())

        override suspend fun getRegistryUserData(email: String): Result<Map<String, Any>?, Error> =
            Result.Success(null)

        override suspend fun listAllAuthUsers(): Result<List<String>, Error> =
            Result.Success(emptyList())

        override fun getReadCount(): Long = 0L
    }

    private class FakeLocalStorage : LocalStorageDataSource {
        private var inactiveUsers = "[]"

        override fun getLastScanDate(): Result<Long, Error> = Result.Success(0L)
        override fun setLastScanDate(date: Long): EmptyResult<Error> = EmptySuccessResult
        override fun getReadsUsedToday(): Result<Int, Error> = Result.Success(0)
        override fun setReadsUsedToday(count: Int): EmptyResult<Error> = EmptySuccessResult
        override fun getProcessedCollections(): Result<String, Error> = Result.Success("[]")
        override fun setProcessedCollections(json: String): EmptyResult<Error> = EmptySuccessResult
        override fun getInactiveUsers(): Result<String, Error> = Result.Success(inactiveUsers)
        override fun setInactiveUsers(json: String): EmptyResult<Error> {
            inactiveUsers = json
            return EmptySuccessResult
        }

        override fun clearProcessedCollections(): EmptyResult<Error> = EmptySuccessResult
        override fun getCachedCollections(): Result<String, Error> = Result.Success("[]")
        override fun setCachedCollections(json: String): EmptyResult<Error> = EmptySuccessResult
        override fun clearProgress(): EmptyResult<Error> = EmptySuccessResult
    }

    private class TestDispatchers(
        dispatcher: CoroutineDispatcher,
    ) : Dispatchers {
        override val main: CoroutineDispatcher = dispatcher
        override val io: CoroutineDispatcher = dispatcher
        override val default: CoroutineDispatcher = dispatcher
        override val unconfined: CoroutineDispatcher = dispatcher
    }

    private companion object {
        fun user(id: String) = InactiveUser(
            id = id,
            isAnonymous = true,
            lastDrinkDate = 0L,
            allDates = emptyMap(),
            totalDays = 0,
            drinkDays = 0,
        )
    }
}
