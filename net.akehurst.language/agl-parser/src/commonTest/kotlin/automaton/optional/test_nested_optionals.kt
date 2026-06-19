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

package net.akehurst.language.automaton.leftcorner

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import testFixture.utils.AutomatonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_nested_optionals : test_AutomatonAbstract() {
    /*
        S = 'i' 'a' Rs 'z' ;
        Rs = R+ ;
        R = Os 'i' 't' ;
        Os = Bo Co Do ;
        Bo = 'b'?
        Co = 'c'?
        Do = 'd'?
     */
    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = ruleSet("Test") {
        concatenation("S") { literal("i"); literal("a"); ref("Rs"); literal("z") }
        multi("Rs", 1, -1, "R")
        concatenation("R") { ref("Os"); literal("i"); literal("t") }
        concatenation("Os") { ref("Bo"); ref("Co"); ref("Do") }
        multi("Bo", 0, 1, "'b'")
        multi("Co", 0, 1, "'c'")
        multi("Do", 0, 1, "'d'")
        literal("'b'", "b")
        literal("'c'", "c")
        literal("'d'", "d")
    } as RuntimeRuleSet

    private val _t0 = rrs.rule[0]  // 'i'
    private val _t1 = rrs.rule[1]  // 'a'
    private val _t2 = rrs.rule[2]  // 'z'
    private val S = rrs.rule[3]  // S
    private val Rs = rrs.rule[4]  // Rs
    private val _t5 = rrs.rule[5]  // 't'
    private val R = rrs.rule[6]  // R
    private val Os = rrs.rule[7]  // Os
    private val Bo = rrs.rule[8]  // Bo
    private val Co = rrs.rule[9]  // Co
    private val Do = rrs.rule[10]  // Do
    private val _t11 = rrs.rule[11]  // 'b'
    private val _t12 = rrs.rule[12]  // 'c'
    private val _t13 = rrs.rule[13]  // 'd'
    private val rG = rrs.goalRuleFor[S]

    @Test
    fun parse_iaitz() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "iaitz")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t0, oN, ER)   // 'i'
            state(S, oN, 1)   // S = 'i' . 'a' Rs 'z'
            state(_t1, oN, ER)   // 'a'
            state(S, oN, 2)   // S = 'i' 'a' . Rs 'z'
            state(_t11, oN, ER)   // 'b'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(Bo, LE, ER)   // [EMPTY 'b'] .
            state(Os, oN, 1)   // Os = Bo . Co Do
            state(_t12, oN, ER)   // 'c'
            state(Co, LE, ER)   // [EMPTY 'c'] .
            state(Os, oN, 2)   // Os = Bo Co . Do
            state(_t13, oN, ER)   // 'd'
            state(Do, LE, ER)   // [EMPTY 'd'] .
            state(Os, oN, ER)   // Os = Bo Co Do .
            state(R, oN, 1)   // R = Os . 'i' 't'
            state(R, oN, 2)   // R = Os 'i' . 't'
            state(_t5, oN, ER)   // 't'
            state(R, oN, ER)   // R = Os 'i' 't' .
            state(Rs, LI, ER)   // [R] .
            state(Rs, LI, 1)   // [R . R]
            state(S, oN, 3)   // S = 'i' 'a' Rs . 'z'
            state(_t2, oN, ER)   // 'z'
            state(S, oN, ER)   // S = 'i' 'a' Rs 'z' .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(_t1); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(S, oN, 1); lhg(setOf(_t1), setOf(EOT)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(_t0, oN, ER); tgt(R, oN, 2); lhg(_t5); prevPair(RP(S, oN, 2), RP(R, oN, 1)) }
            trans(WIDTH) { src(S, oN, 1); tgt(_t1, oN, ER); lhg(setOf(_t11, _t12, _t13, _t0)); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t1, oN, ER); tgt(S, oN, 2); lhg(setOf(_t11, _t12, _t13, _t0)); prevPair(RP(rG, oN, SR), RP(S, oN, 1)) }
            trans(WIDTH) { src(S, oN, 2); tgt(_t11, oN, ER); lhg(setOf(_t12, _t13, _t0)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 2); tgt(EMPTY_LIST, oN, ER); lhg(setOf(_t12, _t13, _t0)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(Bo, LE, ER); lhg(setOf(_t12, _t13, _t0), setOf(_t12, _t13, _t0)); prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(Co, LE, ER); lhg(setOf(RT, _t13), setOf(RT, _t13)); prevPair(RP(S, oN, 2), RP(Os, oN, 1)) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(Do, LE, ER); lhg(setOf(RT), setOf(RT)); prevPair(RP(S, oN, 2), RP(Os, oN, 2)) }
            trans(HEIGHT) { src(Bo, LE, ER); tgt(Os, oN, 1); lhg(setOf(_t12, _t13, _t0), setOf(_t0)); prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(WIDTH) { src(Os, oN, 1); tgt(_t12, oN, ER); lhg(setOf(RT, _t13)); ctx(S, oN, 2) }
            trans(WIDTH) { src(Os, oN, 1); tgt(EMPTY_LIST, oN, ER); lhg(setOf(RT, _t13)); ctx(S, oN, 2) }
            trans(GRAFT) { src(Co, LE, ER); tgt(Os, oN, 2); lhg(setOf(RT, _t13)); prevPair(RP(S, oN, 2), RP(Os, oN, 1)) }
            trans(WIDTH) { src(Os, oN, 2); tgt(_t13, oN, ER); lhg(RT); ctx(S, oN, 2) }
            trans(WIDTH) { src(Os, oN, 2); tgt(EMPTY_LIST, oN, ER); lhg(RT); ctx(S, oN, 2) }
            trans(GRAFT) { src(Do, LE, ER); tgt(Os, oN, ER); lhg(RT); prevPair(RP(S, oN, 2), RP(Os, oN, 2)) }
            trans(HEIGHT) { src(Os, oN, ER); tgt(R, oN, 1); lhg(setOf(_t0), setOf(_t11, _t12, _t13, _t0, _t2)); prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(WIDTH) { src(R, oN, 1); tgt(_t0, oN, ER); lhg(_t5); ctx(S, oN, 2) }
            trans(WIDTH) { src(R, oN, 2); tgt(_t5, oN, ER); lhg(RT); ctx(S, oN, 2) }
            trans(GRAFT) { src(_t5, oN, ER); tgt(R, oN, ER); lhg(RT); prevPair(RP(S, oN, 2), RP(R, oN, 2)) }
            trans(HEIGHT) { src(R, oN, ER); tgt(Rs, LI, ER); lhg(setOf(_t2), setOf(_t2)); prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(HEIGHT) { src(R, oN, ER); tgt(Rs, LI, 1); lhg(setOf(_t11, _t12, _t13, _t0), setOf(_t2)); prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(GRAFT) { src(Rs, LI, ER); tgt(S, oN, 3); lhg(_t2); prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(WIDTH) { src(S, oN, 3); tgt(_t2, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(S, oN, ER); lhg(RT); prevPair(RP(rG, oN, SR), RP(S, oN, 3)) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }


    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = listOf("iaitz", "iabitz", "iacitz", "iaditz", "iabcditz", "iaititz", "iaitititz")
        sentences.forEach {
            val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, oN, 1)   // S = 'i' . 'a' Rs 'z'
            state(S, oN, 2)   // S = 'i' 'a' . Rs 'z'
            state(S, oN, 3)   // S = 'i' 'a' Rs . 'z'
            state(S, oN, ER)   // S = 'i' 'a' Rs 'z' .
            state(Rs, LI, 1)   // [R . R]
            state(Rs, LI, ER)   // [R] .
            state(R, oN, 1)   // R = Os . 'i' 't'
            state(R, oN, 2)   // R = Os 'i' . 't'
            state(R, oN, ER)   // R = Os 'i' 't' .
            state(Os, oN, 1)   // Os = Bo . Co Do
            state(Os, oN, 2)   // Os = Bo Co . Do
            state(Os, oN, ER)   // Os = Bo Co Do .
            state(Bo, LI, ER)   // ['b'] .
            state(Bo, LE, ER)   // [EMPTY 'b'] .
            state(Co, LI, ER)   // ['c'] .
            state(Co, LE, ER)   // [EMPTY 'c'] .
            state(Do, LI, ER)   // ['d'] .
            state(Do, LE, ER)   // [EMPTY 'd'] .
            state(_t0, oN, ER)   // 'i'
            state(_t1, oN, ER)   // 'a'
            state(_t11, oN, ER)   // 'b'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(_t12, oN, ER)   // 'c'
            state(_t13, oN, ER)   // 'd'
            state(_t5, oN, ER)   // 't'
            state(_t2, oN, ER)   // 'z'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(_t1); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 1); tgt(_t1, oN, ER); lhg(setOf(_t11, _t12, _t13, _t0)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 2); tgt(_t11, oN, ER); lhg(setOf(_t12, _t13, _t0)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 2); tgt(EMPTY_LIST, oN, ER); lhg(setOf(_t12, _t13, _t0)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 3); tgt(_t2, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(WIDTH) { src(Rs, LI, 1); tgt(_t11, oN, ER); lhg(setOf(_t12, _t13, _t0)); ctx(S, oN, 2) }
            trans(WIDTH) { src(Rs, LI, 1); tgt(EMPTY_LIST, oN, ER); lhg(setOf(_t12, _t13, _t0)); ctx(S, oN, 2) }
            trans(GRAFT) { src(Rs, LI, ER); tgt(S, oN, 3); lhg(_t2); prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(WIDTH) { src(R, oN, 1); tgt(_t0, oN, ER); lhg(_t5); ctx(RP(Rs, oLI, 1), RP(S, oN, 2)) }
            trans(WIDTH) { src(R, oN, 2); tgt(_t5, oN, ER); lhg(setOf(_t11, _t12, _t13, _t0, _t2)); ctx(RP(S, oN, 2), RP(Rs, oLI, 1)) }
            trans(HEIGHT) { src(R, oN, ER); tgt(Rs, LI, ER); lhg(setOf(_t2), setOf(_t2)); prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(HEIGHT) { src(R, oN, ER); tgt(Rs, LI, 1); lhg(setOf(_t11, _t12, _t13, _t0), setOf(_t2)); prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(GRAFT) { src(R, oN, ER); tgt(Rs, LI, ER); lhg(_t2); prevPair(RP(S, oN, 2), RP(Rs, oLI, 1)) }
            trans(GRAFT) { src(R, oN, ER); tgt(Rs, LI, 1); lhg(setOf(_t11, _t12, _t13, _t0)); prevPair(RP(S, oN, 2), RP(Rs, oLI, 1)) }
            trans(WIDTH) { src(Os, oN, 1); tgt(_t12, oN, ER); lhg(setOf(_t13, _t0)); ctx(RP(S, oN, 2), RP(Rs, oLI, 1)) }
            trans(WIDTH) { src(Os, oN, 1); tgt(EMPTY_LIST, oN, ER); lhg(setOf(_t13, _t0)); ctx(RP(S, oN, 2), RP(Rs, oLI, 1)) }
            trans(WIDTH) { src(Os, oN, 2); tgt(_t13, oN, ER); lhg(_t0); ctx(RP(S, oN, 2), RP(Rs, oLI, 1)) }
            trans(WIDTH) { src(Os, oN, 2); tgt(EMPTY_LIST, oN, ER); lhg(_t0); ctx(RP(S, oN, 2), RP(Rs, oLI, 1)) }
            trans(HEIGHT) { src(Os, oN, ER); tgt(R, oN, 1); lhg(setOf(_t0), setOf(_t11, _t12, _t13, _t0, _t2)); prevPair(RP(rG, oN, SR), RP(S, oN, 2)); prevPair (RP(S, oN, 2), RP(Rs, oLI, 1)) }
            trans(HEIGHT) { src(Bo, LI, ER); tgt(Os, oN, 1); lhg(setOf(_t12, _t13, _t0), setOf(_t0)); prevPair(RP(rG, oN, SR), RP(S, oN, 2)); prevPair (RP(S, oN, 2), RP(Rs, oLI, 1)) }
            trans(HEIGHT) { src(Bo, LE, ER); tgt(Os, oN, 1); lhg(setOf(_t12, _t13, _t0), setOf(_t0)); prevPair(RP(S, oN, 2), RP(Rs, oLI, 1)); prevPair (RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(GRAFT) { src(Co, LI, ER); tgt(Os, oN, 2); lhg(setOf(_t13, _t0)); prevPair(RP(S, oN, 2), RP(Os, oN, 1)); prevPair (RP(Rs, oLI, 1), RP(Os, oN, 1)) }
            trans(GRAFT) { src(Co, LE, ER); tgt(Os, oN, 2); lhg(setOf(_t13, _t0)); prevPair(RP(S, oN, 2), RP(Os, oN, 1)); prevPair (RP(Rs, oLI, 1), RP(Os, oN, 1)) }
            trans(GRAFT) { src(Do, LI, ER); tgt(Os, oN, ER); lhg(_t0); prevPair(RP(S, oN, 2), RP(Os, oN, 2)); prevPair (RP(Rs, oLI, 1), RP(Os, oN, 2)) }
            trans(GRAFT) { src(Do, LE, ER); tgt(Os, oN, ER); lhg(_t0); prevPair(RP(S, oN, 2), RP(Os, oN, 2)); prevPair (RP(Rs, oLI, 1), RP(Os, oN, 2)) }
            trans(HEIGHT) { src(_t0, oN, ER); tgt(S, oN, 1); lhg(setOf(_t1), setOf(EOT)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(_t0, oN, ER); tgt(R, oN, 2); lhg(_t5); prevPair(RP(Rs, oLI, 1), RP(R, oN, 1)); prevPair (RP(S, oN, 2), RP(R, oN, 1)) }
            trans(GRAFT) { src(_t1, oN, ER); tgt(S, oN, 2); lhg(setOf(_t11, _t12, _t13, _t0)); prevPair(RP(rG, oN, SR), RP(S, oN, 1)) }
            trans(HEIGHT) { src(_t11, oN, ER); tgt(Bo, LI, ER); lhg(setOf(_t12, _t13, _t0), setOf(_t12, _t13, _t0)); prevPair(RP(rG, oN, SR), RP(S, oN, 2)); prevPair (RP(S, oN, 2), RP(Rs, oLI, 1)) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(Bo, LE, ER); lhg(setOf(_t12, _t13, _t0), setOf(_t12, _t13, _t0)); prevPair(RP(rG, oN, SR), RP(S, oN, 2)); prevPair (RP(S, oN, 2), RP(Rs, oLI, 1)) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(Do, LE, ER); lhg(setOf(_t0), setOf(_t0)); prevPair(RP(S, oN, 2), RP(Os, oN, 2)); prevPair (RP(Rs, oLI, 1), RP(Os, oN, 2)) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(Co, LE, ER); lhg(setOf(_t13, _t0), setOf(_t13, _t0)); prevPair(RP(S, oN, 2), RP(Os, oN, 1)); prevPair (RP(Rs, oLI, 1), RP(Os, oN, 1)) }
            trans(HEIGHT) { src(_t12, oN, ER); tgt(Co, LI, ER); lhg(setOf(_t13, _t0), setOf(_t13, _t0)); prevPair(RP(S, oN, 2), RP(Os, oN, 1)); prevPair (RP(Rs, oLI, 1), RP(Os, oN, 1)) }
            trans(HEIGHT) { src(_t13, oN, ER); tgt(Do, LI, ER); lhg(setOf(_t0), setOf(_t0)); prevPair(RP(S, oN, 2), RP(Os, oN, 2)); prevPair (RP(Rs, oLI, 1), RP(Os, oN, 2)) }
            trans(GRAFT) { src(_t5, oN, ER); tgt(R, oN, ER); lhg(setOf(_t11, _t12, _t13, _t0, _t2)); prevPair(RP(S, oN, 2), RP(R, oN, 2)); prevPair (RP(Rs, oLI, 1), RP(R, oN, 2)) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(S, oN, ER); lhg(EOT); prevPair(RP(rG, oN, SR), RP(S, oN, 3)) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_dmdBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_dmdBuild.nonSkipTerminals), rrs_dmdBuild)
        val sentences = listOf("iaitz", "iabitz", "iacitz", "iaditz", "iabcditz")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) {
                println("Sentence: $sen")
                result.issues.forEach { println(it) }
            }
        }
        val automaton_noBuild = rrs_dmdBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--Build-on-Demand--")
        println(rrs_dmdBuild.usedAutomatonToString("S"))

        val noBuildStates = (automaton_noBuild as ParserStateSet).allBuiltStates
        val preBuildStates = (automaton_preBuild as ParserStateSet).allBuiltStates
        assertEquals(preBuildStates.size, noBuildStates.size)
    }
}