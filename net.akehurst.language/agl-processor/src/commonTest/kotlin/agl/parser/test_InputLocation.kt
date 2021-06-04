/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.parser

import net.akehurst.language.agl.automaton.AutomatonKind
import net.akehurst.language.agl.parser.ScanOnDemandParser
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
        val sp = ScanOnDemandParser(S)

        val actual = sp.parse("S", "abc", AutomatonKind.LOOKAHEAD_1)

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
        val sp = ScanOnDemandParser(S)

        val actual = sp.parse("S", "abc", AutomatonKind.LOOKAHEAD_1)

        assertNotNull(actual)
        assertEquals(InputLocation(0, 1, 1, 3), actual.root.location)
        assertEquals(InputLocation(0, 1, 1, 1), actual.root.asBranch.children[0].location)
        assertEquals(InputLocation(1, 2, 1, 1), actual.root.asBranch.children[1].location)
        assertEquals(InputLocation(2, 3, 1, 1), actual.root.asBranch.children[2].location)
    }

    @Test
    fun a_b_c() {
        val sp = ScanOnDemandParser(S)

        val actual = sp.parse("S", "a b c", AutomatonKind.LOOKAHEAD_1)

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
        val sp = ScanOnDemandParser(S)

        val actual = sp.parse("S", """
            a
            b
            c
        """.trimIndent(), AutomatonKind.LOOKAHEAD_1)

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
        val sp = ScanOnDemandParser(S)

        val actual = sp.parse("S", "aaabbbccc", AutomatonKind.LOOKAHEAD_1)

        assertNotNull(actual)
        assertEquals(InputLocation(0, 1, 1, 9), actual.root.location)
        assertEquals(InputLocation(0, 1, 1, 3), actual.root.asBranch.children[0].location)
        assertEquals(InputLocation(3, 4, 1, 3), actual.root.asBranch.children[1].location)
        assertEquals(InputLocation(6, 7, 1, 3), actual.root.asBranch.children[2].location)
    }

    @Test
    fun a_b_c() {
        val sp = ScanOnDemandParser(S)

        val actual = sp.parse("S", "aaa bbb ccc", AutomatonKind.LOOKAHEAD_1)

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
        val sp = ScanOnDemandParser(S)

        val actual = sp.parse("S", """
            aaa
            bbb
            ccc
        """.trimIndent(), AutomatonKind.LOOKAHEAD_1)

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
        val sp = ScanOnDemandParser(S)
        val sentence = """
            
            aaa
            bbb
            ccc
        """.trimIndent()
        val actual = sp.parse("S", sentence, AutomatonKind.LOOKAHEAD_1)

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
