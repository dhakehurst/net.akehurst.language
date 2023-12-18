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

import net.akehurst.language.agl.parser.LeftCornerParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.agl.scanner.ScannerOnDemand
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_ifthenelse_conditional : test_AutomatonAbstract() {

    // S =  expr ;
    // expr = var | conditional ;
    // conditional = ifthenelse | ifthen;
    // ifthenelse = 'if' expr 'then' expr 'else' expr ;
    // ifthen = 'if' expr 'then' expr ;
    // var = "[A-Z]" ;

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { ref("expr") }
        choice("expr", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
            ref("VAR")
            ref("conditional")
        }
        choice("conditional", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("ifthen")
            ref("ifthenelse")
        }
        concatenation("ifthen") { literal("if"); ref("expr"); literal("then"); ref("expr") }
        concatenation("ifthenelse") { literal("if"); ref("expr"); literal("then"); ref("expr"); literal("else"); ref("expr") }
        pattern("VAR", "[A-Z]")
        preferenceFor("expr") {
            right("ifthen", setOf("'then'"))
            right("ifthenelse", setOf("'else'"))
        }
    }
    private val SM = rrs.fetchStateSetFor("S", AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val S = rrs.findRuntimeRule("S")
    private val rIfThenElse = rrs.findRuntimeRule("ifthenelse")
    private val rIfThen = rrs.findRuntimeRule("ifthen")
    private val rConditional = rrs.findRuntimeRule("conditional")
    private val rExpr = rrs.findRuntimeRule("expr")
    private val tIF = rrs.findRuntimeRule("'if'")
    private val tTHEN = rrs.findRuntimeRule("'then'")
    private val tELSE = rrs.findRuntimeRule("'else'")
    private val tVAR = rrs.findRuntimeRule("VAR")

    @Test
    fun automaton_parse_ifXthenY() {
        val parser = LeftCornerParser(ScannerOnDemand(rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "ifXthenY")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(G, o0, SR)       // G = . S
            state(tVAR)           // VAR
            state(tIF)           // 'if'
            state(rIfThenElse, o0, p1)   // ifthenelse = 'if' . expr 'then' expr 'else' expr ;
            state(rIfThen, o0, p1)       // ifthen = 'if' . expr 'then' expr ;
            state(rExpr)       // G = S .

            trans(WIDTH) { src(G, o0, SR); tgt(tIF); lhg(setOf(tVAR, tIF)); ctx(G, o0, SR) }
            trans(WIDTH) { src(rIfThenElse, o0, p1); tgt(tIF); lhg(setOf(tVAR, tIF)); ctx(G, o0, SR) }
            trans(WIDTH) { src(rIfThenElse, o0, p1); tgt(tVAR); lhg(setOf(tTHEN)); ctx(G, o0, SR) }

            trans(HEIGHT) { src(tIF); tgt(rIfThen, o0, p1); lhg(setOf(tVAR, tIF)); ctx(G, o0, SR) }
            trans(HEIGHT) { src(tIF); tgt(rIfThenElse, o0, p1); lhg(setOf(tVAR, tIF)); ctx(G, o0, SR) }

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_ifthenelse() {
        val parser = LeftCornerParser(ScannerOnDemand(rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "ifXthenYelseZ")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_ifXthenifYthenZelseW() {
        val parser = LeftCornerParser(ScannerOnDemand(rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "ifXthenifYthenZelseW")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = setOf("ifXthenY", "ifXthenYelseZ", "ifXthenifYthenZelseW", "ifXthenYelseifZthenW", "X")
        sentences.forEach {
            val parser = LeftCornerParser(ScannerOnDemand(rrs.nonSkipTerminals), rrs)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size, result.issues.joinToString("\n") { it.toString() })
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(G, o0, SR)
            state(tVAR)
            state(tIF)
            state(G)
            state(S)
            state(rIfThenElse)
            state(rIfThenElse, o0, 4)
            state(rIfThen)
            state(rIfThenElse, o0, 2)
            state(rIfThen, o0, 2)
            state(rExpr, o1, ER)
            state(tTHEN)
            state(rConditional, o0, ER)
            state(tELSE)
            state(rConditional, o1, ER)
            state(rExpr, o0, ER)
            state(RP(rIfThenElse, o0, p1), RP(rIfThen, o0, p1))
            state(rIfThen, o0, p3)
            state(rIfThenElse, o0, p5)
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("abc", "abd")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) result.issues.forEach { println(it) }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertMatches(
            automaton_preBuild, automaton_noBuild, AutomatonTest.MatchConfiguration(
                in_actual_substitue_lookahead_RT_with = setOf(EOT)
            )
        )
    }
}