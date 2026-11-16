package com.alcodb.optimizer.feature_scan.domain.repository

import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error

interface UserDeletionRepository {
    suspend fun deleteAllInactiveUsers(): EmptyResult<Error>
    suspend fun deleteUser(userId: String, isAnonymous: Boolean): EmptyResult<Error>
}
