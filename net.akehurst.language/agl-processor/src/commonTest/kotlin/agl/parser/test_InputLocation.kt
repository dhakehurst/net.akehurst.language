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

import net.akehurst.language.agl.agl.parser.SentenceDefault
import net.akehurst.language.agl.parser.LeftCornerParser
import net.akehurst.language.agl.regex.RegexEnginePlatform
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.agl.scanner.ScannerOnDemand
import net.akehurst.language.agl.sppt.TreeDataComplete
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.sppt.SpptDataNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

fun SpptDataNode.children(treeData: TreeDataComplete, alternative: Int = 0) = treeData.childrenFor(this)[alternative].second

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
        val ss = SentenceDefault(sentence)
        val sp = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, S.terminals), S)

        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 3), ss.locationForNode(result.sppt!!.treeData.userRoot!!))
        assertEquals(InputLocation(0, 1, 1, 1), ss.locationForNode(result.sppt!!.treeData.userRoot!!.children(result.sppt!!.treeData)[0]))
        assertEquals(InputLocation(1, 2, 1, 1), ss.locationForNode(result.sppt!!.treeData.userRoot!!.children(result.sppt!!.treeData)[1]))
        assertEquals(InputLocation(2, 3, 1, 1), ss.locationForNode(result.sppt!!.treeData.userRoot!!.children(result.sppt!!.treeData)[2]))

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
        val ss = SentenceDefault(sentence)
        val sp = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, S.terminals), S)

        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 3), ss.locationForNode(result.sppt!!.treeData.userRoot!!))
        assertEquals(InputLocation(0, 1, 1, 1), ss.locationForNode(result.sppt!!.treeData.userRoot!!.children(result.sppt!!.treeData)[0]))
        assertEquals(InputLocation(1, 2, 1, 1), ss.locationForNode(result.sppt!!.treeData.userRoot!!.children(result.sppt!!.treeData)[1]))
        assertEquals(InputLocation(2, 3, 1, 1), ss.locationForNode(result.sppt!!.treeData.userRoot!!.children(result.sppt!!.treeData)[2]))

    }

    @Test
    fun a_b_c() {
        val sentence = "a b c"
        val ss = SentenceDefault(sentence)
        val sp = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, S.terminals), S)

        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val td = result.sppt!!.treeData
        val ur = td.userRoot!!
        val url = ss.locationForNode(ur)
        val children = ur.children(td)
        val c0 = ss.locationForNode(children[0])                          // 'a'
        assertEquals(1, td.skipNodesAfter(children[0]).size)
        val c1 = ss.locationForNode(td.skipNodesAfter(children[0])[0])    // ' '
        val c2 = ss.locationForNode(children[1])                          // 'b'
        assertEquals(1, td.skipNodesAfter(children[1]).size)
        val c3 = ss.locationForNode(td.skipNodesAfter(children[1])[0])    // ' '
        val c4 = ss.locationForNode(children[2])                          // 'c'
        assertEquals(0, td.skipNodesAfter(children[2]).size)

        assertEquals(InputLocation(0, 1, 1, 5), url)
        // 'a'
        assertEquals(InputLocation(0, 1, 1, 1), c0)
        // ' '
        assertEquals(InputLocation(1, 2, 1, 1), c1)
        // 'b'
        assertEquals(InputLocation(2, 3, 1, 1), c2)
        // ' '
        assertEquals(InputLocation(3, 4, 1, 1), c3)
        // 'c'
        assertEquals(InputLocation(4, 5, 1, 1), c4)

    }

    @Test
    fun aNLbNLc() {
        val sentence = """
            a
            b
            c
        """.trimIndent()
        val ss = SentenceDefault(sentence)
        val sp = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, S.terminals), S)

        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val sppt = result.sppt!!
        val userRoot = sppt.treeData.userRoot
        assertEquals(InputLocation(0, 1, 1, 5), ss.locationForNode(userRoot))
        assertEquals("a", ss.matchedTextNoSkip(userRoot.children(sppt.treeData)[0]))
        assertEquals(InputLocation(0, 1, 1, 1), ss.locationForNode(userRoot.children(sppt.treeData)[0]))
        assertEquals("b", ss.matchedTextNoSkip(userRoot.children(sppt.treeData)[1]))
        assertEquals(InputLocation(2, 1, 2, 1), ss.locationForNode(userRoot.children(sppt.treeData)[1]))
        assertEquals("c", ss.matchedTextNoSkip(userRoot.children(sppt.treeData)[2]))
        assertEquals(InputLocation(4, 1, 3, 1), ss.locationForNode(userRoot.children(sppt.treeData)[2]))

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
        val ss = SentenceDefault(sentence)
        val sp = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, S.terminals), S)

        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 9), ss.locationForNode(result.sppt!!.treeData.root!!))
        assertEquals(InputLocation(0, 1, 1, 3), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0]))
        assertEquals(InputLocation(3, 4, 1, 3), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1]))
        assertEquals(InputLocation(6, 7, 1, 3), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2]))

    }

    @Test
    fun a_b_c() {
        val sentence = "aaa bbb ccc"
        val ss = SentenceDefault(sentence)
        val sp = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, S.terminals), S)

        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 11), ss.locationForNode(result.sppt!!.treeData.root!!))
        assertEquals(InputLocation(0, 1, 1, 3), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0]))
        assertEquals(InputLocation(3, 4, 1, 1), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1]))
        assertEquals(InputLocation(4, 5, 1, 3), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2]))
        assertEquals(InputLocation(7, 8, 1, 1), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[3]))
        assertEquals(InputLocation(8, 9, 1, 3), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[4]))

    }

    @Test
    fun aNLbNLc() {
        val sentence = """
            aaa
            bbb
            ccc
        """.trimIndent()
        val ss = SentenceDefault(sentence)
        val sp = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, S.terminals), S)

        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 11), ss.locationForNode(result.sppt!!.treeData.root!!))
        assertEquals("aaa", ss.matchedTextNoSkip(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0]))
        assertEquals(InputLocation(0, 1, 1, 3), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0]))
        assertEquals("\n", ss.matchedTextNoSkip(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1]))
        assertEquals(InputLocation(3, 4, 1, 1), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1]))
        assertEquals("bbb", ss.matchedTextNoSkip(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2]))
        assertEquals(InputLocation(4, 1, 2, 3), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2]))
        assertEquals("\n", ss.matchedTextNoSkip(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[3]))
        assertEquals(InputLocation(7, 4, 2, 1), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[3]))
        assertEquals("ccc", ss.matchedTextNoSkip(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[4]))
        assertEquals(InputLocation(8, 1, 3, 3), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[4]))

    }

    @Test
    fun NLaNLbNLc() {
        val sentence = """
            
            aaa
            bbb
            ccc
        """.trimIndent()
        val ss = SentenceDefault(sentence)
        val sp = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, S.terminals), S)
        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 12), ss.locationForNode(result.sppt!!.treeData.root!!))
        assertEquals("\n", ss.matchedTextNoSkip(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0]))
        assertEquals(InputLocation(0, 1, 1, 1), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0]))
        assertEquals("aaa", ss.matchedTextNoSkip(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1]))
        assertEquals(InputLocation(1, 1, 2, 3), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1]))
        assertEquals("\n", ss.matchedTextNoSkip(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2]))
        assertEquals(InputLocation(4, 4, 2, 1), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2]))
        assertEquals("bbb", ss.matchedTextNoSkip(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[3]))
        assertEquals(InputLocation(5, 1, 3, 3), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[3]))
        assertEquals("\n", ss.matchedTextNoSkip(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[4]))
        assertEquals(InputLocation(8, 4, 3, 1), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[4]))
        assertEquals("ccc", ss.matchedTextNoSkip(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[5]))
        assertEquals(InputLocation(9, 1, 4, 3), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[5]))

    }

    @Test
    fun aNLbSPSPNLc() {
        val sentence = """
            aaa
              bbb
            ccc
        """.trimIndent()
        val ss = SentenceDefault(sentence)
        val sp = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, S.terminals), S)

        val result = sp.parseForGoal("S", sentence)

        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        assertEquals(InputLocation(0, 1, 1, 13), ss.locationForNode(result.sppt!!.treeData.root!!))
        assertEquals("aaa", ss.matchedTextNoSkip(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0]))
        assertEquals(InputLocation(0, 1, 1, 3), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[0]))
        assertEquals("\n  ", ss.matchedTextNoSkip(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1]))
        assertEquals(InputLocation(3, 4, 1, 3), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[1]))
        assertEquals("bbb", ss.matchedTextNoSkip(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2]))
        assertEquals(InputLocation(6, 3, 2, 3), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[2]))
        assertEquals("\n", ss.matchedTextNoSkip(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[3]))
        assertEquals(InputLocation(9, 6, 2, 1), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[3]))
        assertEquals("ccc", ss.matchedTextNoSkip(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[4]))
        assertEquals(InputLocation(10, 1, 3, 3), ss.locationForNode(result.sppt!!.treeData.root!!.children(result.sppt!!.treeData)[4]))

    }
}
