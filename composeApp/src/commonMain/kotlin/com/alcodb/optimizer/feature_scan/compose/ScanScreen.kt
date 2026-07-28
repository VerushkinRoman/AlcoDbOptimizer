package com.alcodb.optimizer.feature_scan.compose

import alcodboptimizer.composeapp.generated.resources.Res
import alcodboptimizer.composeapp.generated.resources.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import com.alcodb.optimizer.feature_scan.compose.components.UserDetailCard
import com.alcodb.optimizer.feature_scan.compose.components.UserListItem
import com.alcodb.optimizer.feature_scan.presentation.ScanViewModel
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenAction
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenEvent
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.viewStates().collectAsState()
    val action by viewModel.viewActions().collectAsState(initial = null)
    val snackbarHostState = remember { SnackbarHostState() }

    val scanCompleteMsg = stringResource(Res.string.scan_complete, state.inactiveUsers.size)
    val dailyLimitMsgTemplate = stringResource(Res.string.daily_limit_reached)
    val allDeletedMsg = stringResource(Res.string.all_deleted)
    val userDeletedPattern = stringResource(Res.string.user_deleted, "")
    val firebaseOkMsg = stringResource(Res.string.firebase_initialized)
    val firebaseFailMsg = stringResource(Res.string.firebase_init_failed)
    val restoreCompletePattern = stringResource(Res.string.restore_complete, "")
    val restoreNoBackupsMsg = stringResource(Res.string.restore_no_backups)
    val restoreSelectUserMsg = stringResource(Res.string.restore_select_user)
    val actualizeCompletePattern = stringResource(Res.string.actualize_complete, 0)

    LaunchedEffect(action) {
        action?.let {
            when (it) {
                is ScanScreenAction.ShowError -> {
                    snackbarHostState.showSnackbar(it.message)
                }
                is ScanScreenAction.ShowScanComplete -> {
                    snackbarHostState.showSnackbar(scanCompleteMsg)
                }
                is ScanScreenAction.ShowDailyLimitReached -> {
                    snackbarHostState.showSnackbar(dailyLimitMsgTemplate.replace("%1\$d", it.limit.toString()))
                }
                is ScanScreenAction.ShowDeleteComplete -> {
                    snackbarHostState.showSnackbar(allDeletedMsg)
                }
                is ScanScreenAction.ShowUserDeleted -> {
                    snackbarHostState.showSnackbar(userDeletedPattern.replace("%1\$s", it.userId))
                }
                is ScanScreenAction.FirebaseInitialized -> {
                    snackbarHostState.showSnackbar(firebaseOkMsg)
                }
                is ScanScreenAction.FirebaseInitFailed -> {
                    snackbarHostState.showSnackbar(firebaseFailMsg)
                }
                is ScanScreenAction.ShowRestoreComplete -> {
                    snackbarHostState.showSnackbar(restoreCompletePattern.replace("%1\$s", it.userId))
                }
                is ScanScreenAction.ShowActualizeComplete -> {
                    snackbarHostState.showSnackbar(actualizeCompletePattern.replace("%1\$d", it.removedCount.toString()))
                }
            }
            viewModel.obtainEvent(ScanScreenEvent.ClearAction)
        }
    }

    // Delete All confirmation dialog
    if (state.isDeleteAllConfirmVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.obtainEvent(ScanScreenEvent.OnDeleteAllDismiss) },
            title = { Text(stringResource(Res.string.delete_all_title)) },
            text = {
                Text(stringResource(Res.string.delete_all_message, state.inactiveUsers.size))
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.obtainEvent(ScanScreenEvent.OnDeleteAllConfirm) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(Res.string.delete_all))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.obtainEvent(ScanScreenEvent.OnDeleteAllDismiss) }
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    // Delete single user confirmation dialog
    if (state.isDeleteSingleConfirmVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.obtainEvent(ScanScreenEvent.OnDeleteSingleDismiss) },
            title = { Text(stringResource(Res.string.delete_user_title)) },
            text = {
                Text(stringResource(Res.string.delete_user_message, state.selectedUser?.id ?: ""))
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.obtainEvent(ScanScreenEvent.OnDeleteSingleConfirm) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text(stringResource(Res.string.delete))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.obtainEvent(ScanScreenEvent.OnDeleteSingleDismiss) }
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    // User detail dialog
    state.selectedUser?.let { user ->
        AlertDialog(
            onDismissRequest = { viewModel.obtainEvent(ScanScreenEvent.OnUserDetailDismiss) },
            text = {
                UserDetailCard(
                    user = user,
                    onDeleteClick = { viewModel.obtainEvent(ScanScreenEvent.OnDeleteSingleUserClick) },
                    onDismiss = { viewModel.obtainEvent(ScanScreenEvent.OnUserDetailDismiss) },
                )
            },
            confirmButton = {},
            dismissButton = {},
        )
    }

    // Restore from backup dialog
    if (state.isRestoreDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.obtainEvent(ScanScreenEvent.OnRestoreDismiss) },
            title = { Text(stringResource(Res.string.restore_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = state.backupFilter,
                        onValueChange = { viewModel.obtainEvent(ScanScreenEvent.OnRestoreFilterChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.restore_filter_hint)) },
                        singleLine = true,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val filteredEntries = state.backupEntries.filter { entry ->
                        state.backupFilter.isEmpty() || entry.user.id.contains(state.backupFilter, ignoreCase = true)
                    }

                    if (filteredEntries.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.restore_no_backups),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(filteredEntries) { entry ->
                                val isSelected = state.selectedBackupEntry?.fileName == entry.fileName &&
                                        state.selectedBackupEntry?.user?.id == entry.user.id
                                val deletedAtDate = try {
                                    Instant.fromEpochMilliseconds(entry.deletedAt)
                                        .toLocalDateTime(TimeZone.currentSystemDefault())
                                        .date
                                        .toString()
                                } catch (_: Exception) {
                                    "unknown"
                                }
                                val lastDrinkDate = try {
                                    LocalDate.fromEpochDays(entry.user.lastDrinkDate.toInt())
                                        .toString()
                                } catch (_: Exception) {
                                    "N/A"
                                }

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.obtainEvent(ScanScreenEvent.OnRestoreSelectEntry(entry)) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) {
                                            MaterialTheme.colorScheme.primaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surface
                                        },
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                    ) {
                                        Text(
                                            text = entry.user.id,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                        )
                                        Text(
                                            text = stringResource(Res.string.restore_deleted_at, deletedAtDate),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        Text(
                                            text = stringResource(Res.string.restore_last_drink, lastDrinkDate),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        Text(
                                            text = stringResource(Res.string.restore_drink_days, entry.user.drinkDays),
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.obtainEvent(ScanScreenEvent.OnRestoreConfirm) },
                    enabled = state.selectedBackupEntry != null && !state.isRestoring,
                ) {
                    if (state.isRestoring) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(16.dp).width(16.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(stringResource(Res.string.restore_confirm))
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.obtainEvent(ScanScreenEvent.OnRestoreDismiss) }
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    // Actualize registry dialog
    if (state.isActualizeDialogVisible) {
        AlertDialog(
            onDismissRequest = { viewModel.obtainEvent(ScanScreenEvent.OnActualizeDismiss) },
            title = { Text(stringResource(Res.string.actualize_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isActualizing) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp).width(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(Res.string.actualize_searching))
                        }
                    } else {
                        Text(
                            text = stringResource(Res.string.actualize_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(Res.string.actualize_orphan_count, state.orphanedRegistryUsers.size, state.totalRegistryUsers),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = state.actualizeFilter,
                            onValueChange = { viewModel.obtainEvent(ScanScreenEvent.OnActualizeFilterChanged(it)) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(Res.string.actualize_hint)) },
                            singleLine = true,
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val filtered = state.orphanedRegistryUsers.filter { user ->
                            state.actualizeFilter.isEmpty() || user.email.contains(state.actualizeFilter, ignoreCase = true)
                        }

                        if (filtered.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.actualize_no_orphans),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 16.dp),
                            )
                        } else {
                            val clipboardManager = LocalClipboardManager.current
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 400.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                items(filtered) { user ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text(
                                            text = user.email,
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                        )
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString(user.email))
                                            },
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.ContentCopy,
                                                contentDescription = null,
                                                modifier = Modifier.height(16.dp).width(16.dp),
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.obtainEvent(ScanScreenEvent.OnActualizeRemoveSingle(user)) },
                                        ) {
                                            Text(stringResource(Res.string.actualize_remove_single))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (!state.isActualizing && state.orphanedRegistryUsers.isNotEmpty()) {
                    Button(
                        onClick = { viewModel.obtainEvent(ScanScreenEvent.OnActualizeRemoveAll) },
                        enabled = !state.isRemovingOrphans,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        if (state.isRemovingOrphans) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(16.dp).width(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onError,
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(stringResource(Res.string.actualize_remove_all))
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { viewModel.obtainEvent(ScanScreenEvent.OnActualizeDismiss) }
                ) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
        ) {
            // Read limit input (always visible)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = state.readLimit.toString(),
                        onValueChange = { value ->
                            value.filter { it.isDigit() }.toIntOrNull()?.let { limit ->
                                viewModel.obtainEvent(ScanScreenEvent.OnReadLimitChanged(limit))
                            }
                        },
                        modifier = Modifier.width(100.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        enabled = !state.isScanning,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        label = { Text(stringResource(Res.string.read_limit_label)) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress card
            if (state.isScanning || state.collectionsTotal > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        if (state.isScanning) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.height(20.dp).width(20.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(Res.string.scanning),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        LinearProgressIndicator(
                            progress = {
                                if (state.collectionsTotal > 0) {
                                    state.collectionsProcessed.toFloat() / state.collectionsTotal
                                } else {
                                    0f
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = stringResource(Res.string.collections_format, state.collectionsProcessed, state.collectionsTotal),
                            style = MaterialTheme.typography.bodySmall,
                        )

                        Text(
                            text = stringResource(Res.string.reads_format, state.readsUsed, state.maxReads),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (state.readsUsed > state.maxReads * 0.8) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Deletion progress
            if (state.isDeleting) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp).width(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(Res.string.deleting),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = {
                                if (state.deletionTotal > 0) {
                                    state.deletedCount.toFloat() / state.deletionTotal
                                } else {
                                    0f
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.error,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(Res.string.deleting_progress, state.deletedCount, state.deletionTotal),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Action buttons - row 1: Start/Stop + Delete All
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!state.isScanning) {
                    Button(
                        onClick = { viewModel.obtainEvent(ScanScreenEvent.OnStartScan) },
                        modifier = Modifier.weight(1f),
                        enabled = !state.isInitializingFirebase,
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            when {
                                state.isComplete -> stringResource(Res.string.start_scan)
                                state.collectionsProcessed > 0 -> stringResource(Res.string.continue_scan)
                                else -> stringResource(Res.string.start_scan)
                            }
                        )
                    }
                } else {
                    Button(
                        onClick = { viewModel.obtainEvent(ScanScreenEvent.OnStopScan) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.stop))
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.obtainEvent(ScanScreenEvent.OnDeleteAllClick) },
                    enabled = state.inactiveUsers.isNotEmpty() && !state.isScanning,
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action buttons - row 2: Reset Counter + Restore + Reset All
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = { viewModel.obtainEvent(ScanScreenEvent.OnResetCounter) },
                    enabled = !state.isScanning,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(Res.string.reset_counter))
                }

                OutlinedButton(
                    onClick = { viewModel.obtainEvent(ScanScreenEvent.OnRestoreClick) },
                    enabled = !state.isScanning,
                    modifier = Modifier.weight(1f),
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(Res.string.restore))
                }

                OutlinedButton(
                    onClick = { viewModel.obtainEvent(ScanScreenEvent.OnActualizeClick) },
                    enabled = !state.isScanning,
                    modifier = Modifier.weight(1f),
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(Res.string.actualize))
                }

                OutlinedButton(
                    onClick = { viewModel.obtainEvent(ScanScreenEvent.OnResetAll) },
                    enabled = !state.isScanning,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(Res.string.reset_all))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Results
            if (state.inactiveUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = when {
                            state.isScanning -> stringResource(Res.string.scanning_database)
                            state.collectionsProcessed == 0 -> stringResource(Res.string.press_start_scan)
                            else -> stringResource(Res.string.no_inactive_users)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                Text(
                    text = stringResource(Res.string.inactive_users_count, state.inactiveUsers.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(state.inactiveUsers) { user ->
                        UserListItem(
                            user = user,
                            onClick = { viewModel.obtainEvent(ScanScreenEvent.OnUserClick(user)) },
                        )
                    }
                }
            }
        }
    }
}
