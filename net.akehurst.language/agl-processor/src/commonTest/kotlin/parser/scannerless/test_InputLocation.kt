package net.akehurst.language.parser.scannerless

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


class test_InputLocation_singleLine {

    val S = runtimeRuleSet {
        concatenation("S") {
            literal("a")
            literal("b")
            literal("c")
        }
    }

    @Test
    fun abc() {
        val sp = ScannerlessParser(S)

        val actual = sp.parse("S", "abc")

        assertNotNull(actual)
        assertEquals(InputLocation(0, 1, 1, 3), actual.root.location)
        assertEquals(InputLocation(0, 1, 1, 1), actual.root.asBranch.children[0].location)
        assertEquals(InputLocation(1, 2, 1, 1), actual.root.asBranch.children[1].location)
        assertEquals(InputLocation(2, 3, 1, 1), actual.root.asBranch.children[2].location)
    }

}

class test_InputLocation_multiLine {

    val S = runtimeRuleSet {
        skip("WS") {
            pattern("\\s+")
        }
        concatenation("S") {
            literal("a")
            literal("b")
            literal("c")
        }
    }

    @Test
    fun abc() {
        val sp = ScannerlessParser(S)

        val actual = sp.parse("S", "abc")

        assertNotNull(actual)
        assertEquals(InputLocation(0, 1, 1, 3), actual.root.location)
        assertEquals(InputLocation(0, 1, 1, 1), actual.root.asBranch.children[0].location)
        assertEquals(InputLocation(1, 2, 1, 1), actual.root.asBranch.children[1].location)
        assertEquals(InputLocation(2, 3, 1, 1), actual.root.asBranch.children[2].location)
    }

    @Test
    fun a_b_c() {
        val sp = ScannerlessParser(S)

        val actual = sp.parse("S", "a b c")

        assertNotNull(actual)
        assertEquals(InputLocation(0, 1, 1, 5), actual.root.location)
        assertEquals(InputLocation(0, 1, 1, 1), actual.root.asBranch.children[0].location)
        assertEquals(InputLocation(1, 2, 1, 1), actual.root.asBranch.children[1].location)
        assertEquals(InputLocation(2, 3, 1, 1), actual.root.asBranch.children[2].location)
        assertEquals(InputLocation(3, 4, 1, 1), actual.root.asBranch.children[3].location)
        assertEquals(InputLocation(4, 5, 1, 1), actual.root.asBranch.children[4].location)
    }


    @Test
    fun aNLbNLc() {
        val sp = ScannerlessParser(S)

        val actual = sp.parse("S", """
            a
            b
            c
        """.trimIndent())

        assertNotNull(actual)
        assertEquals(InputLocation(0, 1, 1, 5), actual.root.location)
        assertEquals("a", actual.root.asBranch.children[0].matchedText)
        assertEquals(InputLocation(0, 1, 1, 1), actual.root.asBranch.children[0].location)
        assertEquals("\n", actual.root.asBranch.children[1].matchedText)
        assertEquals(InputLocation(1, 2, 1, 1), actual.root.asBranch.children[1].location)
        assertEquals("b", actual.root.asBranch.children[2].matchedText)
        assertEquals(InputLocation(2, 1, 2, 1), actual.root.asBranch.children[2].location)
        assertEquals("\n", actual.root.asBranch.children[3].matchedText)
        assertEquals(InputLocation(3, 2, 2, 1), actual.root.asBranch.children[3].location)
        assertEquals("c", actual.root.asBranch.children[4].matchedText)
        assertEquals(InputLocation(4, 1, 3, 1), actual.root.asBranch.children[4].location)
    }
}

class test_InputLocation_multiLine2 {

    val S = runtimeRuleSet {
        skip("WS") {
            pattern("\\s+")
        }
        concatenation("S") {
            literal("aaa")
            literal("bbb")
            literal("ccc")
        }
    }

