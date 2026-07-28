package com.alcodb.optimizer.feature_scan.presentation.models

import com.alcodb.optimizer.feature_scan.domain.models.BackupEntry
import com.alcodb.optimizer.feature_scan.domain.models.InactiveUser
import com.alcodb.optimizer.feature_scan.domain.models.OrphanedRegistryUser

data class ScanScreenState(
    val inactiveUsers: List<InactiveUser> = emptyList(),
    val isScanning: Boolean = false,
    val isDeleting: Boolean = false,
    val deletedCount: Int = 0,
    val deletionTotal: Int = 0,
    val collectionsTotal: Int = 0,
    val collectionsProcessed: Int = 0,
    val readsUsed: Int = 0,
    val maxReads: Int = 1000,
    val readLimit: Int = 1000,
    val selectedUser: InactiveUser? = null,
    val isDeleteAllConfirmVisible: Boolean = false,
    val isDeleteSingleConfirmVisible: Boolean = false,
    val isInitializingFirebase: Boolean = true,
    val errorMessage: String? = null,
    val lastScanDate: Long = 0L,
    val canScanToday: Boolean = true,
    val isComplete: Boolean = false,
    val isRestoreDialogVisible: Boolean = false,
    val backupEntries: List<BackupEntry> = emptyList(),
    val backupFilter: String = "",
    val selectedBackupEntry: BackupEntry? = null,
    val isRestoring: Boolean = false,
    val isActualizeDialogVisible: Boolean = false,
    val isActualizing: Boolean = false,
    val orphanedRegistryUsers: List<OrphanedRegistryUser> = emptyList(),
    val totalRegistryUsers: Int = 0,
    val actualizeFilter: String = "",
    val isRemovingOrphans: Boolean = false,
)
