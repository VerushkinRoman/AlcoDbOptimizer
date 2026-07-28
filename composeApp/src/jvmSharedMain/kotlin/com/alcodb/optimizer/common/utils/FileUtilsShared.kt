package com.alcodb.optimizer.common.utils

import java.io.File

fun writeToFileImpl(path: String, content: String) {
    try {
        val file = File(path)
        file.parentFile?.mkdirs()
        file.writeText(content)
    } catch (e: Exception) {
        println("[FileUtils] writeToFile error: ${e.message}")
    }
}

fun readFromFileImpl(path: String): String? {
    return try {
        val file = File(path)
        if (file.exists()) file.readText() else null
    } catch (e: Exception) {
        println("[FileUtils] readFromFile error: ${e.message}")
        null
    }
}

fun getBackupDirImpl(): String = "backups"

fun getDataDirImpl(): String = "data"
