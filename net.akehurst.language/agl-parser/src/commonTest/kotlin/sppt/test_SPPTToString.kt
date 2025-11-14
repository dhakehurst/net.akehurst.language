/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.sppt

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sppt.treedata.SPPTParserDefault
import kotlin.test.Test
import kotlin.test.assertEquals

class test_SPPTToString {

    @Test
    fun emptyNode() {
        val rrs = runtimeRuleSet {
            concatenation("S") { empty() }
        }
        val expected = """
            S { }
        """.trimIndent()
        val actual = SPPTParserDefault(rrs).parse(expected).toStringAll.trim()

        assertEquals(expected, actual)
    }

    @Test
    fun oneLeafChild() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val expected = """
            S { 'a' }
        """.trimIndent()
        val actual = SPPTParserDefault(rrs).parse(expected).toStringAll.trim()

        assertEquals(expected, actual)
    }

    @Test
    fun oneBranchChild() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("A") }
            concatenation("A") { literal("a") }
        }
        val expected = """
            S { A { 'a' } }
        """.trimIndent()
        val actual = SPPTParserDefault(rrs).parse(expected).toStringAll.trim()

        assertEquals(expected, actual)
    }

    @Test
    fun multipleOneBranchChildren() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("A"); ref("B"); ref("C") }
            concatenation("A") { literal("a") }
            concatenation("B") { literal("b") }
            concatenation("C") { literal("c") }
        }
        val expected = """
            S {
              A { 'a' }
              B { 'b' }
              C { 'c' }
            }
        """.trimIndent()
        val actual = SPPTParserDefault(rrs).parse(expected).toStringAll.trim()

        assertEquals(expected, actual)
    }

    @Test
    fun multipleMultiBranchChildren() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("A"); ref("B"); ref("C") }
            concatenation("A") { literal("a"); literal("a"); literal("a") }
            concatenation("B") { literal("b"); literal("b"); literal("b") }
            concatenation("C") { literal("c"); literal("c"); literal("c") }
        }
        val expected = """
            S {
              A {
                'a'
                'a'
                'a'
              }
              B {
                'b'
                'b'
                'b'
              }
              C {
                'c'
                'c'
                'c'
              }
            }
        """.trimIndent()
        val actual = SPPTParserDefault(rrs).parse(expected).toStringAll.trim()

        assertEquals(expected, actual)
    }

    @Test
    fun oneBranchAlternativeChild() {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.AMBIGUOUS) {
                ref("A1")
                ref("A2")
            }
            concatenation("A1") { literal("a") }
            concatenation("A2") { literal("a") }
        }
        val tree1 = """
            S { A1 { 'a' } }
        """.trimIndent()
        val tree2 = """
            S { A2 { 'a' } }
        """.trimIndent()
        val spptParser = SPPTParserDefault(rrs)
        spptParser.parse(tree1)
        val actual = spptParser.parse(tree2, true).toStringAll

        val expected = """
            S* { A1 { 'a' } }
            S* { A2 { 'a' } }
        """.trimIndent()

        assertEquals(expected, actual)
    }
}