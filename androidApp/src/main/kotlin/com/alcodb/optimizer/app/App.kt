package com.alcodb.optimizer.app

import android.app.Application
import com.alcodb.optimizer.common.core.platform.PlatformConfiguration
import com.alcodb.optimizer.common.core.platform.PlatformSDK

class App : Application() {
    override fun onCreate() {
        super.onCreate()

        PlatformSDK.init(
            PlatformConfiguration(
                isDebug = false,
                firebaseConfigPath = resolveFirebaseConfig(),
            )
        )
    }

    private fun resolveFirebaseConfig(): String {
        // Try environment variable
        System.getenv("FIREBASE_CONFIG_PATH")?.let { return it }

        // Try system property
        System.getProperty("firebase.config.path")?.let { return it }

        // Try internal storage
        val internalFile = java.io.File(filesDir, "firebase-service-account.json")
        if (internalFile.exists()) {
            return internalFile.absolutePath
        }

        // Try external storage
        val externalFile = java.io.File(getExternalFilesDir(null), "firebase-service-account.json")
        if (externalFile.exists()) {
            return externalFile.absolutePath
        }

        throw IllegalStateException(
            "Firebase service account key not found. " +
                    "Place firebase-service-account.json in app's internal storage."
        )
    }
}
