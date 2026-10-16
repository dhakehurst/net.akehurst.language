package net.akehurst.datastore.text

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_AglTextDsProvider {

    @Test
    fun construct() {
        val sut = AglTextProvider()

        assertNotNull(sut)
    }

    @Test
    fun identity() {
        val sut = AglTextProvider()

        val actual = sut.identity
        assertEquals("net.akehurst.datastore.text.AglTextProvider", actual.value)
    }
}