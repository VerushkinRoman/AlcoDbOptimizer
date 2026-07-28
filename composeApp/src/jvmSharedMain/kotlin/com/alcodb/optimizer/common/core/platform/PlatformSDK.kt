package com.alcodb.optimizer.common.core.platform

import com.alcodb.optimizer.common.core.di.Inject
import com.alcodb.optimizer.common.data.di.commonDataModule
import com.alcodb.optimizer.common.data.datasource.FirestoreDataSource
import com.alcodb.optimizer.common.data.datasource.FirestoreDataSourceImpl
import com.alcodb.optimizer.common.domain.di.commonDomainModule
import com.alcodb.optimizer.feature_scan.data.di.scanDataModule
import com.alcodb.optimizer.feature_scan.domain.di.scanDomainModule
import com.alcodb.optimizer.feature_scan.presentation.di.scanViewModelModule
import org.kodein.di.DI
import org.kodein.di.bindSingleton
import org.kodein.di.direct

internal val platformModule: DI.Module = DI.Module("PlatformModule") {
    bindSingleton<FirestoreDataSource> {
        FirestoreDataSourceImpl()
    }
}

object PlatformSDK {
    fun init(configuration: PlatformConfiguration) {
        val umbrellaModule = DI.Module("umbrellaModule") {
            bindSingleton<PlatformConfiguration> { configuration }
        }

        Inject.createDependencies(
            DI {
                importAll(
                    umbrellaModule,
                    platformModule,
                    commonDomainModule,
                    commonDataModule,
                    scanDomainModule,
                    scanDataModule,
                    scanViewModelModule,
                )
            }.direct
        )
    }
}
