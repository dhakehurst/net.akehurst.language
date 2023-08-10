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
import net.akehurst.language.agl.sppt.TreeDataComplete
import net.akehurst.language.agl.syntaxAnalyser.locationIn
import net.akehurst.language.agl.syntaxAnalyser.matchedTextNoSkip
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.SpptDataNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

fun SpptDataNode.children(treeData: TreeDataComplete<SpptDataNode>, alternative: Int = 0) = treeData.childrenFor(this)[alternative].second

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
        val sentence = "abc"
        val sp = ScanOnDemandParser(S)

        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 3), result.sppt!!.treeData.root!!.locationIn(sentence))
        assertEquals(InputLocation(0, 1, 1, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0].locationIn(sentence))
        assertEquals(InputLocation(1, 2, 1, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1].locationIn(sentence))
        assertEquals(InputLocation(2, 3, 1, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2].locationIn(sentence))

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
        val sentence = "abc"
        val sp = ScanOnDemandParser(S)

        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 3), result.sppt!!.treeData.root!!.locationIn(sentence))
        assertEquals(InputLocation(0, 1, 1, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0].locationIn(sentence))
        assertEquals(InputLocation(1, 2, 1, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1].locationIn(sentence))
        assertEquals(InputLocation(2, 3, 1, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2].locationIn(sentence))

    }

    @Test
    fun a_b_c() {
        val sentence = "a b c"
        val sp = ScanOnDemandParser(S)

        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 5), result.sppt!!.treeData.root!!.locationIn(sentence))
        assertEquals(InputLocation(0, 1, 1, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0].locationIn(sentence))
        assertEquals(InputLocation(1, 2, 1, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1].locationIn(sentence))
        assertEquals(InputLocation(2, 3, 1, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2].locationIn(sentence))
        assertEquals(InputLocation(3, 4, 1, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[3].locationIn(sentence))
        assertEquals(InputLocation(4, 5, 1, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[4].locationIn(sentence))

    }


    @Test
    fun aNLbNLc() {
        val sentence = """
            a
            b
            c
        """.trimIndent()
        val sp = ScanOnDemandParser(S)

        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val sppt = result.sppt!!
        val userRoot = sppt.treeData.userRoot
        assertEquals(InputLocation(0, 1, 1, 5), userRoot.locationIn(sentence))
        assertEquals("a", userRoot.children(sppt.treeData)[0].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(0, 1, 1, 1), userRoot.children(sppt.treeData)[0].locationIn(sentence))
        assertEquals("\n", userRoot.children(sppt.treeData)[1].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(1, 2, 1, 1), userRoot.children(sppt.treeData)[1].locationIn(sentence))
        assertEquals("b", userRoot.children(sppt.treeData)[2].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(2, 1, 2, 1), userRoot.children(sppt.treeData)[2].locationIn(sentence))
        assertEquals("\n", userRoot.children(sppt.treeData)[3].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(3, 2, 2, 1), userRoot.children(sppt.treeData)[3].locationIn(sentence))
        assertEquals("c", userRoot.children(sppt.treeData)[4].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(4, 1, 3, 1), userRoot.children(sppt.treeData)[4].locationIn(sentence))

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
        val sentence = "aaabbbccc"
        val sp = ScanOnDemandParser(S)

        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 9), result.sppt!!.treeData.root!!.locationIn(sentence))
        assertEquals(InputLocation(0, 1, 1, 3), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0].locationIn(sentence))
        assertEquals(InputLocation(3, 4, 1, 3), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1].locationIn(sentence))
        assertEquals(InputLocation(6, 7, 1, 3), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2].locationIn(sentence))

    }

    @Test
    fun a_b_c() {
        val sentence = "aaa bbb ccc"
        val sp = ScanOnDemandParser(S)

        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 11), result.sppt!!.treeData.root!!.locationIn(sentence))
        assertEquals(InputLocation(0, 1, 1, 3), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0].locationIn(sentence))
        assertEquals(InputLocation(3, 4, 1, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1].locationIn(sentence))
        assertEquals(InputLocation(4, 5, 1, 3), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2].locationIn(sentence))
        assertEquals(InputLocation(7, 8, 1, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[3].locationIn(sentence))
        assertEquals(InputLocation(8, 9, 1, 3), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[4].locationIn(sentence))

    }

    @Test
    fun aNLbNLc() {
        val sentence = """
            aaa
            bbb
            ccc
        """.trimIndent()
        val sp = ScanOnDemandParser(S)

        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 11), result.sppt!!.treeData.root!!.locationIn(sentence))
        assertEquals("aaa", result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(0, 1, 1, 3), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0].locationIn(sentence))
        assertEquals("\n", result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(3, 4, 1, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1].locationIn(sentence))
        assertEquals("bbb", result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(4, 1, 2, 3), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2].locationIn(sentence))
        assertEquals("\n", result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[3].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(7, 4, 2, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[3].locationIn(sentence))
        assertEquals("ccc", result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[4].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(8, 1, 3, 3), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[4].locationIn(sentence))

    }

    @Test
    fun NLaNLbNLc() {
        val sentence = """
            
            aaa
            bbb
            ccc
        """.trimIndent()
        val sp = ScanOnDemandParser(S)
        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 12), result.sppt!!.treeData.root!!.locationIn(sentence))
        assertEquals("\n", result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(0, 1, 1, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0].locationIn(sentence))
        assertEquals("aaa", result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(1, 1, 2, 3), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1].locationIn(sentence))
        assertEquals("\n", result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(4, 4, 2, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2].locationIn(sentence))
        assertEquals("bbb", result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[3].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(5, 1, 3, 3), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[3].locationIn(sentence))
        assertEquals("\n", result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[4].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(8, 4, 3, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[4].locationIn(sentence))
        assertEquals("ccc", result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[5].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(9, 1, 4, 3), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[5].locationIn(sentence))

    }

    @Test
    fun aNLbSPSPNLc() {
        val sentence = """
            aaa
              bbb
            ccc
        """.trimIndent()
        val sp = ScanOnDemandParser(S)

        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 13), result.sppt!!.treeData.root!!.locationIn(sentence))
        assertEquals("aaa", result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(0, 1, 1, 3), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0].locationIn(sentence))
        assertEquals("\n  ", result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(3, 4, 1, 3), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1].locationIn(sentence))
        assertEquals("bbb", result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(6, 3, 2, 3), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2].locationIn(sentence))
        assertEquals("\n", result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[3].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(9, 6, 2, 1), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[3].locationIn(sentence))
        assertEquals("ccc", result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[4].matchedTextNoSkip(sentence))
        assertEquals(InputLocation(10, 1, 3, 3), result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[4].locationIn(sentence))

    }
}
