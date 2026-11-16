package com.alcodb.optimizer.common.data.di

import com.alcodb.optimizer.common.data.datasource.LocalStorageDataSource
import com.alcodb.optimizer.common.data.datasource.LocalStorageDataSourceImpl
import com.russhwolf.settings.Settings
import org.kodein.di.DI
import org.kodein.di.bindProvider
import org.kodein.di.bindSingleton
import org.kodein.di.instance

val commonDataModule = DI.Module("CommonDataModule") {
    bindSingleton<Settings> {
        Settings()
    }

    bindProvider<LocalStorageDataSource> {
        LocalStorageDataSourceImpl(settings = instance())
    }
}
