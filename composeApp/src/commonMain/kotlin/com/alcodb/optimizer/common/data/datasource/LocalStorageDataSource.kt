package com.alcodb.optimizer.common.data.datasource

interface LocalStorageDataSource {
    fun getLastScanDate(): Long
    fun setLastScanDate(date: Long)
    fun getReadsUsedToday(): Int
    fun setReadsUsedToday(count: Int)
    fun getProcessedCollections(): String
    fun setProcessedCollections(json: String)
    fun getInactiveUsers(): String
    fun setInactiveUsers(json: String)
    fun clearProcessedCollections()
    fun clearProgress()
}
