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

import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


internal class test_InputLocation_singleLine {

    private companion object {
        val S = runtimeRuleSet {
            concatenation("S") {
                literal("a")
                literal("b")
                literal("c")
            }
        }
    }

    @Test
    fun abc() {
        val sp = ScanOnDemandParser(S)

        val result = sp.parseForGoal("S", "abc")

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 3), result.sppt!!.root.location)
        assertEquals(InputLocation(0, 1, 1, 1), result.sppt!!.root.asBranch.children[0].location)
        assertEquals(InputLocation(1, 2, 1, 1), result.sppt!!.root.asBranch.children[1].location)
        assertEquals(InputLocation(2, 3, 1, 1), result.sppt!!.root.asBranch.children[2].location)

    }

}

class test_InputLocation_multiLine {
    private companion object {
        val S = runtimeRuleSet {
            concatenation("WS", true) { pattern("\\s+") }
            concatenation("S") {
                literal("a")
                literal("b")
                literal("c")
            }
        }
    }

    @Test
    fun abc() {
        val sp = ScanOnDemandParser(S)

        val result = sp.parseForGoal("S", "abc")

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 3), result.sppt!!.root.location)
        assertEquals(InputLocation(0, 1, 1, 1), result.sppt!!.root.asBranch.children[0].location)
        assertEquals(InputLocation(1, 2, 1, 1), result.sppt!!.root.asBranch.children[1].location)
        assertEquals(InputLocation(2, 3, 1, 1), result.sppt!!.root.asBranch.children[2].location)

    }

    @Test
    fun a_b_c() {
        val sp = ScanOnDemandParser(S)

        val result = sp.parseForGoal("S", "a b c")

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 5), result.sppt!!.root.location)
        assertEquals(InputLocation(0, 1, 1, 1), result.sppt!!.root.asBranch.children[0].location)
        assertEquals(InputLocation(1, 2, 1, 1), result.sppt!!.root.asBranch.children[1].location)
        assertEquals(InputLocation(2, 3, 1, 1), result.sppt!!.root.asBranch.children[2].location)
        assertEquals(InputLocation(3, 4, 1, 1), result.sppt!!.root.asBranch.children[3].location)
        assertEquals(InputLocation(4, 5, 1, 1), result.sppt!!.root.asBranch.children[4].location)

    }


    @Test
    fun aNLbNLc() {
        val sp = ScanOnDemandParser(S)

        val result = sp.parseForGoal(
            "S", """
            a
            b
            c
        """.trimIndent()
        )

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 5), result.sppt!!.root.location)
        assertEquals("a", result.sppt!!.root.asBranch.children[0].matchedText)
        assertEquals(InputLocation(0, 1, 1, 1), result.sppt!!.root.asBranch.children[0].location)
        assertEquals("\n", result.sppt!!.root.asBranch.children[1].matchedText)
        assertEquals(InputLocation(1, 2, 1, 1), result.sppt!!.root.asBranch.children[1].location)
        assertEquals("b", result.sppt!!.root.asBranch.children[2].matchedText)
        assertEquals(InputLocation(2, 1, 2, 1), result.sppt!!.root.asBranch.children[2].location)
        assertEquals("\n", result.sppt!!.root.asBranch.children[3].matchedText)
        assertEquals(InputLocation(3, 2, 2, 1), result.sppt!!.root.asBranch.children[3].location)
        assertEquals("c", result.sppt!!.root.asBranch.children[4].matchedText)
        assertEquals(InputLocation(4, 1, 3, 1), result.sppt!!.root.asBranch.children[4].location)

    }
}

class test_InputLocation_multiLine2 {
    private companion object {
        val S = runtimeRuleSet {
            concatenation("WS", true) { pattern("\\s+") }
            concatenation("S") {
                literal("aaa")
                literal("bbb")
                literal("ccc")
            }
        }
    }

