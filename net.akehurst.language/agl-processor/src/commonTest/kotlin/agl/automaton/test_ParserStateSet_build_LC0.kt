/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.automaton

import agl.automaton.automaton
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import kotlin.test.Test


class test_ParserStateSet_build_LC0 : test_AutomatonUtilsAbstract() {

    private companion object {
        val automatonKind = AutomatonKind.LOOKAHEAD_NONE
    }

    @Test
    fun singleLiteral() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val G = SM.startState.runtimeRules.first()
        val a = rrs.findRuntimeRule("'a'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S
            val s1 = state(RP(G, 0, EOR))      // G = S .
            val s2 = state(RP(S, 0, EOR))      // S = 'a' .
            val s3 = state(RP(a, 0, EOR))      // 'a'

            transition(null, s0, s3, WIDTH, setOf(ANY), setOf(), null)
            transition(s0, s3, s2, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, SOR)))
            transition(s0, s2, s1, GRAFT, setOf(UP), setOf(ANY), listOf(RP(G, 0, SOR)))
            transition(null, s1, s1, GOAL, setOf(), setOf(), null)
        }

        super.assertEquals(expected, actual)

        val sentences = listOf(
            "a"
        )
        val parser = ScanOnDemandParser(rrs)
        for (sentence in sentences) {
            parser.parse("S", sentence, AutomatonKind.LOOKAHEAD_1)
        }
        //println(rrs.usedAutomatonToString("S"))
    }

    @Test
    fun concatLiterals() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); literal("b"); literal("c"); literal("d") }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val G = SM.startState.runtimeRules.first()
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val d = rrs.findRuntimeRule("'d'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S
            val s1 = state(RP(a, 0, EOR))      // a
            val s2 = state(RP(S, 0, 1))   // S = a . b c d
            val s3 = state(RP(b, 0, EOR))      // b
            val s4 = state(RP(S, 0, 2))   // S = a b . c d
            val s5 = state(RP(c, 0, EOR))      // c
            val s6 = state(RP(S, 0, 3))   // S = a b c . d
            val s7 = state(RP(d, 0, EOR))      // d
            val s8 = state(RP(S, 0, EOR))      // S = a b c d .
            val s9 = state(RP(G, 0, EOR))      // G = S .

            transition(null, s0, s1, WIDTH, setOf(ANY), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, SOR)))
            transition(s0, s2, s3, WIDTH, setOf(ANY), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, 1)))
            transition(s0, s4, s5, WIDTH, setOf(ANY), setOf(), null)
            transition(s4, s5, s6, GRAFT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, 2)))
            transition(s0, s6, s7, WIDTH, setOf(ANY), setOf(), null)
            transition(s6, s7, s8, GRAFT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, 3)))
            transition(s0, s8, s9, GRAFT, setOf(UP), setOf(ANY), listOf(RP(G, 0, SOR)))
            transition(null, s9, s9, GOAL, setOf(), setOf(), null)
        }

        super.assertEquals(expected, actual)

        val sentences = listOf(
            "abcd"
        )
        val parser = ScanOnDemandParser(rrs)
        for (sentence in sentences) {
            parser.parse("S", sentence, automatonKind)
        }
    }

    @Test
    fun choiceLiterals() {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a");
                literal("b");
                literal("c");
                literal("d")
            }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val G = SM.startState.runtimeRules.first()
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val d = rrs.findRuntimeRule("'d'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S
            val s1 = state(RP(a, 0, EOR))      // a
            val s2 = state(RP(b, 0, EOR))      // b
            val s3 = state(RP(c, 0, EOR))      // c
            val s4 = state(RP(d, 0, EOR))      // d
            val s5 = state(RP(S, 0, EOR))      // S = a .
            val s6 = state(RP(S, 1, EOR))      // S = b .
            val s7 = state(RP(S, 2, EOR))      // S = c .
            val s8 = state(RP(S, 3, EOR))      // S = d .
            val s9 = state(RP(G, 0, EOR))      // G = S .

            transition(null, s0, s1, WIDTH, setOf(ANY), setOf(), null)
            transition(null, s0, s2, WIDTH, setOf(ANY), setOf(), null)
            transition(null, s0, s3, WIDTH, setOf(ANY), setOf(), null)
            transition(null, s0, s4, WIDTH, setOf(ANY), setOf(), null)
            transition(s0, s1, s5, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, SOR)))
            transition(s0, s2, s6, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(S, 1, SOR)))
            transition(s0, s3, s7, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(S, 2, SOR)))
            transition(s0, s4, s8, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(S, 3, SOR)))
            transition(s0, s5, s9, GRAFT, setOf(UP), setOf(ANY), listOf(RP(G, 0, SOR)))
            transition(s0, s6, s9, GRAFT, setOf(UP), setOf(ANY), listOf(RP(G, 0, SOR)))
            transition(s0, s7, s9, GRAFT, setOf(UP), setOf(ANY), listOf(RP(G, 0, SOR)))
            transition(s0, s8, s9, GRAFT, setOf(UP), setOf(ANY), listOf(RP(G, 0, SOR)))
            transition(null, s9, s9, GOAL, setOf(), setOf(), null)
        }

        super.assertEquals(expected, actual)

        val sentences = listOf(
            "a",
            "b",
            "c",
            "d"
        )
        val parser = ScanOnDemandParser(rrs)
        for (sentence in sentences) {
            parser.parse("S", sentence, automatonKind)
        }
    }

    @Test
    fun concatNonTerm() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("A"); ref("B"); ref("C") }
            concatenation("A") { literal("a") }
            concatenation("B") { literal("b") }
            concatenation("C") { literal("c") }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val G = SM.startState.runtimeRules.first()
        val A = rrs.findRuntimeRule("A")
        val B = rrs.findRuntimeRule("B")
        val C = rrs.findRuntimeRule("C")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S
            val s1 = state(RP(a, 0, EOR))      // a
            val s2 = state(RP(A, 0, EOR))      // A = a .
            val s3 = state(RP(S, 0, 1))   // S = A . B C
            val s4 = state(RP(b, 0, EOR))      // b
            val s5 = state(RP(B, 0, EOR))      // B = b .
            val s6 = state(RP(S, 0, 2))   // S = A B . C
            val s7 = state(RP(c, 0, EOR))      // c
            val s8 = state(RP(C, 0, EOR))      // C = c .
            val s9 = state(RP(S, 0, EOR))      // S = A B C .
            val s10 = state(RP(G, 0, EOR))      // G = S .

            transition(null, s0, s1, WIDTH, setOf(ANY), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(A, 0, SOR)))
            transition(s0, s2, s3, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, SOR)))
            transition(s0, s3, s4, WIDTH, setOf(ANY), setOf(), null)
            transition(s3, s4, s5, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(B, 0, SOR)))
            transition(s3, s5, s6, GRAFT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, 1)))
            transition(s0, s6, s7, WIDTH, setOf(ANY), setOf(), null)
            transition(s6, s7, s8, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(C, 0, SOR)))
            transition(s6, s8, s9, GRAFT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, 2)))
            transition(s0, s9, s10, GRAFT, setOf(UP), setOf(ANY), listOf(RP(G, 0, SOR)))
            transition(null, s10, s10, GOAL, setOf(), setOf(), null)
        }

        super.assertEquals(expected, actual)

        val sentences = listOf(
            "abc"
        )
        val parser = ScanOnDemandParser(rrs)
        for (sentence in sentences) {
            parser.parse("S", sentence, automatonKind)
        }
        //println(rrs.usedAutomatonToString("S"))
    }

    @Test
    fun empty() {
        val rrs = runtimeRuleSet {
            concatenation("S") { empty() }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val G = SM.startState.runtimeRules.first()
        val eS = S.rhs.items[0]

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S
            val s1 = state(RP(eS, 0, EOR))      // eS
            val s2 = state(RP(S, 0, EOR))      // S = eS .
            val s3 = state(RP(G, 0, EOR))      // G = S .

            transition(null, s0, s1, WIDTH, setOf(ANY), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, SOR)))
            transition(s0, s2, s3, GRAFT, setOf(UP), setOf(ANY), listOf(RP(G, 0, SOR)))
            transition(null, s3, s3, GOAL, setOf(), setOf(), null)
        }

        super.assertEquals(expected, actual)

        val sentences = listOf(
            ""
        )
        val parser = ScanOnDemandParser(rrs)
        for (sentence in sentences) {
            parser.parse("S", sentence, automatonKind)
        }
    }

    @Test
    fun concatAllOptional() {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("oA"); ref("oB"); ref("oC") }
            choice("oA", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { literal("a"); empty() }
            choice("oB", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { literal("b"); empty() }
            choice("oC", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { literal("c"); empty() }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val G = SM.startState.runtimeRules.first()
        val oA = rrs.findRuntimeRule("oA")
        val oB = rrs.findRuntimeRule("oB")
        val oC = rrs.findRuntimeRule("oC")
        val eoA = oA.rhs.items[1]
        val eoB = oB.rhs.items[1]
        val eoC = oC.rhs.items[1]
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S
            val s1 = state(RP(a, 0, EOR))      // a
            val s2 = state(RP(eoA, 0, EOR))    // empty-oA
            val s3 = state(RP(oA, 0, EOR))     // oA = a .
            val s4 = state(RP(oA, 1, EOR))     // oA =  .
            val s5 = state(RP(S, 0, 1))   // S = oA . oB oC
            val s6 = state(RP(b, 0, EOR))      // b
            val s7 = state(RP(eoB, 0, EOR))    // empty-oB
            val s8 = state(RP(oB, 0, EOR))     // oB = b .
            val s9 = state(RP(oB, 1, EOR))     // oB = .
            val s10 = state(RP(S, 0, 2))   // S = oA oB . oC
            val s11 = state(RP(c, 0, EOR))      // c
            val s12 = state(RP(eoC, 0, EOR))    // empty-oC
            val s13 = state(RP(oC, 0, EOR))     // oC = c .
            val s14 = state(RP(oC, 1, EOR))     // oC = .
            val s15 = state(RP(S, 0, EOR))      // S = oA oB oC .
            val s16 = state(RP(G, 0, EOR))      // G = S .

            transition(null, s0, s1, WIDTH, setOf(ANY), setOf(), null)
            transition(null, s0, s2, WIDTH, setOf(ANY), setOf(), null)
            transition(s0, s1, s3, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(oA, 0, SOR)))
            transition(s0, s2, s4, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(oA, 1, SOR)))
            transition(s0, s3, s5, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, 0)))
            transition(s0, s4, s5, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, 0)))
            transition(s0, s5, s6, WIDTH, setOf(ANY), setOf(), null)
            transition(s0, s5, s7, WIDTH, setOf(ANY), setOf(), null)
            transition(s5, s6, s8, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(oB, 0, SOR)))
            transition(s5, s7, s9, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(oB, 1, SOR)))
            transition(s5, s8, s10, GRAFT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, 1)))
            transition(s5, s9, s10, GRAFT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, 1)))
            transition(s0, s10, s11, WIDTH, setOf(ANY), setOf(), null)
            transition(s0, s10, s12, WIDTH, setOf(ANY), setOf(), null)
            transition(s10, s11, s13, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(oC, 0, SOR)))
            transition(s10, s12, s14, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(oC, 1, SOR)))
            transition(s10, s13, s15, GRAFT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, 2)))
            transition(s10, s14, s15, GRAFT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, 2)))
            transition(s0, s15, s16, GRAFT, setOf(UP), setOf(ANY), listOf(RP(G, 0, 0)))
            transition(null, s16, s16, GOAL, setOf(), setOf(), null)
        }

        super.assertEquals(expected, actual)

        val sentences = listOf(
            "abc",
            "ab",
            "ac",
            "bc",
            "a",
            "b",
            "c",
            ""
        )
        val parser = ScanOnDemandParser(rrs)
        for (sentence in sentences) {
            parser.parse("S", sentence, automatonKind)
        }
    }

    @Test
    fun duplicateContentOfRule() {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ABC1")
                ref("ABC2")
            }
            concatenation("ABC1") { literal("a"); literal("b"); literal("c") }
            concatenation("ABC2") { literal("a"); literal("b"); literal("c") }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val G = SM.startState.runtimeRules.first()
        val ABC1 = rrs.findRuntimeRule("ABC1")
        val ABC2 = rrs.findRuntimeRule("ABC2")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")

        //val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S
            val s1 = state(RP(a, 0, EOR))      // a
            val s2 = state(RP(ABC1, 0, 1), RP(ABC2, 0, 1))       // ABC1 = a . b c, ABC2 = a . b c
            val s3 = state(RP(b, 0, SOR))      // b
            val s4 = state(RP(ABC1, 0, 2), RP(ABC2, 0, 2))      // ABC1 = a b . c, ABC2 = a b . c
            val s5 = state(RP(c, 0, EOR))      // c
            val s6 = state(RP(ABC1, 0, EOR), RP(ABC2, 0, EOR))    // ABC1 = a b c ., ABC2 = a b c .
            val s7 = state(RP(S, 0, EOR), RP(S, 1, EOR))          // S = ABC1 ., S = ABC2 .
            val s8 = state(RP(G, 0, SOR))                             // G = S .

            transition(null, s0, s1, WIDTH, setOf(ANY), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(ABC1, 0, SOR), RP(ABC2, 0, SOR)))
            transition(s0, s2, s3, WIDTH, setOf(ANY), setOf(ANY), null)
            transition(s0, s3, s4, GRAFT, setOf(ANY), setOf(ANY), listOf(RP(ABC1, 0, 1), RP(ABC2, 0, 1)))
            transition(s0, s4, s5, WIDTH, setOf(ANY), setOf(ANY), null)
            transition(s0, s5, s6, GRAFT, setOf(ANY), setOf(ANY), listOf(RP(ABC1, 0, 2), RP(ABC2, 0, 2)))
            transition(s0, s6, s7, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(S, 0, SOR), RP(S, 1, SOR)))
            transition(s0, s7, s8, GRAFT, setOf(UP), setOf(ANY), listOf(RP(G, 0, SOR)))
            transition(null, s8, s8, GOAL, setOf(), setOf(), null)
        }

        //super.assertEquals(expected, actual)

        val sentences = listOf(
            "abc",
            "abc"
        )
        val parser = ScanOnDemandParser(rrs)
        for (sentence in sentences) {
            parser.parse("S", sentence, automatonKind)
        }
        println(rrs.usedAutomatonToString("S"))
    }

    @Test
    fun duplicateBeginningOfRule() {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ABC")
                ref("ABD")
            }
            concatenation("ABC") { literal("a"); literal("b"); literal("c") }
            concatenation("ABD") { literal("a"); literal("b"); literal("d") }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S


        }

        //super.assertEquals(expected, actual)

        val sentences = listOf(
            "abc",
            "abd"
        )
        val parser = ScanOnDemandParser(rrs)
        for (sentence in sentences) {
            parser.parse("S", sentence, automatonKind)
        }
        println(rrs.usedAutomatonToString("S"))
    }

    @Test
    fun duplicateOffsetPartOfRule() {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ABC")
                ref("XABD")
            }
            concatenation("ABC") { literal("a"); literal("b"); literal("c") }
            concatenation("XABD") { literal("x"); literal("a"); literal("b"); literal("d") }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()

        val actual = SM.build()
        //println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S


        }

        //super.assertEquals(expected, actual)

        val sentences = listOf(
            "abc",
            "xabd"
        )
        val parser = ScanOnDemandParser(rrs)
        for (sentence in sentences) {
            parser.parse("S", sentence, automatonKind)
        }
        println(rrs.usedAutomatonToString("S"))
    }

    @Test
    fun duplicateContentOfRuleConcatAndMultix4() {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("AAAA")
                ref("AAAAm")
            }
            concatenation("AAAA") { literal("a"); literal("a"); literal("a"); literal("a") }
            multi("AAAAm", 4, 4, "'a'")
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()

        val actual = SM.build()
        //println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S


        }

        //super.assertEquals(expected, actual)

        val sentences = listOf(
            "aaaa"
        )
        val parser = ScanOnDemandParser(rrs)
        for (sentence in sentences) {
            parser.parse("S", sentence, automatonKind)
        }
        println(rrs.usedAutomatonToString("S"))
    }

    @Test
    fun duplicateContentOfRuleConcatAndMultixN() {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("AAAA")
                ref("AAAAm")
            }
            concatenation("AAAA") { literal("a"); literal("a"); literal("a"); literal("a") }
            multi("AAAAm", 0, -1, "'a'")
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()

        val actual = SM.build()
        //println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S


        }

        //super.assertEquals(expected, actual)

        val sentences = listOf(
            "a",
            "aa",
            "aaa",
            "aaaa",
            "aaaaa",
            "aaaaaa",
            "aaaaaaa"
        )
        val parser = ScanOnDemandParser(rrs)
        for (sentence in sentences) {
            parser.parse("S", sentence, automatonKind)
        }
        println(rrs.usedAutomatonToString("S"))
    }

    @Test
    fun duplicateContentOfRuleMultiAndMulti() {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("AAAAm1")
                ref("AAAAm2")
            }
            multi("AAAAm1", 1, 4, "'a'")
            multi("AAAAm2", 3, 7, "'a'")
            literal("'a'", "a")
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()

        val actual = SM.build()
        //println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S


        }

        //super.assertEquals(expected, actual)

        val sentences = listOf(
            "a",
            "aa",
            "aaa",
            "aaaa",
            "aaaaa",
            "aaaaaa",
            "aaaaaaa"
        )
        val parser = ScanOnDemandParser(rrs)
        for (sentence in sentences) {
            parser.parse("S", sentence, automatonKind)
        }
        println(rrs.usedAutomatonToString("S"))
    }

    @Test
    fun AhoSetiUlman_Grm_4_1() {
        val rrs = runtimeRuleSet {
            choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("E1")
                ref("E2")
            }
            concatenation("E1") { ref("E"); literal("+"); ref("T") }
            concatenation("E2") { ref("T"); }
            choice("T", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("T1")
                ref("T2")
            }
            concatenation("T1") { ref("T"); literal("*"); ref("F") }
            concatenation("T2") { ref("F") }
            choice("F", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("F1")
                ref("F2")
            }
            concatenation("F1") { literal("("); literal("id"); literal(")") }
            concatenation("F2") { literal("id"); }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S


        }

        //super.assertEquals(expected, actual)

        val sentences = listOf(
            "id",
            "(id)",
            "..."
        )
        val parser = ScanOnDemandParser(rrs)
        for (sentence in sentences) {
            parser.parse("S", sentence, automatonKind)
        }
        println(rrs.usedAutomatonToString("S"))
    }

    @Test
    fun AhoSetiUlman_Ex_4_7_5() {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("S1")
                ref("S2")
                ref("S3")
                ref("S4")
            }
            concatenation("S1") { ref("A"); literal("a") }
            concatenation("S2") { literal("b"); ref("A"); literal("c") }
            concatenation("S3") { ref("B"); literal("c") }
            concatenation("S4") { literal("b"); ref("B"); literal("a") }
            concatenation("A") { literal("d") }
            concatenation("B") { literal("d") }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()

        //val actual = SM.build()
        //println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S


        }

        //super.assertEquals(expected, actual)

        val sentences = listOf(
            "da",
            "bdc",
            "dc",
            "bda"
        )
        val parser = ScanOnDemandParser(rrs)
        for (sentence in sentences) {
            parser.parse("S", sentence, automatonKind)
        }
        println(rrs.usedAutomatonToString("S"))
    }

    @Test
    fun leftRecursive() {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { ref("S"); literal("a") }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()

        //val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S


        }

        //super.assertEquals(expected, actual)

        val sentences = listOf(
            "a",
            "aa",
            "aaa",
            "aaaa"
        )
        val parser = ScanOnDemandParser(rrs)
        for (sentence in sentences) {
            parser.parse("S", sentence, automatonKind)
        }
        println(rrs.usedAutomatonToString("S"))
    }

    @Test
    fun rightRecursive() {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { literal("a"); ref("S") }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val G = SM.startState.runtimeRules.first()
        val a = rrs.findRuntimeRule("'a'")
        val S1 = rrs.findRuntimeRule("S1")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S
            val s1 = state(RP(a, 0, EOR))      // a
            val s2 = state(RP(S, 0, EOR))      // S = a .
            val s3 = state(RP(S1, 0, 1))  // S1 = a . S
            val s4 = state(RP(G, 0, EOR))      // G = S .
            val s5 = state(RP(S1, 0, EOR))     // S1 = a S .
            val s6 = state(RP(S, 1, EOR))      // S = S1 .

            transition(null, s0, s1, WIDTH, setOf(ANY), emptySet(), null)
            transition(listOf(s0,s3), s1, s2, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(S,0,SOR)))
            transition(listOf(s0,s3), s1, s3, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(S1,0,SOR)))
            transition(s0, s2, s4, GRAFT, setOf(UP), setOf(ANY), listOf(RP(G,0,SOR)))
            transition(s3, s2, s5, GRAFT, setOf(ANY), setOf(ANY), listOf(RP(S1,0,1)))
            transition(listOf(s0,s3), s3, s1, WIDTH, setOf(ANY), emptySet(), null)
            transition(null, s4, s4, GOAL, emptySet(), emptySet(), null)
            transition(listOf(s0,s3), s5, s6, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(S,1,SOR)))
            transition(s3, s5, s6, HEIGHT, setOf(ANY), setOf(ANY), listOf(RP(S,1,SOR)))
            transition(s3, s6, s5, GRAFT, setOf(ANY), setOf(ANY), listOf(RP(S1,0,1)))
            transition(s0, s6, s4, GRAFT, setOf(UP), setOf(ANY), listOf(RP(G,0,SOR)))

        }

        super.assertEquals(expected, actual)

        val sentences = listOf(
            "a",
            "aa",
            "aaa",
            "aaaa"
        )
        val parser = ScanOnDemandParser(rrs)
        for (sentence in sentences) {
            parser.parse("S", sentence, automatonKind)
        }
        println(rrs.usedAutomatonToString("S"))
    }
}