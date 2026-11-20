package com.alcodb.optimizer.feature_scan.domain.models

data class DeleteAllResult(
    val deleted: Int,
    val total: Int,
    val failedUsers: List<InactiveUser>,
) {
    val failed: Int get() = failedUsers.size
}
