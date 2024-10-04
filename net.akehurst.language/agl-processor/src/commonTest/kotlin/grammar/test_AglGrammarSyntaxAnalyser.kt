/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.grammar.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.grammar.api.*
import net.akehurst.language.grammar.asm.GrammarModelDefault
import net.akehurst.language.grammar.asm.grammar
import net.akehurst.language.grammar.processor.AglGrammar
import net.akehurst.language.grammar.processor.AglGrammarSyntaxAnalyser
import net.akehurst.language.grammar.processor.ConverterToRuntimeRules
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import net.akehurst.language.sppt.api.SharedPackedParseTree
import net.akehurst.language.test.FixMethodOrder
import net.akehurst.language.test.MethodSorters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.JVM)
class test_AglGrammarSyntaxAnalyser {

    private companion object {
        val conv = ConverterToRuntimeRules(AglGrammar.grammar)
        val rrs = conv.runtimeRuleSet
        val scanner = ScannerOnDemand(RegexEnginePlatform, rrs.terminals)
        val parser = LeftCornerParser(scanner, rrs)

        fun parse(sentence: String): SharedPackedParseTree {
            val conv = ConverterToRuntimeRules(AglGrammar.grammar)
            val rrs = conv.runtimeRuleSet
            val scanner = ScannerOnDemand(RegexEnginePlatform, rrs.terminals)
            val parser = LeftCornerParser(scanner, rrs)
            val res = parser.parse(sentence, Agl.parseOptions { goalRuleName(AglGrammar.goalRuleName) })
            assertTrue(res.issues.isEmpty(), res.issues.toString())
            return res.sppt!!
        }

        fun test(grammarStr: String, expected: Grammar) {
            val sppt = parse(grammarStr)
            val sut = AglGrammarSyntaxAnalyser()
            val res = sut.transform(sppt) { _, _ -> TODO() }
            assertTrue(res.issues.isEmpty(), res.issues.toString())
            val expectedMdl = GrammarModelDefault(SimpleName("Test"), listOf(expected.namespace as GrammarNamespace))
            assertEquals(expectedMdl.asString(), res.asm!!.asString())
        }
    }

    @Test
    fun rule_one_terminal() {
        val sentence = """
            namespace test
            grammar Test {
              a = 'a';
            }
        """.trimIndent()
        val expected = grammar("test", "Test") {
            concatenation("a") { lit("a") }
        }

        test(sentence, expected)
    }

    @Test
    fun extends_one() {
        val sentence = """
            namespace ns.test
            grammar Test : ns.test.Test2 {
              leaf a = 'a' ;
            }
        """.trimIndent()
        val expected = grammar("ns.test", "Test") {
            extends("ns.test.Test2")
            concatenation("a", isLeaf = true) { lit("a") }
        }

        test(sentence, expected)
    }

    @Test
    fun extends_two() {
        val sentence = """
            namespace ns.test
            grammar Test : ns.test.Test2, AA {
              leaf a = 'a' ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())

        assertEquals(2, res.asm!!.allDefinitions[0].extends.size)
    }

    @Test
    fun rule_two_terminal() {
        val sentence = """
            namespace test
            grammar Test {
              a = 'a' 'b' ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())
    }

    @Test
    fun rule_empty() {
        val sentence = """
            namespace test
            grammar Test {
              a =  ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())
    }

    @Test
    fun rule_simpleChoice() {
        val sentence = """
            namespace test
            grammar Test {
              a = 'a' | 'b' ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())
    }

    @Test
    fun rule_prioChoice() {
        val sentence = """
            namespace test
            grammar Test {
              a = 'a' < 'a' ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())
    }

    @Test
    fun rule_ambigChoice() {
        val sentence = """
            namespace test
            grammar Test {
              a = 'a' || 'a' ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())
    }

    @Test
    fun rule_leaf_terminal() {
        val sentence = """
            namespace ns.test
            grammar Test {
              leaf a = 'a' ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())

        assertTrue(res.asm!!.allDefinitions[0].findAllResolvedGrammarRule(GrammarRuleName("a")) != null)
    }

    @Test
    fun rule_nonTerminal() {
        val sentence = """
            namespace ns.test
            grammar Test {
               S = A ;
               A = 'a' ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }

        assertTrue(res.asm!!.allDefinitions[0].findAllResolvedGrammarRule(GrammarRuleName("S")) != null)
        assertTrue(res.asm!!.allDefinitions[0].findAllResolvedGrammarRule(GrammarRuleName("A")) != null)
    }

