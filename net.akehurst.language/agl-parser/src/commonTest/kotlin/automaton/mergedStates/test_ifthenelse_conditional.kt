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
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_ifthenelse_conditional : test_AutomatonAbstract() {

    // S =  expr ;
    // expr = var | conditional ;
    // conditional = ifthenelse | ifthen;
    // ifthenelse = 'if' expr 'then' expr 'else' expr ;
    // ifthen = 'if' expr 'then' expr ;
    // var = "[A-Z]" ;

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = ruleSet("Test") {
        concatenation("S") { ref("expr") }
        choicePriority("expr") {
            ref("VAR")
            ref("conditional")
        }
        choiceLongest("conditional") {
            ref("ifthen")
            ref("ifthenelse")
        }
        concatenation("ifthen") { literal("if"); ref("expr"); literal("then"); ref("expr") }
        concatenation("ifthenelse") { literal("if"); ref("expr"); literal("then"); ref("expr"); literal("else"); ref("expr") }
        pattern("VAR", "[A-Z]")
        preferenceFor("expr") {
            right(listOf("ifthen"), setOf("'then'"))
            right(listOf("ifthenelse"), setOf("'else'"))
        }
    } as RuntimeRuleSet

    private val S = rrs.rule[0]  // S
    private val expr = rrs.rule[1]  // expr
    private val conditional = rrs.rule[2]  // conditional
    private val _t3 = rrs.rule[3]  // 'if'
    private val _t4 = rrs.rule[4]  // 'then'
    private val ifthen = rrs.rule[5]  // ifthen
    private val _t6 = rrs.rule[6]  // 'else'
    private val ifthenelse = rrs.rule[7]  // ifthenelse
    private val _t8 = rrs.rule[8]  // VAR
    private val rG = rrs.goalRuleFor[S]

    @Test
    fun automaton_parse_ifXthenY() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "ifXthenY")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t8, oN, ER)   // VAR("[A-Z]")
            state(_t3, oN, ER)   // 'if'
            state(ifthen, oN, 1)   // ifthen = 'if' . expr 'then' expr
            state(ifthenelse, oN, 1)   // ifthenelse = 'if' . expr 'then' expr 'else' expr
            state(expr, o0, ER)   // expr = VAR .
            state(ifthen, oN, 2)   // ifthen = 'if' expr . 'then' expr
            state(ifthenelse, oN, 2)   // ifthenelse = 'if' expr . 'then' expr 'else' expr
            state(_t4, oN, ER)   // 'then'
            state(ifthen, oN, 3)   // ifthen = 'if' expr 'then' . expr
            state(ifthenelse, oN, 3)   // ifthenelse = 'if' expr 'then' . expr 'else' expr
            state(ifthen, oN, ER)   // ifthen = 'if' expr 'then' expr .
            state(conditional, o0, ER)   // conditional = ifthen .
            state(expr, o1, ER)   // expr = conditional .
            state(S, oN, ER)   // S = expr .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t8, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t8, oN, ER); tgt(expr, o0, ER); lhg(setOf(RT), setOf(RT)); lhg(setOf(_t4), setOf(_t4));  prevPair(RP(rG, oN, SR), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 3)) }
            trans(HEIGHT) { src(_t3, oN, ER); tgt(ifthen, oN, 1); lhg(setOf(_t8,_t3), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(_t3, oN, ER); tgt(ifthenelse, oN, 1); lhg(setOf(_t8,_t3), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(WIDTH) { src(ifthen, oN, 1); tgt(_t8, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ifthen, oN, 1); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ifthenelse, oN, 1); tgt(_t8, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ifthenelse, oN, 1); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthen, oN, 2); lhg(_t4);  prevPair(RP(rG, oN, SR), RP(ifthen, oN, 1)) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthenelse, oN, 2); lhg(_t4);  prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 1)) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthen, oN, ER); lhg(RT);  prevPair(RP(rG, oN, SR), RP(ifthen, oN, 3)) }
            trans(WIDTH) { src(ifthen, oN, 2); tgt(_t4, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ifthenelse, oN, 2); tgt(_t4, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t4, oN, ER); tgt(ifthen, oN, 3); lhg(setOf(_t8,_t3));  prevPair(RP(rG, oN, SR), RP(ifthen, oN, 2)) }
            trans(GRAFT) { src(_t4, oN, ER); tgt(ifthenelse, oN, 3); lhg(setOf(_t8,_t3));  prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 2)) }
            trans(WIDTH) { src(ifthen, oN, 3); tgt(_t8, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ifthen, oN, 3); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ifthenelse, oN, 3); tgt(_t8, oN, ER); lhg(_t6); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ifthenelse, oN, 3); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(ifthen, oN, ER); tgt(conditional, o0, ER); lhg(setOf(RT), setOf(RT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(conditional, o0, ER); tgt(expr, o1, ER); lhg(setOf(RT), setOf(RT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(expr, o1, ER); tgt(S, oN, ER); lhg(setOf(RT), setOf(RT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_ifthenelse() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "ifXthenYelseZ")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t8, oN, ER)   // VAR("[A-Z]")
            state(_t3, oN, ER)   // 'if'
            state(ifthen, oN, 1)   // ifthen = 'if' . expr 'then' expr
            state(ifthenelse, oN, 1)   // ifthenelse = 'if' . expr 'then' expr 'else' expr
            state(expr, o0, ER)   // expr = VAR .
            state(ifthen, oN, 2)   // ifthen = 'if' expr . 'then' expr
            state(ifthenelse, oN, 2)   // ifthenelse = 'if' expr . 'then' expr 'else' expr
            state(_t4, oN, ER)   // 'then'
            state(ifthen, oN, 3)   // ifthen = 'if' expr 'then' . expr
            state(ifthenelse, oN, 3)   // ifthenelse = 'if' expr 'then' . expr 'else' expr
            state(ifthenelse, oN, 4)   // ifthenelse = 'if' expr 'then' expr . 'else' expr
            state(_t6, oN, ER)   // 'else'
            state(ifthenelse, oN, 5)   // ifthenelse = 'if' expr 'then' expr 'else' . expr
            state(ifthenelse, oN, ER)   // ifthenelse = 'if' expr 'then' expr 'else' expr .
            state(conditional, o1, ER)   // conditional = ifthenelse .
            state(expr, o1, ER)   // expr = conditional .
            state(S, oN, ER)   // S = expr .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t8, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t8, oN, ER); tgt(expr, o0, ER); lhg(setOf(RT), setOf(RT)); lhg(setOf(_t6), setOf(_t6)); lhg(setOf(_t4), setOf(_t4));  prevPair(RP(rG, oN, SR), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 5)) }
            trans(HEIGHT) { src(_t3, oN, ER); tgt(ifthen, oN, 1); lhg(setOf(_t8,_t3), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(_t3, oN, ER); tgt(ifthenelse, oN, 1); lhg(setOf(_t8,_t3), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(WIDTH) { src(ifthen, oN, 1); tgt(_t8, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ifthen, oN, 1); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ifthenelse, oN, 1); tgt(_t8, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ifthenelse, oN, 1); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthen, oN, 2); lhg(_t4);  prevPair(RP(rG, oN, SR), RP(ifthen, oN, 1)) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthenelse, oN, 2); lhg(_t4);  prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 1)) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthenelse, oN, 4); lhg(_t6);  prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthenelse, oN, ER); lhg(RT);  prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 5)) }
            trans(WIDTH) { src(ifthen, oN, 2); tgt(_t4, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ifthenelse, oN, 2); tgt(_t4, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t4, oN, ER); tgt(ifthen, oN, 3); lhg(setOf(_t8,_t3));  prevPair(RP(rG, oN, SR), RP(ifthen, oN, 2)) }
            trans(GRAFT) { src(_t4, oN, ER); tgt(ifthenelse, oN, 3); lhg(setOf(_t8,_t3));  prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 2)) }
            trans(WIDTH) { src(ifthen, oN, 3); tgt(_t8, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ifthen, oN, 3); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ifthenelse, oN, 3); tgt(_t8, oN, ER); lhg(_t6); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ifthenelse, oN, 3); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ifthenelse, oN, 4); tgt(_t6, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(GRAFT) { src(_t6, oN, ER); tgt(ifthenelse, oN, 5); lhg(setOf(_t8,_t3));  prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 4)) }
            trans(WIDTH) { src(ifthenelse, oN, 5); tgt(_t8, oN, ER); lhg(RT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(ifthenelse, oN, 5); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(ifthenelse, oN, ER); tgt(conditional, o1, ER); lhg(setOf(RT), setOf(RT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(conditional, o1, ER); tgt(expr, o1, ER); lhg(setOf(RT), setOf(RT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(expr, o1, ER); tgt(S, oN, ER); lhg(setOf(RT), setOf(RT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_ifXthenifYthenZelseW() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "ifXthenifYthenZelseW")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t8, oN, ER)   // VAR("[A-Z]")
            state(_t3, oN, ER)   // 'if'
            state(ifthen, oN, 1)   // ifthen = 'if' . expr 'then' expr
            state(ifthenelse, oN, 1)   // ifthenelse = 'if' . expr 'then' expr 'else' expr
            state(expr, o0, ER)   // expr = VAR .
            state(ifthen, oN, 2)   // ifthen = 'if' expr . 'then' expr
            state(ifthenelse, oN, 2)   // ifthenelse = 'if' expr . 'then' expr 'else' expr
            state(_t4, oN, ER)   // 'then'
            state(ifthen, oN, 3)   // ifthen = 'if' expr 'then' . expr
            state(ifthenelse, oN, 3)   // ifthenelse = 'if' expr 'then' . expr 'else' expr
            state(ifthen, oN, ER)   // ifthen = 'if' expr 'then' expr .
            state(conditional, o0, ER)   // conditional = ifthen .
            state(expr, o1, ER)   // expr = conditional .
            state(ifthenelse, oN, 4)   // ifthenelse = 'if' expr 'then' expr . 'else' expr
            state(_t6, oN, ER)   // 'else'
            state(ifthenelse, oN, 5)   // ifthenelse = 'if' expr 'then' expr 'else' . expr
            state(ifthenelse, oN, ER)   // ifthenelse = 'if' expr 'then' expr 'else' expr .
            state(conditional, o1, ER)   // conditional = ifthenelse .
            state(S, oN, ER)   // S = expr .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t8, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t8, oN, ER); tgt(expr, o0, ER); lhg(setOf(RT), setOf(RT)); lhg(setOf(_t6), setOf(_t6)); lhg(setOf(_t4), setOf(_t4));  prevPair(RP(rG, oN, SR), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 5)) }
            trans(HEIGHT) { src(_t3, oN, ER); tgt(ifthen, oN, 1); lhg(setOf(_t8,_t3), setOf(RT,EOT,_t6));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 3)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)) }
            trans(HEIGHT) { src(_t3, oN, ER); tgt(ifthenelse, oN, 1); lhg(setOf(_t8,_t3), setOf(RT,EOT,_t6));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 3)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)) }
            trans(WIDTH) { src(ifthen, oN, 1); tgt(_t8, oN, ER); lhg(_t4); ctx(RP(rG, oN, SR),RP(ifthen, oN, 3),RP(ifthenelse, oN, 3)) }
            trans(WIDTH) { src(ifthen, oN, 1); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(RP(rG, oN, SR),RP(ifthen, oN, 3),RP(ifthenelse, oN, 3)) }
            trans(WIDTH) { src(ifthenelse, oN, 1); tgt(_t8, oN, ER); lhg(_t4); ctx(RP(rG, oN, SR),RP(ifthenelse, oN, 3),RP(ifthen, oN, 3)) }
            trans(WIDTH) { src(ifthenelse, oN, 1); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(RP(rG, oN, SR),RP(ifthenelse, oN, 3),RP(ifthen, oN, 3)) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthen, oN, 2); lhg(_t4);  prevPair(RP(rG, oN, SR), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 1)) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthenelse, oN, 2); lhg(_t4);  prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 1)) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthen, oN, ER); lhg(RT);  prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 3)) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthenelse, oN, 4); lhg(_t6);  prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 3)) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthenelse, oN, ER); lhg(RT);  prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 5)) }
            trans(WIDTH) { src(ifthen, oN, 2); tgt(_t4, oN, ER); lhg(setOf(_t8,_t3)); ctx(RP(rG, oN, SR),RP(ifthen, oN, 3),RP(ifthenelse, oN, 3)) }
            trans(WIDTH) { src(ifthenelse, oN, 2); tgt(_t4, oN, ER); lhg(setOf(_t8,_t3)); ctx(RP(rG, oN, SR),RP(ifthen, oN, 3),RP(ifthenelse, oN, 3)) }
            trans(GRAFT) { src(_t4, oN, ER); tgt(ifthen, oN, 3); lhg(setOf(_t8,_t3));  prevPair(RP(rG, oN, SR), RP(ifthen, oN, 2)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 2)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 2)) }
            trans(GRAFT) { src(_t4, oN, ER); tgt(ifthenelse, oN, 3); lhg(setOf(_t8,_t3));  prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 2)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 2)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 2)) }
            trans(WIDTH) { src(ifthen, oN, 3); tgt(_t8, oN, ER); lhg(RT); ctx(RP(rG, oN, SR),RP(ifthen, oN, 3),RP(ifthenelse, oN, 3)) }
            trans(WIDTH) { src(ifthen, oN, 3); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(RP(rG, oN, SR),RP(ifthen, oN, 3),RP(ifthenelse, oN, 3)) }
            trans(WIDTH) { src(ifthenelse, oN, 3); tgt(_t8, oN, ER); lhg(_t6); ctx(RP(rG, oN, SR),RP(ifthenelse, oN, 3),RP(ifthen, oN, 3)) }
            trans(WIDTH) { src(ifthenelse, oN, 3); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(RP(rG, oN, SR),RP(ifthenelse, oN, 3),RP(ifthen, oN, 3)) }
            trans(HEIGHT) { src(ifthen, oN, ER); tgt(conditional, o0, ER); lhg(setOf(RT), setOf(RT)); lhg(setOf(_t6), setOf(_t6));  prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(conditional, o0, ER); tgt(expr, o1, ER); lhg(setOf(_t6), setOf(_t6));  prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)) }
            trans(GRAFT) { src(expr, o1, ER); tgt(ifthenelse, oN, 4); lhg(_t6);  prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)) }
            trans(HEIGHT) { src(expr, o1, ER); tgt(S, oN, ER); lhg(setOf(RT), setOf(RT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(expr, o1, ER); tgt(ifthen, oN, ER); lhg(RT);  prevPair(RP(rG, oN, SR), RP(ifthen, oN, 3)) }
            trans(WIDTH) { src(ifthenelse, oN, 4); tgt(_t6, oN, ER); lhg(setOf(_t8,_t3)); ctx(RP(rG, oN, SR),RP(ifthenelse, oN, 3),RP(ifthen, oN, 3)) }
            trans(GRAFT) { src(_t6, oN, ER); tgt(ifthenelse, oN, 5); lhg(setOf(_t8,_t3));  prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 4)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 4)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 4)) }
            trans(WIDTH) { src(ifthenelse, oN, 5); tgt(_t8, oN, ER); lhg(RT); ctx(RP(rG, oN, SR),RP(ifthenelse, oN, 3),RP(ifthen, oN, 3)) }
            trans(WIDTH) { src(ifthenelse, oN, 5); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(RP(rG, oN, SR),RP(ifthenelse, oN, 3),RP(ifthen, oN, 3)) }
            trans(HEIGHT) { src(ifthenelse, oN, ER); tgt(conditional, o1, ER); lhg(setOf(RT), setOf(RT)); lhg(setOf(_t6), setOf(_t6));  prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 3)) }
            trans(HEIGHT) { src(conditional, o1, ER); tgt(expr, o1, ER); lhg(setOf(RT), setOf(RT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 3)) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = setOf("ifXthenY", "ifXthenYelseZ", "ifXthenifYthenZelseW", "ifXthenYelseifZthenW", "X")
        sentences.forEach {
            val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size, result.issues.joinToString("\n") { it.toString() })
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, oN, ER)   // S = expr .
            state(expr, o0, ER)   // expr = VAR .
            state(expr, o1, ER)   // expr = conditional .
            state(conditional, o0, ER)   // conditional = ifthen .
            state(conditional, o1, ER)   // conditional = ifthenelse .
            state(ifthen, oN, 1)   // ifthen = 'if' . expr 'then' expr
            state(ifthen, oN, 2)   // ifthen = 'if' expr . 'then' expr
            state(ifthen, oN, 3)   // ifthen = 'if' expr 'then' . expr
            state(ifthen, oN, ER)   // ifthen = 'if' expr 'then' expr .
            state(ifthenelse, oN, 1)   // ifthenelse = 'if' . expr 'then' expr 'else' expr
            state(ifthenelse, oN, 2)   // ifthenelse = 'if' expr . 'then' expr 'else' expr
            state(ifthenelse, oN, 3)   // ifthenelse = 'if' expr 'then' . expr 'else' expr
            state(ifthenelse, oN, 4)   // ifthenelse = 'if' expr 'then' expr . 'else' expr
            state(ifthenelse, oN, 5)   // ifthenelse = 'if' expr 'then' expr 'else' . expr
            state(ifthenelse, oN, ER)   // ifthenelse = 'if' expr 'then' expr 'else' expr .
            state(_t8, oN, ER)   // VAR("[A-Z]")
            state(_t3, oN, ER)   // 'if'
            state(_t4, oN, ER)   // 'then'
            state(_t6, oN, ER)   // 'else'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t8, oN, ER); lhg(EOT); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT);  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(expr, o0, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthen, oN, ER); lhg(setOf(EOT,_t4,_t6));  prevPair(RP(rG, oN, SR), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 3)) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthenelse, oN, ER); lhg(setOf(EOT,_t4,_t6));  prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 5)) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthen, oN, 2); lhg(_t4);  prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 1)) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthenelse, oN, 4); lhg(_t6);  prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 3)) }
            trans(GRAFT) { src(expr, o0, ER); tgt(ifthenelse, oN, 2); lhg(_t4);  prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 1)) }
            trans(HEIGHT) { src(expr, o1, ER); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT));  prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(GRAFT) { src(expr, o1, ER); tgt(ifthen, oN, ER); lhg(setOf(EOT,_t4,_t6));  prevPair(RP(rG, oN, SR), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 3)) }
            trans(GRAFT) { src(expr, o1, ER); tgt(ifthenelse, oN, ER); lhg(setOf(EOT,_t4,_t6));  prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 5)) }
            trans(GRAFT) { src(expr, o1, ER); tgt(ifthen, oN, 2); lhg(_t4);  prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 1)) }
            trans(GRAFT) { src(expr, o1, ER); tgt(ifthenelse, oN, 2); lhg(_t4);  prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 1)) }
            trans(GRAFT) { src(expr, o1, ER); tgt(ifthenelse, oN, 4); lhg(_t6);  prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 3)) }
            trans(HEIGHT) { src(conditional, o0, ER); tgt(expr, o1, ER); lhg(setOf(EOT,_t4,_t6), setOf(EOT,_t4,_t6));  prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 3)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(conditional, o1, ER); tgt(expr, o1, ER); lhg(setOf(EOT,_t4,_t6), setOf(EOT,_t4,_t6));  prevPair(RP(rG, oN, SR), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 1)) }
            trans(WIDTH) { src(ifthen, oN, 1); tgt(_t8, oN, ER); lhg(_t4); ctx(RP(ifthenelse, oN, 5),RP(ifthenelse, oN, 3),RP(ifthenelse, oN, 1),RP(ifthen, oN, 3),RP(ifthen, oN, 1),RP(rG, oN, SR)) }
            trans(WIDTH) { src(ifthen, oN, 1); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(RP(ifthenelse, oN, 5),RP(ifthenelse, oN, 3),RP(ifthenelse, oN, 1),RP(ifthen, oN, 3),RP(ifthen, oN, 1),RP(rG, oN, SR)) }
            trans(WIDTH) { src(ifthen, oN, 2); tgt(_t4, oN, ER); lhg(setOf(_t8,_t3)); ctx(RP(rG, oN, SR),RP(ifthenelse, oN, 5),RP(ifthenelse, oN, 3),RP(ifthenelse, oN, 1),RP(ifthen, oN, 3),RP(ifthen, oN, 1)) }
            trans(WIDTH) { src(ifthen, oN, 3); tgt(_t8, oN, ER); lhg(setOf(EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(ifthenelse, oN, 5),RP(ifthenelse, oN, 3),RP(ifthenelse, oN, 1),RP(ifthen, oN, 3),RP(ifthen, oN, 1)) }
            trans(WIDTH) { src(ifthen, oN, 3); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(RP(rG, oN, SR),RP(ifthenelse, oN, 5),RP(ifthenelse, oN, 3),RP(ifthenelse, oN, 1),RP(ifthen, oN, 3),RP(ifthen, oN, 1)) }
            trans(HEIGHT) { src(ifthen, oN, ER); tgt(conditional, o0, ER); lhg(setOf(EOT,_t4,_t6), setOf(EOT,_t4,_t6));  prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 3)) }
            trans(WIDTH) { src(ifthenelse, oN, 1); tgt(_t8, oN, ER); lhg(_t4); ctx(RP(ifthenelse, oN, 1),RP(ifthenelse, oN, 3),RP(ifthenelse, oN, 5),RP(rG, oN, SR),RP(ifthen, oN, 3),RP(ifthen, oN, 1)) }
            trans(WIDTH) { src(ifthenelse, oN, 1); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(RP(ifthenelse, oN, 1),RP(ifthenelse, oN, 3),RP(ifthenelse, oN, 5),RP(rG, oN, SR),RP(ifthen, oN, 3),RP(ifthen, oN, 1)) }
            trans(WIDTH) { src(ifthenelse, oN, 2); tgt(_t4, oN, ER); lhg(setOf(_t8,_t3)); ctx(RP(ifthenelse, oN, 1),RP(ifthenelse, oN, 5),RP(ifthenelse, oN, 3),RP(rG, oN, SR),RP(ifthen, oN, 3),RP(ifthen, oN, 1)) }
            trans(WIDTH) { src(ifthenelse, oN, 3); tgt(_t8, oN, ER); lhg(_t6); ctx(RP(ifthenelse, oN, 5),RP(ifthenelse, oN, 3),RP(rG, oN, SR),RP(ifthen, oN, 3),RP(ifthen, oN, 1),RP(ifthenelse, oN, 1)) }
            trans(WIDTH) { src(ifthenelse, oN, 3); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(RP(ifthenelse, oN, 5),RP(ifthenelse, oN, 3),RP(rG, oN, SR),RP(ifthen, oN, 3),RP(ifthen, oN, 1),RP(ifthenelse, oN, 1)) }
            trans(WIDTH) { src(ifthenelse, oN, 4); tgt(_t6, oN, ER); lhg(setOf(_t8,_t3)); ctx(RP(ifthenelse, oN, 5),RP(rG, oN, SR),RP(ifthen, oN, 3),RP(ifthen, oN, 1),RP(ifthenelse, oN, 1),RP(ifthenelse, oN, 3)) }
            trans(WIDTH) { src(ifthenelse, oN, 5); tgt(_t8, oN, ER); lhg(setOf(EOT,_t4,_t6)); ctx(RP(rG, oN, SR),RP(ifthen, oN, 3),RP(ifthen, oN, 1),RP(ifthenelse, oN, 1),RP(ifthenelse, oN, 5),RP(ifthenelse, oN, 3)) }
            trans(WIDTH) { src(ifthenelse, oN, 5); tgt(_t3, oN, ER); lhg(setOf(_t8,_t3)); ctx(RP(rG, oN, SR),RP(ifthen, oN, 3),RP(ifthen, oN, 1),RP(ifthenelse, oN, 1),RP(ifthenelse, oN, 5),RP(ifthenelse, oN, 3)) }
            trans(HEIGHT) { src(ifthenelse, oN, ER); tgt(conditional, o1, ER); lhg(setOf(EOT,_t4,_t6), setOf(EOT,_t4,_t6));  prevPair(RP(rG, oN, SR), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 3)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)) }
            trans(HEIGHT) { src(_t8, oN, ER); tgt(expr, o0, ER); lhg(setOf(EOT,_t4,_t6), setOf(EOT,_t4,_t6));  prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 3)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 3)) }
            trans(HEIGHT) { src(_t3, oN, ER); tgt(ifthen, oN, 1); lhg(setOf(_t8,_t3), setOf(EOT,_t4,_t6));  prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 3)) }
            trans(HEIGHT) { src(_t3, oN, ER); tgt(ifthenelse, oN, 1); lhg(setOf(_t8,_t3), setOf(EOT,_t4,_t6));  prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 1)); prevPair(RP(rG, oN, SR), RP(rG, oN, SR)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 5)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 1)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 1)); prevPair(RP(rG, oN, SR), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 3)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 3)) }
            trans(GRAFT) { src(_t4, oN, ER); tgt(ifthen, oN, 3); lhg(setOf(_t8,_t3));  prevPair(RP(rG, oN, SR), RP(ifthen, oN, 2)); prevPair(RP(ifthenelse, oN, 5), RP(ifthen, oN, 2)); prevPair(RP(ifthenelse, oN, 3), RP(ifthen, oN, 2)); prevPair(RP(ifthenelse, oN, 1), RP(ifthen, oN, 2)); prevPair(RP(ifthen, oN, 3), RP(ifthen, oN, 2)); prevPair(RP(ifthen, oN, 1), RP(ifthen, oN, 2)) }
            trans(GRAFT) { src(_t4, oN, ER); tgt(ifthenelse, oN, 3); lhg(setOf(_t8,_t3));  prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 2)); prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 2)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 2)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 2)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 2)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 2)) }
            trans(GRAFT) { src(_t6, oN, ER); tgt(ifthenelse, oN, 5); lhg(setOf(_t8,_t3));  prevPair(RP(ifthenelse, oN, 5), RP(ifthenelse, oN, 4)); prevPair(RP(rG, oN, SR), RP(ifthenelse, oN, 4)); prevPair(RP(ifthen, oN, 3), RP(ifthenelse, oN, 4)); prevPair(RP(ifthen, oN, 1), RP(ifthenelse, oN, 4)); prevPair(RP(ifthenelse, oN, 1), RP(ifthenelse, oN, 4)); prevPair(RP(ifthenelse, oN, 3), RP(ifthenelse, oN, 4)) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Ignore
    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("ifXthenY", "ifXthenYelseZ", "ifXthenifYthenZelseW")
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