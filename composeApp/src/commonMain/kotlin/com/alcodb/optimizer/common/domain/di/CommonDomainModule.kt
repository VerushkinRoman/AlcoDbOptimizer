package com.alcodb.optimizer.common.domain.di

import com.alcodb.optimizer.common.domain.utils.Dispatchers
import com.alcodb.optimizer.common.domain.utils.DispatchersImpl
import org.kodein.di.DI
import org.kodein.di.bindSingleton

val commonDomainModule = DI.Module("CommonDomainModule") {
    bindSingleton<Dispatchers> {
        DispatchersImpl()
    }
}
