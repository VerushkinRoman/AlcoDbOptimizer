package com.alcodb.optimizer.feature_scan.domain.di

import com.alcodb.optimizer.feature_scan.domain.repository.ScanRepository
import org.kodein.di.DI

val scanDomainModule = DI.Module("ScanDomainModule") {
    // ScanRepository is provided by scanDataModule
}
