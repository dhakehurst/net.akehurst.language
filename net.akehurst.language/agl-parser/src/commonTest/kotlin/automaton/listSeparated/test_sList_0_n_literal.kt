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

class test_sList_0_n_literal : test_AutomatonAbstract() {
    // S =  ['a' / 'b']* ;

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = ruleSet("Test") {
        sList("S", 0, -1, "'a'", "'b'")
        literal("'a'", "a")
        literal("'b'", "b")
    } as RuntimeRuleSet

    private val S = rrs.rule[0]  // S
    private val _t1 = rrs.rule[1]  // 'a'
    private val _t2 = rrs.rule[2]  // 'b'
    private val rG = rrs.goalRuleFor[S]

    @Test
    fun parse_empty() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "")
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t1, oN, ER)   // 'a'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(S, SE, ER)   // [EMPTY 'a' sep 'b'] .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(EOT,_t2)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(S, SE, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GOAL) { src(S, SE, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_a() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "a")
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t1, oN, ER)   // 'a'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(S, SI, ER)   // ['a' sep 'b'] .
            state(S, SI, 1)   // ['b' . 'a' sep 'b']
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(EOT,_t2)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, SI, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, SI, 1); lhg(setOf(_t2), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GOAL) { src(S, SI, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_aba() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "aba")
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t1, oN, ER)   // 'a'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(S, SI, ER)   // ['a' sep 'b'] .
            state(S, SI, 1)   // ['b' . 'a' sep 'b']
            state(_t2, oN, ER)   // 'b'
            state(S, SI, 2)   // ['a' . 'a' sep 'b']
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(EOT,_t2)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, SI, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, SI, 1); lhg(setOf(_t2), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GRAFT) { src(_t1, oN, ER); tgt(S, SI, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(S, oSI, pSI))
            }
            trans(GRAFT) { src(_t1, oN, ER); tgt(S, SI, 1); lhg(_t2);
                prevPair(RP(rG, oN, SR), RP(S, oSI, pSI))
            }
            trans(GOAL) { src(S, SI, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(WIDTH) { src(S, SI, 1); tgt(_t2, oN, ER); lhg(_t1); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(S, SI, 2); lhg(_t1);
                prevPair(RP(rG, oN, SR), RP(S, oSI, pSS))
            }
            trans(WIDTH) { src(S, SI, 2); tgt(_t1, oN, ER); lhg(setOf(RT,_t2)); ctx(rG, oN, SR) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_empty_and_abababa() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "")
        parser.parseForGoal("S", "abababa")
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t1, oN, ER)   // 'a'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(S, SE, ER)   // [EMPTY 'a' sep 'b'] .
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, SI, ER)   // ['a' sep 'b'] .
            state(S, SI, 1)   // ['b' . 'a' sep 'b']
            state(_t2, oN, ER)   // 'b'
            state(S, SI, 2)   // ['a' . 'a' sep 'b']

            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(EOT,_t2)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, SI, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, SI, 1); lhg(setOf(_t2), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GRAFT) { src(_t1, oN, ER); tgt(S, SI, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(S, oSI, pSI))
            }
            trans(GRAFT) { src(_t1, oN, ER); tgt(S, SI, 1); lhg(_t2);
                prevPair(RP(rG, oN, SR), RP(S, oSI, pSI))
            }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(S, SE, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GOAL) { src(S, SE, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GOAL) { src(S, SI, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(WIDTH) { src(S, SI, 1); tgt(_t2, oN, ER); lhg(_t1); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(S, SI, 2); lhg(_t1);
                prevPair(RP(rG, oN, SR), RP(S, oSI, pSS))
            }
            trans(WIDTH) { src(S, SI, 2); tgt(_t1, oN, ER); lhg(setOf(RT,_t2)); ctx(rG, oN, SR) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, SI, 2)   // ['a' . 'a' sep 'b']
            state(S, SI, 1)   // ['b' . 'a' sep 'b']
            state(S, SI, ER)   // ['a' sep 'b'] .
            state(S, SE, ER)   // [EMPTY 'a' sep 'b'] .
            state(_t1, oN, ER)   // 'a'
            state(_t2, oN, ER)   // 'b'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>

            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(EOT,_t2)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, SI, 2); tgt(_t1, oN, ER); lhg(setOf(EOT,_t2)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, SI, 1); tgt(_t2, oN, ER); lhg(_t1); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, SI, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GOAL) { src(S, SE, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, SI, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, SI, 1); lhg(setOf(_t2), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GRAFT) { src(_t1, oN, ER); tgt(S, SI, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(S, oSI, pSI))
            }
            trans(GRAFT) { src(_t1, oN, ER); tgt(S, SI, 1); lhg(_t2);
                prevPair(RP(rG, oN, SR), RP(S, oSI, pSI))
            }
            trans(GRAFT) { src(_t2, oN, ER); tgt(S, SI, 2); lhg(_t1);
                prevPair(RP(rG, oN, SR), RP(S, oSI, pSS))
            }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(S, SE, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("", "a", "aba", "ababa", "abababa")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) result.issues.forEach { println(it) }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")as ParserStateSet
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