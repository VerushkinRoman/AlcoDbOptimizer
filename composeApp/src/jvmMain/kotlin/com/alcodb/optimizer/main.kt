package com.alcodb.optimizer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.alcodb.optimizer.common.compose.AlcoDbOptimizerApp
import com.alcodb.optimizer.common.core.platform.PlatformConfiguration
import com.alcodb.optimizer.common.core.platform.PlatformSDK

fun main() = application {
    PlatformSDK.init(
        PlatformConfiguration(
            isDebug = true,
            firebaseConfigPath = resolveFirebaseConfig(),
        )
    )

    Window(
        title = "AlcoDbOptimizer",
        state = rememberWindowState(),
        onCloseRequest = ::exitApplication,
    ) {
        AlcoDbOptimizerApp(
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private fun resolveFirebaseConfig(): String {
    // Try environment variable
    System.getenv("FIREBASE_CONFIG_PATH")?.let { return it }

    // Try system property
    System.getProperty("firebase.config.path")?.let { return it }

    // Try config directory
    val configPaths = listOf(
        "config/firebase-service-account.json",
        "../config/firebase-service-account.json",
    )

    for (path in configPaths) {
        val file = java.io.File(path)
        if (file.exists()) {
            return file.absolutePath
        }
    }

    throw IllegalStateException(
        "Firebase service account key not found. " +
                "Set FIREBASE_CONFIG_PATH environment variable or place " +
                "firebase-service-account.json in config/ directory."
    )
}
