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

package net.akehurst.language.parser.sppt

import net.akehurst.language.api.sppt.SPPTNode
import net.akehurst.language.ogl.runtime.converter.Converter
import net.akehurst.language.ogl.runtime.structure.RuntimeRule
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_SPPTParser {

    @Test
    fun construct() {
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        val grammar = gb.grammar

        val converter = Converter(grammar)
        val rrb = converter.builder
        val sut = SPPTParser(rrb)

        assertNotNull(sut)
    }

    @Test
    fun leaf_literal() {
        val rules = mutableListOf<RuntimeRule>()
        rules.add(RuntimeRule(0, "a", RuntimeRuleKind.TERMINAL, false, false, false, -1))
        val rrs = RuntimeRuleSet(rules)
        val rrb = RuntimeRuleSetBuilder(rrs)

        val sut = SPPTParser(rrb)

        sut.leaf("a")
    }

    @Test
    fun leaf_pattern() {
        val rules = mutableListOf<RuntimeRule>()
        rules.add(RuntimeRule(0, "[a-z]", RuntimeRuleKind.TERMINAL, true, false, false, -1))
        val rrs = RuntimeRuleSet(rules)
        val rrb = RuntimeRuleSetBuilder(rrs)

        val sut = SPPTParser(rrb)

        sut.leaf("[a-z]", "a")
    }

    @Test
    fun emptyLeaf() {
        val rules = mutableListOf<RuntimeRule>()
        rules.add(RuntimeRule(0, "a", RuntimeRuleKind.NON_TERMINAL, false, false, false, -1))
        rules.add(RuntimeRule(1, "${'$'}empty.a", RuntimeRuleKind.TERMINAL, false, false, true, 0))
        val rrs = RuntimeRuleSet(rules)
        val rrb = RuntimeRuleSetBuilder(rrs)

        val sut = SPPTParser(rrb)

        sut.emptyLeaf("a")
    }

    @Test
    fun branch() {
        val rules = mutableListOf<RuntimeRule>()
        rules.add(RuntimeRule(0, "a", RuntimeRuleKind.NON_TERMINAL, false, false, false, -1))
        rules.add(RuntimeRule(1, "${'$'}empty.a", RuntimeRuleKind.TERMINAL, false, false, true, 0))
        val rrs = RuntimeRuleSet(rules)
        val rrb = RuntimeRuleSetBuilder(rrs)

        val sut = SPPTParser(rrb)

        sut.branch("a", listOf<SPPTNode>( sut.emptyLeaf("a") ))
    }

    @Test
    fun parse_leaf_literal() {
        val rules = mutableListOf<RuntimeRule>()
        rules.add(RuntimeRule(0, "a", RuntimeRuleKind.TERMINAL, false, false, false, -1))
        val rrs = RuntimeRuleSet(rules)
        val rrb = RuntimeRuleSetBuilder(rrs)

        val sut = SPPTParser(rrb)

        val treeString = """
            'a'
        """.trimIndent()

        val actual = sut.addTree(treeString)

        assertNotNull(actual)
        assertEquals(" a : 'a'" , actual.toStringAll)
    }

    @Test
    fun parse_leaf_pattern() {
        val rules = mutableListOf<RuntimeRule>()
        rules.add(RuntimeRule(0, "[a-z]", RuntimeRuleKind.TERMINAL, true, false, false, -1))
        val rrs = RuntimeRuleSet(rules)
        val rrb = RuntimeRuleSetBuilder(rrs)

        val sut = SPPTParser(rrb)

        val treeString = """
            '[a-z]' : 'a'
        """.trimIndent()

        val actual = sut.addTree(treeString)

        assertNotNull(actual)
        assertEquals(" [a-z] : 'a'" , actual.toStringAll)
    }
}