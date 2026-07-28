package com.alcodb.optimizer.feature_scan.domain.models

import kotlinx.serialization.Serializable

@Serializable
data class InactiveUser(
    val id: String,
    val isAnonymous: Boolean,
    val lastDrinkDate: Long,
    val allDates: Map<String, Long>,
    val totalDays: Int,
    val drinkDays: Int,
    val registryData: RegistryData? = null,
)

enum class UserType {
    ANONYMOUS,
    REGISTERED,
}
