package com.alcodb.optimizer.feature_scan.domain.repository

import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.domain.models.OrphanedRegistryUser

interface RegistryCleanupRepository {
    suspend fun findOrphanedRegistryUsers(): Result<Pair<Int, List<OrphanedRegistryUser>>, Error>
    suspend fun removeRegistryUser(email: String): EmptyResult<Error>
    suspend fun removeAllOrphanedRegistryUsers(users: List<OrphanedRegistryUser>): EmptyResult<Error>
}
