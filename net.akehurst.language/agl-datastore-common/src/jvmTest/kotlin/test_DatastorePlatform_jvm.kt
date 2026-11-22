package net.akehurst.datastore.common

import net.akehurst.datastore.api.DatastorePlatform

import kotlin.test.Test
import kotlin.test.assertEquals

class test_DatastorePlatform_jvm {
    @Test
    fun current_is_JVM() {
        assertEquals(DatastorePlatform.JVM, DatastorePlatform.current)
    }
}
