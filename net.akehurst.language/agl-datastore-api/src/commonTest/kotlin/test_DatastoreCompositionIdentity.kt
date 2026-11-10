package net.akehurst.datastore.api

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class test_DatastoreCompositionIdentity {

    @Test
    fun valid() {
        listOf("a", "Z", "doors-reqs", "doors_reqs", "a1", "A-1_b-2", "a--", "a__").forEach { v ->
            assertTrue(DatastoreCompositionIdentity.isValid(v), v)
            assertTrue(DatastoreCompositionIdentity(v).isValid, v)
        }
    }

    @Test
    fun invalid() {
        listOf("", " ", " a", "a ", "1a", "-a", "_a", "a b", "a.b", "a/b", "a\\b", "a:b", "a\$b", "ä", "aé", "a\n").forEach { v ->
            assertFalse(DatastoreCompositionIdentity.isValid(v), "'$v'")
            assertFalse(DatastoreCompositionIdentity(v).isValid, "'$v'")
        }
    }
}
