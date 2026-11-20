package com.alcodb.optimizer.feature_scan.presentation.coordinators

import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.EmptySuccessResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.domain.models.DeleteAllResult
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import com.alcodb.optimizer.feature_scan.domain.repository.UserDeletionRepository
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenState
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeletionCoordinatorTest {
    private val repository = FakeDeletionRepository()
    private val coordinator = DeletionCoordinator(repository)

    @Test
    fun startDeleteAllClearsFailureAndExportState() {
        val state = ScanScreenState(
            isDeleteAllConfirmVisible = true,
            isDeleteFailedDialogVisible = true,
            failedDeletionUsers = listOf(user("failed")),
            exportedFailedUsersPath = "old.csv",
            failedUsersExportError = true,
        )

        val result = coordinator.startDeleteAll(state)

        assertFalse(result.isDeleteAllConfirmVisible)
        assertFalse(result.isDeleteFailedDialogVisible)
        assertEquals(emptyList(), result.failedDeletionUsers)
        assertNull(result.exportedFailedUsersPath)
        assertFalse(result.failedUsersExportError)
    }

    @Test
    fun applyResultOpensDialogWhenSomeUsersFailed() {
        val failed = listOf(user("first"), user("second"))
        val result = DeleteAllResult(deleted = 3, total = 5, failedUsers = failed)

        val state = coordinator.applyResult(ScanScreenState(), result)

        assertTrue(state.isDeleteFailedDialogVisible)
        assertEquals(failed, state.failedDeletionUsers)
    }

    @Test
    fun applyResultClosesDialogWhenAllUsersDeleted() {
        val state = coordinator.applyResult(
            state = ScanScreenState(
                isDeleteFailedDialogVisible = true,
                failedDeletionUsers = listOf(user("old")),
            ),
            result = DeleteAllResult(deleted = 2, total = 2, failedUsers = emptyList()),
        )

        assertFalse(state.isDeleteFailedDialogVisible)
        assertEquals(emptyList(), state.failedDeletionUsers)
    }

    @Test
    fun startRetryClosesDialogButKeepsFailedUsers() {
        val failed = listOf(user("failed"))
        val state = ScanScreenState(
            isDeleteFailedDialogVisible = true,
            failedDeletionUsers = failed,
            exportedFailedUsersPath = "old.csv",
            failedUsersExportError = true,
        )

        val result = coordinator.startRetry(state)

        assertFalse(result.isDeleteFailedDialogVisible)
        assertEquals(failed, result.failedDeletionUsers)
        assertNull(result.exportedFailedUsersPath)
        assertFalse(result.failedUsersExportError)
    }

    @Test
    fun dismissFailedClearsDialogAndExportFeedback() {
        val state = ScanScreenState(
            isDeleteFailedDialogVisible = true,
            exportedFailedUsersPath = "old.csv",
            failedUsersExportError = true,
        )

        val result = coordinator.dismissFailed(state)

        assertFalse(result.isDeleteFailedDialogVisible)
        assertNull(result.exportedFailedUsersPath)
        assertFalse(result.failedUsersExportError)
    }

    @Test
    fun exportSuccessStoresPathAndClearsError() {
        val state = coordinator.applyExport(
            state = ScanScreenState(failedUsersExportError = true),
            path = "backups/failed_users.csv",
        )

        assertEquals("backups/failed_users.csv", state.exportedFailedUsersPath)
        assertFalse(state.failedUsersExportError)
    }

    @Test
    fun exportErrorClearsPathAndSetsFlag() {
        val state = coordinator.applyExportError(
            ScanScreenState(exportedFailedUsersPath = "old.csv"),
        )

        assertNull(state.exportedFailedUsersPath)
        assertTrue(state.failedUsersExportError)
    }

    @Test
    fun exportDelegatesUsersAndReturnsRepositoryResult() = runTest {
        val users = listOf(user("first"), user("second"))
        val expected = Result.Success("backups/failed_users.csv")

        val result = coordinator.exportFailedUsers(users)

        assertEquals(users, repository.exportedUsers)
        assertEquals(expected, result)
    }

    private class FakeDeletionRepository : UserDeletionRepository {
        var exportedUsers: List<InactiveUser> = emptyList()

        override suspend fun deleteAllInactiveUsers(): Result<DeleteAllResult, Error> =
            Result.Success(DeleteAllResult(deleted = 0, total = 0, failedUsers = emptyList()))

        override suspend fun deleteUser(userId: String, isAnonymous: Boolean): EmptyResult<Error> =
            EmptySuccessResult

        override suspend fun exportFailedUsers(users: List<InactiveUser>): Result<String, Error> {
            exportedUsers = users
            return Result.Success("backups/failed_users.csv")
        }
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
