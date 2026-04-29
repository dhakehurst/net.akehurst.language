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

class test_aObOcO : test_AutomatonAbstract() {
    /*
        S = a? b? c?;
     */
    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = ruleSet("Test") {
        concatenation("S") { ref("aOpt"); ref("bOpt"); ref("cOpt") }
        multi("aOpt", 0, 1, "'a'")
        multi("bOpt", 0, 1, "'b'")
        multi("cOpt", 0, 1, "'c'")
        literal("'a'", "a")
        literal("'b'", "b")
        literal("'c'", "c")
    } as RuntimeRuleSet

    private val S = rrs.rule[0]  // S
    private val aOpt = rrs.rule[1]  // aOpt
    private val bOpt = rrs.rule[2]  // bOpt
    private val cOpt = rrs.rule[3]  // cOpt
    private val _t4 = rrs.rule[4]  // 'a'
    private val _t5 = rrs.rule[5]  // 'b'
    private val _t6 = rrs.rule[6]  // 'c'
    private val rG = rrs.goalRuleFor[S]

    @Test
    fun parse_a() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "a")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t4, oN, ER)   // 'a'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(aOpt, LI, ER)   // ['a'] .
            state(S, oN, 1)   // S = aOpt . bOpt cOpt
            state(_t5, oN, ER)   // 'b'
            state(bOpt, LE, ER)   // [EMPTY 'b'] .
            state(S, oN, 2)   // S = aOpt bOpt . cOpt
            state(_t6, oN, ER)   // 'c'
            state(cOpt, LE, ER)   // [EMPTY 'c'] .
            state(S, oN, ER)   // S = aOpt bOpt cOpt .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t4, oN, ER); lhg(setOf(EOT, _t5, _t6)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(setOf(EOT, _t5, _t6)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t4, oN, ER); tgt(aOpt, LI, ER); lhg(setOf(EOT, _t5, _t6), setOf(EOT, _t5, _t6)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(bOpt, LE, ER); lhg(setOf(RT, _t6), setOf(RT, _t6)); prevPair(RP(rG, oN, SR), RP(S, oN, 1)) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(cOpt, LE, ER); lhg(setOf(RT), setOf(RT)); prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(HEIGHT) { src(aOpt, LI, ER); tgt(S, oN, 1); lhg(setOf(EOT, _t5, _t6), setOf(EOT)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(WIDTH) { src(S, oN, 1); tgt(_t5, oN, ER); lhg(setOf(RT, _t6)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 1); tgt(EMPTY_LIST, oN, ER); lhg(setOf(RT, _t6)); ctx(rG, oN, SR) }
            trans(GRAFT) { src(bOpt, LE, ER); tgt(S, oN, 2); lhg(setOf(RT, _t6)); prevPair(RP(rG, oN, SR), RP(S, oN, 1)) }
            trans(WIDTH) { src(S, oN, 2); tgt(_t6, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 2); tgt(EMPTY_LIST, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(GRAFT) { src(cOpt, LE, ER); tgt(S, oN, ER); lhg(RT); prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_b() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "b")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t4, oN, ER)   // 'a'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(aOpt, LE, ER)   // [EMPTY 'a'] .
            state(S, oN, 1)   // S = aOpt . bOpt cOpt
            state(_t5, oN, ER)   // 'b'
            state(bOpt, LI, ER)   // ['b'] .
            state(S, oN, 2)   // S = aOpt bOpt . cOpt
            state(_t6, oN, ER)   // 'c'
            state(cOpt, LE, ER)   // [EMPTY 'c'] .
            state(S, oN, ER)   // S = aOpt bOpt cOpt .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t4, oN, ER); lhg(setOf(EOT,_t5,_t6)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(setOf(EOT,_t5,_t6)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(aOpt, LE, ER); lhg(setOf(EOT,_t5,_t6), setOf(EOT,_t5,_t6));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(cOpt, LE, ER); lhg(setOf(RT), setOf(RT));  prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(HEIGHT) { src(aOpt, LE, ER); tgt(S, oN, 1); lhg(setOf(EOT,_t5,_t6), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(WIDTH) { src(S, oN, 1); tgt(_t5, oN, ER); lhg(setOf(RT,_t6)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 1); tgt(EMPTY_LIST, oN, ER); lhg(setOf(RT,_t6)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t5, oN, ER); tgt(bOpt, LI, ER); lhg(setOf(RT,_t6), setOf(RT,_t6));  prevPair(RP(rG, oN, SR), RP(S, oN, 1)) }
            trans(GRAFT) { src(bOpt, LI, ER); tgt(S, oN, 2); lhg(setOf(RT,_t6));  prevPair(RP(rG, oN, SR), RP(S, oN, 1)) }
            trans(WIDTH) { src(S, oN, 2); tgt(_t6, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 2); tgt(EMPTY_LIST, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(GRAFT) { src(cOpt, LE, ER); tgt(S, oN, ER); lhg(RT);  prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_c() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "c")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t4, oN, ER)   // 'a'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(aOpt, LE, ER)   // [EMPTY 'a'] .
            state(S, oN, 1)   // S = aOpt . bOpt cOpt
            state(_t5, oN, ER)   // 'b'
            state(bOpt, LE, ER)   // [EMPTY 'b'] .
            state(S, oN, 2)   // S = aOpt bOpt . cOpt
            state(_t6, oN, ER)   // 'c'
            state(cOpt, LI, ER)   // ['c'] .
            state(S, oN, ER)   // S = aOpt bOpt cOpt .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t4, oN, ER); lhg(setOf(EOT,_t5,_t6)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(setOf(EOT,_t5,_t6)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(aOpt, LE, ER); lhg(setOf(EOT,_t5,_t6), setOf(EOT,_t5,_t6));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(bOpt, LE, ER); lhg(setOf(RT,_t6), setOf(RT,_t6));  prevPair(RP(rG, oN, SR), RP(S, oN, 1)) }
            trans(HEIGHT) { src(aOpt, LE, ER); tgt(S, oN, 1); lhg(setOf(EOT,_t5,_t6), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(WIDTH) { src(S, oN, 1); tgt(_t5, oN, ER); lhg(setOf(RT,_t6)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 1); tgt(EMPTY_LIST, oN, ER); lhg(setOf(RT,_t6)); ctx(rG, oN, SR) }
            trans(GRAFT) { src(bOpt, LE, ER); tgt(S, oN, 2); lhg(setOf(RT,_t6));  prevPair(RP(rG, oN, SR), RP(S, oN, 1)) }
            trans(WIDTH) { src(S, oN, 2); tgt(_t6, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 2); tgt(EMPTY_LIST, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t6, oN, ER); tgt(cOpt, LI, ER); lhg(setOf(RT), setOf(RT));  prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(GRAFT) { src(cOpt, LI, ER); tgt(S, oN, ER); lhg(RT);  prevPair(RP(rG, oN, SR), RP(S, oN, 2)) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = listOf("", "a", "b", "ab", "c", "ac", "bc", "abc")
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
            state(S, oN, 1)   // S = aOpt . bOpt cOpt
            state(S, oN, 2)   // S = aOpt bOpt . cOpt
            state(S, oN, ER)   // S = aOpt bOpt cOpt .
            state(aOpt, LI, ER)   // ['a'] .
            state(aOpt, LE, ER)   // [EMPTY 'a'] .
            state(bOpt, LI, ER)   // ['b'] .
            state(bOpt, LE, ER)   // [EMPTY 'b'] .
            state(cOpt, LI, ER)   // ['c'] .
            state(cOpt, LE, ER)   // [EMPTY 'c'] .
            state(_t4, oN, ER)   // 'a'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(_t5, oN, ER)   // 'b'
            state(_t6, oN, ER)   // 'c'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t4, oN, ER); lhg(setOf(EOT, _t5, _t6)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(setOf(EOT, _t5, _t6)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 1); tgt(_t5, oN, ER); lhg(setOf(EOT, _t6)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 1); tgt(EMPTY_LIST, oN, ER); lhg(setOf(EOT, _t6)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 2); tgt(_t6, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 2); tgt(EMPTY_LIST, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(GOAL) {
                src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(aOpt, LI, ER); tgt(S, oN, 1); lhg(setOf(EOT, _t5, _t6), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(aOpt, LE, ER); tgt(S, oN, 1); lhg(setOf(EOT, _t5, _t6), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GRAFT) {
                src(bOpt, LI, ER); tgt(S, oN, 2); lhg(setOf(EOT, _t6));
                prevPair(RP(rG, oN, SR), RP(S, oN, 1))
            }
            trans(GRAFT) {
                src(bOpt, LE, ER); tgt(S, oN, 2); lhg(setOf(EOT, _t6));
                prevPair(RP(rG, oN, SR), RP(S, oN, 1))
            }
            trans(GRAFT) {
                src(cOpt, LI, ER); tgt(S, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(S, oN, 2))
            }
            trans(GRAFT) {
                src(cOpt, LE, ER); tgt(S, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(S, oN, 2))
            }
            trans(HEIGHT) {
                src(_t4, oN, ER); tgt(aOpt, LI, ER); lhg(setOf(EOT, _t5, _t6), setOf(EOT, _t5, _t6));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(EMPTY_LIST, oN, ER); tgt(bOpt, LE, ER); lhg(setOf(EOT, _t6), setOf(EOT, _t6));
                prevPair(RP(rG, oN, SR), RP(S, oN, 1))
            }
            trans(HEIGHT) {
                src(EMPTY_LIST, oN, ER); tgt(aOpt, LE, ER); lhg(setOf(EOT, _t5, _t6), setOf(EOT, _t5, _t6));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) {
                src(EMPTY_LIST, oN, ER); tgt(cOpt, LE, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(S, oN, 2))
            }
            trans(HEIGHT) {
                src(_t5, oN, ER); tgt(bOpt, LI, ER); lhg(setOf(EOT, _t6), setOf(EOT, _t6));
                prevPair(RP(rG, oN, SR), RP(S, oN, 1))
            }
            trans(HEIGHT) {
                src(_t6, oN, ER); tgt(cOpt, LI, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(S, oN, 2))
            }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("", "a", "b", "ab", "c", "ac", "bc", "abc")
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