package com.alcodb.optimizer.feature_scan.domain.repository

import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.domain.models.DeleteAllResult
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser

interface UserDeletionRepository {
    suspend fun deleteAllInactiveUsers(): Result<DeleteAllResult, Error>
    suspend fun deleteUser(userId: String, isAnonymous: Boolean): EmptyResult<Error>
    suspend fun exportFailedUsers(users: List<InactiveUser>): Result<String, Error>
}
