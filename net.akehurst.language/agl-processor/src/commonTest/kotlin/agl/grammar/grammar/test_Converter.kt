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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetTest.matches
import kotlin.test.Test
import kotlin.test.assertTrue

internal class test_Converter {

    @Test
    fun construct() {
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {

        }
        assertTrue(expected.matches(actual))
    }

    @Test
    fun emptyRule() {
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").empty()
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {

            concatenation("S") { empty() }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun leaf_terminalLiteralRule() {
        // S = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.leaf("A").concatenation(gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            literal("A", "a")
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun single_terminalLiteralRule_when_used_twice() {
        // S = 'a' 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").concatenation(gb.terminalLiteral("a"), gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            concatenation("S") { ref("'a'"); ref("'a'") }
            literal("a")
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun leaf_terminalPatternRule() {
        // S = "[a-c]" ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.leaf("ABC").concatenation(gb.terminalPattern("[a-c]"))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            pattern("ABC", "[a-c]")
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun concatenationLiteralRule() {
        // S = 'a' 'b' 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").concatenation(gb.terminalLiteral("a"), gb.terminalLiteral("b"), gb.terminalLiteral("c"))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            concatenation("S") { literal("a"); literal("b"); literal("c") }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun concatenationNonTerminalRule() {
        // S = a b c ;
        // a = 'a' ;
        // b = 'b' ;
        // c = 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("a").concatenation(gb.terminalLiteral("a"))
        gb.rule("b").concatenation(gb.terminalLiteral("b"))
        gb.rule("c").concatenation(gb.terminalLiteral("c"))
        gb.rule("S").concatenation(gb.nonTerminal("a"), gb.nonTerminal("b"), gb.nonTerminal("c"))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            concatenation("S") { ref("a"); ref("b"); ref("c") }
            concatenation("a") { literal("a") }
            concatenation("b") { literal("b") }
            concatenation("c") { literal("c") }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun choiceEqualLiteralRule() {
        // S = 'a' | 'b' | 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").choiceLongestFromConcatenation(gb.concatenation(gb.terminalLiteral("a")), gb.concatenation(gb.terminalLiteral("b")), gb.concatenation(gb.terminalLiteral("c")))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            choiceLongest("S") {
                literal("a")
                literal("b")
                literal("c")
            }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun choiceEqualNonTerminalRule() {
        // S = a | b | c ;
        // a = 'a' ;
        // b = 'b' ;
        // c = 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("a").concatenation(gb.terminalLiteral("a"))
        gb.rule("b").concatenation(gb.terminalLiteral("b"))
        gb.rule("c").concatenation(gb.terminalLiteral("c"))
        gb.rule("S").choiceLongestFromConcatenation(gb.concatenation(gb.nonTerminal("a")), gb.concatenation(gb.nonTerminal("b")), gb.concatenation(gb.nonTerminal("c")))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            choiceLongest("S") {
                ref("a")
                ref("b")
                ref("c")
            }
            concatenation("a") { literal("a") }
            concatenation("b") { literal("b") }
            concatenation("c") { literal("c") }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun choiceEqualNestedConcatenationLiteralRule() {
        // S = 'a' 'b' | 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").choiceLongestFromConcatenation(gb.concatenation(gb.terminalLiteral("a"), gb.terminalLiteral("b")), gb.concatenation(gb.terminalLiteral("c")))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            choiceLongest("S") {
                concatenation { literal("a"); literal("b") }
                literal("c")
            }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun choicePriorityLiteralRule() {
        // S = 'a' < 'b' < 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").choicePriority(
            gb.concatenation(gb.terminalLiteral("a")),
            gb.concatenation(gb.terminalLiteral("b")),
            gb.concatenation(gb.terminalLiteral("c"))
        )
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            choicePriority("S") {
                literal("a")
                literal("b")
                literal("c")
            }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun choicePriorityNonTerminalRule() {
        // S = a < b < c ;
        // a = 'a' ;
        // b = 'b' ;
        // c = 'c' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("a").concatenation(gb.terminalLiteral("a"))
        gb.rule("b").concatenation(gb.terminalLiteral("b"))
        gb.rule("c").concatenation(gb.terminalLiteral("c"))
        gb.rule("S").choicePriority(
            gb.concatenation(gb.nonTerminal("a")),
            gb.concatenation(gb.nonTerminal("b")),
            gb.concatenation(gb.nonTerminal("c"))
        )
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            choicePriority("S") {
                ref("a")
                ref("b")
                ref("c")
            }
            concatenation("a") { literal("a") }
            concatenation("b") { literal("b") }
            concatenation("c") { literal("c") }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun multi_0_1_LiteralRule() {
        // S = 'a'? ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").multi(0, 1, gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            multi("S", 0, 1, "'a'")
            literal("a")
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun multi_0_1_NonTerminalRule() {
        // S = a? ;
        // a = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").multi(0, 1, gb.nonTerminal("a"))
        gb.rule("a").concatenation(gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            multi("S", 0, 1, "a")
            concatenation("a") { literal("a") }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun multi_0_n_LiteralRule() {
        // S = 'a'* ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").multi(0, -1, gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            multi("S", 0, -1, "'a'")
            literal( "a")
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun multi_0_n_NonTerminalRule() {
        // S = a* ;
        // a = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").multi(0, -1, gb.nonTerminal("a"))
        gb.rule("a").concatenation(gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            multi("S", 0, -1, "a")
            concatenation("a") { literal("a") }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun multi_1_n_LiteralRule() {
        // S = 'a'+ ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").multi(1, -1, gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            multi("S", 1, -1, "'a'")
            literal("a")
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun multi_1_n_NonTerminalRule() {
        // S = a+ ;
        // a = 'a' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").multi(1, -1, gb.nonTerminal("a"))
        gb.rule("a").concatenation(gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            multi("S", 1, -1, "a")
            concatenation("a") { literal("a") }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun sList_0_1_LiteralRule() {
        // S = ['a' / ',']? ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").separatedList(0, 1, gb.terminalLiteral(","), gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            sList("S", 0, 1, "'a'", "','")
            literal("a")
            literal(",")
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun sList_0_1_NonTerminalRule() {
        // S = [a / c]? ;
        // a = 'a' ;
        // c = ',' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").separatedList(0, 1, gb.nonTerminal("c"), gb.nonTerminal("a"))
        gb.rule("a").concatenation(gb.terminalLiteral("a"))
        gb.rule("c").concatenation(gb.terminalLiteral(","))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            sList("S", 0, 1, "a", "c")
            concatenation("a") { literal("a") }
            concatenation("c") { literal(",") }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun sList_0_n_LiteralRule() {
        // r = ['a' / ',']* ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").separatedList(0, -1, gb.terminalLiteral(","), gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            sList("S", 0, -1, "'a'", "','")
            literal("a")
            literal( ",")
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun sList_0_n_NonTerminalRule() {
        // S = [a / c]* ;
        // a = 'a' ;
        // c = ',' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").separatedList(0, -1, gb.nonTerminal("c"), gb.nonTerminal("a"))
        gb.rule("a").concatenation(gb.terminalLiteral("a"))
        gb.rule("c").concatenation(gb.terminalLiteral(","))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            sList("S", 0, -1, "a", "c")
            concatenation("a") { literal("a") }
            concatenation("c") { literal(",") }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun sList_1_n_LiteralRule() {
        // r = ['a' / ',']+ ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").separatedList(1, -1, gb.terminalLiteral(","), gb.terminalLiteral("a"))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            sList("S", 1, -1, "'a'", "','")
            literal("a")
            literal( ",")
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun sList_1_n_NonTerminalRule() {
        // S = [a / c]+ ;
        // a = 'a' ;
        // c = ',' ;
        val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
        gb.rule("S").separatedList(1, -1, gb.nonTerminal("c"), gb.nonTerminal("a"))
        gb.rule("a").concatenation(gb.terminalLiteral("a"))
        gb.rule("c").concatenation(gb.terminalLiteral(","))
        val grammar = gb.grammar

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = runtimeRuleSet {
            sList("S", 1, -1, "a", "c")
            concatenation("a") { literal("a") }
            concatenation("c") { literal(",") }
        }

        assertTrue(expected.matches(actual))
    }

}