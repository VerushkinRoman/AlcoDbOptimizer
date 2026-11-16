package com.alcodb.optimizer.common.data.datasource

import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result

interface LocalStorageDataSource {
    fun getLastScanDate(): Result<Long, Error>
    fun setLastScanDate(date: Long): EmptyResult<Error>
    fun getReadsUsedToday(): Result<Int, Error>
    fun setReadsUsedToday(count: Int): EmptyResult<Error>
    fun getProcessedCollections(): Result<String, Error>
    fun setProcessedCollections(json: String): EmptyResult<Error>
    fun getInactiveUsers(): Result<String, Error>
    fun setInactiveUsers(json: String): EmptyResult<Error>
    fun clearProcessedCollections(): EmptyResult<Error>
    fun getCachedCollections(): Result<String, Error>
    fun setCachedCollections(json: String): EmptyResult<Error>
    fun clearProgress(): EmptyResult<Error>
}
