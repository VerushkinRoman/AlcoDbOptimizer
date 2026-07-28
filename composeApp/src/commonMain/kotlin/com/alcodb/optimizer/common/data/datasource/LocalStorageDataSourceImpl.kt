package com.alcodb.optimizer.common.data.datasource

import com.alcodb.optimizer.common.utils.getDataDir
import com.alcodb.optimizer.common.utils.readFromFile
import com.alcodb.optimizer.common.utils.writeToFile
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set

class LocalStorageDataSourceImpl(
    private val settings: Settings
) : LocalStorageDataSource {
    override fun getLastScanDate(): Long = settings.getLong(LAST_SCAN_DATE_KEY, 0L)
    override fun setLastScanDate(date: Long) {
        settings[LAST_SCAN_DATE_KEY] = date
    }

    override fun getReadsUsedToday(): Int = settings.getInt(READS_USED_KEY, 0)
    override fun setReadsUsedToday(count: Int) {
        settings[READS_USED_KEY] = count
    }

    override fun getProcessedCollections(): String = readFromFile(getDataFilePath(PROCESSED_COLLECTIONS_FILE)) ?: "[]"
    override fun setProcessedCollections(json: String) {
        writeToFile(getDataFilePath(PROCESSED_COLLECTIONS_FILE), json)
    }

    override fun getInactiveUsers(): String = readFromFile(getDataFilePath(INACTIVE_USERS_FILE)) ?: "[]"
    override fun setInactiveUsers(json: String) {
        writeToFile(getDataFilePath(INACTIVE_USERS_FILE), json)
    }

    override fun clearProcessedCollections() {
        try {
            java.io.File(getDataFilePath(PROCESSED_COLLECTIONS_FILE)).delete()
        } catch (_: Exception) {
        }
    }

    override fun clearProgress() {
        settings.remove(LAST_SCAN_DATE_KEY)
        settings.remove(READS_USED_KEY)
        try {
            java.io.File(getDataFilePath(PROCESSED_COLLECTIONS_FILE)).delete()
            java.io.File(getDataFilePath(INACTIVE_USERS_FILE)).delete()
        } catch (_: Exception) {
        }
    }

    private fun getDataFilePath(fileName: String): String = "${getDataDir()}/$fileName"

    companion object {
        private const val LAST_SCAN_DATE_KEY = "last_scan_date"
        private const val READS_USED_KEY = "reads_used_today"
        private const val PROCESSED_COLLECTIONS_FILE = "processed_collections.json"
        private const val INACTIVE_USERS_FILE = "inactive_users.json"
    }
}
