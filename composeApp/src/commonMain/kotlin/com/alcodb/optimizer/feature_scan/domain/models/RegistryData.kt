package com.alcodb.optimizer.feature_scan.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class RegistryData(
    val nickname: String = "",
    val email: String = "",
    val locale: String = "",
    val token: String = "",
    val donations: Double = 0.0,
)
