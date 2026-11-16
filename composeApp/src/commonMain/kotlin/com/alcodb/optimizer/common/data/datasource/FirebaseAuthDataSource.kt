package com.alcodb.optimizer.common.data.datasource

import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result

interface FirebaseAuthDataSource {
    suspend fun deleteAuthUser(uid: String): EmptyResult<Error>
    suspend fun deleteAuthUserByEmail(email: String): EmptyResult<Error>
    suspend fun listAllAuthUsers(): Result<List<String>, Error>
}
