package com.alcodb.optimizer.feature_scan.compose.components

import alcodboptimizer.composeapp.generated.resources.Res
import alcodboptimizer.composeapp.generated.resources.*
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.alcodb.optimizer.feature_scan.domain.models.ExpectedDay
import com.alcodb.optimizer.feature_scan.domain.models.ExpectedDrinkType
import com.alcodb.optimizer.feature_scan.domain.models.FoundCollection
import com.alcodb.optimizer.feature_scan.presentation.models.LostSearchDateTarget
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenEvent
import com.alcodb.optimizer.feature_scan.presentation.models.ScanScreenState
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LostDataSearchDialog(
    state: ScanScreenState,
    onEvent: (ScanScreenEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onEvent(ScanScreenEvent.OnLostSearchDismiss) },
        title = { Text(stringResource(Res.string.lost_search_title)) },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    BoundarySection(
                        label = stringResource(Res.string.lost_search_start),
                        date = state.lostSearchStartDate,
                        exact = state.lostSearchStartExact,
                        months = state.lostSearchStartMonths,
                        onPick = {
                            onEvent(ScanScreenEvent.OnLostSearchDatePickerOpen(LostSearchDateTarget.Start))
                        },
                        onExactChanged = {
                            onEvent(ScanScreenEvent.OnLostSearchStartExactChanged(it))
                        },
                        onMonthsClick = {
                            onEvent(ScanScreenEvent.OnLostSearchMonthsDialogOpen(forStart = true))
                        },
                    )
                }

                item {
                    BoundarySection(
                        label = stringResource(Res.string.lost_search_end),
                        date = state.lostSearchEndDate,
                        exact = state.lostSearchEndExact,
                        months = state.lostSearchEndMonths,
                        onPick = {
                            onEvent(ScanScreenEvent.OnLostSearchDatePickerOpen(LostSearchDateTarget.End))
                        },
                        onExactChanged = {
                            onEvent(ScanScreenEvent.OnLostSearchEndExactChanged(it))
                        },
                        onMonthsClick = {
                            onEvent(ScanScreenEvent.OnLostSearchMonthsDialogOpen(forStart = false))
                        },
                    )
                }

                item {
                    Text(
                        text = stringResource(Res.string.lost_search_days),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }

                items(state.lostSearchExpectedDays, key = { it.date.toString() }) { day ->
                    ExpectedDayRow(
                        day = day,
                        onRemove = { onEvent(ScanScreenEvent.OnLostSearchRemoveDay(day.date)) },
                        onExactChanged = {
                            onEvent(ScanScreenEvent.OnLostSearchDayExactChanged(day.date, it))
                        },
                        onToleranceChanged = {
                            onEvent(ScanScreenEvent.OnLostSearchDayToleranceChanged(day.date, it))
                        },
                        onDrinkTypeChanged = {
                            onEvent(ScanScreenEvent.OnLostSearchDayDrinkTypeChanged(day.date, it))
                        },
                    )
                }

                item {
                    OutlinedButton(
                        onClick = {
                            onEvent(ScanScreenEvent.OnLostSearchDatePickerOpen(LostSearchDateTarget.New))
                        },
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(Res.string.lost_search_add_date))
                    }
                }

                if (state.isLostSearchRunning) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.height(20.dp).width(20.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(Res.string.lost_search_running))
                        }
                    }
                }

                if (state.lostSearchHasRun) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(
                                    Res.string.lost_search_found,
                                    state.lostSearchResults.size,
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = stringResource(
                                    Res.string.lost_search_progress,
                                    state.lostSearchChecked,
                                    state.lostSearchTotal,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (state.lostSearchStoppedByLimit) {
                                Text(
                                    text = stringResource(Res.string.lost_search_limit),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    if (state.lostSearchResults.isEmpty()) {
                        item {
                            Text(
                                text = stringResource(Res.string.lost_search_no_results),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        items(state.lostSearchResults, key = { it.id }) { collection ->
                            FoundCollectionCard(collection)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onEvent(ScanScreenEvent.OnLostSearchRun) },
                enabled = !state.isLostSearchRunning && (
                    state.lostSearchStartDate != null ||
                        state.lostSearchEndDate != null ||
                        state.lostSearchExpectedDays.isNotEmpty()
                    ),
            ) {
                Text(stringResource(Res.string.lost_search_run))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onEvent(ScanScreenEvent.OnLostSearchDismiss) }) {
                Text(stringResource(Res.string.lost_search_close))
            }
        },
    )

    if (state.isLostSearchMonthsDialogVisible) {
        MonthsDialog(state = state, onEvent = onEvent)
    }

    state.lostSearchDatePickerTarget?.let { target ->
        val initialMillis = when (target) {
            LostSearchDateTarget.Start -> state.lostSearchStartDate?.toUtcMillis()
            LostSearchDateTarget.End -> state.lostSearchEndDate?.toUtcMillis()
            LostSearchDateTarget.New -> null
        }
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { onEvent(ScanScreenEvent.OnLostSearchDatePickerDismiss) },
            confirmButton = {
                Button(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val date = Instant.fromEpochMilliseconds(millis)
                                .toLocalDateTime(TimeZone.UTC)
                                .date
                            when (target) {
                                LostSearchDateTarget.Start -> onEvent(
                                    ScanScreenEvent.OnLostSearchStartDateChanged(date),
                                )
                                LostSearchDateTarget.End -> onEvent(
                                    ScanScreenEvent.OnLostSearchEndDateChanged(date),
                                )
                                LostSearchDateTarget.New -> onEvent(
                                    ScanScreenEvent.OnLostSearchAddDay(date),
                                )
                            }
                        }
                        onEvent(ScanScreenEvent.OnLostSearchDatePickerDismiss)
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) {
                    Text(stringResource(Res.string.lost_search_ok))
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { onEvent(ScanScreenEvent.OnLostSearchDatePickerDismiss) }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun BoundarySection(
    label: String,
    date: LocalDate?,
    exact: Boolean,
    months: Int,
    onPick: () -> Unit,
    onExactChanged: (Boolean) -> Unit,
    onMonthsClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
        )
        OutlinedButton(onClick = onPick, modifier = Modifier.fillMaxWidth()) {
            Text(date?.let(::formatDate) ?: stringResource(Res.string.lost_search_pick_date))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = exact,
                onClick = { onExactChanged(true) },
                label = { Text(stringResource(Res.string.lost_search_exact)) },
            )
            FilterChip(
                selected = !exact,
                onClick = { onExactChanged(false) },
                label = { Text(stringResource(Res.string.lost_search_approx)) },
            )
            if (!exact) {
                OutlinedButton(onClick = onMonthsClick) {
                    Text(stringResource(Res.string.lost_search_months, months))
                }
            }
        }
    }
}

@Composable
private fun ExpectedDayRow(
    day: ExpectedDay,
    onRemove: () -> Unit,
    onExactChanged: (Boolean) -> Unit,
    onToleranceChanged: (Int) -> Unit,
    onDrinkTypeChanged: (ExpectedDrinkType) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatDate(day.date),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(Res.string.lost_search_remove_date),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = day.exact,
                    onClick = { onExactChanged(true) },
                    label = { Text(stringResource(Res.string.lost_search_exact)) },
                )
                FilterChip(
                    selected = !day.exact,
                    onClick = { onExactChanged(false) },
                    label = { Text(stringResource(Res.string.lost_search_approx)) },
                )
                if (!day.exact) {
                    OutlinedTextField(
                        value = day.dayTolerance.toString(),
                        onValueChange = { value ->
                            value.filter { it.isDigit() }.toIntOrNull()?.let(onToleranceChanged)
                        },
                        modifier = Modifier.width(110.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text(stringResource(Res.string.lost_search_days_tolerance)) },
                        textStyle = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = day.drinkType == ExpectedDrinkType.Full,
                    onClick = { onDrinkTypeChanged(ExpectedDrinkType.Full) },
                    label = { Text(stringResource(Res.string.lost_search_many)) },
                )
                FilterChip(
                    selected = day.drinkType == ExpectedDrinkType.Half,
                    onClick = { onDrinkTypeChanged(ExpectedDrinkType.Half) },
                    label = { Text(stringResource(Res.string.lost_search_half)) },
                )
            }
        }
    }
}

