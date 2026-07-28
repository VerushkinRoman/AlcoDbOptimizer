package com.alcodb.optimizer.common.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ScanProgress(
    val lastScanDate: Long = 0L,
    val readsUsedToday: Int = 0,
    val processedCollections: List<String> = emptyList(),
    val allCollections: List<String> = emptyList(),
    val isComplete: Boolean = false,
)
