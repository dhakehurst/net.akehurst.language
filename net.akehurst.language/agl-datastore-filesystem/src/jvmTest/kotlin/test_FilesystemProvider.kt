package net.akehurst.datastore.filesystem

import net.akehurst.datastore.filesystem.FilesystemProvider
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_FilesystemProvider {

    @Test
    fun construct() {
        val sut = FilesystemProvider()

        assertNotNull(sut)
    }

    @Test
    fun identity() {
        val sut = FilesystemProvider()

        val actual = sut.identity
        assertEquals("net.akehurst.datastore.filesystem.FilesystemProvider", actual.value)
    }
}