package com.alcodb.optimizer.feature_scan.domain.repository

import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.domain.models.BackupEntry

interface BackupRepository {
    suspend fun getBackupEntries(): Result<List<BackupEntry>, Error>
    suspend fun restoreUser(entry: BackupEntry): EmptyResult<Error>
}
