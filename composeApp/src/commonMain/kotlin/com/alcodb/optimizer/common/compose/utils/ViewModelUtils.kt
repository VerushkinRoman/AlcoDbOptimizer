package com.alcodb.optimizer.common.compose.utils

import androidx.compose.runtime.Composable
import org.kodein.di.compose.rememberInstance

/**
 * Получает ViewModel из DI и запоминает её в текущей композиции.
 */
@Composable
inline fun <reified T : Any> rememberViewModel() = rememberInstance<T>()