    @Test
    fun abc() {
        val sp = ScannerlessParser(S)

        val actual = sp.parse("S", "aaabbbccc")

        assertNotNull(actual)
        assertEquals(InputLocation(0, 1, 1, 9), actual.root.location)
        assertEquals(InputLocation(0, 1, 1, 3), actual.root.asBranch.children[0].location)
        assertEquals(InputLocation(3, 4, 1, 3), actual.root.asBranch.children[1].location)
        assertEquals(InputLocation(6, 7, 1, 3), actual.root.asBranch.children[2].location)
    }

    @Test
    fun a_b_c() {
        val sp = ScannerlessParser(S)

        val actual = sp.parse("S", "aaa bbb ccc")

        assertNotNull(actual)
        assertEquals(InputLocation(0, 1, 1, 11), actual.root.location)
        assertEquals(InputLocation(0, 1, 1, 3), actual.root.asBranch.children[0].location)
        assertEquals(InputLocation(3, 4, 1, 1), actual.root.asBranch.children[1].location)
        assertEquals(InputLocation(4, 5, 1, 3), actual.root.asBranch.children[2].location)
        assertEquals(InputLocation(7, 8, 1, 1), actual.root.asBranch.children[3].location)
        assertEquals(InputLocation(8, 9, 1, 3), actual.root.asBranch.children[4].location)
    }


    @Test
    fun aNLbNLc() {
        val sp = ScannerlessParser(S)

        val actual = sp.parse("S", """
            aaa
            bbb
            ccc
        """.trimIndent())

        assertNotNull(actual)
        assertEquals(InputLocation(0, 1, 1, 11), actual.root.location)
        assertEquals("aaa", actual.root.asBranch.children[0].matchedText)
        assertEquals(InputLocation(0, 1, 1, 3), actual.root.asBranch.children[0].location)
        assertEquals("\n", actual.root.asBranch.children[1].matchedText)
        assertEquals(InputLocation(3, 4, 1, 1), actual.root.asBranch.children[1].location)
        assertEquals("bbb", actual.root.asBranch.children[2].matchedText)
        assertEquals(InputLocation(4, 1, 2, 3), actual.root.asBranch.children[2].location)
        assertEquals("\n", actual.root.asBranch.children[3].matchedText)
        assertEquals(InputLocation(7, 4, 2, 1), actual.root.asBranch.children[3].location)
        assertEquals("ccc", actual.root.asBranch.children[4].matchedText)
        assertEquals(InputLocation(8, 1, 3, 3), actual.root.asBranch.children[4].location)
    }

    @Test
    fun NLaNLbNLc() {
        val sp = ScannerlessParser(S)

        val actual = sp.parse("S", """
            
            aaa
            bbb
            ccc
        """.trimIndent())

        assertNotNull(actual)
        assertEquals(InputLocation(0, 1, 1, 12), actual.root.location)
        assertEquals("\n", actual.root.asBranch.children[0].matchedText)
        assertEquals(InputLocation(0, 1, 1, 1), actual.root.asBranch.children[0].location)
        assertEquals("aaa", actual.root.asBranch.children[1].matchedText)
        assertEquals(InputLocation(1, 1, 2, 3), actual.root.asBranch.children[1].location)
        assertEquals("\n", actual.root.asBranch.children[2].matchedText)
        assertEquals(InputLocation(4, 4, 2, 1), actual.root.asBranch.children[2].location)
        assertEquals("bbb", actual.root.asBranch.children[3].matchedText)
        assertEquals(InputLocation(5, 1, 3, 3), actual.root.asBranch.children[3].location)
        assertEquals("\n", actual.root.asBranch.children[4].matchedText)
        assertEquals(InputLocation(8, 4, 3, 1), actual.root.asBranch.children[4].location)
        assertEquals("ccc", actual.root.asBranch.children[5].matchedText)
        assertEquals(InputLocation(9, 1, 4, 3), actual.root.asBranch.children[5].location)
    }
}
