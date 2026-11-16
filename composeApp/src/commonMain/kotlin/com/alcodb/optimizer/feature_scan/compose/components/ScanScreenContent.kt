package com.alcodb.optimizer.feature_scan.compose.components

import alcodboptimizer.composeapp.generated.resources.Res
import alcodboptimizer.composeapp.generated.resources.actualize
import alcodboptimizer.composeapp.generated.resources.app_name
import alcodboptimizer.composeapp.generated.resources.auth_check
import alcodboptimizer.composeapp.generated.resources.collections_format
import alcodboptimizer.composeapp.generated.resources.continue_scan
import alcodboptimizer.composeapp.generated.resources.deleting
import alcodboptimizer.composeapp.generated.resources.deleting_progress
import alcodboptimizer.composeapp.generated.resources.filter_registered
import alcodboptimizer.composeapp.generated.resources.inactive_users_count
import alcodboptimizer.composeapp.generated.resources.lost_search
import alcodboptimizer.composeapp.generated.resources.no_inactive_users
import alcodboptimizer.composeapp.generated.resources.press_start_scan
import alcodboptimizer.composeapp.generated.resources.read_limit_label
import alcodboptimizer.composeapp.generated.resources.reads_format
import alcodboptimizer.composeapp.generated.resources.reset_all
import alcodboptimizer.composeapp.generated.resources.reset_counter
import alcodboptimizer.composeapp.generated.resources.restore
import alcodboptimizer.composeapp.generated.resources.scanning
import alcodboptimizer.composeapp.generated.resources.scanning_database
import alcodboptimizer.composeapp.generated.resources.start_scan
import alcodboptimizer.composeapp.generated.resources.stop
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenEvent
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenState
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ScanScreenContent(
    state: ScanScreenState,
    snackbarHostState: SnackbarHostState,
    onEvent: (ScanScreenEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
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
                            onEvent(ScanScreenEvent.OnReadLimitChanged(limit))
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
                    onClick = { onEvent(ScanScreenEvent.OnStartScan) },
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
                    onClick = { onEvent(ScanScreenEvent.OnStopScan) },
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
                onClick = { onEvent(ScanScreenEvent.OnDeleteAllClick) },
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
                onClick = { onEvent(ScanScreenEvent.OnResetCounter) },
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
                onClick = { onEvent(ScanScreenEvent.OnRestoreClick) },
                enabled = !state.isScanning,
                modifier = Modifier.weight(1f),
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(Res.string.restore))
            }

            OutlinedButton(
                onClick = { onEvent(ScanScreenEvent.OnActualizeClick) },
                enabled = !state.isScanning,
                modifier = Modifier.weight(1f),
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(stringResource(Res.string.actualize))
            }

            OutlinedButton(
                onClick = { onEvent(ScanScreenEvent.OnResetAll) },
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

        Spacer(modifier = Modifier.height(4.dp))

        // Action buttons - row 3: Auth Check
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = { onEvent(ScanScreenEvent.OnAuthCheckClick) },
                enabled = !state.isScanning,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.auth_check))
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Action buttons - row 4: Lost Data Search
        OutlinedButton(
            onClick = { onEvent(ScanScreenEvent.OnLostSearchClick) },
            enabled = !state.isScanning,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(Res.string.lost_search))
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.inactive_users_count, state.inactiveUsers.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = state.showOnlyRegistered,
                    onClick = { onEvent(ScanScreenEvent.OnToggleRegisteredOnly) },
                    label = { Text(stringResource(Res.string.filter_registered)) },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val filteredUsers = if (state.showOnlyRegistered) {
                state.inactiveUsers.filter { it.id.contains("@") }
            } else {
                state.inactiveUsers
            }

            if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.no_inactive_users),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filteredUsers) { user ->
                        UserListItem(
                            user = user,
                            onClick = { onEvent(ScanScreenEvent.OnUserClick(user)) },
                        )
                    }
                }
            }
        }
    }
}
}
