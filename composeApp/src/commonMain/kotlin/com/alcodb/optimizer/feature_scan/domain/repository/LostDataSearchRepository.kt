package com.alcodb.optimizer.feature_scan.domain.repository

import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.domain.models.LostDataSearchCriteria
import com.alcodb.optimizer.feature_scan.domain.models.LostDataSearchResult

interface LostDataSearchRepository {
    suspend fun searchLostData(
        criteria: LostDataSearchCriteria,
        maxReads: Int = 1000,
    ): Result<LostDataSearchResult, Error>
}
