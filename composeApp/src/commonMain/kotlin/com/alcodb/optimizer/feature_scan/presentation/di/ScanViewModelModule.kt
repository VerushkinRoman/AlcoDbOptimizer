package com.alcodb.optimizer.feature_scan.presentation.di

import com.alcodb.optimizer.common.core.di.Inject
import com.alcodb.optimizer.common.core.platform.PlatformConfiguration
import com.alcodb.optimizer.common.data.datasource.FirestoreDataSource
import com.alcodb.optimizer.common.domain.utils.Result
import com.alcodb.optimizer.feature_scan.presentation.ScanViewModel
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance

val scanViewModelModule = DI.Module("ScanViewModelModule") {
    bindProvider {
        ScanViewModel(
            scanRepository = instance(),
            initFirebase = {
                val config = Inject.instance<PlatformConfiguration>()
                val dataSource = Inject.instance<FirestoreDataSource>()
                val result = dataSource.initialize(config.firebaseConfigPath)
                if (result is Result.Error) {
                    throw IllegalStateException("Firebase initialization failed: ${result.error}")
                }
            },
        )
    }
}
