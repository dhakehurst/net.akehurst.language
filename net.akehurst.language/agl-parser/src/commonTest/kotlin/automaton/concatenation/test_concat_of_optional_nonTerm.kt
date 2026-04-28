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

package net.akehurst.language.automaton.leftcorner.concatenation

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.automaton.leftcorner.ParserStateSet
import net.akehurst.language.automaton.leftcorner.automaton
import net.akehurst.language.automaton.leftcorner.test_AutomatonAbstract
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import testFixture.utils.AutomatonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_concat_of_optional_nonTerm : test_AutomatonAbstract() {

    //    GenericMethodInvocation = TypeArguments? MethodInvocation ;
    //    MethodInvocation = IDENTIFIER ArgumentList ;
    //    ArgumentList = '(' Arguments ')' ;
    //    Arguments = [ Expression / ',' ]* ;

    // S =  optA B ;
    // optA = A? ;
    // A = a
    // B = b L ;
    // L = c Es d ;
    // Es = [ e / f ]*

    val rrs = ruleSet("Test") {
        concatenation("S") { ref("optA"); ref("B") }
        multi("optA", 0, 1, "A")
        concatenation("A") { literal("a") }
        concatenation("B") { literal("b"); ref("L") }
        concatenation("L") { literal("c"); ref("Es"); literal("d") }
        sList("Es", 0, -1, "'e'", "'f'")
        literal("'e'", "e")
        literal("'f'", "f")
    } as RuntimeRuleSet

    private val S = rrs.rule[0]  // S
    private val optA = rrs.rule[1]  // optA
    private val _t2 = rrs.rule[2]  // 'a'
    private val A = rrs.rule[3]  // A
    private val _t4 = rrs.rule[4]  // 'b'
    private val B = rrs.rule[5]  // B
    private val _t6 = rrs.rule[6]  // 'c'
    private val _t7 = rrs.rule[7]  // 'd'
    private val L = rrs.rule[8]  // L
    private val Es = rrs.rule[9]  // Es
    private val _t10 = rrs.rule[10]  // 'e'
    private val _t11 = rrs.rule[11]  // 'f'
    private val rG = rrs.goalRuleFor[S]

    @Test
    fun parse_bcd() {
        //TODO: is there a way to reset the rrs if it needs it?
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "bcd")
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        println(rrs.usedAutomatonToString("S"))

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t2, oN, ER)   // 'a'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(optA, LE, ER)   // optA = A .
            state(S, oN, 1)   // S = optA . B
            state(_t4, oN, ER)   // 'b'
            state(B, oN, 1)   // B = 'b' . L
            state(_t6, oN, ER)   // 'c'
            state(L, oN, 1)   // L = 'c' . Es 'd'
            state(_t10, oN, ER)   // 'e'
            state(Es, SE, ER)   // Es = 'e' .
            state(L, oN, 2)   // L = 'c' Es . 'd'
            state(_t7, oN, ER)   // 'd'
            state(L, oN, ER)   // L = 'c' Es 'd' .
            state(B, oN, ER)   // B = 'b' L .
            state(S, oN, ER)   // S = optA B .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t2, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(optA, LE, ER); lhg(setOf(_t4), setOf(_t4)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(Es, SE, ER); lhg(setOf(_t7), setOf(_t7)); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(HEIGHT) { src(optA, LE, ER); tgt(S, oN, 1); lhg(setOf(_t4), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 1); tgt(_t4, oN, ER); lhg(_t6); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t4, oN, ER); tgt(B, oN, 1); lhg(setOf(_t6), setOf(RT)); ctx(S, oN, 1); pctx(rG, oN, SR) }
            trans(WIDTH) { src(B, oN, 1); tgt(_t6, oN, ER); lhg(setOf(_t10, _t7)); ctx(S, oN, 1) }
            trans(HEIGHT) { src(_t6, oN, ER); tgt(L, oN, 1); lhg(setOf(_t10, _t7), setOf(RT)); ctx(B, oN, 1); pctx(S, oN, 1) }
            trans(WIDTH) { src(L, oN, 1); tgt(_t10, oN, ER); lhg(setOf(_t11, _t7)); ctx(B, oN, 1) }
            trans(WIDTH) { src(L, oN, 1); tgt(EMPTY_LIST, oN, ER); lhg(_t7); ctx(B, oN, 1) }
            trans(GRAFT) { src(Es, SE, ER); tgt(L, oN, 2); lhg(_t7); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(WIDTH) { src(L, oN, 2); tgt(_t7, oN, ER); lhg(RT); ctx(B, oN, 1) }
            trans(GRAFT) { src(_t7, oN, ER); tgt(L, oN, ER); lhg(RT); ctx(L, oN, 2); pctx(B, oN, 1) }
            trans(GRAFT) { src(L, oN, ER); tgt(B, oN, ER); lhg(RT); ctx(B, oN, 1); pctx(S, oN, 1) }
            trans(GRAFT) { src(B, oN, ER); tgt(S, oN, ER); lhg(RT); ctx(S, oN, 1); pctx(rG, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
        }

        AutomatonTest.assertEquals(expected, actual)

    }

    @Test
    fun parse_abcd() {
        //TODO: is there a way to reset the rrs if it needs it?
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "abcd")
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        println(rrs.usedAutomatonToString("S"))

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t2, oN, ER)   // 'a'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(A, oN, ER)   // A = 'a' .
            state(optA, LI, ER)   // optA = A .
            state(S, oN, 1)   // S = optA . B
            state(_t4, oN, ER)   // 'b'
            state(B, oN, 1)   // B = 'b' . L
            state(_t6, oN, ER)   // 'c'
            state(L, oN, 1)   // L = 'c' . Es 'd'
            state(_t10, oN, ER)   // 'e'
            state(Es, SE, ER)   // Es = 'e' .
            state(L, oN, 2)   // L = 'c' Es . 'd'
            state(_t7, oN, ER)   // 'd'
            state(L, oN, ER)   // L = 'c' Es 'd' .
            state(B, oN, ER)   // B = 'b' L .
            state(S, oN, ER)   // S = optA B .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t2, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t2, oN, ER); tgt(A, oN, ER); lhg(setOf(_t4), setOf(_t4)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(Es, SE, ER); lhg(setOf(_t7), setOf(_t7)); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(HEIGHT) { src(A, oN, ER); tgt(optA, LI, ER); lhg(setOf(_t4), setOf(_t4)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(optA, LI, ER); tgt(S, oN, 1); lhg(setOf(_t4), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 1); tgt(_t4, oN, ER); lhg(_t6); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t4, oN, ER); tgt(B, oN, 1); lhg(setOf(_t6), setOf(RT)); ctx(S, oN, 1); pctx(rG, oN, SR) }
            trans(WIDTH) { src(B, oN, 1); tgt(_t6, oN, ER); lhg(setOf(_t10, _t7)); ctx(S, oN, 1) }
            trans(HEIGHT) { src(_t6, oN, ER); tgt(L, oN, 1); lhg(setOf(_t10, _t7), setOf(RT)); ctx(B, oN, 1); pctx(S, oN, 1) }
            trans(WIDTH) { src(L, oN, 1); tgt(_t10, oN, ER); lhg(setOf(_t11, _t7)); ctx(B, oN, 1) }
            trans(WIDTH) { src(L, oN, 1); tgt(EMPTY_LIST, oN, ER); lhg(_t7); ctx(B, oN, 1) }
            trans(GRAFT) { src(Es, SE, ER); tgt(L, oN, 2); lhg(_t7); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(WIDTH) { src(L, oN, 2); tgt(_t7, oN, ER); lhg(RT); ctx(B, oN, 1) }
            trans(GRAFT) { src(_t7, oN, ER); tgt(L, oN, ER); lhg(RT); ctx(L, oN, 2); pctx(B, oN, 1) }
            trans(GRAFT) { src(L, oN, ER); tgt(B, oN, ER); lhg(RT); ctx(B, oN, 1); pctx(S, oN, 1) }
            trans(GRAFT) { src(B, oN, ER); tgt(S, oN, ER); lhg(RT); ctx(S, oN, 1); pctx(rG, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
        }

        AutomatonTest.assertEquals(expected, actual)

    }

    @Test
    fun parse_abced() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "abced")
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        println(rrs.usedAutomatonToString("S"))

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t2, oN, ER)   // 'a'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(A, oN, ER)   // A = 'a' .
            state(optA, LI, ER)   // optA = A .
            state(S, oN, 1)   // S = optA . B
            state(_t4, oN, ER)   // 'b'
            state(B, oN, 1)   // B = 'b' . L
            state(_t6, oN, ER)   // 'c'
            state(L, oN, 1)   // L = 'c' . Es 'd'
            state(_t10, oN, ER)   // 'e'
            state(Es, SI, ER)   // ['e' sep 'f'] .
            state(Es, SI, 1)   // ['f' . 'e' sep 'f']
            state(L, oN, 2)   // L = 'c' Es . 'd'
            state(_t7, oN, ER)   // 'd'
            state(L, oN, ER)   // L = 'c' Es 'd' .
            state(B, oN, ER)   // B = 'b' L .
            state(S, oN, ER)   // S = optA B .
            state(rG, oN, ER)   // <GOAL> = S .

            trans(WIDTH) { src(rG, oN, SR); tgt(_t2, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t2, oN, ER); tgt(A, oN, ER); lhg(setOf(_t4), setOf(_t4)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(A, oN, ER); tgt(optA, LI, ER); lhg(setOf(_t4), setOf(_t4)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(optA, LI, ER); tgt(S, oN, 1); lhg(setOf(_t4), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 1); tgt(_t4, oN, ER); lhg(_t6); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t4, oN, ER); tgt(B, oN, 1); lhg(setOf(_t6), setOf(RT)); ctx(S, oN, 1); pctx(rG, oN, SR) }
            trans(WIDTH) { src(B, oN, 1); tgt(_t6, oN, ER); lhg(setOf(_t10, _t7)); ctx(S, oN, 1) }
            trans(HEIGHT) { src(_t6, oN, ER); tgt(L, oN, 1); lhg(setOf(_t10, _t7), setOf(RT)); ctx(B, oN, 1); pctx(S, oN, 1) }
            trans(WIDTH) { src(L, oN, 1); tgt(_t10, oN, ER); lhg(setOf(_t11, _t7)); ctx(B, oN, 1) }
            trans(WIDTH) { src(L, oN, 1); tgt(EMPTY_LIST, oN, ER); lhg(_t7); ctx(B, oN, 1) }
            trans(HEIGHT) { src(_t10, oN, ER); tgt(Es, SI, ER); lhg(setOf(_t7), setOf(_t7)); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(HEIGHT) { src(_t10, oN, ER); tgt(Es, SI, 1); lhg(setOf(_t11), setOf(_t7)); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(GRAFT) { src(Es, SI, ER); tgt(L, oN, 2); lhg(_t7); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(WIDTH) { src(L, oN, 2); tgt(_t7, oN, ER); lhg(RT); ctx(B, oN, 1) }
            trans(GRAFT) { src(_t7, oN, ER); tgt(L, oN, ER); lhg(RT); ctx(L, oN, 2); pctx(B, oN, 1) }
            trans(GRAFT) { src(L, oN, ER); tgt(B, oN, ER); lhg(RT); ctx(B, oN, 1); pctx(S, oN, 1) }
            trans(GRAFT) { src(B, oN, ER); tgt(S, oN, ER); lhg(RT); ctx(S, oN, 1); pctx(rG, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
        }

        AutomatonTest.assertEquals(expected, actual)

    }

    @Test
    fun sentences() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val sentences = listOf("bcd", "abcd", "bced", "abced", "bcefed", "abcefed")
        for (sent in sentences) {
            println("Parsing sentence '$sent'")
            val result = parser.parseForGoal("S", sent)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(_t2, oN, ER)   // 'a'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(optA, LE, ER)   // [EMPTY A] .
            state(S, oN, 1)   // S = optA . B
            state(_t4, oN, ER)   // 'b'
            state(B, oN, 1)   // B = 'b' . L
            state(_t6, oN, ER)   // 'c'
            state(L, oN, 1)   // L = 'c' . Es 'd'
            state(_t10, oN, ER)   // 'e'
            state(Es, SE, ER)   // [EMPTY 'e' sep 'f'] .
            state(L, oN, 2)   // L = 'c' Es . 'd'
            state(_t7, oN, ER)   // 'd'
            state(L, oN, ER)   // L = 'c' Es 'd' .
            state(B, oN, ER)   // B = 'b' L .
            state(S, oN, ER)   // S = optA B .
            state(rG, oN, ER)   // <GOAL> = S .
            state(A, oN, ER)   // A = 'a' .
            state(optA, LI, ER)   // [A] .
            state(Es, SI, ER)   // ['e' sep 'f'] .
            state(Es, SI, 1)   // ['f' . 'e' sep 'f']
            state(_t11, oN, ER)   // 'f'
            state(Es, SI, 2)   // ['e' . 'e' sep 'f']

            trans(WIDTH) { src(rG, oN, SR); tgt(_t2, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t2, oN, ER); tgt(A, oN, ER); lhg(setOf(_t4), setOf(_t4)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(optA, LE, ER); lhg(setOf(_t4), setOf(_t4)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(Es, SE, ER); lhg(setOf(_t7), setOf(_t7)); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(HEIGHT) { src(optA, LE, ER); tgt(S, oN, 1); lhg(setOf(_t4), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 1); tgt(_t4, oN, ER); lhg(_t6); ctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t4, oN, ER); tgt(B, oN, 1); lhg(setOf(_t6), setOf(RT)); ctx(S, oN, 1); pctx(rG, oN, SR) }
            trans(WIDTH) { src(B, oN, 1); tgt(_t6, oN, ER); lhg(setOf(_t10, _t7)); ctx(S, oN, 1) }
            trans(HEIGHT) { src(_t6, oN, ER); tgt(L, oN, 1); lhg(setOf(_t10, _t7), setOf(RT)); ctx(B, oN, 1); pctx(S, oN, 1) }
            trans(WIDTH) { src(L, oN, 1); tgt(_t10, oN, ER); lhg(setOf(_t11, _t7)); ctx(B, oN, 1) }
            trans(WIDTH) { src(L, oN, 1); tgt(EMPTY_LIST, oN, ER); lhg(_t7); ctx(B, oN, 1) }
            trans(HEIGHT) { src(_t10, oN, ER); tgt(Es, SI, ER); lhg(setOf(_t7), setOf(_t7)); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(HEIGHT) { src(_t10, oN, ER); tgt(Es, SI, 1); lhg(setOf(_t11), setOf(_t7)); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(GRAFT) { src(_t10, oN, ER); tgt(Es, SI, ER); lhg(RT); ctx(Es, SI, 2); pctx(L, oN, 1) }
            trans(GRAFT) { src(_t10, oN, ER); tgt(Es, SI, 1); lhg(_t11); ctx(Es, SI, 2); pctx(L, oN, 1) }
            trans(GRAFT) { src(Es, SE, ER); tgt(L, oN, 2); lhg(_t7); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(WIDTH) { src(L, oN, 2); tgt(_t7, oN, ER); lhg(RT); ctx(B, oN, 1) }
            trans(GRAFT) { src(_t7, oN, ER); tgt(L, oN, ER); lhg(RT); ctx(L, oN, 2); pctx(B, oN, 1) }
            trans(GRAFT) { src(L, oN, ER); tgt(B, oN, ER); lhg(RT); ctx(B, oN, 1); pctx(S, oN, 1) }
            trans(GRAFT) { src(B, oN, ER); tgt(S, oN, ER); lhg(RT); ctx(S, oN, 1); pctx(rG, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(A, oN, ER); tgt(optA, LI, ER); lhg(setOf(_t4), setOf(_t4)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(optA, LI, ER); tgt(S, oN, 1); lhg(setOf(_t4), setOf(RT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(GRAFT) { src(Es, SI, ER); tgt(L, oN, 2); lhg(_t7); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(WIDTH) { src(Es, SI, 1); tgt(_t11, oN, ER); lhg(_t10); ctx(L, oN, 1) }
            trans(GRAFT) { src(_t11, oN, ER); tgt(Es, SI, 2); lhg(_t10); ctx(Es, SI, 1); pctx(L, oN, 1) }
            trans(WIDTH) { src(Es, SI, 2); tgt(_t10, oN, ER); lhg(setOf(RT, _t11)); ctx(L, oN, 1) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "abcd")
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, oN, 1)   // S = optA . B
            state(S, oN, ER)   // S = optA B .
            state(optA, LI, ER)   // [A] .
            state(optA, LE, ER)   // [EMPTY A] .
            state(A, oN, ER)   // A = 'a' .
            state(B, oN, 1)   // B = 'b' . L
            state(B, oN, ER)   // B = 'b' L .
            state(L, oN, 1)   // L = 'c' . Es 'd'
            state(L, oN, 2)   // L = 'c' Es . 'd'
            state(L, oN, ER)   // L = 'c' Es 'd' .
            state(Es, SI, 2)   // ['e' . 'e' sep 'f']
            state(Es, SI, 1)   // ['f' . 'e' sep 'f']
            state(Es, SI, ER)   // ['e' sep 'f'] .
            state(Es, SE, ER)   // [EMPTY 'e' sep 'f'] .
            state(_t2, oN, ER)   // 'a'
            state(EMPTY_LIST, oN, ER)   // <EMPTY_LIST>
            state(_t4, oN, ER)   // 'b'
            state(_t6, oN, ER)   // 'c'
            state(_t10, oN, ER)   // 'e'
            state(_t11, oN, ER)   // 'f'
            state(_t7, oN, ER)   // 'd'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t2, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(EMPTY_LIST, oN, ER); lhg(_t4); ctx(rG, oN, SR) }
            trans(WIDTH) { src(S, oN, 1); tgt(_t4, oN, ER); lhg(_t6); ctx(rG, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(rG, oN, ER); lhg(EOT); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(optA, LI, ER); tgt(S, oN, 1); lhg(setOf(_t4), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(optA, LE, ER); tgt(S, oN, 1); lhg(setOf(_t4), setOf(EOT)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(A, oN, ER); tgt(optA, LI, ER); lhg(setOf(_t4), setOf(_t4)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(WIDTH) { src(B, oN, 1); tgt(_t6, oN, ER); lhg(setOf(_t10, _t7)); ctx(S, oN, 1) }
            trans(GRAFT) { src(B, oN, ER); tgt(S, oN, ER); lhg(EOT); ctx(S, oN, 1); pctx(rG, oN, SR) }
            trans(WIDTH) { src(L, oN, 1); tgt(_t10, oN, ER); lhg(setOf(_t11, _t7)); ctx(B, oN, 1) }
            trans(WIDTH) { src(L, oN, 1); tgt(EMPTY_LIST, oN, ER); lhg(_t7); ctx(B, oN, 1) }
            trans(WIDTH) { src(L, oN, 2); tgt(_t7, oN, ER); lhg(EOT); ctx(B, oN, 1) }
            trans(GRAFT) { src(L, oN, ER); tgt(B, oN, ER); lhg(EOT); ctx(B, oN, 1); pctx(S, oN, 1) }
            trans(WIDTH) { src(Es, SI, 2); tgt(_t10, oN, ER); lhg(setOf(_t11, _t7)); ctx(L, oN, 1) }
            trans(WIDTH) { src(Es, SI, 1); tgt(_t11, oN, ER); lhg(_t10); ctx(L, oN, 1) }
            trans(GRAFT) { src(Es, SI, ER); tgt(L, oN, 2); lhg(_t7); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(GRAFT) { src(Es, SE, ER); tgt(L, oN, 2); lhg(_t7); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(HEIGHT) { src(_t2, oN, ER); tgt(A, oN, ER); lhg(setOf(_t4), setOf(_t4)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(optA, LE, ER); lhg(setOf(_t4), setOf(_t4)); ctx(rG, oN, SR); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(EMPTY_LIST, oN, ER); tgt(Es, SE, ER); lhg(setOf(_t7), setOf(_t7)); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(HEIGHT) { src(_t4, oN, ER); tgt(B, oN, 1); lhg(setOf(_t6), setOf(EOT)); ctx(S, oN, 1); pctx(rG, oN, SR) }
            trans(HEIGHT) { src(_t6, oN, ER); tgt(L, oN, 1); lhg(setOf(_t10, _t7), setOf(EOT)); ctx(B, oN, 1); pctx(S, oN, 1) }
            trans(HEIGHT) { src(_t10, oN, ER); tgt(Es, SI, ER); lhg(setOf(_t7), setOf(_t7)); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(HEIGHT) { src(_t10, oN, ER); tgt(Es, SI, 1); lhg(setOf(_t11), setOf(_t7)); ctx(L, oN, 1); pctx(B, oN, 1) }
            trans(GRAFT) { src(_t10, oN, ER); tgt(Es, SI, ER); lhg(_t7); ctx(Es, SI, 2); pctx(L, oN, 1) }
            trans(GRAFT) { src(_t10, oN, ER); tgt(Es, SI, 1); lhg(_t11); ctx(Es, SI, 2); pctx(L, oN, 1) }
            trans(GRAFT) { src(_t11, oN, ER); tgt(Es, SI, 2); lhg(_t10); ctx(Es, SI, 1); pctx(L, oN, 1) }
            trans(GRAFT) { src(_t7, oN, ER); tgt(L, oN, ER); lhg(EOT); ctx(L, oN, 2); pctx(B, oN, 1) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("bcd", "abcd", "bced", "abced", "bcefed", "abcefed","abcefefed")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            //val result = parser.parseForGoal("S", "", AutomatonKind.LOOKAHEAD_1)
            if (result.issues.isNotEmpty()) result.issues.forEach { println(it) }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S") as ParserStateSet
        val automaton_preBuild = rrs_preBuild.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertMatches(
            automaton_preBuild, automaton_noBuild, AutomatonTest.MatchConfiguration(
                no_lookahead_compare = true
            )
        )
    }
}