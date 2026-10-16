package net.akehurst.datastore.filesystem

import kotlin.test.Test
import kotlin.test.assertNotNull

class test_FilesystemProvider {


    @Test
    fun provide() {

        val sut = FilesystemProvider()
        val config = datastoreConfiguration {

        }
        val ds = sut.provide(config)

        assertNotNull(ds)
    }

}