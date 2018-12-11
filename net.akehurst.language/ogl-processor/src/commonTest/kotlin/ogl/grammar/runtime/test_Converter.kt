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

import net.akehurst.language.api.grammar.EmptyRule
import net.akehurst.language.api.parser.ParseException
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.ogl.ast.GrammarBuilderDefault
import net.akehurst.language.ogl.ast.NamespaceDefault
import net.akehurst.language.ogl.runtime.structure.RuntimeRule
import kotlin.test.*

class test_Converter {

    private fun checkERule(n: Int, r: RuntimeRule, ptext: String, isSkip: Boolean, ruleThatIsEmpty: RuntimeRule) {
        assertEquals(n, r.number)
        assertEquals(RuntimeRuleKind.TERMINAL, r.kind)
        assertEquals(true, r.isTerminal)
        assertEquals(false, r.isNonTerminal)
        assertEquals(false, r.isPattern)
        assertEquals(RuntimeRuleItemKind.EMPTY, r.rhs.kind)
        assertEquals(true, r.isEmptyRule)
        assertEquals(ruleThatIsEmpty.emptyRuleItem, r)
        assertEquals(false, r.isNonTerminal)
        assertEquals(isSkip, r.isSkip)
        assertEquals(ptext, r.name)
        assertEquals(ptext, r.patternText)
        assertEquals(ruleThatIsEmpty, r.rhs.items[0])
    }

    private fun checkTRule(n: Int, r: RuntimeRule, ptext: String, isPattern: Boolean, isSkip: Boolean) {
        assertEquals(n, r.number)
        assertEquals(RuntimeRuleKind.TERMINAL, r.kind)
        assertEquals(true, r.isTerminal)
        assertEquals(false, r.isNonTerminal)
        assertEquals(isPattern, r.isPattern)
        assertEquals(false, r.isEmptyRule)
        assertEquals(false, r.isNonTerminal)
        assertEquals(isSkip, r.isSkip)
        assertEquals(ptext, r.name)
        assertEquals(ptext, r.patternText)
        assertFailsWith(ParseException::class) {
            r.rhs
        }
    }

    private fun checkNRule(n: Int, r: RuntimeRule, name: String, isSkip: Boolean, itemKind: RuntimeRuleItemKind, numItems: Int) {
        assertEquals(n, r.number)
        assertEquals(RuntimeRuleKind.NON_TERMINAL, r.kind)
        assertEquals(false, r.isTerminal)
        assertEquals(true, r.isNonTerminal)
        assertEquals(name, r.name)
        assertEquals(numItems, r.rhs.items.size)
        assertEquals(itemKind, r.rhs.kind)
    }

    private fun checkItems(owner: RuntimeRule, vararg items: RuntimeRule) {
        assertTrue(owner.rhs.items.contentEquals(items))
    }

    @Test
    fun construct() {
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        val grammar = gb.grammar

        val sut = Converter(grammar)

        assertNotNull(sut)
    }

    @Test
    fun emptyRule() {
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").empty()
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(2, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkERule(1, actual.runtimeRules[1], "§empty.r", false, actual.runtimeRules[0])

    }

    @Test
    fun terminalLiteralRule() {
        // r = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").choiceEqual(gb.concatenation(gb.terminalLiteral("a")))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(2, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(1, actual.runtimeRules[1], "a", false, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1])
    }

    @Test
    fun terminalLiteralRule_2() {
        // r = 'a' 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").choiceEqual(gb.concatenation(gb.terminalLiteral("a"),gb.terminalLiteral("a")))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(2, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 2)
        this.checkTRule(1, actual.runtimeRules[1], "a", false, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1],actual.runtimeRules[1])
    }

    @Test
    fun terminalPatternRule() {
        // r = "[a-c]" ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").choiceEqual(gb.concatenation(gb.terminalPattern("[a-c]")))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(2, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(1, actual.runtimeRules[1], "[a-c]", true, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1])
    }

    @Test
    fun concatenationLiteralRule() {
        // r = 'a' 'b' 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").choiceEqual(gb.concatenation(gb.terminalLiteral("a"), gb.terminalLiteral("b"), gb.terminalLiteral("c")))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(4, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 3)
        this.checkTRule(1, actual.runtimeRules[1], "a", false, false)
        this.checkTRule(2, actual.runtimeRules[2], "b", false, false)
        this.checkTRule(3, actual.runtimeRules[3], "c", false, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[2], actual.runtimeRules[3])
    }