    @Test
    fun abc() {
        val sp = ScanOnDemandParser(S)

        val result = sp.parseForGoal("S", "aaabbbccc")

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 9), result.sppt!!.root.location)
        assertEquals(InputLocation(0, 1, 1, 3), result.sppt!!.root.asBranch.children[0].location)
        assertEquals(InputLocation(3, 4, 1, 3), result.sppt!!.root.asBranch.children[1].location)
        assertEquals(InputLocation(6, 7, 1, 3), result.sppt!!.root.asBranch.children[2].location)

    }

    @Test
    fun a_b_c() {
        val sp = ScanOnDemandParser(S)

        val result = sp.parseForGoal("S", "aaa bbb ccc")

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 11), result.sppt!!.root.location)
        assertEquals(InputLocation(0, 1, 1, 3), result.sppt!!.root.asBranch.children[0].location)
        assertEquals(InputLocation(3, 4, 1, 1), result.sppt!!.root.asBranch.children[1].location)
        assertEquals(InputLocation(4, 5, 1, 3), result.sppt!!.root.asBranch.children[2].location)
        assertEquals(InputLocation(7, 8, 1, 1), result.sppt!!.root.asBranch.children[3].location)
        assertEquals(InputLocation(8, 9, 1, 3), result.sppt!!.root.asBranch.children[4].location)

    }

    @Test
    fun aNLbNLc() {
        val sp = ScanOnDemandParser(S)

        val result = sp.parseForGoal(
            "S", """
            aaa
            bbb
            ccc
        """.trimIndent()
        )

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 11), result.sppt!!.root.location)
        assertEquals("aaa", result.sppt!!.root.asBranch.children[0].matchedText)
        assertEquals(InputLocation(0, 1, 1, 3), result.sppt!!.root.asBranch.children[0].location)
        assertEquals("\n", result.sppt!!.root.asBranch.children[1].matchedText)
        assertEquals(InputLocation(3, 4, 1, 1), result.sppt!!.root.asBranch.children[1].location)
        assertEquals("bbb", result.sppt!!.root.asBranch.children[2].matchedText)
        assertEquals(InputLocation(4, 1, 2, 3), result.sppt!!.root.asBranch.children[2].location)
        assertEquals("\n", result.sppt!!.root.asBranch.children[3].matchedText)
        assertEquals(InputLocation(7, 4, 2, 1), result.sppt!!.root.asBranch.children[3].location)
        assertEquals("ccc", result.sppt!!.root.asBranch.children[4].matchedText)
        assertEquals(InputLocation(8, 1, 3, 3), result.sppt!!.root.asBranch.children[4].location)

    }

    @Test
    fun NLaNLbNLc() {
        val sp = ScanOnDemandParser(S)
        val sentence = """
            
            aaa
            bbb
            ccc
        """.trimIndent()
        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 12), result.sppt!!.root.location)
        assertEquals("\n", result.sppt!!.root.asBranch.children[0].matchedText)
        assertEquals(InputLocation(0, 1, 1, 1), result.sppt!!.root.asBranch.children[0].location)
        assertEquals("aaa", result.sppt!!.root.asBranch.children[1].matchedText)
        assertEquals(InputLocation(1, 1, 2, 3), result.sppt!!.root.asBranch.children[1].location)
        assertEquals("\n", result.sppt!!.root.asBranch.children[2].matchedText)
        assertEquals(InputLocation(4, 4, 2, 1), result.sppt!!.root.asBranch.children[2].location)
        assertEquals("bbb", result.sppt!!.root.asBranch.children[3].matchedText)
        assertEquals(InputLocation(5, 1, 3, 3), result.sppt!!.root.asBranch.children[3].location)
        assertEquals("\n", result.sppt!!.root.asBranch.children[4].matchedText)
        assertEquals(InputLocation(8, 4, 3, 1), result.sppt!!.root.asBranch.children[4].location)
        assertEquals("ccc", result.sppt!!.root.asBranch.children[5].matchedText)
        assertEquals(InputLocation(9, 1, 4, 3), result.sppt!!.root.asBranch.children[5].location)

    }

    @Test
    fun aNLbSPSPNLc() {
        val sp = ScanOnDemandParser(S)

        val result = sp.parseForGoal(
            "S", """
            aaa
              bbb
            ccc
        """.trimIndent()
        )

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 13), result.sppt!!.root.location)
        assertEquals("aaa", result.sppt!!.root.asBranch.children[0].matchedText)
        assertEquals(InputLocation(0, 1, 1, 3), result.sppt!!.root.asBranch.children[0].location)
        assertEquals("\n  ", result.sppt!!.root.asBranch.children[1].matchedText)
        assertEquals(InputLocation(3, 4, 1, 3), result.sppt!!.root.asBranch.children[1].location)
        assertEquals("bbb", result.sppt!!.root.asBranch.children[2].matchedText)
        assertEquals(InputLocation(6, 3, 2, 3), result.sppt!!.root.asBranch.children[2].location)
        assertEquals("\n", result.sppt!!.root.asBranch.children[3].matchedText)
        assertEquals(InputLocation(9, 6, 2, 1), result.sppt!!.root.asBranch.children[3].location)
        assertEquals("ccc", result.sppt!!.root.asBranch.children[4].matchedText)
        assertEquals(InputLocation(10, 1, 3, 3), result.sppt!!.root.asBranch.children[4].location)

    }
}