    @Test
    fun rule_nonTerminalQualified() {
        val sentence = """
            namespace ns.test
            grammar Base {
                A = 'a' ;
            }
            grammar Test {
              S = Base.A ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())
        val gBase = res.asm!!.allDefinitions[0]
        val gTest = res.asm!!.allDefinitions[1]
        assertTrue(gTest.findAllResolvedGrammarRule(GrammarRuleName("S")) != null)
        val rS = gTest.findAllResolvedGrammarRule(GrammarRuleName("S")) as NormalRule
        assertTrue(rS.rhs is NonTerminal)
        //("Base", (rS.rhs as NonTerminal).referencedRuleOrNull(gTest)?.grammar?.name)
        assertEquals(null, (rS.rhs as NonTerminal).referencedRuleOrNull(gTest)?.grammar?.name) //is null because GrammarReferences are not resolved until semantic analysis
    }

    @Test
    fun rule_skip_terminal() {
        val sentence = """
            namespace ns.test
            grammar Test {
              skip WS = "\s+" ;
              a = 'a' ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())

        assertTrue(res.asm!!.allDefinitions[0].findAllResolvedGrammarRule(GrammarRuleName("a")) != null)
    }


    @Test
    fun rule_multiplicity_0n() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a'* ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())

        assertTrue(res.asm!!.allDefinitions[0].findAllResolvedGrammarRule(GrammarRuleName("a")) != null)
    }

    @Test
    fun rule_multiplicity_1n() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a'+ ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())

        assertTrue(res.asm!!.allDefinitions[0].findAllResolvedGrammarRule(GrammarRuleName("a")) != null)
    }

    @Test
    fun rule_multiplicity_01() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a'? ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())

        assertTrue(res.asm!!.allDefinitions[0].findAllResolvedGrammarRule(GrammarRuleName("a")) != null)
    }

    @Test
    fun rule_multiplicity_4n() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a'4+ ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())

        assertTrue(res.asm!!.allDefinitions[0].findAllResolvedGrammarRule(GrammarRuleName("a")) != null)
    }

    @Test
    fun rule_multiplicity_4() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a'4 ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())

        assertTrue(res.asm!!.allDefinitions[0].findAllResolvedGrammarRule(GrammarRuleName("a")) != null)
    }

    @Test
    fun rule_multiplicity_b4() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a'{4} ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())

        assertTrue(res.asm!!.allDefinitions[0].findAllResolvedGrammarRule(GrammarRuleName("a")) != null)
    }

    @Test
    fun rule_multiplicity_47() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a'4..7 ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())

        assertTrue(res.asm!!.allDefinitions[0].findAllResolvedGrammarRule(GrammarRuleName("a")) != null)
    }

    @Test
    fun rule_multiplicity_b47() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a'{4..7} ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())

        assertTrue(res.asm!!.allDefinitions[0].findAllResolvedGrammarRule(GrammarRuleName("a")) != null)
    }

    @Test
    fun rule_sepList() {
        val sentence = """
            namespace ns.test
            grammar Test {
              S = [A / ',']2+ ;
              A = 'a' ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())

        //assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }

    @Test
    fun rule_embedded() {
        val sentence = """
            namespace ns.test
            grammar Test {
              S = [A / ',']2+ ;
              A = Inn::S ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())

        //assertTrue(res.asm!![0].findNonTerminalRule("a") != null)
    }

    @Test
    fun rule_group() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a' ( 'b' 'c' ) 'd' ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())

        assertTrue(res.asm!!.allDefinitions[0].findAllResolvedGrammarRule(GrammarRuleName("a")) != null)
    }

    @Test
    fun rule_group_choice() {
        val sentence = """
            namespace ns.test
            grammar Test {
              a = 'a' ( C | B ) 'd' ;
              B = 'b' ;
              C = 'c' ;
            }
        """.trimIndent()
        val sppt = parse(sentence)
        //println(sppt.toStringAll)
        val sut = AglGrammarSyntaxAnalyser()
        val res = sut.transform(sppt) { _, _ -> TODO() }
        println(res.asm!!.allDefinitions[0].toString())

        assertTrue(res.asm!!.allDefinitions[0].findAllResolvedGrammarRule(GrammarRuleName("a")) != null)
    }
}