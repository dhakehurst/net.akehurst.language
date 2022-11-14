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

package net.akehurst.language.agl.grammar.grammar

import net.akehurst.language.agl.grammar.grammar.asm.GrammarBuilderDefault
import net.akehurst.language.agl.grammar.grammar.asm.NamespaceDefault
import net.akehurst.language.agl.runtime.structure.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class test_Converter {

    private fun checkRule(
        number: Int,
        r: RuntimeRule,
        tag: String,
        value: String,
        ruleKind: RuntimeRuleKind,
        isPattern: Boolean,
        isSkip: Boolean,
        embeddedRuntimeRuleSet: RuntimeRuleSet?,
        embeddedStartRule: RuntimeRule?,
        itemsKind: RuntimeRuleRhsItemsKind,
        choiceKind: RuntimeRuleChoiceKind,
        listKind: RuntimeRuleListKind,
        multiMin: Int,
        multiMax: Int,
        itemsSize: Int
    ) {
        assertEquals(number, r.ruleNumber)
        assertEquals(tag, r.tag)
        assertEquals(value, r.value)
        assertEquals(ruleKind, r.kind)
        assertEquals(isPattern, r.isPattern)
        assertEquals(isSkip, r.isSkip)
        assertEquals(embeddedRuntimeRuleSet, r.embeddedRuntimeRuleSet)
        assertEquals(embeddedStartRule, r.embeddedStartRule)

        if (null != r.rhsOpt) {
            assertEquals(itemsKind, r.rhs.itemsKind)
            assertEquals(listKind, r.rhs.listKind)
            assertEquals(choiceKind, r.rhs.choiceKind)
            assertEquals(multiMin, r.rhs.multiMin)
            assertEquals(multiMax, r.rhs.multiMax)
            assertEquals(itemsSize, r.rhs.items.size)
        }
    }

    /** check empty rule */
    private fun checkERule(n: Int, r: RuntimeRule, ptext: String, isSkip: Boolean, ruleThatIsEmpty: RuntimeRule) {
        checkRule(
            n, r, ptext, ptext, RuntimeRuleKind.TERMINAL, false, isSkip, null, null,
            RuntimeRuleRhsItemsKind.EMPTY, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, 1
        )
        assertEquals(ruleThatIsEmpty, r.rhs.items[0])
    }

    /** check terminal (pattern or literal) rule */
    private fun checkTRule(n: Int, r: RuntimeRule, tag: String, ptext: String, isPattern: Boolean, isSkip: Boolean) {
        checkRule(
            n, r, tag, ptext, RuntimeRuleKind.TERMINAL, isPattern, isSkip, null, null,
            RuntimeRuleRhsItemsKind.EMPTY, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, 0
        )
        assertEquals(null, r.rhsOpt)
    }

    /** check concatenation rule */
    private fun checkNRule(n: Int, r: RuntimeRule, tag: String, isSkip: Boolean, numItems: Int) {
        checkRule(
            n, r, tag, "", RuntimeRuleKind.NON_TERMINAL, false, isSkip, null, null,
            RuntimeRuleRhsItemsKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, numItems
        )
    }

    /** check choice rule */
    private fun checkCRule(n: Int, r: RuntimeRule, tag: String, isSkip: Boolean, choiceKind: RuntimeRuleChoiceKind, numItems: Int) {
        checkRule(
            n, r, tag, "", RuntimeRuleKind.NON_TERMINAL, false, isSkip, null, null,
            RuntimeRuleRhsItemsKind.CHOICE, choiceKind, RuntimeRuleListKind.NONE, -1, 0, numItems
        )
    }

    /** check list rule */
    private fun checkLRule(n: Int, r: RuntimeRule, tag: String, isSkip: Boolean, listKind: RuntimeRuleListKind, numItems: Int, min: Int, max: Int) {
        checkRule(
            n, r, tag, "", RuntimeRuleKind.NON_TERMINAL, false, isSkip, null, null,
            RuntimeRuleRhsItemsKind.LIST, RuntimeRuleChoiceKind.NONE, listKind, min, max, numItems
        )
    }

    private fun checkItems(owner: RuntimeRule, vararg items: RuntimeRule) {
        assertTrue(owner.rhs.items.contentEquals(items))
    }

    @Test
    fun construct() {
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        assertNotNull(sut)
    }

    @Test
    fun emptyRule() {
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").empty()
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(2, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, 1)
        this.checkERule(1, actual.runtimeRules[1], "§empty.r", false, actual.runtimeRules[0])

    }

    @Test
    fun terminalLiteralRule() {
        // r = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").concatenation(gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(2, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, 1)
        this.checkTRule(1, actual.runtimeRules[1], "'a'", "a", false, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1])
    }

    @Test
    fun terminalLiteralRule_2() {
        // r = 'a' 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").concatenation(gb.terminalLiteral("a"), gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(2, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, 2)
        this.checkTRule(1, actual.runtimeRules[1], "'a'", "a", false, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[1])
    }

    @Test
    fun terminalPatternRule() {
        // r = "[a-c]" ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").concatenation(gb.terminalPattern("[a-c]"))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(2, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, 1)
        this.checkTRule(1, actual.runtimeRules[1], "\"[a-c]\"", "[a-c]", true, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1])
    }

    @Test
    fun concatenationLiteralRule() {
        // r = 'a' 'b' 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").concatenation(gb.terminalLiteral("a"), gb.terminalLiteral("b"), gb.terminalLiteral("c"))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(4, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "r", false, 3)
        this.checkTRule(1, actual.runtimeRules[1], "'a'", "a", false, false)
        this.checkTRule(2, actual.runtimeRules[2], "'b'", "b", false, false)
        this.checkTRule(3, actual.runtimeRules[3], "'c'", "c", false, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[2], actual.runtimeRules[3])
    }

    @Test
    fun concatenationNonTerminalRule() {
        // r = a b c ;
        // a = 'a' ;
        // b = 'b' ;
        // c = 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("a").concatenation(gb.terminalLiteral("a"))
        gb.rule("b").concatenation(gb.terminalLiteral("b"))
        gb.rule("c").concatenation(gb.terminalLiteral("c"))
        gb.rule("r").concatenation(gb.nonTerminal("a"), gb.nonTerminal("b"), gb.nonTerminal("c"))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(7, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "a", false, 1)
        this.checkTRule(1, actual.runtimeRules[1], "'a'", "a", false, false)
        this.checkNRule(2, actual.runtimeRules[2], "b", false, 1)
        this.checkTRule(3, actual.runtimeRules[3], "'b'", "b", false, false)
        this.checkNRule(4, actual.runtimeRules[4], "c", false, 1)
        this.checkTRule(5, actual.runtimeRules[5], "'c'", "c", false, false)
        this.checkNRule(6, actual.runtimeRules[6], "r", false, 3)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1])
        this.checkItems(actual.runtimeRules[2], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[4], actual.runtimeRules[5])
        this.checkItems(actual.runtimeRules[6], actual.runtimeRules[0], actual.runtimeRules[2], actual.runtimeRules[4])
    }

    @Test
    fun choiceEqualLiteralRule() {
        // r = 'a' | 'b' | 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").choiceLongestFromConcatenation(gb.concatenation(gb.terminalLiteral("a")), gb.concatenation(gb.terminalLiteral("b")), gb.concatenation(gb.terminalLiteral("c")))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(4, actual.runtimeRules.size)
        this.checkCRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleChoiceKind.LONGEST_PRIORITY, 3)
        this.checkTRule(1, actual.runtimeRules[1], "'a'", "a", false, false)
        this.checkTRule(2, actual.runtimeRules[2], "'b'", "b", false, false)
        this.checkTRule(3, actual.runtimeRules[3], "'c'", "c", false, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[2], actual.runtimeRules[3])
    }

    @Test
    fun choiceEqualNonTerminalRule() {
        // r = a | b | c ;
        // a = 'a' ;
        // b = 'b' ;
        // c = 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("a").concatenation(gb.terminalLiteral("a"))
        gb.rule("b").concatenation(gb.terminalLiteral("b"))
        gb.rule("c").concatenation(gb.terminalLiteral("c"))
        gb.rule("r").choiceLongestFromConcatenation(gb.concatenation(gb.nonTerminal("a")), gb.concatenation(gb.nonTerminal("b")), gb.concatenation(gb.nonTerminal("c")))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(7, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "a", false, 1)
        this.checkTRule(1, actual.runtimeRules[1], "'a'", "a", false, false)
        this.checkNRule(2, actual.runtimeRules[2], "b", false, 1)
        this.checkTRule(3, actual.runtimeRules[3], "'b'", "b", false, false)
        this.checkNRule(4, actual.runtimeRules[4], "c", false, 1)
        this.checkTRule(5, actual.runtimeRules[5], "'c'", "c", false, false)
        this.checkCRule(6, actual.runtimeRules[6], "r", false, RuntimeRuleChoiceKind.LONGEST_PRIORITY, 3)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1])
        this.checkItems(actual.runtimeRules[2], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[4], actual.runtimeRules[5])
        this.checkItems(actual.runtimeRules[6], actual.runtimeRules[0], actual.runtimeRules[2], actual.runtimeRules[4])
    }

    @Test
    fun choiceEqualNestedConcatenationLiteralRule() {
        // r = 'a' 'b' | 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").choiceLongestFromConcatenation(gb.concatenation(gb.terminalLiteral("a"), gb.terminalLiteral("b")), gb.concatenation(gb.terminalLiteral("c")))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(5, actual.runtimeRules.size)
        this.checkCRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleChoiceKind.LONGEST_PRIORITY, 2)
        this.checkTRule(1, actual.runtimeRules[1], "'a'", "a", false, false)
        this.checkTRule(2, actual.runtimeRules[2], "'b'", "b", false, false)
        this.checkNRule(3, actual.runtimeRules[3], "§r§choice1", false, 2)
        this.checkTRule(4, actual.runtimeRules[4], "'c'", "c", false, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[3], actual.runtimeRules[4])
        this.checkItems(actual.runtimeRules[3], actual.runtimeRules[1], actual.runtimeRules[2])
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

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(4, actual.runtimeRules.size)
        this.checkCRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleChoiceKind.PRIORITY_LONGEST, 3)
        this.checkTRule(1, actual.runtimeRules[1], "'a'", "a", false, false)
        this.checkTRule(2, actual.runtimeRules[2], "'b'", "b", false, false)
        this.checkTRule(3, actual.runtimeRules[3], "'c'", "c", false, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[2], actual.runtimeRules[3])
    }

    @Test
    fun choicePriorityNonTerminalRule() {
        // r = a < b < c ;
        // a = 'a' ;
        // b = 'b' ;
        // c = 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("a").concatenation(gb.terminalLiteral("a"))
        gb.rule("b").concatenation(gb.terminalLiteral("b"))
        gb.rule("c").concatenation(gb.terminalLiteral("c"))
        gb.rule("r").choicePriority(
            gb.concatenation(gb.nonTerminal("a")),
            gb.concatenation(gb.nonTerminal("b")),
            gb.concatenation(gb.nonTerminal("c"))
        )
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(7, actual.runtimeRules.size)
        this.checkNRule(0, actual.runtimeRules[0], "a", false, 1)
        this.checkTRule(1, actual.runtimeRules[1], "'a'", "a", false, false)
        this.checkNRule(2, actual.runtimeRules[2], "b", false, 1)
        this.checkTRule(3, actual.runtimeRules[3], "'b'", "b", false, false)
        this.checkNRule(4, actual.runtimeRules[4], "c", false, 1)
        this.checkTRule(5, actual.runtimeRules[5], "'c'", "c", false, false)
        this.checkCRule(6, actual.runtimeRules[6], "r", false, RuntimeRuleChoiceKind.PRIORITY_LONGEST, 3)

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

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(3, actual.runtimeRules.size)
        this.checkLRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleListKind.MULTI, 2, 0, 1)
        this.checkTRule(1, actual.runtimeRules[1], "'a'", "a", false, false)
        this.checkERule(2, actual.runtimeRules[2], "§empty.r", false, actual.runtimeRules[0])

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[2])
        this.checkItems(actual.runtimeRules[2], actual.runtimeRules[0])
    }

    @Test
    fun multi_0_1_NonTerminalRule() {
        // r = a? ;
        // a = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").multi(0, 1, gb.nonTerminal("a"))
        gb.rule("a").concatenation(gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(4, actual.runtimeRules.size)
        this.checkLRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleListKind.MULTI, 2, 0, 1)
        this.checkNRule(1, actual.runtimeRules[1], "a", false, 1)
        this.checkTRule(2, actual.runtimeRules[2], "'a'", "a", false, false)
        this.checkERule(3, actual.runtimeRules[3], "§empty.r", false, actual.runtimeRules[0])

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[1], actual.runtimeRules[2])
        this.checkItems(actual.runtimeRules[3], actual.runtimeRules[0])
    }

    @Test
    fun multi_0_n_LiteralRule() {
        // r = 'a'* ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").multi(0, -1, gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(3, actual.runtimeRules.size)
        this.checkLRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleListKind.MULTI, 2, 0, -1)
        this.checkTRule(1, actual.runtimeRules[1], "'a'", "a", false, false)
        this.checkERule(2, actual.runtimeRules[2], "§empty.r", false, actual.runtimeRules[0])

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[2])
        this.checkItems(actual.runtimeRules[2], actual.runtimeRules[0])
    }

    @Test
    fun multi_0_n_NonTerminalRule() {
        // r = a* ;
        // a = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").multi(0, -1, gb.nonTerminal("a"))
        gb.rule("a").concatenation(gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(4, actual.runtimeRules.size)
        this.checkLRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleListKind.MULTI, 2, 0, -1)
        this.checkNRule(1, actual.runtimeRules[1], "a", false, 1)
        this.checkTRule(2, actual.runtimeRules[2], "'a'", "a", false, false)
        this.checkERule(3, actual.runtimeRules[3], "§empty.r", false, actual.runtimeRules[0])

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[1], actual.runtimeRules[2])
        this.checkItems(actual.runtimeRules[3], actual.runtimeRules[0])
    }

    @Test
    fun multi_1_n_LiteralRule() {
        // r = 'a'+ ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").multi(1, -1, gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(2, actual.runtimeRules.size)
        this.checkLRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleListKind.MULTI, 1, 1, -1)
        this.checkTRule(1, actual.runtimeRules[1], "'a'", "a", false, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1])
    }

    @Test
    fun multi_1_n_NonTerminalRule() {
        // r = a+ ;
        // a = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").multi(1, -1, gb.nonTerminal("a"))
        gb.rule("a").concatenation(gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(3, actual.runtimeRules.size)
        this.checkLRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleListKind.MULTI, 1, 1, -1)
        this.checkNRule(1, actual.runtimeRules[1], "a", false, 1)
        this.checkTRule(2, actual.runtimeRules[2], "'a'", "a", false, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1])
        this.checkItems(actual.runtimeRules[1], actual.runtimeRules[2])
    }

    @Test
    fun sList_0_1_LiteralRule() {
        // r = ['a' / ',']? ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").separatedList(0, 1, gb.terminalLiteral(","), gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(4, actual.runtimeRules.size)
        this.checkLRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleListKind.SEPARATED_LIST, 3, 0, 1)
        this.checkTRule(1, actual.runtimeRules[1], "'a'", "a", false, false)
        this.checkTRule(2, actual.runtimeRules[2], "','", ",", false, false)
        this.checkERule(3, actual.runtimeRules[3], "§empty.r", false, actual.runtimeRules[0])

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[2], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[3], actual.runtimeRules[0])
    }

    @Test
    fun sList_0_1_NonTerminalRule() {
        // r = [a / ',']? ;
        // a = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").separatedList(0, 1, gb.terminalLiteral(","), gb.nonTerminal("a"))
        gb.rule("a").concatenation(gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(5, actual.runtimeRules.size)
        this.checkLRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleListKind.SEPARATED_LIST, 3, 0, 1)
        this.checkNRule(1, actual.runtimeRules[1], "a", false, 1)
        this.checkTRule(2, actual.runtimeRules[2], "'a'", "a", false, false)
        this.checkTRule(3, actual.runtimeRules[3], "','", ",", false, false)
        this.checkERule(4, actual.runtimeRules[4], "§empty.r", false, actual.runtimeRules[0])

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[3], actual.runtimeRules[4])
        this.checkItems(actual.runtimeRules[1], actual.runtimeRules[2])
        this.checkItems(actual.runtimeRules[4], actual.runtimeRules[0])
    }

    @Test
    fun sList_0_n_LiteralRule() {
        // r = ['a' / ',']* ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").separatedList(0, -1, gb.terminalLiteral(","), gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(4, actual.runtimeRules.size)
        this.checkLRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleListKind.SEPARATED_LIST, 3, 0, -1)
        this.checkTRule(1, actual.runtimeRules[1], "'a'", "a", false, false)
        this.checkTRule(2, actual.runtimeRules[2], "','", ",", false, false)
        this.checkERule(3, actual.runtimeRules[3], "§empty.r", false, actual.runtimeRules[0])

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[2], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[3], actual.runtimeRules[0])
    }

    @Test
    fun sList_0_n_NonTerminalRule() {
        // r = [a / ',']* ;
        // a = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").separatedList(0, -1, gb.terminalLiteral(","), gb.nonTerminal("a"))
        gb.rule("a").choiceLongestFromConcatenation(gb.concatenation(gb.terminalLiteral("a")))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(5, actual.runtimeRules.size)
        this.checkLRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleListKind.SEPARATED_LIST, 3, 0, -1)
        this.checkNRule(1, actual.runtimeRules[1], "a", false, 1)
        this.checkTRule(2, actual.runtimeRules[2], "'a'", "a", false, false)
        this.checkTRule(3, actual.runtimeRules[3], "','", ",", false, false)
        this.checkERule(4, actual.runtimeRules[4], "§empty.r", false, actual.runtimeRules[0])

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[3], actual.runtimeRules[4])
        this.checkItems(actual.runtimeRules[1], actual.runtimeRules[2])
        this.checkItems(actual.runtimeRules[4], actual.runtimeRules[0])
    }

    @Test
    fun sList_1_n_LiteralRule() {
        // r = ['a' / ',']+ ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").separatedList(1, -1, gb.terminalLiteral(","), gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(3, actual.runtimeRules.size)
        this.checkLRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleListKind.SEPARATED_LIST, 2, 1, -1)
        this.checkTRule(1, actual.runtimeRules[1], "'a'", "a", false, false)
        this.checkTRule(2, actual.runtimeRules[2], "','", ",", false, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[2])
    }

    @Test
    fun sList_1_n_NonTerminalRule() {
        // r = [a / ',']+ ;
        // a = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("r").separatedList(1, -1, gb.terminalLiteral(","), gb.nonTerminal("a"))
        gb.rule("a").choiceLongestFromConcatenation(gb.concatenation(gb.terminalLiteral("a")))
        val grammar = gb.grammar

        val sut = ConverterToRuntimeRules(grammar)

        val actual = sut.runtimeRuleSet

        assertEquals(4, actual.runtimeRules.size)
        this.checkLRule(0, actual.runtimeRules[0], "r", false, RuntimeRuleListKind.SEPARATED_LIST, 2, 1, -1)
        this.checkNRule(1, actual.runtimeRules[1], "a", false, 1)
        this.checkTRule(2, actual.runtimeRules[2], "'a'", "a", false, false)
        this.checkTRule(3, actual.runtimeRules[3], "','", ",", false, false)

        this.checkItems(actual.runtimeRules[0], actual.runtimeRules[1], actual.runtimeRules[3])
        this.checkItems(actual.runtimeRules[1], actual.runtimeRules[2])
    }

}