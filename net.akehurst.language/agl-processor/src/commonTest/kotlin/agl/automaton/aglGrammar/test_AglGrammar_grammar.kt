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

package net.akehurst.language.agl.automaton

import net.akehurst.language.agl.language.grammar.AglGrammarGrammar
import net.akehurst.language.agl.language.grammar.ConverterToRuntimeRules
import net.akehurst.language.agl.parser.LeftCornerParser
import net.akehurst.language.agl.regex.RegexEnginePlatform
import net.akehurst.language.agl.scanner.ScannerOnDemand
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test

internal class test_AglGrammar_grammar : test_AutomatonAbstract() {

    private val grammar = AglGrammarGrammar
    private val converterToRuntimeRules = ConverterToRuntimeRules(grammar)
    private val scanner = ScannerOnDemand(RegexEnginePlatform, converterToRuntimeRules.runtimeRuleSet.terminals.toList())
    private val parser = LeftCornerParser(scanner, converterToRuntimeRules.runtimeRuleSet)
    private val rrs = parser.runtimeRuleSet

    private val R_grammarDefinition = rrs.findRuntimeRule("grammarDefinition")
    private val R_namespace = rrs.findRuntimeRule("namespace")
    private val R_rule = rrs.findRuntimeRule("rule")

    private val R_isSkip = rrs.findRuntimeRule("isSkip")
    private val R_isLeaf = rrs.findRuntimeRule("isLeaf")

    private val T_IDENTIFIER = rrs.findRuntimeRule("IDENTIFIER")
    private val T_namespace = rrs.findRuntimeRule("'namespace'")
    private val T_grammar = rrs.findRuntimeRule("'grammar'")

    private val SM = rrs.fetchStateSetFor(R_grammarDefinition, AutomatonKind.LOOKAHEAD_1)
    private val s0 = SM.startState
    private val G = s0.runtimeRules.first()

    private val lhs_IDENTIFIER = LookaheadSetPart(false, false, false, setOf(T_IDENTIFIER))

    private val goal = "grammarDefinition"

    @Test
    fun parse_xxx() {
        val parser = LeftCornerParser(scanner, rrs)
        parser.parseForGoal(goal, "namespace test grammar Test { S = 'a' ; }")
        val actual = parser.runtimeRuleSet.fetchStateSetFor(R_grammarDefinition, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString(goal))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, goal, false) {


        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser_preBuild = LeftCornerParser(scanner, rrs_preBuild)
        val parser_noBuild = LeftCornerParser(scanner, rrs_noBuild)
        val sentences = listOf(
            "namespace test grammar Test { r = 'a' ; }",
            "namespace test.ns1 grammar Test { r = 'a' ; }",
            "namespace test.ns1.ns2 grammar Test { r = 'a' ; }",
            "namespace test grammar Test1 extends Test2 { r = 'a' ; }",
            "namespace test grammar Test { r = 'a' 'b' ; }",
            "namespace test grammar Test { r = 'a' 'b' 'c' ; }",
            "namespace test grammar Test { r = 'a' | 'b' ; }",
            "namespace test grammar Test { r = 'a' | 'b' | 'c' ; }",

            )
        for (sen in sentences) {
            val result = parser_noBuild.parseForGoal(goal, sen)
            if (result.issues.isNotEmpty()) {
                println("--Error: No Build--")
                println("Sentence: $sen")
                result.issues.forEach { println(it) }
            }
            val result2 = parser_preBuild.parseForGoal(goal, sen)
            if (result2.issues.isNotEmpty()) {
                println("--Error: Pre Build--")
                println("Sentence: $sen")
                result2.issues.forEach { println(it) }
            }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor(goal)
        val automaton_preBuild = rrs_preBuild.buildFor(goal, AutomatonKind.LOOKAHEAD_1)

        println("--No Build--")
        println(rrs_preBuild.usedAutomatonToString(goal))
        println("--Pre Build--")
        println(rrs_noBuild.usedAutomatonToString(goal))

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild)
    }
}