package com.alcodb.optimizer.common.domain.utils

import kotlinx.coroutines.CoroutineDispatcher

interface Dispatchers {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val unconfined: CoroutineDispatcher
}

class DispatchersImpl : Dispatchers {
    override val main: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Main
    override val io: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO
    override val default: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Default
    override val unconfined: CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
}
