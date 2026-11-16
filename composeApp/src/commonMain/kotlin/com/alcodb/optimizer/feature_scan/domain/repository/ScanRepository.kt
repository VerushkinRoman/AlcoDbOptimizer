package com.alcodb.optimizer.feature_scan.domain.repository

/**
 * Полный контракт фичи сканирования. Представляет собой композицию узких
 * интерфейсов, чтобы потребители зависели только от нужной им части.
 */
interface ScanRepository :
    ScanSessionRepository,
    UserDeletionRepository,
    BackupRepository,
    RegistryCleanupRepository,
    AuthCleanupRepository,
    LostDataSearchRepository
