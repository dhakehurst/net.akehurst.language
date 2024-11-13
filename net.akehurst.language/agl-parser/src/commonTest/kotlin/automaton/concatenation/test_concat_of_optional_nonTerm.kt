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

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.automaton.leftcorner.AutomatonTest
import net.akehurst.language.automaton.leftcorner.automaton
import net.akehurst.language.automaton.leftcorner.test_AutomatonAbstract
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
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

    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("optA"); ref("B") }
            multi("optA", 0, 1, "A")
            concatenation("A") { literal("a") }
            concatenation("B") { literal("b"); ref("L") }
            concatenation("L") { literal("c"); ref("Es"); literal("d") }
            sList("Es", 0, -1, "'e'", "'f'")
            literal("'e'", "e")
            literal("'f'", "f")
        }

        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState.runtimeRules.first()
        val optA = rrs.findRuntimeRule("optA")
        val A = rrs.findRuntimeRule("A")
        val E_optA = EMPTY
        val B = rrs.findRuntimeRule("B")
        val L = rrs.findRuntimeRule("L")
        val Es = rrs.findRuntimeRule("Es")
        val E_Es = EMPTY
        val T_a = rrs.findRuntimeRule("'a'")
        val T_b = rrs.findRuntimeRule("'b'")
        val T_c = rrs.findRuntimeRule("'c'")
        val T_d = rrs.findRuntimeRule("'d'")
        val T_e = rrs.findRuntimeRule("'e'")
        val T_f = rrs.findRuntimeRule("'f'")
    }

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
            val s0 = state(RP(G, oN, SR))       // G = . S
            val s1 = state(RP(T_a, oN, EOR))       // 'a'
            val s2 = state(RP(E_optA, o0, EOR))
            val s3 = state(RP(optA, o1, EOR))
            val s4 = state(RP(S, oN, 1))
            val s5 = state(RP(T_b, oN, EOR))
            val s6 = state(RP(B, oN, 1))
            val s7 = state(RP(T_c, oN, EOR))
            val s8 = state(RP(L, oN, 1))
            val s9 = state(RP(T_e, oN, EOR))
            val s10 = state(RP(E_Es, oN, EOR))
            val s11 = state(RP(Es, oSI, EOR))
            val s12 = state(RP(L, oN, 2))
            val s13 = state(RP(T_d, oN, EOR))
            val s14 = state(RP(L, oN, EOR))
            val s15 = state(RP(B, oN, EOR))
            val s16 = state(RP(S, oN, EOR))
            val s17 = state(RP(G, oN, EOR))
            // S =  optA B ;
            // optA = A? ;
            // A = 'a'
            // B = b ;
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
            val s0 = state(RP(G, oN, SOR))       // G = . S
            val s1 = state(RP(T_a, oN, EOR))       // 'a'
            val s2 = state(RP(E_optA, o0, EOR))
            val s3 = state(RP(optA, o1, ER))
            val s4 = state(RP(S, oN, 1))
            val s5 = state(RP(T_b, oN, ER))
            val s6 = state(RP(B, oN, 1))
            val s7 = state(RP(T_c, oN, ER))
            val s8 = state(RP(L, oN, 1))
            val s9 = state(RP(T_e, oN, ER))
            val s10 = state(RP(E_Es, oN, ER))
            val s11 = state(RP(Es, oSI, ER))
            val s12 = state(RP(L, oN, 2))
            val s13 = state(RP(T_d, oN, ER))
            val s14 = state(RP(L, oN, ER))
            val s15 = state(RP(B, oN, ER))
            val s16 = state(RP(S, oN, ER))
            val s17 = state(RP(G, oN, ER))
            // S =  optA B ;
            // optA = A? ;
            // A = 'a'
            // B = b ;
        }

        AutomatonTest.assertEquals(expected, actual)

    }

    @Test
    fun parse_abced() {
        //TODO: is there a way to reset the rrs if it needs it?
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "abced")
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        println(rrs.usedAutomatonToString("S"))

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, oN, SOR))       // G = . S
            val s1 = state(RP(T_a, oN, ER))       // 'a'
            val s2 = state(RP(E_optA, o0, ER))
            val s3 = state(RP(optA, o1, ER))
            val s4 = state(RP(S, oN, 1))
            val s5 = state(RP(T_b, oN, ER))
            val s6 = state(RP(B, oN, 1))
            val s7 = state(RP(T_c, oN, ER))
            val s8 = state(RP(L, oN, 1))
            val s9 = state(RP(T_e, oN, ER))
            val s10 = state(RP(E_Es, oN, ER))
            val s11 = state(RP(Es, oSI, ER))
            val s12 = state(RP(L, oN, 2))
            val s13 = state(RP(T_d, oN, ER))
            val s14 = state(RP(L, oN, ER))
            val s15 = state(RP(B, oN, ER))
            val s16 = state(RP(S, oN, ER))
            val s17 = state(RP(G, oN, ER))
            // S =  optA B ;
            // optA = A? ;
            // A = 'a'
            // B = b ;
        }

        AutomatonTest.assertEquals(expected, actual)

    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val sentences = listOf("bcd", "abcd", "bced", "abced", "bcefed", "abcefed")
        for (sent in sentences) {
            println("Parsing sentence '$sent'")
            val result = parser.parseForGoal("S", sent)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {

        }

        AutomatonTest.assertEquals(expected, actual)
    }

}