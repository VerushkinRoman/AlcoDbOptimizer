package com.alcodb.optimizer.common.utils

actual fun writeToFile(path: String, content: String) = writeToFileImpl(path, content)
actual fun readFromFile(path: String): String? = readFromFileImpl(path)
actual fun getBackupDir(): String = getBackupDirImpl()
actual fun getDataDir(): String = getDataDirImpl()
