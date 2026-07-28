package com.alcodb.optimizer.common.data.models

import kotlinx.serialization.Serializable

@Serializable
data class DataDayData(
    val date: Long,
    val drinkType: String?,
)

typealias CalendarData = Map<String, DataDayData>
