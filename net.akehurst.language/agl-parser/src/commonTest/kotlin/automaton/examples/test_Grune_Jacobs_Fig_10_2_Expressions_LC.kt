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

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
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

class test_Grune_Jacobs_Fig_10_2_Expressions_LC : test_AutomatonAbstract() {

    //    S = E
    //    E = E1 | T
    //    E1 = E a T
    //    T = T1 | F
    //    T1 = T m F
    //    F = v | F2
    //    F2 = ( E )

    private val rrs = ruleSet("Test") {
        concatenation("S") { ref("E") }
        choiceLongest("E") {
            ref("E1")
            ref("T")
        }
        concatenation("E1") { ref("E"); literal("a"); ref("T") }
        choiceLongest("T") {
            ref("T1")
            ref("F")
        }
        concatenation("T1") { ref("T"); literal("m"); ref("F") }
        choiceLongest("F") {
            literal("v")
            ref("F2")
        }
        concatenation("F2") { literal("("); ref("E"); literal(")") }
    } as RuntimeRuleSet

    private val S = rrs.rule[0]  // S
    private val E = rrs.rule[1]  // E
    private val _t2 = rrs.rule[2]  // 'a'
    private val E1 = rrs.rule[3]  // E1
    private val T = rrs.rule[4]  // T
    private val _t5 = rrs.rule[5]  // 'm'
    private val T1 = rrs.rule[6]  // T1
    private val _t7 = rrs.rule[7]  // 'v'
    private val F = rrs.rule[8]  // F
    private val _t9 = rrs.rule[9]  // '('
    private val _t10 = rrs.rule[10]  // ')'
    private val F2 = rrs.rule[11]  // F2
    private val rG = rrs.goalRuleFor[S]

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = setOf("v", "vav", "vmv", "vavmv", "vmvav")
        sentences.forEach {
            val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false)  {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, oN, ER)   // S = E .
            state(E, o0, ER)   // E = E1 .
            state(E, o1, ER)   // E = T .
            state(E1, oN, 1)   // E1 = E . 'a' T
            state(E1, oN, 2)   // E1 = E 'a' . T
            state(E1, oN, ER)   // E1 = E 'a' T .
            state(T, o0, ER)   // T = T1 .
            state(T, o1, ER)   // T = F .
            state(T1, oN, 1)   // T1 = T . 'm' F
            state(T1, oN, 2)   // T1 = T 'm' . F
            state(T1, oN, ER)   // T1 = T 'm' F .
            state(F, o0, ER)   // F = 'v' .
            state(F, o1, ER)   // F = F2 .
            state(F2, oN, 1)   // F2 = '(' . E ')'
            state(F2, oN, 2)   // F2 = '(' E . ')'
            state(F2, oN, ER)   // F2 = '(' E ')' .
            state(_t2, oN, ER)   // 'a'
            state(_t5, oN, ER)   // 'm'
            state(_t7, oN, ER)   // 'v'
            state(_t9, oN, ER)   // '('
            state(_t10, oN, ER)   // ')'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t7, oN, ER); lhg(setOf(EOT,_t5,_t2)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(_t9, oN, ER); lhg(setOf(_t7,_t9)); ctx(rG, oN, SR) }
            // HEIGHT/GRAFT/GOAL transitions register explicit (prevPrev, prev) pairs via prevPair(...)
            // — see TransPrev kdoc. The legacy ctx(...)+pctx(...) cross-product would over-approximate
            // by fabricating pairs that cannot occur on the GSS at runtime.
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT)
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o0, ER); tgt(E1, oN, 1); lhg(setOf(_t2), setOf(EOT,_t10,_t2))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(F2, oN, 1))
                prevPair(RP(E1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(T1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(F2, oN, 1), RP(F2, oN, 1))
            }
            trans(GRAFT) { src(E, o0, ER); tgt(F2, oN, 2); lhg(_t10)
                prevPair(RP(rG, oN, SR), RP(F2, oN, 1))
                prevPair(RP(E1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(T1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(F2, oN, 1), RP(F2, oN, 1))
            }
            trans(HEIGHT) { src(E, o1, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
            }
            trans(HEIGHT) { src(E, o1, ER); tgt(E1, oN, 1); lhg(setOf(_t2), setOf(EOT,_t10,_t2))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(F2, oN, 1))
                prevPair(RP(E1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(T1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(F2, oN, 1), RP(F2, oN, 1))
            }
            trans(GRAFT) { src(E, o1, ER); tgt(F2, oN, 2); lhg(_t10)
                prevPair(RP(rG, oN, SR), RP(F2, oN, 1))
                prevPair(RP(E1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(T1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(F2, oN, 1), RP(F2, oN, 1))
            }
            trans(WIDTH) { src(E1, oN, 1); tgt(_t2, oN, ER); lhg(setOf(_t7,_t9)); ctx(RP(F2, oN, 1),RP(rG, oN, SR)) }
            trans(WIDTH) { src(E1, oN, 2); tgt(_t7, oN, ER); lhg(setOf(EOT,_t2,_t5,_t10)); ctx(RP(F2, oN, 1),RP(rG, oN, SR)) }
            trans(WIDTH) { src(E1, oN, 2); tgt(_t9, oN, ER); lhg(setOf(_t7,_t9)); ctx(RP(F2, oN, 1),RP(rG, oN, SR)) }
            trans(HEIGHT) { src(E1, oN, ER); tgt(E, o0, ER); lhg(setOf(EOT,_t10,_t2), setOf(EOT,_t10,_t2))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(F2, oN, 1))
                prevPair(RP(E1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(T1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(F2, oN, 1), RP(F2, oN, 1))
            }
            trans(HEIGHT) { src(T, o0, ER); tgt(E, o1, ER); lhg(setOf(EOT,_t10,_t2), setOf(EOT,_t10,_t2))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(F2, oN, 1))
                prevPair(RP(E1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(T1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(F2, oN, 1), RP(F2, oN, 1))
            }
            trans(HEIGHT) { src(T, o0, ER); tgt(T1, oN, 1); lhg(setOf(_t5), setOf(EOT,_t2,_t5,_t10))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(E1, oN, 2))
                prevPair(RP(F2, oN, 1), RP(E1, oN, 2))
                prevPair(RP(rG, oN, SR), RP(F2, oN, 1))
                prevPair(RP(E1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(T1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(F2, oN, 1), RP(F2, oN, 1))
            }
            trans(GRAFT) { src(T, o0, ER); tgt(E1, oN, ER); lhg(setOf(EOT,_t10,_t2))
                prevPair(RP(rG, oN, SR), RP(E1, oN, 2))
                prevPair(RP(F2, oN, 1), RP(E1, oN, 2))
            }
            trans(HEIGHT) { src(T, o1, ER); tgt(E, o1, ER); lhg(setOf(EOT,_t10,_t2), setOf(EOT,_t10,_t2))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(F2, oN, 1))
                prevPair(RP(E1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(T1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(F2, oN, 1), RP(F2, oN, 1))
            }
            trans(HEIGHT) { src(T, o1, ER); tgt(T1, oN, 1); lhg(setOf(_t5), setOf(EOT,_t2,_t5,_t10))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(E1, oN, 2))
                prevPair(RP(F2, oN, 1), RP(E1, oN, 2))
                prevPair(RP(rG, oN, SR), RP(F2, oN, 1))
                prevPair(RP(E1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(T1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(F2, oN, 1), RP(F2, oN, 1))
            }
            trans(GRAFT) { src(T, o1, ER); tgt(E1, oN, ER); lhg(setOf(EOT,_t10,_t2))
                prevPair(RP(rG, oN, SR), RP(E1, oN, 2))
                prevPair(RP(F2, oN, 1), RP(E1, oN, 2))
            }
            trans(WIDTH) { src(T1, oN, 1); tgt(_t5, oN, ER); lhg(setOf(_t7,_t9)); ctx(RP(rG, oN, SR),RP(E1, oN, 2),RP(F2, oN, 1)) }
            trans(WIDTH) { src(T1, oN, 2); tgt(_t7, oN, ER); lhg(setOf(EOT,_t2,_t5,_t10)); ctx(RP(rG, oN, SR),RP(E1, oN, 2),RP(F2, oN, 1)) }
            trans(WIDTH) { src(T1, oN, 2); tgt(_t9, oN, ER); lhg(setOf(_t7,_t9)); ctx(RP(rG, oN, SR),RP(E1, oN, 2),RP(F2, oN, 1)) }
            trans(HEIGHT) { src(T1, oN, ER); tgt(T, o0, ER); lhg(setOf(EOT,_t2,_t5,_t10), setOf(EOT,_t2,_t5,_t10))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(E1, oN, 2))
                prevPair(RP(F2, oN, 1), RP(E1, oN, 2))
                prevPair(RP(rG, oN, SR), RP(F2, oN, 1))
                prevPair(RP(E1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(T1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(F2, oN, 1), RP(F2, oN, 1))
            }
            trans(HEIGHT) { src(F, o0, ER); tgt(T, o1, ER); lhg(setOf(EOT,_t2,_t5,_t10), setOf(EOT,_t2,_t5,_t10))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(E1, oN, 2))
                prevPair(RP(F2, oN, 1), RP(E1, oN, 2))
                prevPair(RP(rG, oN, SR), RP(F2, oN, 1))
                prevPair(RP(E1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(T1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(F2, oN, 1), RP(F2, oN, 1))
            }
            trans(GRAFT) { src(F, o0, ER); tgt(T1, oN, ER); lhg(setOf(EOT,_t2,_t5,_t10))
                prevPair(RP(E1, oN, 2), RP(T1, oN, 2))
                prevPair(RP(rG, oN, SR), RP(T1, oN, 2))
                prevPair(RP(F2, oN, 1), RP(T1, oN, 2))
            }
            trans(HEIGHT) { src(F, o1, ER); tgt(T, o1, ER); lhg(setOf(EOT,_t2,_t5,_t10), setOf(EOT,_t2,_t5,_t10))
                // 7 pairs (same shape as F.o0 → T.o1). prev=T1.2 cases are absent: when the
                // F sits in the F-position of T1 (prev=T1.2), the only valid follow-up is
                // GRAFT into T1.ER, never HEIGHT to T.o1 — so the builder filters them out.
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(E1, oN, 2))
                prevPair(RP(F2, oN, 1), RP(E1, oN, 2))
                prevPair(RP(rG, oN, SR), RP(F2, oN, 1))
                prevPair(RP(E1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(T1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(F2, oN, 1), RP(F2, oN, 1))
            }
            trans(GRAFT) { src(F, o1, ER); tgt(T1, oN, ER); lhg(setOf(EOT,_t2,_t5,_t10))
                prevPair(RP(E1, oN, 2), RP(T1, oN, 2))
                prevPair(RP(rG, oN, SR), RP(T1, oN, 2))
                prevPair(RP(F2, oN, 1), RP(T1, oN, 2))
            }
            trans(WIDTH) { src(F2, oN, 1); tgt(_t7, oN, ER); lhg(setOf(_t5,_t10,_t2)); ctx(RP(T1, oN, 2),RP(E1, oN, 2),RP(rG, oN, SR),RP(F2, oN, 1)) }
            trans(WIDTH) { src(F2, oN, 1); tgt(_t9, oN, ER); lhg(setOf(_t7,_t9)); ctx(RP(T1, oN, 2),RP(E1, oN, 2),RP(rG, oN, SR),RP(F2, oN, 1)) }
            trans(WIDTH) { src(F2, oN, 2); tgt(_t10, oN, ER); lhg(setOf(EOT,_t2,_t5,_t10)); ctx(RP(E1, oN, 2),RP(rG, oN, SR),RP(T1, oN, 2),RP(F2, oN, 1)) }
            trans(HEIGHT) { src(F2, oN, ER); tgt(F, o1, ER); lhg(setOf(EOT,_t2,_t5,_t10), setOf(EOT,_t2,_t5,_t10))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(E1, oN, 2))
                prevPair(RP(F2, oN, 1), RP(E1, oN, 2))
                prevPair(RP(E1, oN, 2), RP(T1, oN, 2))
                prevPair(RP(rG, oN, SR), RP(T1, oN, 2))
                prevPair(RP(F2, oN, 1), RP(T1, oN, 2))
                prevPair(RP(rG, oN, SR), RP(F2, oN, 1))
                prevPair(RP(E1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(T1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(F2, oN, 1), RP(F2, oN, 1))
            }
            trans(GRAFT) { src(_t2, oN, ER); tgt(E1, oN, 2); lhg(setOf(_t7,_t9))
                prevPair(RP(rG, oN, SR), RP(E1, oN, 1))
                prevPair(RP(F2, oN, 1), RP(E1, oN, 1))
            }
            trans(GRAFT) { src(_t5, oN, ER); tgt(T1, oN, 2); lhg(setOf(_t7,_t9))
                prevPair(RP(rG, oN, SR), RP(T1, oN, 1))
                prevPair(RP(E1, oN, 2), RP(T1, oN, 1))
                prevPair(RP(F2, oN, 1), RP(T1, oN, 1))
            }
            trans(HEIGHT) { src(_t7, oN, ER); tgt(F, o0, ER); lhg(setOf(EOT,_t2,_t5,_t10), setOf(EOT,_t2,_t5,_t10))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(E1, oN, 2))
                prevPair(RP(F2, oN, 1), RP(E1, oN, 2))
                prevPair(RP(E1, oN, 2), RP(T1, oN, 2))
                prevPair(RP(rG, oN, SR), RP(T1, oN, 2))
                prevPair(RP(F2, oN, 1), RP(T1, oN, 2))
                prevPair(RP(rG, oN, SR), RP(F2, oN, 1))
                prevPair(RP(E1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(T1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(F2, oN, 1), RP(F2, oN, 1))
            }
            trans(HEIGHT) { src(_t9, oN, ER); tgt(F2, oN, 1); lhg(setOf(_t7,_t9), setOf(EOT,_t2,_t5,_t10))
                prevPair(RP(rG, oN, SR), RP(rG, oN, SR))
                prevPair(RP(rG, oN, SR), RP(E1, oN, 2))
                prevPair(RP(F2, oN, 1), RP(E1, oN, 2))
                prevPair(RP(E1, oN, 2), RP(T1, oN, 2))
                prevPair(RP(rG, oN, SR), RP(T1, oN, 2))
                prevPair(RP(F2, oN, 1), RP(T1, oN, 2))
                prevPair(RP(rG, oN, SR), RP(F2, oN, 1))
                prevPair(RP(E1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(T1, oN, 2), RP(F2, oN, 1))
                prevPair(RP(F2, oN, 1), RP(F2, oN, 1))
            }
            trans(GRAFT) { src(_t10, oN, ER); tgt(F2, oN, ER); lhg(setOf(EOT,_t2,_t5,_t10))
                prevPair(RP(rG, oN, SR), RP(F2, oN, 2))
                prevPair(RP(E1, oN, 2), RP(F2, oN, 2))
                prevPair(RP(T1, oN, 2), RP(F2, oN, 2))
                prevPair(RP(F2, oN, 1), RP(F2, oN, 2))
            }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        // Sentence list must exercise every reachable (prevPrev, prev) pair so the
        // on-demand automaton enumerates the same context pairs as the preBuild one.
        // In particular we need:
        //  - F2.1 paired with each of {rG.SR, E1.2, T1.2, F2.1} as prevPrev
        //    (i.e. '(' must appear at top-level, after 'a', after 'm', and inside another '(').
        //  - E1.2 / T1.2 paired with F2.1 as prevPrev
        //    (i.e. 'a' / 'm' must occur inside parentheses).
        val sentences = listOf(
            "v", "vav", "vmv", "vavmv", "vmvav",
            "(v)", "(vav)", "(vmv)",                    // F2.1 with prevPrev=rG.SR  +  inner E1/T1 with prevPrev=F2.1
            "(v)mv", "vm(v)", "(v)m(v)",                // F2.1 with prevPrev=T1.2
            "(v)av", "va(v)", "(v)a(v)",                // F2.1 with prevPrev=E1.2
            "((v))", "((vav))", "(va(v))", "(vm(v))",   // F2.1 with prevPrev=F2.1
            // Inner E1/T1 reductions inside parens that themselves sit after 'a' / 'm'
            // — needed so that E.o0,ER / E1.ER / T.o0,ER / T1.ER are reached with
            // (prevPrev=E1.2, prev=F2.1) and (prevPrev=T1.2, prev=F2.1).
            "va(vav)", "vm(vav)", "va(vmv)", "vm(vmv)",
            // Inner E1 / T1 reductions inside parens where prev=E1.2 / T1.2 itself
            // (prevPrev=F2.1) — exercised when the inner expression chains 'a' and 'm'.
            "(vavmv)", "(vmvav)",
            // Deep nesting: inner reductions where prev=F2.1 and prevPrev=F2.1 with
            // a non-trivial inner expression so all reduce states are reached.
            "((vavmv))", "((vmvav))",
            // F.o1,ER (and the chained F2.ER / '(',ER reductions) reached *inside*
            // an outer paren — i.e. the F itself is a parenthesised expression that
            // sits inside another paren. Needed for the (prevPrev, prev=F2.1) pairs
            // where prevPrev ∈ {E1.2, T1.2, F2.1}.
            "va((v))", "vm((v))", "(((v)))",
            // (prevPrev=E1.2, prev=T1.2) for F.o1,ER / F2.ER / '(',ER — an inner
            // paren as the F of a T1 whose T1 is itself the right operand of an
            // outer 'a': stack is ... E1.2 → T1.2 → F2.1 → inner.
            "vavm(v)", "vavm(vav)", "vavm(vmv)"
        )
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
        println(rrs_preBuild.usedAutomatonToString("S", true))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S", true))

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild, AutomatonTest.MatchConfiguration(no_lookahead_compare = true))
    }
}