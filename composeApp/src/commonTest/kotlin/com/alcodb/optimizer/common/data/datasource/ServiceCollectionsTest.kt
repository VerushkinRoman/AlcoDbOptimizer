package com.alcodb.optimizer.common.data.datasource

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServiceCollectionsTest {
    @Test
    fun allServiceCollectionsAreRecognized() {
        val expected = setOf(
            "Collection_of_all_users",
            "users",
            "anonymous_user",
            "email_index",
            "Avatars",
            "AvatarLarge",
            "Global Notifications",
        )

        assertTrue(ServiceCollections.names.containsAll(expected))
        expected.forEach { name ->
            assertTrue(ServiceCollections.isService(name), "$name must be protected")
        }
    }

    @Test
    fun regularUserCollectionIsNotService() {
        assertFalse(ServiceCollections.isService("user@example.com"))
        assertFalse(ServiceCollections.isService("anonymous-user-id"))
    }
}
