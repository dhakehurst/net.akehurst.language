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

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
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

class test_vma_sList_root_choicePriority : test_AutomatonAbstract() {

    // S =  E ;
    // E = R < M < A ;
    // R = V ;
    // A = [ E / 'a' ]2+ ;
    // M = [ E / 'm' ]2+ ;
    // V = 'v'
    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = ruleSet("Test") {
        concatenation("S") { ref("E") }
        choicePriority("E") {
            ref("R")
            ref("M")
            ref("A")
        }
        concatenation("R") { ref("V") }
        concatenation("V") { literal("v") }
        sList("M", 2, -1, "E", "'m'")
        sList("A", 2, -1, "E", "'a'")
        literal("'m'", "m")
        literal("'a'", "a")
    } as RuntimeRuleSet

    private val S = rrs.rule[0]  // S
    private val E = rrs.rule[1]  // E
    private val R = rrs.rule[2]  // R
    private val _t3 = rrs.rule[3]  // 'v'
    private val V = rrs.rule[4]  // V
    private val M = rrs.rule[5]  // M
    private val A = rrs.rule[6]  // A
    private val _t7 = rrs.rule[7]  // 'm'
    private val _t8 = rrs.rule[8]  // 'a'
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
            state(_t3, oN, ER)   // 'v'
            state(V, oN, ER)   // V = 'v' .
            state(R, oN, ER)   // R = V .
            state(E, o0, ER)   // E = R .
            state(S, oN, ER)   // S = E .
            state(A, SI, 1)   // ['a' . E sep 'a']
            state(M, SI, 1)   // ['m' . E sep 'm']
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t3, oN, ER); lhg(setOf(EOT, _t7, _t8)); ctx(rG, oN, SR) }
            trans(HEIGHT) {
                src(_t3, oN, ER); tgt(V, oN, ER); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(V, oN, ER); tgt(R, oN, ER); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(R, oN, ER); tgt(E, o0, ER); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(A, SI, 1); lhg(setOf(_t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(M, SI, 1); lhg(setOf(_t7), setOf(EOT, _t7, _t8));
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
    fun automaton_parse_vavav() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "vavav")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t3, oN, ER)   // 'v'
            state(V, oN, ER)   // V = 'v' .
            state(R, oN, ER)   // R = V .
            state(E, o0, ER)   // E = R .
            state(S, oN, ER)   // S = E .
            state(A, SI, 1)   // ['a' . E sep 'a']
            state(M, SI, 1)   // ['m' . E sep 'm']
            state(_t8, oN, ER)   // 'a'
            state(A, SI, 2)   // [E . E sep 'a']
            state(A, SI, ER)   // [E sep 'a'] .
            state(E, o2, ER)   // E = A .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t3, oN, ER); lhg(setOf(EOT, _t7, _t8)); ctx(rG, oN, SR) }
            trans(HEIGHT) {
                src(_t3, oN, ER); tgt(V, oN, ER); lhg(setOf(RT, _t8, _t7), setOf(RT, _t8, _t7)); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(V, oN, ER); tgt(R, oN, ER); lhg(setOf(RT, _t8, _t7), setOf(RT, _t8, _t7)); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(R, oN, ER); tgt(E, o0, ER); lhg(setOf(RT, _t8, _t7), setOf(RT, _t8, _t7)); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(M, SI, 1); lhg(setOf(_t7), setOf(RT, EOT, _t8, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(A, SI, 1); lhg(setOf(_t8), setOf(RT, EOT, _t8, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(A, SI, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(A, SI, 1); lhg(_t8);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
            trans(GOAL) {
                src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(WIDTH) { src(A, SI, 1); tgt(_t8, oN, ER); lhg(_t3); ctx(rG, oN, SR) }
            trans(GRAFT) {
                src(_t8, oN, ER); tgt(A, SI, 2); lhg(_t3);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSS))
            }
            trans(WIDTH) { src(A, SI, 2); tgt(_t3, oN, ER); lhg(setOf(RT, _t8, _t7)); ctx(rG, oN, SR) }
            trans(HEIGHT) {
                src(A, SI, ER); tgt(E, o2, ER); lhg(setOf(RT, _t8, _t7), setOf(RT, _t8, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(S, oN, ER); lhg(setOf(RT), setOf(RT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(A, SI, 1); lhg(setOf(_t8), setOf(RT, _t8, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(M, SI, 1); lhg(setOf(_t7), setOf(RT, _t8, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_vmvav() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "vmvav")
        println(result.sppt!!.toStringAllWithIndent("  "))
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t3, oN, ER)   // 'v'
            state(V, oN, ER)   // V = 'v' .
            state(R, oN, ER)   // R = V .
            state(E, o0, ER)   // E = R .
            state(S, oN, ER)   // S = E .
            state(A, SI, 1)   // ['a' . E sep 'a']
            state(M, SI, 1)   // ['m' . E sep 'm']
            state(_t7, oN, ER)   // 'm'
            state(M, SI, 2)   // [E . E sep 'm']
            state(M, SI, ER)   // [E sep 'm'] .
            state(_t8, oN, ER)   // 'a'
            state(E, o1, ER)   // E = M .
            state(A, SI, 2)   // [E . E sep 'a']
            state(A, SI, ER)   // [E sep 'a'] .
            state(E, o2, ER)   // E = A .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t3, oN, ER); lhg(setOf(EOT, _t7, _t8)); ctx(rG, oN, SR) }
            trans(HEIGHT) {
                src(_t3, oN, ER); tgt(V, oN, ER); lhg(setOf(RT, _t7, _t8), setOf(RT, _t7, _t8)); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(V, oN, ER); tgt(R, oN, ER); lhg(setOf(RT, _t7, _t8), setOf(RT, _t7, _t8)); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(R, oN, ER); tgt(E, o0, ER); lhg(setOf(RT, _t7, _t8), setOf(RT, _t7, _t8)); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(M, SI, 1); lhg(setOf(_t7), setOf(RT, EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(A, SI, 1); lhg(setOf(_t8), setOf(RT, EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(M, SI, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(M, SI, 1); lhg(_t7);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(A, SI, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(A, SI, 1); lhg(_t8);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GOAL) {
                src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(WIDTH) { src(A, SI, 1); tgt(_t8, oN, ER); lhg(_t3); ctx(RP(M, oSI, pSI), RP(rG, oN, SR)) }
            trans(WIDTH) { src(M, SI, 1); tgt(_t7, oN, ER); lhg(_t3); ctx(rG, oN, SR) }
            trans(GRAFT) {
                src(_t7, oN, ER); tgt(M, SI, 2); lhg(_t3);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSS))
            }
            trans(WIDTH) { src(M, SI, 2); tgt(_t3, oN, ER); lhg(setOf(RT, _t7, _t8)); ctx(rG, oN, SR) }
            trans(HEIGHT) {
                src(M, SI, ER); tgt(E, o1, ER); lhg(setOf(RT, _t7, _t8), setOf(RT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GRAFT) {
                src(_t8, oN, ER); tgt(A, SI, 2); lhg(_t3);
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSS))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSS))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(S, oN, ER); lhg(setOf(RT), setOf(RT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(A, SI, 1); lhg(setOf(_t8), setOf(RT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(M, SI, 1); lhg(setOf(_t7), setOf(RT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(WIDTH) { src(A, SI, 2); tgt(_t3, oN, ER); lhg(setOf(RT, _t7, _t8)); ctx(RP(rG, oN, SR), RP(M, oSI, pSI)) }
            trans(HEIGHT) {
                src(A, SI, ER); tgt(E, o2, ER); lhg(setOf(RT, _t7, _t8), setOf(RT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(S, oN, ER); lhg(setOf(RT), setOf(RT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(M, SI, 1); lhg(setOf(_t7), setOf(RT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(A, SI, 1); lhg(setOf(_t8), setOf(RT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o2, ER); tgt(M, SI, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o2, ER); tgt(M, SI, 1); lhg(_t7);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
            }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_vavmvav() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "vavmvav")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t3, oN, ER)   // 'v'
            state(V, oN, ER)   // V = 'v' .
            state(R, oN, ER)   // R = V .
            state(E, o0, ER)   // E = R .
            state(S, oN, ER)   // S = E .
            state(A, SI, 1)   // ['a' . E sep 'a']
            state(M, SI, 1)   // ['m' . E sep 'm']
            state(_t8, oN, ER)   // 'a'
            state(A, SI, 2)   // [E . E sep 'a']
            state(A, SI, ER)   // [E sep 'a'] .
            state(_t7, oN, ER)   // 'm'
            state(E, o2, ER)   // E = A .
            state(M, SI, 2)   // [E . E sep 'm']
            state(M, SI, ER)   // [E sep 'm'] .
            state(E, o1, ER)   // E = M .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t3, oN, ER); lhg(setOf(EOT, _t7, _t8)); ctx(rG, oN, SR) }
            trans(HEIGHT) {
                src(_t3, oN, ER); tgt(V, oN, ER); lhg(setOf(RT, _t8, _t7), setOf(RT, _t8, _t7)); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(V, oN, ER); tgt(R, oN, ER); lhg(setOf(RT, _t8, _t7), setOf(RT, _t8, _t7)); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(R, oN, ER); tgt(E, o0, ER); lhg(setOf(RT, _t8, _t7), setOf(RT, _t8, _t7)); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(M, SI, 1); lhg(setOf(_t7), setOf(RT, EOT, _t8, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(A, SI, 1); lhg(setOf(_t8), setOf(RT, EOT, _t8, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(A, SI, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(A, SI, 1); lhg(_t8);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(M, SI, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(M, SI, 1); lhg(_t7);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(GOAL) {
                src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(WIDTH) { src(A, SI, 1); tgt(_t8, oN, ER); lhg(_t3); ctx(RP(rG, oN, SR), RP(M, oSI, pSI)) }
            trans(WIDTH) { src(M, SI, 1); tgt(_t7, oN, ER); lhg(_t3); ctx(RP(A, oSI, pSI), RP(rG, oN, SR)) }
            trans(GRAFT) {
                src(_t8, oN, ER); tgt(A, SI, 2); lhg(_t3);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSS))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSS))
            }
            trans(WIDTH) { src(A, SI, 2); tgt(_t3, oN, ER); lhg(setOf(RT, _t8, _t7)); ctx(RP(rG, oN, SR), RP(M, oSI, pSI)) }
            trans(HEIGHT) {
                src(A, SI, ER); tgt(E, o2, ER); lhg(setOf(RT, _t8, _t7), setOf(RT, _t8, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(_t7, oN, ER); tgt(M, SI, 2); lhg(_t3);
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSS))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSS))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(S, oN, ER); lhg(setOf(RT), setOf(RT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(M, SI, 1); lhg(setOf(_t7), setOf(RT, _t8, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(A, SI, 1); lhg(setOf(_t8), setOf(RT, _t8, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o2, ER); tgt(M, SI, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o2, ER); tgt(M, SI, 1); lhg(_t7);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(WIDTH) { src(M, SI, 2); tgt(_t3, oN, ER); lhg(setOf(RT, _t8, _t7)); ctx(RP(rG, oN, SR), RP(A, oSI, pSI)) }
            trans(HEIGHT) {
                src(M, SI, ER); tgt(E, o1, ER); lhg(setOf(RT, _t8, _t7), setOf(RT, _t8, _t7));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(M, SI, 1); lhg(setOf(_t7), setOf(RT, _t8, _t7));
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(A, SI, 1); lhg(setOf(_t8), setOf(RT, _t8, _t7));
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o1, ER); tgt(A, SI, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o1, ER); tgt(A, SI, 1); lhg(_t8);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
            }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "v/v")

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, oN, ER)   // S = E .
            state(E, o0, ER)   // E = R .
            state(E, o1, ER)   // E = M .
            state(E, o2, ER)   // E = A .
            state(R, oN, ER)   // R = V .
            state(V, oN, ER)   // V = 'v' .
            state(M, SI, 2)   // [E . E sep 'm']
            state(M, SI, 1)   // ['m' . E sep 'm']
            state(M, SI, ER)   // [E sep 'm'] .
            state(A, SI, 2)   // [E . E sep 'a']
            state(A, SI, 1)   // ['a' . E sep 'a']
            state(A, SI, ER)   // [E sep 'a'] .
            state(_t3, oN, ER)   // 'v'
            state(_t7, oN, ER)   // 'm'
            state(_t8, oN, ER)   // 'a'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t3, oN, ER); lhg(setOf(EOT, _t7, _t8)); ctx(rG, oN, SR) }
            trans(GOAL) {
                src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(A, SI, 1); lhg(setOf(_t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(M, SI, 1); lhg(setOf(_t7), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(A, SI, ER); lhg(setOf(EOT, _t7, _t8));
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(A, SI, 1); lhg(_t8);
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(M, SI, ER); lhg(setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o0, ER); tgt(M, SI, 1); lhg(_t7);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(A, SI, 1); lhg(setOf(_t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(M, SI, 1); lhg(setOf(_t7), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o1, ER); tgt(A, SI, ER); lhg(setOf(EOT, _t7, _t8));
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o1, ER); tgt(A, SI, 1); lhg(_t8);
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o1, ER); tgt(M, SI, ER); lhg(setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o1, ER); tgt(M, SI, 1); lhg(_t7);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(M, SI, 1); lhg(setOf(_t7), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(E, o2, ER); tgt(A, SI, 1); lhg(setOf(_t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o2, ER); tgt(M, SI, ER); lhg(setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o2, ER); tgt(M, SI, 1); lhg(_t7);
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o2, ER); tgt(A, SI, ER); lhg(setOf(EOT, _t7, _t8));
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(GRAFT) {
                src(E, o2, ER); tgt(A, SI, 1); lhg(_t8);
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
            }
            trans(HEIGHT) {
                src(R, oN, ER); tgt(E, o0, ER); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(V, oN, ER); tgt(R, oN, ER); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(WIDTH) { src(M, SI, 2); tgt(_t3, oN, ER); lhg(setOf(EOT, _t7, _t8)); ctx(RP(rG, oN, SR), RP(M, oSI, pSI), RP(A, oSI, pSI)) }
            trans(WIDTH) { src(M, SI, 1); tgt(_t7, oN, ER); lhg(_t3); ctx(RP(M, oSI, pSI), RP(A, oSI, pSI), RP(rG, oN, SR)) }
            trans(HEIGHT) {
                src(M, SI, ER); tgt(E, o1, ER); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(WIDTH) { src(A, SI, 2); tgt(_t3, oN, ER); lhg(setOf(EOT, _t7, _t8)); ctx(RP(A, oSI, pSI), RP(rG, oN, SR), RP(M, oSI, pSI)) }
            trans(WIDTH) { src(A, SI, 1); tgt(_t8, oN, ER); lhg(_t3); ctx(RP(rG, oN, SR), RP(A, oSI, pSI), RP(M, oSI, pSI)) }
            trans(HEIGHT) {
                src(A, SI, ER); tgt(E, o2, ER); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(_t3, oN, ER); tgt(V, oN, ER); lhg(setOf(EOT, _t7, _t8), setOf(EOT, _t7, _t8));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSI))
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSI))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSI))
            }
            trans(GRAFT) {
                src(_t7, oN, ER); tgt(M, SI, 2); lhg(_t3);
                prevPair(RP(M, oSI, pSI), RP(M, oSI, pSS))
                prevPair(RP(A, oSI, pSI), RP(M, oSI, pSS))
                prevPair(RP(rG, oN, SR), RP(M, oSI, pSS))
            }
            trans(GRAFT) {
                src(_t8, oN, ER); tgt(A, SI, 2); lhg(_t3);
                prevPair(RP(rG, oN, SR), RP(A, oSI, pSS))
                prevPair(RP(A, oSI, pSI), RP(A, oSI, pSS))
                prevPair(RP(M, oSI, pSI), RP(A, oSI, pSS))
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
        val sentences = listOf("v", "vav", "vavav", "vmv", "vmvmv", "vavmv", "vmvav")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) result.issues.forEach { println("$sen: $it") }
        }
        val automaton_preBuild = rrs_preBuild.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")

        println("--No Build Run--")
        val result_noBuild = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild).parseForGoal("S", "vmvav")
        println(result_noBuild.sppt!!.toStringAllWithIndent("  "))

        println("--Build Run--")
        val result_build = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_preBuild.nonSkipTerminals), rrs_preBuild).parseForGoal("S", "vmvav")
        println(result_build.sppt!!.toStringAllWithIndent("  "))

        println("--Build SM--")
        println(rrs_preBuild.usedAutomatonToString("S"))

        println("--No Build SM--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild, config = AutomatonTest.MatchConfiguration(no_lookahead_compare = true))
    }
}