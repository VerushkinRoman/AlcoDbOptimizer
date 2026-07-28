package com.alcodb.optimizer.feature_scan.domain.models

data class BackupEntry(
    val fileName: String,
    val deletedAt: Long,
    val user: InactiveUser,
)
