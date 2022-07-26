package test

import kotlin.test.Test
import kotlin.test.assertTrue

// this enables the kotlin JS test folder to be generated with all needed files
// the tests are overwritten with custom JS test code
// in order to check the JS API
class test_Dummy {

    @Test
    fun t() {
        assertTrue(true)
    }
}