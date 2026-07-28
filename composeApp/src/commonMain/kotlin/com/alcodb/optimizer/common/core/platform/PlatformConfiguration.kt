package com.alcodb.optimizer.common.core.platform

expect class PlatformConfiguration {
    val isDebug: Boolean
    val firebaseConfigPath: String
}
