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

package net.akehurst.language.ogl.grammar.runtime

import net.akehurst.language.api.parser.ParseException
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.ogl.ast.GrammarBuilderDefault
import net.akehurst.language.ogl.ast.NamespaceDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class test_Converter {

    @Test
    fun construct() {
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        val grammar = gb.grammar

        val sut =  Converter(grammar)

        assertNotNull(sut)
    }

    @Test
    fun emptyRule() {
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").empty()
        val grammar = gb.grammar

        val sut =  Converter(grammar)

        val actual = sut.transform()

        assertEquals(2, actual.runtimeRules.size)
        assertEquals(0, actual.runtimeRules[0].number)
        assertEquals(1, actual.runtimeRules[1].number)
        assertEquals(RuntimeRuleKind.NON_TERMINAL, actual.runtimeRules[0].kind)
        assertEquals("r", actual.runtimeRules[0].name)
        assertEquals(RuntimeRuleKind.TERMINAL, actual.runtimeRules[1].kind)
        assertEquals(RuntimeRuleItemKind.CONCATENATION, actual.runtimeRules[0].rhs.kind)
        assertEquals(RuntimeRuleItemKind.EMPTY, actual.runtimeRules[1].rhs.kind)
        assertEquals(1, actual.runtimeRules[0].rhs.items.size)
        assertEquals(1, actual.runtimeRules[1].rhs.items.size)
        assertEquals(actual.runtimeRules[0], actual.runtimeRules[1].ruleThatIsEmpty)

    }

    @Test
    fun terminalLiteralRule() {
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").choice(gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut =  Converter(grammar)

        val actual = sut.transform()

        assertEquals(2, actual.runtimeRules.size)
        assertEquals(0, actual.runtimeRules[0].number)
        assertEquals(1, actual.runtimeRules[1].number)
        assertEquals(RuntimeRuleKind.NON_TERMINAL, actual.runtimeRules[1].kind)
        assertEquals("r", actual.runtimeRules[1].name)
        assertEquals(RuntimeRuleKind.TERMINAL, actual.runtimeRules[0].kind)
        assertEquals("a", actual.runtimeRules[0].name)
        assertEquals("a", actual.runtimeRules[0].patternText)
        assertEquals(true, actual.runtimeRules[0].isTerminal)
        assertEquals(false, actual.runtimeRules[0].isNonTerminal)
        assertEquals(false, actual.runtimeRules[0].isPattern)
        assertEquals(false, actual.runtimeRules[0].isSkip)

        assertEquals(RuntimeRuleItemKind.CONCATENATION, actual.runtimeRules[1].rhs.kind)
        assertFailsWith(ParseException::class) {
            actual.runtimeRules[0].rhs
        }
        assertEquals(1, actual.runtimeRules[1].rhs.items.size)
        assertEquals(actual.runtimeRules[0], actual.runtimeRules[1].rhs.items[0])

    }

    @Test
    fun terminalPatternRule() {
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").choice(gb.terminalPattern("[a-c]"))
        val grammar = gb.grammar

        val sut =  Converter(grammar)

        val actual = sut.transform()

        assertEquals(2, actual.runtimeRules.size)
        assertEquals(0, actual.runtimeRules[0].number)
        assertEquals(1, actual.runtimeRules[1].number)
        assertEquals(RuntimeRuleKind.NON_TERMINAL, actual.runtimeRules[1].kind)
        assertEquals("r", actual.runtimeRules[1].name)
        assertEquals(RuntimeRuleKind.TERMINAL, actual.runtimeRules[0].kind)
        assertEquals("[a-c]", actual.runtimeRules[0].name)
        assertEquals("[a-c]", actual.runtimeRules[0].patternText)
        assertEquals(true, actual.runtimeRules[0].isTerminal)
        assertEquals(false, actual.runtimeRules[0].isNonTerminal)
        assertEquals(true, actual.runtimeRules[0].isPattern)
        assertEquals(false, actual.runtimeRules[0].isSkip)

        assertEquals(RuntimeRuleItemKind.CONCATENATION, actual.runtimeRules[1].rhs.kind)
        assertFailsWith(ParseException::class) {
            actual.runtimeRules[0].rhs
        }
        assertEquals(1, actual.runtimeRules[1].rhs.items.size)
        assertEquals(actual.runtimeRules[0], actual.runtimeRules[1].rhs.items[0])

    }
}