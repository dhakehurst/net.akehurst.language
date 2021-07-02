/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.agl.ast.GrammarBuilderDefault
import net.akehurst.language.agl.ast.NamespaceDefault
import net.akehurst.language.agl.grammar.grammar.ConverterToRuntimeRules
import net.akehurst.language.agl.parser.InputFromString
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.api.sppt.SPPTNode
import kotlin.test.*

class test_SPPTParser {

    @Test
    fun construct() {
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        val grammar = gb.grammar

        val converter = ConverterToRuntimeRules(grammar)
        val rrb = converter.builder
        val sut = SPPTParser(rrb)

        assertNotNull(sut)
    }

    @Test
    fun leaf_literal() {
        val rrb = RuntimeRuleSetBuilder()
        rrb.literal("a")

        val sut = SPPTParser(rrb)
        val input = InputFromString(10,"")
        val actual = sut.leaf("'a'", "a", input, 0,1)

        assertNotNull(actual)
        assertTrue(actual.isLeaf)
        assertFalse(actual.isPattern)
        assertFalse(actual.isEmptyLeaf)
        assertEquals("a", actual.matchedText)
        assertEquals(1, actual.matchedTextLength)
        assertFalse(actual.isBranch)
        assertFalse(actual.isSkip)

    }

    @Test
    fun leaf_pattern() {
        val rrb = RuntimeRuleSetBuilder()
        rrb.pattern("[a-z]")

        val sut = SPPTParser(rrb)
        val input = InputFromString(10,"")
        val actual = sut.leaf("\"[a-z]\"", "a", input, 0,1)

        assertNotNull(actual)
        assertTrue(actual.isLeaf)
        assertTrue(actual.isPattern)
        assertFalse(actual.isEmptyLeaf)
        assertEquals("a", actual.matchedText)
        assertEquals(1, actual.matchedTextLength)
        assertFalse(actual.isBranch)
        assertFalse(actual.isSkip)
    }

    @Test
    fun emptyLeaf() {
        val rrb = RuntimeRuleSetBuilder()
        rrb.rule("a").empty()

        val sut = SPPTParser(rrb)
        val input = InputFromString(10,"")
        val actual = sut.emptyLeaf("a", input, 0,1)
        assertNotNull(actual)
        assertTrue(actual.isLeaf)
        assertFalse(actual.isPattern)
        assertTrue(actual.isEmptyLeaf)
        assertEquals("", actual.matchedText)
        assertEquals(0, actual.matchedTextLength)
        assertFalse(actual.isBranch)
        assertFalse(actual.isSkip)
    }

    @Test
    fun branch_empty() {
        val rrb = RuntimeRuleSetBuilder()
        rrb.rule("a").empty()

        val sut = SPPTParser(rrb)
        val input = InputFromString(10,"")
        val actual = sut.branch(input, "a", 0, listOf<SPPTNode>(sut.emptyLeaf("a", input, 0,1)))

        assertNotNull(actual)
        assertFalse(actual.isLeaf)
        assertFalse(actual.isEmptyLeaf)
        assertEquals("", actual.matchedText)
        assertEquals(0, actual.matchedTextLength)
        assertTrue(actual.isBranch)
        assertFalse(actual.isSkip)
    }

    @Test
    fun parse_leaf_literal() {
        val rrb = RuntimeRuleSetBuilder()
        rrb.literal("a")

        val sut = SPPTParser(rrb)

        val treeString = """
            'a'
        """.trimIndent()

        val actual = sut.addTree(treeString)

        assertNotNull(actual)
        assertEquals(" 'a'", actual.toStringAll)
    }

    @Test
    fun parse_leaf_literal_backslash() {
        val rrb = RuntimeRuleSetBuilder()
        rrb.literal("BS","\\\\")

        val sut = SPPTParser(rrb)

        val treeString = """
            BS : '\'
        """.trimIndent()

        val actual = sut.addTree(treeString)

        assertNotNull(actual)
        assertEquals(" '\\'", actual.toStringAll)
    }

    @Test
    fun parse_leaf_pattern() {
        val rrb = RuntimeRuleSetBuilder()
        rrb.pattern("[a-z]")

        val sut = SPPTParser(rrb)

        val treeString = """
            "[a-z]" : 'a'
        """.trimIndent()

        val actual = sut.addTree(treeString)

        assertNotNull(actual)
        assertEquals("\"[a-z]\" : 'a'", actual.toStringAll.trim())
    }

    @Test
    fun parse_branch_empty() {
        val rrb = RuntimeRuleSetBuilder()
        rrb.rule("a").empty()

        val sut = SPPTParser(rrb)

        val treeString = """
            a { §empty }
        """.trimIndent()

        val actual = sut.addTree(treeString)

        assertNotNull(actual)
        assertEquals(" a { §empty.a }", actual.toStringAll)
    }

    @Test
    fun parse_branch() {
        val rrb = RuntimeRuleSetBuilder()
        val lit = rrb.literal("a")
        rrb.rule("a").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY, lit)

        val sut = SPPTParser(rrb)

        val treeString = """
            a { 'a' }
        """.trimIndent()

        val actual = sut.addTree(treeString)

        assertNotNull(actual)
        assertEquals(" a { 'a' }", actual.toStringAll)
    }


}