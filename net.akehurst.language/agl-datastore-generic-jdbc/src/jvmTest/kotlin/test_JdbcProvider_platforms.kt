package net.akehurst.datastore.generic.jdbc

import net.akehurst.datastore.api.DatastorePlatform
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_JdbcProvider_platforms {
    @Test
    fun jvm_only() {
        val p = JdbcProvider()
        assertEquals(setOf(DatastorePlatform.JVM), p.supportedPlatforms)
        assertTrue(p.isSupportedOnCurrentPlatform)
    }
}
