package com.alcodb.optimizer.common.data.datasource

/**
 * Полный контракт Firestore. Композиция узких интерфейсов: потребители
 * зависят только от нужной им части.
 */
interface FirestoreDataSource :
    FirestoreDocumentDataSource,
    FirebaseAuthDataSource,
    RegistryDataSource
