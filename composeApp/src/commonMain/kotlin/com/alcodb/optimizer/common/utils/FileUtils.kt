package com.alcodb.optimizer.common.utils

expect fun writeToFile(path: String, content: String)
expect fun readFromFile(path: String): String?
expect fun getBackupDir(): String
expect fun getDataDir(): String