    @Test
    fun concatenationNonTerminalRule() {
        // r = a b c ;
        // a = 'a' ;
        // b = 'b' ;
        // c = 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("a").choiceEqual(gb.concatenation(gb.terminalLiteral("a")))
        gb.rule("b").choiceEqual(gb.concatenation(gb.terminalLiteral("b")))
        gb.rule("c").choiceEqual(gb.concatenation(gb.terminalLiteral("c")))
        gb.rule("r").choiceEqual(gb.concatenation(gb.nonTerminal("a"), gb.nonTerminal("b"), gb.nonTerminal("c")))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(7, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "a", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(1, actual.runtimeRules[1], "a", false, false)
        this.checkNRule(2, actual.runtimeRules[2], "b", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(3, actual.runtimeRules[3], "b", false, false)
        this.checkNRule(4, actual.runtimeRules[4], "c", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(5, actual.runtimeRules[5], "c", false, false)
        this.checkNRule(6, actual.runtimeRules[6], "r", false, RuntimeRuleItemKind.CONCATENATION, 3)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1])
        this.checkItems(actual.runtimeRules[2], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[4], actual.runtimeRules[5])
        this.checkItems(actual.runtimeRules[6], actual.runtimeRules[0], actual.runtimeRules[2], actual.runtimeRules[4])
    }

    @Test
    fun choiceEqualLiteralRule() {
        // r = 'a' | 'b' | 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").choiceEqual(gb.concatenation(gb.terminalLiteral("a")), gb.concatenation(gb.terminalLiteral("b")), gb.concatenation(gb.terminalLiteral("c")))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(4, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CHOICE_EQUAL, 3)
        this.checkTRule(1, actual.runtimeRules[1], "a", false, false)
        this.checkTRule(2, actual.runtimeRules[2], "b", false, false)
        this.checkTRule(3, actual.runtimeRules[3], "c", false, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[2], actual.runtimeRules[3])
    }

    @Test
    fun choiceEqualNonTerminalRule() {
        // r = a | b | c ;
        // a = 'a' ;
        // b = 'b' ;
        // c = 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("a").choiceEqual(gb.concatenation(gb.terminalLiteral("a")))
        gb.rule("b").choiceEqual(gb.concatenation(gb.terminalLiteral("b")))
        gb.rule("c").choiceEqual(gb.concatenation(gb.terminalLiteral("c")))
        gb.rule("r").choiceEqual(gb.concatenation(gb.nonTerminal("a")), gb.concatenation(gb.nonTerminal("b")), gb.concatenation(gb.nonTerminal("c")))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(7, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "a", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(1, actual.runtimeRules[1], "a", false, false)
        this.checkNRule(2, actual.runtimeRules[2], "b", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(3, actual.runtimeRules[3], "b", false, false)
        this.checkNRule(4, actual.runtimeRules[4], "c", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(5, actual.runtimeRules[5], "c", false, false)
        this.checkNRule(6, actual.runtimeRules[6], "r", false, RuntimeRuleItemKind.CHOICE_EQUAL, 3)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1])
        this.checkItems(actual.runtimeRules[2], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[4], actual.runtimeRules[5])
        this.checkItems(actual.runtimeRules[6], actual.runtimeRules[0], actual.runtimeRules[2], actual.runtimeRules[4])
    }

    @Test
    fun choicePriorityLiteralRule() {
        // r = 'a' < 'b' < 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").choicePriority(
                gb.concatenation(gb.terminalLiteral("a")),
                gb.concatenation(gb.terminalLiteral("b")),
                gb.concatenation(gb.terminalLiteral("c"))
        )
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(4, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CHOICE_PRIORITY, 3)
        this.checkTRule(1, actual.runtimeRules[1], "a", false, false)
        this.checkTRule(2, actual.runtimeRules[2], "b", false, false)
        this.checkTRule(3, actual.runtimeRules[3], "c", false, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[2], actual.runtimeRules[3])
    }

    @Test
    fun choicePriorityNonTerminalRule() {
        // r = a < b < c ;
        // a = 'a' ;
        // b = 'b' ;
        // c = 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("a").choiceEqual(gb.concatenation(gb.terminalLiteral("a")))
        gb.rule("b").choiceEqual(gb.concatenation(gb.terminalLiteral("b")))
        gb.rule("c").choiceEqual(gb.concatenation(gb.terminalLiteral("c")))
        gb.rule("r").choicePriority(
                gb.concatenation(gb.nonTerminal("a")),
                gb.concatenation(gb.nonTerminal("b")),
                gb.concatenation(gb.nonTerminal("c"))
        )
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(7, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "a", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(1, actual.runtimeRules[1], "a", false, false)
        this.checkNRule(2, actual.runtimeRules[2], "b", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(3, actual.runtimeRules[3], "b", false, false)
        this.checkNRule(4, actual.runtimeRules[4], "c", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(5, actual.runtimeRules[5], "c", false, false)
        this.checkNRule(6, actual.runtimeRules[6], "r", false, RuntimeRuleItemKind.CHOICE_PRIORITY, 3)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1])
        this.checkItems(actual.runtimeRules[2], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[4], actual.runtimeRules[5])
        this.checkItems(actual.runtimeRules[6], actual.runtimeRules[0], actual.runtimeRules[2], actual.runtimeRules[4])
    }

    @Test
    fun multi_0_1_LiteralRule() {
        // r = 'a'? ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").multi(0, 1, gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(4, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(1, actual.runtimeRules[1], "a", false, false)
        this.checkNRule(2, actual.runtimeRules[2], "§r§multi0", false, RuntimeRuleItemKind.MULTI, 2)
        this.checkERule(3, actual.runtimeRules[3], "§empty.§r§multi0", false, actual.runtimeRules[2])

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[2])
        this.checkItems(actual.runtimeRules[2], actual.runtimeRules[1], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[3], actual.runtimeRules[2])
    }

    @Test
    fun multi_0_1_NonTerminalRule() {
        // r = a? ;
        // a = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").multi(0, 1, gb.nonTerminal("a"))
        gb.rule("a").choiceEqual(gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(5, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkNRule(1, actual.runtimeRules[1], "a", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(2, actual.runtimeRules[2], "a", false, false)
        this.checkNRule(3, actual.runtimeRules[3], "§r§multi0", false, RuntimeRuleItemKind.MULTI, 2)
        this.checkERule(4, actual.runtimeRules[4], "§empty.§r§multi0", false, actual.runtimeRules[3])

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[1], actual.runtimeRules[2])
        this.checkItems(actual.runtimeRules[3], actual.runtimeRules[1], actual.runtimeRules[4])
        this.checkItems(actual.runtimeRules[4], actual.runtimeRules[3])
    }

    @Test
    fun multi_0_n_LiteralRule() {
        // r = 'a'* ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").multi(0, -1, gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(4, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(1, actual.runtimeRules[1], "a", false, false)
        this.checkNRule(2, actual.runtimeRules[2], "§r§multi0", false, RuntimeRuleItemKind.MULTI, 2)
        this.checkERule(3, actual.runtimeRules[3], "§empty.§r§multi0", false, actual.runtimeRules[2])

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[2])
        this.checkItems(actual.runtimeRules[2], actual.runtimeRules[1], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[3], actual.runtimeRules[2])
    }

    @Test
    fun multi_0_n_NonTerminalRule() {
        // r = a* ;
        // a = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").multi(0, -1, gb.nonTerminal("a"))
        gb.rule("a").choiceEqual(gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(5, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkNRule(1, actual.runtimeRules[1], "a", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(2, actual.runtimeRules[2], "a", false, false)
        this.checkNRule(3, actual.runtimeRules[3], "§r§multi0", false, RuntimeRuleItemKind.MULTI, 2)
        this.checkERule(4, actual.runtimeRules[4], "§empty.§r§multi0", false, actual.runtimeRules[3])

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[1], actual.runtimeRules[2])
        this.checkItems(actual.runtimeRules[3], actual.runtimeRules[1], actual.runtimeRules[4])
        this.checkItems(actual.runtimeRules[4], actual.runtimeRules[3])
    }

    @Test
    fun multi_1_n_LiteralRule() {
        // r = 'a'+ ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").multi(1, -1, gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(3, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(1, actual.runtimeRules[1], "a", false, false)
        this.checkNRule(2, actual.runtimeRules[2], "§r§multi0", false, RuntimeRuleItemKind.MULTI, 1)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[2])
        this.checkItems(actual.runtimeRules[2], actual.runtimeRules[1])
    }

    @Test
    fun multi_1_n_NonTerminalRule() {
        // r = a+ ;
        // a = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").multi(1, -1, gb.nonTerminal("a"))
        gb.rule("a").choiceEqual(gb.concatenation(gb.terminalLiteral("a")))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(4, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkNRule(1, actual.runtimeRules[1], "a", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(2, actual.runtimeRules[2], "a", false, false)
        this.checkNRule(3, actual.runtimeRules[3], "§r§multi0", false, RuntimeRuleItemKind.MULTI, 1)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[3], actual.runtimeRules[1])
        this.checkItems(actual.runtimeRules[1], actual.runtimeRules[2])
    }

    @Test
    fun sList_0_1_LiteralRule() {
        // r = ['a' / ',']? ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").separatedList(0, 1, gb.terminalLiteral(","),gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(5, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(1, actual.runtimeRules[1], "a", false, false)
        this.checkTRule(2, actual.runtimeRules[2], ",", false, false)
        this.checkNRule(3, actual.runtimeRules[3], "§r§sList0", false, RuntimeRuleItemKind.SEPARATED_LIST, 3)
        this.checkERule(4, actual.runtimeRules[4], "§empty.§r§sList0", false, actual.runtimeRules[3])

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[3], actual.runtimeRules[1], actual.runtimeRules[2], actual.runtimeRules[4])
        this.checkItems(actual.runtimeRules[4], actual.runtimeRules[3])
    }

    @Test
    fun sList_0_1_NonTerminalRule() {
        // r = [a / ',']? ;
        // a = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").separatedList(0, 1, gb.terminalLiteral(","), gb.nonTerminal("a"))
        gb.rule("a").choiceEqual(gb.concatenation(gb.terminalLiteral("a")))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(6, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkNRule(1, actual.runtimeRules[1], "a", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(2, actual.runtimeRules[2], "a", false, false)
        this.checkTRule(3, actual.runtimeRules[3], ",", false, false)
        this.checkNRule(4, actual.runtimeRules[4], "§r§sList0", false, RuntimeRuleItemKind.SEPARATED_LIST, 3)
        this.checkERule(5, actual.runtimeRules[5], "§empty.§r§sList0", false, actual.runtimeRules[4])

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[4])
        this.checkItems(actual.runtimeRules[1], actual.runtimeRules[2])
        this.checkItems(actual.runtimeRules[4], actual.runtimeRules[1], actual.runtimeRules[3], actual.runtimeRules[5])
        this.checkItems(actual.runtimeRules[5], actual.runtimeRules[4])
    }

    @Test
    fun sList_0_n_LiteralRule() {
        // r = ['a' / ',']* ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").separatedList(0, -1, gb.terminalLiteral(","),gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(5, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(1, actual.runtimeRules[1], "a", false, false)
        this.checkTRule(2, actual.runtimeRules[2], ",", false, false)
        this.checkNRule(3, actual.runtimeRules[3], "§r§sList0", false, RuntimeRuleItemKind.SEPARATED_LIST, 3)
        this.checkERule(4, actual.runtimeRules[4], "§empty.§r§sList0", false, actual.runtimeRules[3])

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[3], actual.runtimeRules[1], actual.runtimeRules[2], actual.runtimeRules[4])
        this.checkItems(actual.runtimeRules[4], actual.runtimeRules[3])
    }

    @Test
    fun sList_0_n_NonTerminalRule() {
        // r = [a / ',']* ;
        // a = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").separatedList(0, -1, gb.terminalLiteral(","),gb.nonTerminal("a"))
        gb.rule("a").choiceEqual(gb.concatenation(gb.terminalLiteral("a")))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(6, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkNRule(1, actual.runtimeRules[1], "a", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(2, actual.runtimeRules[2], "a", false, false)
        this.checkTRule(3, actual.runtimeRules[3], ",", false, false)
        this.checkNRule(4, actual.runtimeRules[4], "§r§sList0", false, RuntimeRuleItemKind.SEPARATED_LIST, 3)
        this.checkERule(5, actual.runtimeRules[5], "§empty.§r§sList0", false, actual.runtimeRules[4])

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[4])
        this.checkItems(actual.runtimeRules[1], actual.runtimeRules[2])
        this.checkItems(actual.runtimeRules[4], actual.runtimeRules[1], actual.runtimeRules[3], actual.runtimeRules[5])
        this.checkItems(actual.runtimeRules[5], actual.runtimeRules[4])
    }

    @Test
    fun sList_1_n_LiteralRule() {
        // r = ['a' / ',']+ ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").separatedList(1, -1, gb.terminalLiteral(","),gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(4, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(1, actual.runtimeRules[1], "a", false, false)
        this.checkTRule(2, actual.runtimeRules[2], ",", false, false)
        this.checkNRule(3, actual.runtimeRules[3], "§r§sList0", false, RuntimeRuleItemKind.SEPARATED_LIST, 2)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[3], actual.runtimeRules[1], actual.runtimeRules[2])
    }

    @Test
    fun sList_1_n_NonTerminalRule() {
        // r = [a / ',']+ ;
        // a = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").separatedList(1, -1, gb.terminalLiteral(","),gb.nonTerminal("a"))
        gb.rule("a").choiceEqual(gb.concatenation(gb.terminalLiteral("a")))
        val grammar = gb.grammar

        val sut = Converter(grammar)

        val actual = sut.transform()

        assertEquals(5, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkNRule(1, actual.runtimeRules[1], "a", false, RuntimeRuleItemKind.CONCATENATION, 1)
        this.checkTRule(2, actual.runtimeRules[2], "a", false, false)
        this.checkTRule(3, actual.runtimeRules[3], ",", false, false)
        this.checkNRule(4, actual.runtimeRules[4], "§r§sList0", false, RuntimeRuleItemKind.SEPARATED_LIST, 2)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[4])
        this.checkItems(actual.runtimeRules[1], actual.runtimeRules[2])
        this.checkItems(actual.runtimeRules[4], actual.runtimeRules[1], actual.runtimeRules[3])
    }

}