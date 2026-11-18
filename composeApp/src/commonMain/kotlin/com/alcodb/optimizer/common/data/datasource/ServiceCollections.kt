package com.alcodb.optimizer.common.data.datasource

/**
 * Root Firestore collections that belong to the service schema rather than to a user calendar.
 * They must never be scanned as user collections and must never be deleted by the optimizer.
 *
 * The list mirrors the storage schema of the Calendar app:
 * - `Collection_of_all_users` — legacy user registry (`Users` document),
 * - `users` — uid-keyed user profiles and their subcollections (`data`, `settings`, `legacySync`),
 * - `anonymous_user` — data of anonymous sessions,
 * - `email_index` — email to uid mapping,
 * - `Avatars`/`AvatarLarge` — user avatars,
 * - `Global Notifications` — global announcements shown by the Calendar app.
 */
object ServiceCollections {
    val names: Set<String> = setOf(
        "Collection_of_all_users",
        "users",
        "anonymous_user",
        "email_index",
        "Avatars",
        "AvatarLarge",
        "Global Notifications",
    )

    fun isService(collection: String): Boolean = collection in names
}
