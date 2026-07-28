package com.alcodb.optimizer.feature_scan.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class OrphanedRegistryUser(
    val email: String,
)
