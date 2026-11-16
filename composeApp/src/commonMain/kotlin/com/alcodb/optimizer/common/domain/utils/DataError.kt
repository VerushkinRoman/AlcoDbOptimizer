package com.alcodb.optimizer.common.domain.utils

/**
 * Общие ошибки источников данных. Фичи могут маппить их в свои ошибки.
 */
enum class DataError : Error {
    NotInitialized,
    InitializationError,
    ReadError,
    WriteError,
    DeleteError,
}
