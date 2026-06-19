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

class test_sList_compositeMulti : test_AutomatonAbstract() {

    // S = [nl / ';']*
    // nl = N cnm
    // cnm = cn*
    // cn = ',' N
    // N = 'n'

    val rrs = ruleSet("Test") {
        sList("S", 0, -1, "nl", "SMI")
        concatenation("nl") { ref("'n'"); ref("cnm") }
        multi("cnm", 0, -1, "cn")
        concatenation("cn") { ref("CMR"); ref("'n'"); }
        literal("CMR", ",")
        literal("SMI", ";")
        literal("'n'", "n")
    } as RuntimeRuleSet

    private val S = rrs.rule[0]  // S
    private val nl = rrs.rule[1]  // nl
    private val cnm = rrs.rule[2]  // cnm
    private val cn = rrs.rule[3]  // cn
    private val _t4 = rrs.rule[4]  // CMR
    private val _t5 = rrs.rule[5]  // SMI
    private val _t6 = rrs.rule[6]  // 'n'
    private val rG = rrs.goalRuleFor[S]

    @Test
    fun parse_aba() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "aba")
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t6, oN, ER)   // 'n'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>

            trans(WIDTH) { src(rG, oN, SR); tgt(_t6, oN, ER); lhg(setOf(EOT,_t4,_t5)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
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
            state(S, SI, 2)   // [nl . nl sep SMI]
            state(S, SI, 1)   // [SMI . nl sep SMI]
            state(S, SI, ER)   // [nl sep SMI] .
            state(S, SE, ER)   // [EMPTY nl sep SMI] .
            state(nl, oN, 1)   // nl = 'n' . cnm
            state(nl, oN, ER)   // nl = 'n' cnm .
            state(cnm, LI, 1)   // [cn . cn]
            state(cnm, LI, ER)   // [cn] .
            state(cnm, LE, ER)   // [EMPTY cn] .
            state(cn, oN, 1)   // cn = CMR . 'n'
            state(cn, oN, ER)   // cn = CMR 'n' .
            state(_t6, oN, ER)   // 'n'
            state(_t4, oN, ER)   // CMR(',')
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(_t5, oN, ER)   // SMI(';')

            trans(WIDTH) { src(rG, oN, SR); tgt(_t6, oN, ER); lhg(setOf(EOT,_t4,_t5)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, SI, 2); tgt(_t6, oN, ER); lhg(setOf(EOT,_t4,_t5)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, SI, 1); tgt(_t5, oN, ER); lhg(_t6); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, SI, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GOAL) { src(S, SE, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(WIDTH) { src(nl, oN, 1); tgt(_t4, oN, ER); lhg(_t6); ctx(RP(S, oSI, pSI),RP(rG, oN, SR)) }
            trans(WIDTH) { src(nl, oN, 1); tgt(EMPTY_LIST, oN, ER); lhg(setOf(EOT,_t5)); ctx(RP(S, oSI, pSI),RP(rG, oN, SR)) }
            trans(HEIGHT) { src(nl, oN, ER); tgt(S, SI, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(nl, oN, ER); tgt(S, SI, 1); lhg(setOf(_t5), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GRAFT) { src(nl, oN, ER); tgt(S, SI, ER); lhg(EOT);
                prevPair(RP(rG, oN, SR), RP(S, oSI, pSI))
            }
            trans(GRAFT) { src(nl, oN, ER); tgt(S, SI, 1); lhg(_t5);
                prevPair(RP(rG, oN, SR), RP(S, oSI, pSI))
            }
            trans(WIDTH) { src(cnm, LI, 1); tgt(_t4, oN, ER); lhg(_t6); ctx(nl, oN, 1) }
            trans(GRAFT) { src(cnm, LI, ER); tgt(nl, oN, ER); lhg(setOf(EOT,_t5));
                prevPair(RP(S, oSI, pSI), RP(nl, oN, 1))
                prevPair(RP(rG, oN, SR), RP(nl, oN, 1))
            }
            trans(GRAFT) { src(cnm, LE, ER); tgt(nl, oN, ER); lhg(setOf(EOT,_t5));
                prevPair(RP(S, oSI, pSI), RP(nl, oN, 1))
                prevPair(RP(rG, oN, SR), RP(nl, oN, 1))
            }
            trans(WIDTH) { src(cn, oN, 1); tgt(_t6, oN, ER); lhg(setOf(EOT,_t4,_t5)); ctx(RP(cnm, oLI, 1),RP(nl, oN, 1)) }
            trans(HEIGHT) { src(cn, oN, ER); tgt(cnm, LI, ER); lhg(setOf(EOT,_t5), setOf(EOT,_t5));
                prevPair(RP(S, oSI, pSI), RP(nl, oN, 1))
                prevPair(RP(rG, oN, SR), RP(nl, oN, 1))
            }
            trans(HEIGHT) { src(cn, oN, ER); tgt(cnm, LI, 1); lhg(setOf(_t4), setOf(EOT,_t5));
                prevPair(RP(S, oSI, pSI), RP(nl, oN, 1))
                prevPair(RP(rG, oN, SR), RP(nl, oN, 1))
            }
            trans(GRAFT) { src(cn, oN, ER); tgt(cnm, LI, ER); lhg(setOf(EOT,_t5));
                prevPair(RP(nl, oN, 1), RP(cnm, oLI, 1))
            }
            trans(GRAFT) { src(cn, oN, ER); tgt(cnm, LI, 1); lhg(_t4);
                prevPair(RP(nl, oN, 1), RP(cnm, oLI, 1))
            }
            trans(HEIGHT) { src(_t6, oN, ER); tgt(nl, oN, 1); lhg(setOf(EOT,_t4,_t5), setOf(EOT,_t5));
                prevPair(RP(rG, oN, SR), RP(S, oSI, pSI))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GRAFT) { src(_t6, oN, ER); tgt(cn, oN, ER); lhg(setOf(EOT,_t4,_t5));
                prevPair(RP(cnm, oLI, 1), RP(cn, oN, 1))
                prevPair(RP(nl, oN, 1), RP(cn, oN, 1))
            }
            trans(HEIGHT) { src(_t4, oN, ER); tgt(cn, oN, 1); lhg(setOf(_t6), setOf(EOT,_t4,_t5));
                prevPair(RP(nl, oN, 1), RP(cnm, oLI, 1))
                prevPair(RP(S, oSI, pSI), RP(nl, oN, 1))
                prevPair(RP(rG, oN, SR), RP(nl, oN, 1))
            }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(cnm, LE, ER); lhg(setOf(EOT,_t5), setOf(EOT,_t5));
                prevPair(RP(S, oSI, pSI), RP(nl, oN, 1))
                prevPair(RP(rG, oN, SR), RP(nl, oN, 1))
            }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(S, SE, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(GRAFT) { src(_t5, oN, ER); tgt(S, SI, 2); lhg(_t6);
                prevPair(RP(rG, oN, SR), RP(S, oSI, pSS))
            }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("", "n", "n,n", "n,n,n", "n;n", "n;n;n", "n,n;n,n", "n,n,n;n,n,n;n,n,n")
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