@Composable
private fun FoundCollectionCard(collection: FoundCollection) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            Text(
                text = collection.id,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
            )
            if (collection.firstDate != null && collection.lastDate != null) {
                Text(
                    text = stringResource(
                        Res.string.lost_search_range,
                        formatDate(collection.firstDate),
                        formatDate(collection.lastDate),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Text(
                text = stringResource(
                    Res.string.lost_search_counts,
                    collection.totalDays,
                    collection.drinkDays,
                ),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun MonthsDialog(
    state: ScanScreenState,
    onEvent: (ScanScreenEvent) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onEvent(ScanScreenEvent.OnLostSearchMonthsDismiss) },
        title = { Text(stringResource(Res.string.lost_search_months_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.lost_search_months_description))
                OutlinedTextField(
                    value = state.lostSearchMonthsInput,
                    onValueChange = {
                        onEvent(ScanScreenEvent.OnLostSearchMonthsInputChanged(it))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    label = { Text(stringResource(Res.string.lost_search_months_hint)) },
                )
            }
        },
        confirmButton = {
            Button(onClick = { onEvent(ScanScreenEvent.OnLostSearchMonthsConfirm) }) {
                Text(stringResource(Res.string.lost_search_ok))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onEvent(ScanScreenEvent.OnLostSearchMonthsDismiss) }) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}

private fun formatDate(date: LocalDate): String =
    "${date.day}.${date.month.ordinal + 1}.${date.year}"

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()
