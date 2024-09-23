package bugs

import bugs.VC.Companion.asVC2companion
import kotlin.test.Test
import kotlin.test.assertNotNull

class test_JsValueClassIssue {

    @Test
    fun test2() {
        val str = "hello"

        val vc1 = str.asVC2topFun
        assertNotNull(vc1.value)

        val vc2 = str.asVC2companion
        assertNotNull(vc2.value)
    }

}