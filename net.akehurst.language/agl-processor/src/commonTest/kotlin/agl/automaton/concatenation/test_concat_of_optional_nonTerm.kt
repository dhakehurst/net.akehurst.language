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

package net.akehurst.language.agl.automaton.concatenation

import net.akehurst.language.agl.automaton.AutomatonTest
import net.akehurst.language.agl.automaton.automaton
import net.akehurst.language.agl.automaton.test_AutomatonAbstract
import net.akehurst.language.agl.parser.LeftCornerParser
import net.akehurst.language.agl.regex.RegexEnginePlatform
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.agl.scanner.ScannerOnDemand
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_concat_of_optional_nonTerm : test_AutomatonAbstract() {

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
            val s0 = state(RP(G, 0, SR))       // G = . S
            val s1 = state(RP(T_a, 0, EOR))       // 'a'
            val s2 = state(RP(E_optA, 0, EOR))
            val s3 = state(RP(optA, 1, EOR))
            val s4 = state(RP(S, 0, 1))
            val s5 = state(RP(T_b, 0, EOR))
            val s6 = state(RP(B, 0, 1))
            val s7 = state(RP(T_c, 0, EOR))
            val s8 = state(RP(L, 0, 1))
            val s9 = state(RP(T_e, 0, EOR))
            val s10 = state(RP(E_Es, 0, EOR))
            val s11 = state(RP(Es, OLI, EOR))
            val s12 = state(RP(L, 0, 2))
            val s13 = state(RP(T_d, 0, EOR))
            val s14 = state(RP(L, 0, EOR))
            val s15 = state(RP(B, 0, EOR))
            val s16 = state(RP(S, 0, EOR))
            val s17 = state(RP(G, 0, EOR))
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
            val s0 = state(RP(G, 0, SOR))       // G = . S
            val s1 = state(RP(T_a, 0, EOR))       // 'a'
            val s2 = state(RP(E_optA, 0, EOR))
            val s3 = state(RP(optA, 1, ER))
            val s4 = state(RP(S, 0, 1))
            val s5 = state(RP(T_b, 0, ER))
            val s6 = state(RP(B, 0, 1))
            val s7 = state(RP(T_c, 0, ER))
            val s8 = state(RP(L, 0, 1))
            val s9 = state(RP(T_e, 0, ER))
            val s10 = state(RP(E_Es, 0, ER))
            val s11 = state(RP(Es, OLI, ER))
            val s12 = state(RP(L, 0, 2))
            val s13 = state(RP(T_d, 0, ER))
            val s14 = state(RP(L, 0, ER))
            val s15 = state(RP(B, 0, ER))
            val s16 = state(RP(S, 0, ER))
            val s17 = state(RP(G, 0, ER))
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
            val s0 = state(RP(G, 0, SOR))       // G = . S
            val s1 = state(RP(T_a, 0, ER))       // 'a'
            val s2 = state(RP(E_optA, 0, ER))
            val s3 = state(RP(optA, 1, ER))
            val s4 = state(RP(S, 0, 1))
            val s5 = state(RP(T_b, 0, ER))
            val s6 = state(RP(B, 0, 1))
            val s7 = state(RP(T_c, 0, ER))
            val s8 = state(RP(L, 0, 1))
            val s9 = state(RP(T_e, 0, ER))
            val s10 = state(RP(E_Es, 0, ER))
            val s11 = state(RP(Es, OLI, ER))
            val s12 = state(RP(L, 0, 2))
            val s13 = state(RP(T_d, 0, ER))
            val s14 = state(RP(L, 0, ER))
            val s15 = state(RP(B, 0, ER))
            val s16 = state(RP(S, 0, ER))
            val s17 = state(RP(G, 0, ER))
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