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

package net.akehurst.language.automaton.leftcorner.infixExpressions

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.automaton.leftcorner.automaton
import net.akehurst.language.automaton.leftcorner.test_AutomatonAbstract
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import testFixture.utils.AutomatonTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_bodmas_sList_root_choiceLongest : test_AutomatonAbstract() {

    // S =  E
    // E = R | M | A
    // R = v
    // A = [ E / 'a' ]2+
    // M = [ E / 'm' ]2+     mul = [ expr / '*' ]2+

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = ruleSet("Test") {
        concatenation("S") { ref("E") }
        choiceLongest("E") {
            ref("R")
            ref("M")
            ref("A")
        }
        concatenation("R") { literal("v") }
        sList("M", 2, -1, "E", "'m'")
        sList("A", 2, -1, "E", "'a'")
        literal("'m'", "m")
        literal("'a'", "a")
    } as RuntimeRuleSet

    private val S = rrs.rule[0]  // S
    private val E = rrs.rule[1]  // E
    private val _t2 = rrs.rule[2]  // 'v'
    private val R = rrs.rule[3]  // R
    private val M = rrs.rule[4]  // M
    private val A = rrs.rule[5]  // A
    private val _t6 = rrs.rule[6]  // 'm'
    private val _t7 = rrs.rule[7]  // 'a'
    private val rG = rrs.goalRuleFor[S]

    @Test
    fun automaton_parse_v() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "v")
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t2, oN, ER)   // 'v'
            state(R, oN, ER)   // R = 'v' .
            state(E, o0, ER)   // E = R .
            state(S, oN, ER)   // S = E .
            state(M, SI, 1)   // ['m' . E sep 'm']
            state(A, SI, 1)   // ['a' . E sep 'a']
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t2, oN, ER); lhg(setOf(EOT, _t6, _t7)); ctx(rG, oN, SR) }
            trans(HEIGHT) {
                src(_t2, oN, ER); tgt(R, oN, ER); lhg(setOf(EOT, _t6, _t7), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(R, oN, ER); tgt(E, o0, ER); lhg(setOf(EOT, _t6, _t7), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(M, SI, 1); lhg(setOf(_t6), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(A, SI, 1); lhg(setOf(_t7), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GOAL) {
                src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_vmv() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "vmv")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t2, oN, ER)   // 'v'
            state(R, oN, ER)   // R = 'v' .
            state(E, o0, ER)   // E = R .
            state(S, oN, ER)   // S = E .
            state(M, SI, 1)   // ['m' . E sep 'm']
            state(A, SI, 1)   // ['a' . E sep 'a']
            state(_t6, oN, ER)   // 'm'
            state(M, SI, 2)   // [E . E sep 'm']
            state(M, SI, ER)   // [E sep 'm'] .
            state(E, o1, ER)   // E = M .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t2, oN, ER); lhg(setOf(EOT, _t6, _t7)); ctx(rG, oN, SR) }
            trans(HEIGHT) {
                src(_t2, oN, ER); tgt(R, oN, ER); lhg(setOf(RT, _t6, _t7), setOf(RT, _t6, _t7)); lhg(setOf(EOT, _t6, _t7), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(R, oN, ER); tgt(E, o0, ER); lhg(setOf(RT, _t6, _t7), setOf(RT, _t6, _t7)); lhg(setOf(EOT, _t6, _t7), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(M, SI, 1); lhg(setOf(_t6), setOf(RT, EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(A, SI, 1); lhg(setOf(_t7), setOf(RT, EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(M, SI, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(M, SI, 1); lhg(_t6);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(GOAL) {
                src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(WIDTH) { src(M, SI, 1); tgt(_t6, oN, ER); lhg(_t2); ctx(rG, oN, SR) }
            trans(GRAFT) {
                src(_t6, oN, ER); tgt(M, SI, 2); lhg(_t2);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSS))
            }
            trans(WIDTH) { src(M, SI, 2); tgt(_t2, oN, ER); lhg(setOf(RT, _t6, _t7)); ctx(rG, oN, SR) }
            trans(HEIGHT) {
                src(M, SI, ER); tgt(E, o1, ER); lhg(setOf(RT, _t6, _t7), setOf(RT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(S, oN, ER); lhg(setOf(RT), setOf(RT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(M, SI, 1); lhg(setOf(_t6), setOf(RT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(A, SI, 1); lhg(setOf(_t7), setOf(RT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_sentences() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val sentences = listOf("v", "vav", "vavav", "vavav", "vmv", "vmvmv", "vmvav", "vavmv")
        //val sentences = listOf( "vav")
        sentences.forEach {
            println(it)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }
        println(rrs.usedAutomatonToString("S"))
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t2, oN, ER)   // 'v'
            state(R, oN, ER)   // R = 'v' .
            state(E, o0, ER)   // E = R .
            state(S, oN, ER)   // S = E .
            state(M, SI, 1)   // ['m' . E sep 'm']
            state(A, SI, 1)   // ['a' . E sep 'a']
            state(rG, oN, ER)   // <GOAL> = S .
            state(_t7, oN, ER)   // 'a'
            state(A, SI, 2)   // [E . E sep 'a']
            state(A, SI, ER)   // [E sep 'a'] .
            state(E, o2, ER)   // E = A .
            state(_t6, oN, ER)   // 'm'
            state(M, SI, 2)   // [E . E sep 'm']
            state(M, SI, ER)   // [E sep 'm'] .
            state(E, o1, ER)   // E = M .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t2, oN, ER); lhg(setOf(EOT, _t6, _t7)); ctx(rG, oN, SR) }
            trans(HEIGHT) {
                src(_t2, oN, ER); tgt(R, oN, ER); lhg(setOf(RT, _t7, _t6), setOf(RT, _t7, _t6)); lhg(setOf(EOT, _t6, _t7), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(R, oN, ER); tgt(E, o0, ER); lhg(setOf(RT, _t7, _t6), setOf(RT, _t7, _t6)); lhg(setOf(EOT, _t6, _t7), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(M, SI, 1); lhg(setOf(_t6), setOf(RT, EOT, _t7, _t6));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(A, SI, 1); lhg(setOf(_t7), setOf(RT, EOT, _t7, _t6));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(A, SI, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(A, SI, 1); lhg(_t7);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(M, SI, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(M, SI, 1); lhg(_t6);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(GOAL) {
                src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(WIDTH) { src(M, SI, 1); tgt(_t6, oN, ER); lhg(_t2); ctx(RP(rG, oN, SR), RP(A, oSI, pSI)) }
            trans(WIDTH) { src(A, SI, 1); tgt(_t7, oN, ER); lhg(_t2); ctx(RP(rG, oN, SR), RP(M, oSI, pSI)) }
            trans(GRAFT) {
                src(_t7, oN, ER); tgt(A, SI, 2); lhg(_t2);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSS))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSS))
            }
            trans(WIDTH) { src(A, SI, 2); tgt(_t2, oN, ER); lhg(setOf(RT, _t7, _t6)); ctx(RP(rG, oN, SR), RP(M, oSI, pSI)) }
            trans(HEIGHT) {
                src(A, SI, ER); tgt(E, o2, ER); lhg(setOf(RT, _t7, _t6), setOf(RT, _t7, _t6));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(S, oN, ER); lhg(setOf(RT), setOf(RT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(M, SI, 1); lhg(setOf(_t6), setOf(RT, _t7, _t6));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(A, SI, 1); lhg(setOf(_t7), setOf(RT, _t7, _t6));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o2, ER); tgt(M, SI, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o2, ER); tgt(M, SI, 1); lhg(_t6);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(_t6, oN, ER); tgt(M, SI, 2); lhg(_t2);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSS))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSS))
            }
            trans(WIDTH) { src(M, SI, 2); tgt(_t2, oN, ER); lhg(setOf(RT, _t7, _t6)); ctx(RP(rG, oN, SR), RP(A, oSI, pSI)) }
            trans(HEIGHT) {
                src(M, SI, ER); tgt(E, o1, ER); lhg(setOf(RT, _t7, _t6), setOf(RT, _t7, _t6));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(S, oN, ER); lhg(setOf(RT), setOf(RT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(M, SI, 1); lhg(setOf(_t6), setOf(RT, _t7, _t6));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(A, SI, 1); lhg(setOf(_t7), setOf(RT, _t7, _t6));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o1, ER); tgt(A, SI, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o1, ER); tgt(A, SI, 1); lhg(_t7);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = listOf("v", "vav", "vavav", "vmv", "vmvmv", "vmvav", "vavmv")
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
            state(S, oN, ER)   // S = E .
            state(E, o0, ER)   // E = R .
            state(E, o1, ER)   // E = M .
            state(E, o2, ER)   // E = A .
            state(R, oN, ER)   // R = 'v' .
            state(M, SI, 2)   // [E . E sep 'm']
            state(M, SI, 1)   // ['m' . E sep 'm']
            state(M, SI, ER)   // [E sep 'm'] .
            state(A, SI, 2)   // [E . E sep 'a']
            state(A, SI, 1)   // ['a' . E sep 'a']
            state(A, SI, ER)   // [E sep 'a'] .
            state(_t2, oN, ER)   // 'v'
            state(_t6, oN, ER)   // 'm'
            state(_t7, oN, ER)   // 'a'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t2, oN, ER); lhg(setOf(EOT, _t6, _t7)); ctx(rG, oN, SR) }
            trans(GOAL) {
                src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(M, SI, 1); lhg(setOf(_t6), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(A, SI, 1); lhg(setOf(_t7), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(M, SI, ER); lhg(setOf(EOT, _t6, _t7));
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(M, SI, 1); lhg(_t6);
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(A, SI, ER); lhg(setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(A, SI, 1); lhg(_t7);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(M, SI, 1); lhg(setOf(_t6), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(A, SI, 1); lhg(setOf(_t7), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o1, ER); tgt(A, SI, ER); lhg(setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o1, ER); tgt(A, SI, 1); lhg(_t7);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o1, ER); tgt(M, SI, ER); lhg(setOf(EOT, _t6, _t7));
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o1, ER); tgt(M, SI, 1); lhg(_t6);
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(M, SI, 1); lhg(setOf(_t6), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(A, SI, 1); lhg(setOf(_t7), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o2, ER); tgt(A, SI, ER); lhg(setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o2, ER); tgt(A, SI, 1); lhg(_t7);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o2, ER); tgt(M, SI, ER); lhg(setOf(EOT, _t6, _t7));
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o2, ER); tgt(M, SI, 1); lhg(_t6);
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(R, oN, ER); tgt(E, o0, ER); lhg(setOf(EOT, _t6, _t7), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(WIDTH) { src(M, SI, 2); tgt(_t2, oN, ER); lhg(setOf(EOT, _t6, _t7)); ctx(RP(M, oSI, pSI), RP(A, oSI, pSI), RP(rG, oN, SR)) }
            trans(WIDTH) { src(M, SI, 1); tgt(_t6, oN, ER); lhg(_t2); ctx(RP(M, oSI, pSI), RP(A, oSI, pSI), RP(rG, oN, SR)) }
            trans(HEIGHT) {
                src(M, SI, ER); tgt(E, o1, ER); lhg(setOf(EOT, _t6, _t7), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(WIDTH) { src(A, SI, 2); tgt(_t2, oN, ER); lhg(setOf(EOT, _t6, _t7)); ctx(RP(rG, oN, SR), RP(A, oSI, pSI), RP(M, oSI, pSI)) }
            trans(WIDTH) { src(A, SI, 1); tgt(_t7, oN, ER); lhg(_t2); ctx(RP(A, oSI, pSI), RP(M, oSI, pSI), RP(rG, oN, SR)) }
            trans(HEIGHT) {
                src(A, SI, ER); tgt(E, o2, ER); lhg(setOf(EOT, _t6, _t7), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(_t2, oN, ER); tgt(R, oN, ER); lhg(setOf(EOT, _t6, _t7), setOf(EOT, _t6, _t7));
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GRAFT) {
                src(_t6, oN, ER); tgt(M, SI, 2); lhg(_t2);
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSS))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSS))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSS))
            }
            trans(GRAFT) {
                src(_t7, oN, ER); tgt(A, SI, 2); lhg(_t2);
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSS))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSS))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSS))
            }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Ignore
    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("v", "vav", "vavav", "vmv", "vmvmv", "vmvav", "vavmv")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) {
                println("Sentence: $sen")
                result.issues.forEach { println(it) }
            }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild, config = AutomatonTest.MatchConfiguration(no_lookahead_compare = true))
    }
}