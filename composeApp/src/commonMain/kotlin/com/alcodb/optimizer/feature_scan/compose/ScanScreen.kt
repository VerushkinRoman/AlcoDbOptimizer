package com.alcodb.optimizer.feature_scan.compose

import alcodboptimizer.composeapp.generated.resources.Res
import alcodboptimizer.composeapp.generated.resources.actualize_complete
import alcodboptimizer.composeapp.generated.resources.all_deleted
import alcodboptimizer.composeapp.generated.resources.auth_check_complete
import alcodboptimizer.composeapp.generated.resources.daily_limit_reached
import alcodboptimizer.composeapp.generated.resources.delete_partial
import alcodboptimizer.composeapp.generated.resources.error_firebase_not_initialized
import alcodboptimizer.composeapp.generated.resources.error_unknown
import alcodboptimizer.composeapp.generated.resources.firebase_init_failed
import alcodboptimizer.composeapp.generated.resources.firebase_initialized
import alcodboptimizer.composeapp.generated.resources.restore_complete
import alcodboptimizer.composeapp.generated.resources.scan_complete
import alcodboptimizer.composeapp.generated.resources.scan_stopped
import alcodboptimizer.composeapp.generated.resources.user_deleted
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.alcodb.optimizer.feature_scan.compose.components.ScanScreenContent
import com.alcodb.optimizer.feature_scan.compose.dialogs.ActualizeDialog
import com.alcodb.optimizer.feature_scan.compose.dialogs.AuthOrphanDialog
import com.alcodb.optimizer.feature_scan.compose.dialogs.DeleteAllConfirmDialog
import com.alcodb.optimizer.feature_scan.compose.dialogs.DeleteFailedDialog
import com.alcodb.optimizer.feature_scan.compose.dialogs.DeleteSingleConfirmDialog
import com.alcodb.optimizer.feature_scan.compose.dialogs.LostSearchDialog
import com.alcodb.optimizer.feature_scan.compose.dialogs.RestoreDialog
import com.alcodb.optimizer.feature_scan.compose.dialogs.UserDetailDialog
import com.alcodb.optimizer.feature_scan.presentation.ScanViewModel
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenAction
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenEvent
import org.jetbrains.compose.resources.getString

@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.viewStates().collectAsStateWithLifecycle()
    val action by viewModel.viewActions().collectAsStateWithLifecycle(initialValue = null)
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(action) {
        action?.let { currentAction ->
            val message = when (currentAction) {
                is ScanScreenAction.ShowError -> currentAction.message
                ScanScreenAction.ShowScanComplete -> getString(Res.string.scan_complete, state.inactiveUsers.size)
                ScanScreenAction.ShowScanStopped -> getString(Res.string.scan_stopped)
                is ScanScreenAction.ShowDailyLimitReached -> getString(Res.string.daily_limit_reached, currentAction.limit)
                is ScanScreenAction.ShowDeleteComplete -> if (currentAction.failed > 0) {
                    getString(
                        Res.string.delete_partial,
                        currentAction.deleted,
                        currentAction.total,
                        currentAction.failed,
                    )
                } else {
                    getString(Res.string.all_deleted, currentAction.deleted)
                }
                is ScanScreenAction.ShowUserDeleted -> getString(Res.string.user_deleted, currentAction.userId)
                ScanScreenAction.FirebaseInitialized -> getString(Res.string.firebase_initialized)
                ScanScreenAction.FirebaseInitFailed -> getString(Res.string.firebase_init_failed)
                ScanScreenAction.FirebaseNotInitialized ->
                    getString(Res.string.error_firebase_not_initialized)
                ScanScreenAction.UnknownError -> getString(Res.string.error_unknown)
                is ScanScreenAction.ShowRestoreComplete -> getString(Res.string.restore_complete, currentAction.userId)
                is ScanScreenAction.ShowActualizeComplete -> getString(Res.string.actualize_complete, currentAction.removedCount)
                is ScanScreenAction.ShowAuthCheckComplete -> getString(Res.string.auth_check_complete, currentAction.removedCount)
            }
            snackbarHostState.showSnackbar(message)
            viewModel.obtainEvent(ScanScreenEvent.ClearAction)
        }
    }

    val onEvent = viewModel::obtainEvent

    DeleteAllConfirmDialog(state = state, onEvent = onEvent)
    DeleteFailedDialog(state = state, onEvent = onEvent)
    DeleteSingleConfirmDialog(state = state, onEvent = onEvent)
    UserDetailDialog(state = state, onEvent = onEvent)
    RestoreDialog(state = state, onEvent = onEvent)
    ActualizeDialog(state = state, onEvent = onEvent)
    LostSearchDialog(state = state, onEvent = onEvent)
    AuthOrphanDialog(state = state, onEvent = onEvent)

    ScanScreenContent(
        state = state,
        snackbarHostState = snackbarHostState,
        onEvent = onEvent,
        modifier = modifier,
    )
}
