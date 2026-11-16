package com.alcodb.optimizer.feature_scan.data.di

import com.alcodb.optimizer.common.core.di.Inject
import com.alcodb.optimizer.feature_scan.data.datasource.ScanDataSource
import com.alcodb.optimizer.feature_scan.data.datasource.ScanDataSourceImpl
import com.alcodb.optimizer.feature_scan.data.repository.ScanRepositoryImpl
import com.alcodb.optimizer.feature_scan.domain.repository.ScanRepository
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.instance

val scanDataModule = DI.Module("ScanDataModule") {
    bindProvider<ScanDataSource> {
        ScanDataSourceImpl(
            firestoreDataSource = Inject.instance(),
        )
    }

    bindProvider<ScanRepository> {
        ScanRepositoryImpl(
            scanDataSource = instance(),
            firestoreDataSource = Inject.instance(),
            localStorageDataSource = instance(),
            dispatchers = instance(),
        )
    }
}
