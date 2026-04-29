/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_expressions_LLstyle : test_AutomatonAbstract() {

    // S = E
    // E = P
    //   | E '+' P

    // S = E
    // E = P | E1
    // E1 = E o P
    // P = a

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = ruleSet("Test") {
        concatenation("S") { ref("E") }
        choiceLongest("E") {
            ref("P")
            ref("E1")
        }
        concatenation("E1") { ref("E"); literal("o"); ref("P") }
        concatenation("P") { literal("a") }
    } as RuntimeRuleSet

    private val S = rrs.rule[0]  // S
    private val E = rrs.rule[1]  // E
    private val _t2 = rrs.rule[2]  // 'o'
    private val E1 = rrs.rule[3]  // E1
    private val _t4 = rrs.rule[4]  // 'a'
    private val P = rrs.rule[5]  // P
    private val rG = rrs.goalRuleFor[S]

    @Test
    fun automaton_parse_aoa() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "aoa")
        println(parser.runtimeRuleSet.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t4, oN, ER)   // 'a'
            state(P, oN, ER)   // P = 'a' .
            state(E, o0, ER)   // E = P .
            state(S, oN, ER)   // S = E .
            state(E1, oN, 1)   // E1 = E . 'o' P
            state(_t2, oN, ER)   // 'o'
            state(E1, oN, 2)   // E1 = E 'o' . P
            state(E1, oN, ER)   // E1 = E 'o' P .
            state(E, o1, ER)   // E = E1 .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t4, oN, ER); lhg(setOf(EOT,_t2)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t4, oN, ER); tgt(P, oN, ER); lhg(setOf(EOT,_t2), setOf(EOT,_t2)); lhg(setOf(RT), setOf(RT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(E1, oN, 2))
            }
            trans(HEIGHT) { src(P, oN, ER); tgt(E, o0, ER); lhg(setOf(EOT,_t2), setOf(EOT,_t2));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GRAFT) { src(P, oN, ER); tgt(E1, oN, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(E1, oN, 2))
            }
            trans(HEIGHT) { src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o0, ER); tgt(E1, oN, 1); lhg(setOf(_t2), setOf(EOT,_t2));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(WIDTH) { src(E1, oN, 1); tgt(_t2, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(E1, oN, 2); lhg(_t4);
                prevPair(RP(rG, oN, SR), RP(E1, oN, 1))
            }
            trans(WIDTH) { src(E1, oN, 2); tgt(_t4, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(E1, oN, ER); tgt(E, o1, ER); lhg(setOf(RT,_t2), setOf(RT,_t2));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o1, ER); tgt(S, oN, ER); lhg(setOf(RT), setOf(RT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o1, ER); tgt(E1, oN, 1); lhg(setOf(_t2), setOf(RT,_t2));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_aoaoaoa() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "aoaoaoa")
        println(parser.runtimeRuleSet.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t4, oN, ER)   // 'a'
            state(P, oN, ER)   // P = 'a' .
            state(E, o0, ER)   // E = P .
            state(S, oN, ER)   // S = E .
            state(E1, oN, 1)   // E1 = E . 'o' P
            state(_t2, oN, ER)   // 'o'
            state(E1, oN, 2)   // E1 = E 'o' . P
            state(E1, oN, ER)   // E1 = E 'o' P .
            state(E, o1, ER)   // E = E1 .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t4, oN, ER); lhg(setOf(EOT,_t2)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t4, oN, ER); tgt(P, oN, ER); lhg(setOf(EOT,_t2), setOf(EOT,_t2)); lhg(setOf(RT), setOf(RT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(E1, oN, 2))
            }
            trans(HEIGHT) { src(P, oN, ER); tgt(E, o0, ER); lhg(setOf(EOT,_t2), setOf(EOT,_t2));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GRAFT) { src(P, oN, ER); tgt(E1, oN, ER); lhg(RT);
                prevPair(RP(rG, oN, SR), RP(E1, oN, 2))
            }
            trans(HEIGHT) { src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o0, ER); tgt(E1, oN, 1); lhg(setOf(_t2), setOf(EOT,_t2));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(WIDTH) { src(E1, oN, 1); tgt(_t2, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t2, oN, ER); tgt(E1, oN, 2); lhg(_t4);
                prevPair(RP(rG, oN, SR), RP(E1, oN, 1))
            }
            trans(WIDTH) { src(E1, oN, 2); tgt(_t4, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(E1, oN, ER); tgt(E, o1, ER); lhg(setOf(RT,_t2), setOf(RT,_t2));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o1, ER); tgt(S, oN, ER); lhg(setOf(RT), setOf(RT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o1, ER); tgt(E1, oN, 1); lhg(setOf(_t2), setOf(RT,_t2));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "aoaoaoa")
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, oN, ER)   // S = E .
            state(E, o0, ER)   // E = P .
            state(E, o1, ER)   // E = E1 .
            state(P, oN, ER)   // P = 'a' .
            state(E1, oN, 1)   // E1 = E . 'o' P
            state(E1, oN, 2)   // E1 = E 'o' . P
            state(E1, oN, ER)   // E1 = E 'o' P .
            state(_t4, oN, ER)   // 'a'
            state(_t2, oN, ER)   // 'o'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t4, oN, ER); lhg(setOf(EOT,_t2)); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o0, ER); tgt(E1, oN, 1); lhg(setOf(_t2), setOf(EOT,_t2));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o1, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o1, ER); tgt(E1, oN, 1); lhg(setOf(_t2), setOf(EOT,_t2));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(P, oN, ER); tgt(E, o0, ER); lhg(setOf(EOT,_t2), setOf(EOT,_t2));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GRAFT) { src(P, oN, ER); tgt(E1, oN, ER); lhg(setOf(EOT,_t2));
                prevPair(RP(rG, oN, SR), RP(E1, oN, 2))
            }
            trans(WIDTH) { src(E1, oN, 1); tgt(_t2, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(WIDTH) { src(E1, oN, 2); tgt(_t4, oN, ER); lhg(setOf(EOT,_t2)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(E1, oN, ER); tgt(E, o1, ER); lhg(setOf(EOT,_t2), setOf(EOT,_t2));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(_t4, oN, ER); tgt(P, oN, ER); lhg(setOf(EOT,_t2), setOf(EOT,_t2));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(E1, oN, 2))
            }
            trans(GRAFT) { src(_t2, oN, ER); tgt(E1, oN, 2); lhg(_t4);
                prevPair(RP(rG, oN, SR), RP(E1, oN, 1))
            }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("a", "aoa", "aoaoa", "aoaoaoa")
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

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild, AutomatonTest.MatchConfiguration(no_lookahead_compare = true))
    }
}