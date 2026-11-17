package net.akehurst.datastore.api

import kotlin.test.Test
import kotlin.test.assertEquals

class test_DatastorePlatform_jvm {
    @Test
    fun current_is_JVM() {
        assertEquals(DatastorePlatform.JVM, DatastorePlatform.current)
    }
}
