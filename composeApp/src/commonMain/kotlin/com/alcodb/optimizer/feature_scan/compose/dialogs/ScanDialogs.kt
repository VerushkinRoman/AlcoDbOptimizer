package com.alcodb.optimizer.feature_scan.compose.dialogs

import alcodboptimizer.composeapp.generated.resources.Res
import alcodboptimizer.composeapp.generated.resources.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alcodb.optimizer.common.compose.utils.rememberCopyTextToClipboard
import com.alcodb.optimizer.feature_scan.compose.components.LostDataSearchDialog
import com.alcodb.optimizer.feature_scan.compose.components.UserDetailCard
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenEvent
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@Composable
internal fun DeleteAllConfirmDialog(
    state: ScanScreenState,
    onEvent: (ScanScreenEvent) -> Unit,
) {
// Delete All confirmation dialog
if (state.isDeleteAllConfirmVisible) {
    AlertDialog(
        onDismissRequest = { onEvent(ScanScreenEvent.OnDeleteAllDismiss) },
        title = { Text(stringResource(Res.string.delete_all_title)) },
        text = {
            Text(stringResource(Res.string.delete_all_message, state.inactiveUsers.size))
        },
        confirmButton = {
            Button(
                onClick = { onEvent(ScanScreenEvent.OnDeleteAllConfirm) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(Res.string.delete_all))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onEvent(ScanScreenEvent.OnDeleteAllDismiss) }
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

}

@Composable
internal fun DeleteFailedDialog(
    state: ScanScreenState,
    onEvent: (ScanScreenEvent) -> Unit,
) {
    if (!state.isDeleteFailedDialogVisible) return

    AlertDialog(
        onDismissRequest = { onEvent(ScanScreenEvent.OnDeleteFailedDismiss) },
        title = {
            Text(stringResource(Res.string.delete_failed_title, state.failedDeletionUsers.size))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.delete_failed_message),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(state.failedDeletionUsers) { user ->
                        Text(
                            text = user.id,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { onEvent(ScanScreenEvent.OnDeleteFailedExport) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(Res.string.delete_failed_export))
                }
                if (state.exportedFailedUsersPath != null) {
                    Text(
                        text = stringResource(
                            Res.string.delete_failed_exported,
                            state.exportedFailedUsersPath,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                if (state.failedUsersExportError) {
                    Text(
                        text = stringResource(Res.string.delete_failed_export_error),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = { onEvent(ScanScreenEvent.OnDeleteFailedRetry) }) {
                Text(stringResource(Res.string.delete_failed_retry))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onEvent(ScanScreenEvent.OnDeleteFailedDismiss) }) {
                Text(stringResource(Res.string.close))
            }
        },
    )
}

@Composable
internal fun DeleteSingleConfirmDialog(
    state: ScanScreenState,
    onEvent: (ScanScreenEvent) -> Unit,
) {
// Delete single user confirmation dialog
if (state.isDeleteSingleConfirmVisible) {
    AlertDialog(
        onDismissRequest = { onEvent(ScanScreenEvent.OnDeleteSingleDismiss) },
        title = { Text(stringResource(Res.string.delete_user_title)) },
        text = {
            Text(stringResource(Res.string.delete_user_message, state.selectedUser?.id ?: ""))
        },
        confirmButton = {
            Button(
                onClick = { onEvent(ScanScreenEvent.OnDeleteSingleConfirm) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(Res.string.delete))
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onEvent(ScanScreenEvent.OnDeleteSingleDismiss) }
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

}

@Composable
internal fun UserDetailDialog(
    state: ScanScreenState,
    onEvent: (ScanScreenEvent) -> Unit,
) {
// User detail dialog
state.selectedUser?.let { user ->
    AlertDialog(
        onDismissRequest = { onEvent(ScanScreenEvent.OnUserDetailDismiss) },
        text = {                UserDetailCard(
                    user = user,
                    showAllDates = state.showAllDatesInUserDetail,
                    onToggleAllDates = { onEvent(ScanScreenEvent.OnUserDetailToggleAllDates) },
                    onDeleteClick = { onEvent(ScanScreenEvent.OnDeleteSingleUserClick) },
                onDismiss = { onEvent(ScanScreenEvent.OnUserDetailDismiss) },
            )
        },
        confirmButton = {},
        dismissButton = {},
    )
}

}

@Composable
internal fun RestoreDialog(
    state: ScanScreenState,
    onEvent: (ScanScreenEvent) -> Unit,
) {
// Restore from backup dialog
if (state.isRestoreDialogVisible) {
    AlertDialog(
        onDismissRequest = { onEvent(ScanScreenEvent.OnRestoreDismiss) },
        title = { Text(stringResource(Res.string.restore_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = state.backupFilter,
                    onValueChange = { onEvent(ScanScreenEvent.OnRestoreFilterChanged(it)) },
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
                            val selectedEntry = state.selectedBackupEntry
                            val isSelected = selectedEntry != null &&
                                selectedEntry.fileName == entry.fileName &&
                                selectedEntry.user.id == entry.user.id
                            val deletedAtDate = try {
                                Instant.fromEpochMilliseconds(entry.deletedAt)
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                                    .date
                                    .toString()
                            } catch (_: Exception) {
                                stringResource(Res.string.value_unknown)
                                }
                            val lastDrinkDate = try {
                                LocalDate.fromEpochDays(entry.user.lastDrinkDate.toInt())
                                    .toString()                                } catch (_: Exception) {
                                    stringResource(Res.string.value_not_available)
                                }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onEvent(ScanScreenEvent.OnRestoreSelectEntry(entry)) },
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
                onClick = { onEvent(ScanScreenEvent.OnRestoreConfirm) },
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
                onClick = { onEvent(ScanScreenEvent.OnRestoreDismiss) }
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

}

@Composable
internal fun ActualizeDialog(
    state: ScanScreenState,
    onEvent: (ScanScreenEvent) -> Unit,
) {
// Actualize registry dialog
if (state.isActualizeDialogVisible) {
    AlertDialog(
        onDismissRequest = { onEvent(ScanScreenEvent.OnActualizeDismiss) },
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
                        onValueChange = { onEvent(ScanScreenEvent.OnActualizeFilterChanged(it)) },
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
                        val copyTextToClipboard = rememberCopyTextToClipboard()
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
                                            copyTextToClipboard(user.email)
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.height(16.dp).width(16.dp),
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onEvent(ScanScreenEvent.OnActualizeRemoveSingle(user)) },
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
                    onClick = { onEvent(ScanScreenEvent.OnActualizeRemoveAll) },
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
                onClick = { onEvent(ScanScreenEvent.OnActualizeDismiss) }
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

}

@Composable
internal fun LostSearchDialog(
    state: ScanScreenState,
    onEvent: (ScanScreenEvent) -> Unit,
) {
// Lost data search dialog
if (state.isLostSearchDialogVisible) {
    LostDataSearchDialog(
        state = state,
        onEvent = onEvent,
    )
}

}

@Composable
internal fun AuthOrphanDialog(
    state: ScanScreenState,
    onEvent: (ScanScreenEvent) -> Unit,
) {
// Auth orphan check dialog
if (state.isAuthOrphanDialogVisible) {
    AlertDialog(
        onDismissRequest = { onEvent(ScanScreenEvent.OnAuthCheckDismiss) },
        title = { Text(stringResource(Res.string.auth_check_title)) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isCheckingAuthOrphans) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp).width(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(Res.string.auth_check_searching))
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.auth_check_description),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = stringResource(Res.string.auth_check_orphan_count, state.authOrphanEmails.size),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = state.authOrphanFilter,
                        onValueChange = { onEvent(ScanScreenEvent.OnAuthCheckFilterChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(stringResource(Res.string.auth_check_hint)) },
                        singleLine = true,
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val filtered = state.authOrphanEmails.filter { email ->
                        state.authOrphanFilter.isEmpty() || email.contains(state.authOrphanFilter, ignoreCase = true)
                    }

                    if (filtered.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.auth_check_no_orphans),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp),
                        )
                    } else {
                        val copyTextToClipboard = rememberCopyTextToClipboard()
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            items(filtered) { email ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = email,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                    )
                                    IconButton(
                                        onClick = {
                                            copyTextToClipboard(email)
                                        },
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = null,
                                            modifier = Modifier.height(16.dp).width(16.dp),
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = { onEvent(ScanScreenEvent.OnAuthCheckDeleteSingle(email)) },
                                    ) {
                                        Text(stringResource(Res.string.auth_check_remove_single))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!state.isCheckingAuthOrphans && state.authOrphanEmails.isNotEmpty()) {
                Button(
                    onClick = { onEvent(ScanScreenEvent.OnAuthCheckDeleteAll) },
                    enabled = !state.isDeletingAuthOrphans,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    if (state.isDeletingAuthOrphans) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(16.dp).width(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onError,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(stringResource(Res.string.auth_check_remove_all))
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = { onEvent(ScanScreenEvent.OnAuthCheckDismiss) }
            ) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
    }
}

