package com.alcodb.optimizer.common.compose

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.alcodb.optimizer.common.compose.theme.AppTheme
import com.alcodb.optimizer.common.compose.utils.rememberViewModel
import com.alcodb.optimizer.common.core.di.Inject
import com.alcodb.optimizer.feature_scan.compose.ScanScreen
import com.alcodb.optimizer.feature_scan.presentation.ScanViewModel
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenEvent
import org.kodein.di.compose.withDI

@Composable
fun AlcoDbOptimizerApp(
    modifier: Modifier = Modifier,
) = withDI(Inject.di.di) {
    val viewModel: ScanViewModel by rememberViewModel<ScanViewModel>()

    LaunchedEffect(Unit) {
        viewModel.obtainEvent(ScanScreenEvent.OnInitFirebase)
    }

    AppTheme {
        ScanScreen(
            viewModel = viewModel,
            modifier = modifier.fillMaxSize(),
        )
    }
}
