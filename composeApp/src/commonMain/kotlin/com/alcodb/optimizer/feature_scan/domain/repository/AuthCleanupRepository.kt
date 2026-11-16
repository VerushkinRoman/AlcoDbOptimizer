package com.alcodb.optimizer.feature_scan.domain.repository

import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result

interface AuthCleanupRepository {
    suspend fun findOrphanedAuthUsers(): Result<List<String>, Error>
    suspend fun deleteAuthUserByEmail(email: String): EmptyResult<Error>
}
