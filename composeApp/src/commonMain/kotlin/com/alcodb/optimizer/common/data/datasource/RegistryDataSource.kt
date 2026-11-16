package com.alcodb.optimizer.common.data.datasource

import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result

interface RegistryDataSource {
    suspend fun getRegistryEmails(): Result<List<String>, Error>
    suspend fun getRegistryUserData(email: String): Result<Map<String, Any>?, Error>
    suspend fun setRegistryUserData(email: String, data: Map<String, Any?>): EmptyResult<Error>
    suspend fun deleteRegistryEmail(email: String): EmptyResult<Error>
}
