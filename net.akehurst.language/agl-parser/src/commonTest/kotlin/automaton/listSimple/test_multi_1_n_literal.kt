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

import net.akehurst.language.agl.runtime.structure.RulePositionRuntime
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import testFixture.utils.AutomatonTest
import kotlin.test.Test

class test_multi_1_n_literal : test_AutomatonAbstract() {

    // S =  'a'+ ;

    val rrs = ruleSet("Test") {
        multi("S", 1, -1, "'a'")
        literal("'a'", "a")
    } as RuntimeRuleSet

    private val S = rrs.rule[0]  // S
    private val _t1 = rrs.rule[1]  // 'a'
    private val rG = rrs.goalRuleFor[S]

    @Test
    fun parse_aa() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "aa")
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t1, oN, ER)   // 'a'
            state(S, LI, ER)   // ['a'] .
            state(S, LI, 1)   // ['a' . 'a']
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(EOT,_t1)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, LI, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, LI, 1); lhg(setOf(_t1), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GRAFT) { src(_t1, oN, ER); tgt(S, LI, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(S, oLI, 1))
            }
            trans(GRAFT) { src(_t1, oN, ER); tgt(S, LI, 1); lhg(_t1);
                prevPair(RP(rG, oN, SR), RP(S, oLI, 1))
            }
            trans(GOAL) { src(S, LI, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(WIDTH) { src(S, LI, 1); tgt(_t1, oN, ER); lhg(setOf(RT,_t1)); ctx(rG, oN, SR) }
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
            state(S, LI, 1)   // ['a' . 'a']
            state(S, LI, ER)   // ['a'] .
            state(_t1, oN, ER)   // 'a'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t1, oN, ER); lhg(setOf(EOT,_t1)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, LI, 1); tgt(_t1, oN, ER); lhg(setOf(EOT,_t1)); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, LI, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, LI, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(_t1, oN, ER); tgt(S, LI, 1); lhg(setOf(_t1), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GRAFT) { src(_t1, oN, ER); tgt(S, LI, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(S, oLI, 1))
            }
            trans(GRAFT) { src(_t1, oN, ER); tgt(S, LI, 1); lhg(_t1);
                prevPair(RP(rG, oN, SR), RP(S, oLI, 1))
            }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

        @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("a", "aa", "aaa")
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

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild, config = AutomatonTest.MatchConfiguration(no_lookahead_compare = true))
    }
}