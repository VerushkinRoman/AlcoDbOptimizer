package com.alcodb.optimizer.common.data.datasource

import com.alcodb.optimizer.common.domain.utils.DataError
import com.alcodb.optimizer.common.domain.utils.EmptyResult
import com.alcodb.optimizer.common.domain.utils.Error
import com.alcodb.optimizer.common.domain.utils.EmptySuccessResult
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.common.utils.getDataDir
import com.alcodb.optimizer.common.utils.readFromFile
import com.alcodb.optimizer.common.utils.writeToFile
import com.russhwolf.settings.Settings
import com.russhwolf.settings.set
import java.io.File

class LocalStorageDataSourceImpl(
    private val settings: Settings,
) : LocalStorageDataSource {
    override fun getLastScanDate(): Result<Long, Error> = read {
        settings.getLong(LAST_SCAN_DATE_KEY, 0L)
    }

    override fun setLastScanDate(date: Long): EmptyResult<Error> = write {
        settings[LAST_SCAN_DATE_KEY] = date
    }

    override fun getReadsUsedToday(): Result<Int, Error> = read {
        settings.getInt(READS_USED_KEY, 0)
    }

    override fun setReadsUsedToday(count: Int): EmptyResult<Error> = write {
        settings[READS_USED_KEY] = count
    }

    override fun getProcessedCollections(): Result<String, Error> = read {
        readFromFile(getDataFilePath(PROCESSED_COLLECTIONS_FILE)) ?: "[]"
    }

    override fun setProcessedCollections(json: String): EmptyResult<Error> = write {
        writeToFile(getDataFilePath(PROCESSED_COLLECTIONS_FILE), json)
    }

    override fun getInactiveUsers(): Result<String, Error> = read {
        readFromFile(getDataFilePath(INACTIVE_USERS_FILE)) ?: "[]"
    }

    override fun setInactiveUsers(json: String): EmptyResult<Error> = write {
        writeToFile(getDataFilePath(INACTIVE_USERS_FILE), json)
    }

    override fun clearProcessedCollections(): EmptyResult<Error> = delete {
        File(getDataFilePath(PROCESSED_COLLECTIONS_FILE)).delete()
    }

    override fun getCachedCollections(): Result<String, Error> = read {
        readFromFile(getDataFilePath(COLLECTIONS_CACHE_FILE)) ?: "[]"
    }

    override fun setCachedCollections(json: String): EmptyResult<Error> = write {
        writeToFile(getDataFilePath(COLLECTIONS_CACHE_FILE), json)
    }

    override fun clearProgress(): EmptyResult<Error> = delete {
        settings.remove(LAST_SCAN_DATE_KEY)
        settings.remove(READS_USED_KEY)
        File(getDataFilePath(PROCESSED_COLLECTIONS_FILE)).delete()
        File(getDataFilePath(INACTIVE_USERS_FILE)).delete()
        File(getDataFilePath(COLLECTIONS_CACHE_FILE)).delete()
    }

    private fun <T> read(block: () -> T): Result<T, Error> = try {
        Result.Success(block())
    } catch (_: Exception) {
        Result.Error(DataError.ReadError)
    }

    private fun write(block: () -> Unit): EmptyResult<Error> = try {
        block()
        EmptySuccessResult
    } catch (_: Exception) {
        Result.Error(DataError.WriteError)
    }

    private fun delete(block: () -> Unit): EmptyResult<Error> = try {
        block()
        EmptySuccessResult
    } catch (_: Exception) {
        Result.Error(DataError.DeleteError)
    }

    private fun getDataFilePath(fileName: String): String = "${getDataDir()}/$fileName"

    companion object {
        private const val LAST_SCAN_DATE_KEY = "last_scan_date"
        private const val READS_USED_KEY = "reads_used_today"
        private const val PROCESSED_COLLECTIONS_FILE = "processed_collections.json"
        private const val INACTIVE_USERS_FILE = "inactive_users.json"
        private const val COLLECTIONS_CACHE_FILE = "collections_cache.json"
    }
}
