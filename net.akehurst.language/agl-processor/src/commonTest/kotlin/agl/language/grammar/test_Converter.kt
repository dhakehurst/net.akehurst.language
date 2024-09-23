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

package net.akehurst.language.agl.language.grammar

import net.akehurst.language.grammar.asm.builder.grammar
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetTest
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetTest.matches
import net.akehurst.language.agl.runtime.structure.ruleSet
import kotlin.test.Test
import kotlin.test.assertTrue

class test_Converter {

    @Test
    fun construct() {
        val grammar = grammar("test", "test") {
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {

        }
        assertTrue(expected.matches(actual))
    }

    @Test
    fun emptyRule() {
        val grammar = grammar("test", "test") {
            empty("S")
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
            concatenation("S") { empty() }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun leaf_terminalLiteralRule() {
        // S = 'a' ;
        val grammar = grammar("test", "test") {
            concatenation("A", isLeaf = true) { lit("a") }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
            literal("A", "a")
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun single_terminalLiteralRule_when_used_twice() {
        // S = 'a' 'a' ;
        val grammar = grammar("test", "test") {
            concatenation("S") { lit("a"); lit("a") }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
            concatenation("S") { ref("'a'"); ref("'a'") }
            literal("a")
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun leaf_terminalPatternRule() {
        // S = "[a-c]" ;
        val grammar = grammar("test", "test") {
            concatenation("ABC", isLeaf = true) { pat("[a-c]") }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
            pattern("ABC", "[a-c]")
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun concatenationLiteralRule() {
        // S = 'a' 'b' 'c' ;
        val grammar = grammar("test", "test") {
            concatenation("S") { lit("a"); lit("b"); lit("c") }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
            concatenation("S") { literal("a"); literal("b"); literal("c") }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun concatenationNonTerminalRule() {
        // S = A B C ;
        // A = 'a' ;
        // B = 'b' ;
        // C = 'c' ;
        val grammar = grammar("test", "test") {
            concatenation("S") { ref("A"); ref("B"); ref("C") }
            concatenation("A") { lit("a") }
            concatenation("B") { lit("b") }
            concatenation("C") { lit("c") }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
            concatenation("S") { ref("A"); ref("B"); ref("C") }
            concatenation("A") { literal("a") }
            concatenation("B") { literal("b") }
            concatenation("C") { literal("c") }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun choiceEqualLiteralRule() {
        // S = 'a' | 'b' | 'c' ;
        val grammar = grammar("test", "test") {
            choice("S") {
                lit("a")
                lit("b")
                lit("c")
            }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
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
        val grammar = grammar("test", "test") {
            choice("S") {
                ref("a")
                ref("b")
                ref("c")
            }
            concatenation("a") { lit("a") }
            concatenation("b") { lit("b") }
            concatenation("c") { lit("c") }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
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
        val grammar = grammar("test", "test") {
            choice("S") {
                concat { lit("a"); lit("b") }
                concat { lit("c") }
            }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
            choiceLongest("S") {
                concatenation { literal("a"); literal("b") }
                literal("c")
            }
        }

        assertTrue(expected.matches(actual))
    }

    /*
        @Test
        fun choicePriorityLiteralRule() {
            // S = 'a' < 'b' < 'c' ;
            val gb = GrammarBuilderDefault(NamespaceDefault("test"), "test")
            gb.rule("S").choicePriority(
                gb.concatenation(gb.terminalLiteral("a")),
                gb.concatenation(gb.terminalLiteral("b")),
                gb.concatenation(gb.terminalLiteral("c"))
            )
            val grammar = grammar("test", "test") {

            }

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
    */
    @Test
    fun optional_LiteralRule() {
        // S = 'a'? ;
        val grammar = grammar("test", "test") {
            optional("S") { lit("a") }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
            optional("S", "'a'")
            literal("a")
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun optional_NonTerminalRule() {
        // S = a? ;
        // a = 'a' ;
        val grammar = grammar("test", "test") {
            optional("S") { ref("a") }
            concatenation("a") { lit("a") }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
            optional("S", "a")
            concatenation("a") { literal("a") }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun multi_0_n_LiteralRule() {
        // S = 'a'* ;
        val grammar = grammar("test", "Test") {
            list("S", 0, -1) { lit("a") }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("test.Test") {
            multi("S", 0, -1, "'a'")
            literal("a")
        }

        RuntimeRuleSetTest.assertRrsEquals(expected, actual)
    }

    @Test
    fun multi_0_n_NonTerminalRule() {
        // S = a* ;
        // a = 'a' ;
        val grammar = grammar("test", "test") {
            list("S", 0, -1) { ref("a") }
            concatenation("a") { lit("a") }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
            multi("S", 0, -1, "a")
            concatenation("a") { literal("a") }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun multi_1_n_LiteralRule() {
        // S = 'a'+ ;
        val grammar = grammar("test", "test") {
            list("S", 1, -1) { lit("a") }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
            multi("S", 1, -1, "'a'")
            literal("a")
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun multi_1_n_NonTerminalRule() {
        // S = a+ ;
        // a = 'a' ;
        val grammar = grammar("test", "test") {
            list("S", 1, -1) { ref("a") }
            concatenation("a") { lit("a") }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
            multi("S", 1, -1, "a")
            concatenation("a") { literal("a") }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun sList_0_n_LiteralRule() {
        // S = ['a' / ',']* ;
        val grammar = grammar("test", "test") {
            separatedList("S", 0, -1) { lit("a"); lit(",") }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
            sList("S", 0, -1, "'a'", "','")
            literal("a")
            literal(",")
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun sList_0_n_NonTerminalRule() {
        // S = [a / c]* ;
        // a = 'a' ;
        // c = ',' ;
        val grammar = grammar("test", "test") {
            separatedList("S", 0, -1) { ref("a"); ref("c") }
            concatenation("a") { lit("a") }
            concatenation("c") { lit(",") }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
            sList("S", 0, -1, "a", "c")
            concatenation("a") { literal("a") }
            concatenation("c") { literal(",") }
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun sList_1_n_LiteralRule() {
        // S = ['a' / ',']+ ;
        val grammar = grammar("test", "test") {
            separatedList("S", 1, -1) { lit("a"); lit(",") }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("Test") {
            sList("S", 1, -1, "'a'", "','")
            literal("a")
            literal(",")
        }

        assertTrue(expected.matches(actual))
    }

    @Test
    fun sList_1_n_NonTerminalRule() {
        // S = [a / c]+ ;
        // a = 'a' ;
        // c = ',' ;
        val grammar = grammar("test", "Test") {
            separatedList("S", 1, -1) { ref("a"); ref("c") }
            concatenation("a") { lit("a") }
            concatenation("c") { lit(",") }
        }

        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("test.Test") {
            sList("S", 1, -1, "a", "c")
            concatenation("a") { literal("a") }
            concatenation("c") { literal(",") }
        }

        RuntimeRuleSetTest.assertRrsEquals(expected, actual)
    }

    @Test
    fun group_choice_concat_leaf_literal() {
        // S = a (b c | d) e ;
        // leaf a = 'a' ;
        // leaf b = 'b' ;
        // leaf c = 'c' ;
        // leaf d = 'd' ;
        // leaf e = 'e' ;

        val grammar = grammar("test", "Test") {
            concatenation("S") {
                ref("a")
                chc {
                    alt { ref("b"); ref("c") }
                    alt { ref("d") }
                }
                ref("e")
            }
            concatenation("a", isLeaf = true) { lit("a") }
            concatenation("b", isLeaf = true) { lit("b") }
            concatenation("c", isLeaf = true) { lit("c") }
            concatenation("d", isLeaf = true) { lit("d") }
            concatenation("e", isLeaf = true) { lit("e") }
        }
        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§choice1"); ref("e") }
            choiceLongest("§S§choice1", isPseudo = true) {
                concatenation { ref("b"); ref("c") }
                concatenation { ref("d") }
            }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        }

        RuntimeRuleSetTest.assertRrsEquals(expected, actual)
    }

    @Test
    fun group_choice_concat_nonTerm_list() {
        // S = a (BC | d+) e ;
        // BC = b c ;
        // leaf a = 'a' ;
        // leaf b = 'b' ;
        // leaf c = 'c' ;
        // leaf d = 'd' ;
        // leaf e = 'e' ;
        val grammar = grammar("test", "Test") {
            concatenation("S") {
                ref("a")
                chc {
                    alt { ref("BC") }
                    alt { lst(1, -1) { ref("d") } }
                }
                ref("e")
            }
            concatenation("BC") { ref("b"); ref("c") }
            concatenation("a", isLeaf = true) { lit("a") }
            concatenation("b", isLeaf = true) { lit("b") }
            concatenation("c", isLeaf = true) { lit("c") }
            concatenation("d", isLeaf = true) { lit("d") }
            concatenation("e", isLeaf = true) { lit("e") }
        }
        val actual = ConverterToRuntimeRules(grammar).runtimeRuleSet

        val expected = ruleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§choice1"); ref("e") }
            choiceLongest("§S§choice1", isPseudo = true) {
                concatenation { ref("BC") }
                concatenation { ref("§S§multi1") }
            }
            concatenation("BC") { ref("b"); ref("c") }
            multi("§S§multi1", 1, -1, "d", isPseudo = true)
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        }

        RuntimeRuleSetTest.assertRrsEquals(expected, actual)
    }

    @Test
    fun preference() {
        // S = v | A | M
        // v = "[a-z]+"
        // A = S + S
        // M = S / S
        val grammar = grammar("test", "test") {
            TODO()
        }
    }
}