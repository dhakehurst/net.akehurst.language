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

package net.akehurst.language.automaton.leftcorner

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import testFixture.utils.AutomatonTest
import kotlin.test.Test


class test_ParserStateSet_build_LC1 : test_AutomatonUtilsAbstract() {

    private companion object {
        val automatonKind = AutomatonKind.LOOKAHEAD_1
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
            val s0 = state(RP(G, oN, SOR))      // G = . S
            val s1 = state(RP(G, oN, EOR))      // G = S .
            val s2 = state(RP(S, oN, EOR))      // S = 'a' .
            val s3 = state(RP(a, oN, EOR))      // 'a'

            transition(s0, s0, s3, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s3, s2, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, oN, SOR)))
            transition(s0, s2, s1, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, oN, SOR)))
        }

        AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "a"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
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
        val s0 = SM.startState
        val G = s0.runtimeRules.first()
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val d = rrs.findRuntimeRule("'d'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, oN, SOR))      // G = . S
            val s1 = state(RP(a, oN, EOR))      // a
            val s2 = state(RP(S, oN, 1))   // S = a . b c d
            val s3 = state(RP(b, oN, EOR))      // b
            val s4 = state(RP(S, oN, 2))   // S = a b . c d
            val s5 = state(RP(c, oN, EOR))      // c
            val s6 = state(RP(S, oN, 3))   // S = a b c . d
            val s7 = state(RP(d, oN, EOR))      // d
            val s8 = state(RP(S, oN, EOR))      // S = a b c d .
            val s9 = state(RP(G, oN, EOR))      // G = S .

            transition(s0, s0, s1, WIDTH, setOf(b), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(setOf(EOT)), setOf(RP(S, oN, SOR)))
            transition(s0, s2, s3, WIDTH, setOf(c), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(c), setOf(setOf(EOT)), setOf(RP(S, oN, 1)))
            transition(s0, s4, s5, WIDTH, setOf(d), setOf(), null)
            transition(s4, s5, s6, GRAFT, setOf(d), setOf(setOf(EOT)), setOf(RP(S, oN, 2)))
            transition(s0, s6, s7, WIDTH, setOf(EOT), setOf(), null)
            transition(s6, s7, s8, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, oN, 3)))
            transition(s0, s8, s9, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, oN, SOR)))
        }

        AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "abcd"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
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
        val s0 = SM.startState
        val G = s0.runtimeRules.first()
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val d = rrs.findRuntimeRule("'d'")

        val actual = SM.build()

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, oN, SOR))      // G = . S
            val s1 = state(RP(a, oN, ER))      // a
            val s2 = state(RP(b, oN, ER))      // b
            val s3 = state(RP(c, oN, ER))      // c
            val s4 = state(RP(d, oN, ER))      // d
            val s5 = state(RP(S, o0, ER))      // S = a .
            val s6 = state(RP(S, o1, ER))      // S = b .
            val s7 = state(RP(S, o2, ER))      // S = c .
            val s8 = state(RP(S, o3, ER))      // S = d .
            val s9 = state(RP(G, oN, ER))      // G = S .

            transition(s0, s0, s1, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s0, s2, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s0, s3, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s0, s4, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s1, s5, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, o0, SOR)))
            transition(s0, s2, s6, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, o1, SOR)))
            transition(s0, s3, s7, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, o2, SOR)))
            transition(s0, s4, s8, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, o3, SOR)))
            transition(s0, s5, s9, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, oN, SOR)))
            transition(s0, s6, s9, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, oN, SOR)))
            transition(s0, s7, s9, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, oN, SOR)))
            transition(s0, s8, s9, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, oN, SOR)))
        }

        AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "a",
            "b",
            "c",
            "d"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
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
        val s0 = SM.startState
        val G = s0.runtimeRules.first()
        val A = rrs.findRuntimeRule("A")
        val B = rrs.findRuntimeRule("B")
        val C = rrs.findRuntimeRule("C")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")

        val actual = SM.build()

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, oN, SOR))      // G = . S
            val s1 = state(RP(a, oN, ER))      // a
            val s2 = state(RP(A, oN, ER))      // A = a .
            val s3 = state(RP(S, oN, 1))   // S = A . B C
            val s4 = state(RP(b, oN, ER))      // b
            val s5 = state(RP(B, oN, ER))      // B = b .
            val s6 = state(RP(S, oN, 2))   // S = A B . C
            val s7 = state(RP(c, oN, ER))      // c
            val s8 = state(RP(C, oN, ER))      // C = c .
            val s9 = state(RP(S, oN, ER))      // S = A B C .
            val s10 = state(RP(G, oN, ER))      // G = S .

            transition(s0, s0, s1, WIDTH, setOf(b), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(setOf(b)), setOf(RP(A, oN, SOR)))
            transition(s0, s2, s3, HEIGHT, setOf(b), setOf(setOf(EOT)), setOf(RP(S, oN, SOR)))
            transition(s0, s3, s4, WIDTH, setOf(c), setOf(), null)
            transition(s3, s4, s5, HEIGHT, setOf(c), setOf(setOf(c)), setOf(RP(B, oN, SOR)))
            transition(s3, s5, s6, GRAFT, setOf(c), setOf(setOf(EOT)), setOf(RP(S, oN, 1)))
            transition(s0, s6, s7, WIDTH, setOf(EOT), setOf(), null)
            transition(s6, s7, s8, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(C, oN, SOR)))
            transition(s6, s8, s9, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, oN, 2)))
            transition(s0, s9, s10, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, oN, SOR)))
        }

        AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "abc"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
        }
    }

    @Test
    fun empty() {
        val rrs = runtimeRuleSet {
            concatenation("S") { empty() }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()
        val eS = EMPTY

        val actual = SM.build()

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, oN, SOR))      // G = . S
            val s1 = state(RP(eS, oN, ER))      // eS
            val s2 = state(RP(S, oN, ER))      // S = eS .
            val s3 = state(RP(G, oN, ER))      // G = S .

            transition(s0, s0, s1, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, oN, SOR)))
            transition(s0, s2, s3, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, oN, SOR)))
        }

        AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            ""
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
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
        val s0 = SM.startState
        val G = s0.runtimeRules.first()
        val oA = rrs.findRuntimeRule("oA")
        val oB = rrs.findRuntimeRule("oB")
        val oC = rrs.findRuntimeRule("oC")
        val eoA = EMPTY
        val eoB = EMPTY
        val eoC = EMPTY
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, oN, SOR))      // G = . S
            val s1 = state(RP(a, oN, ER))      // a
            val s2 = state(RP(eoA, oN, ER))    // empty-oA
            val s3 = state(RP(oA, o0, ER))     // oA = a .
            val s4 = state(RP(oA, o1, ER))     // oA =  .
            val s5 = state(RP(S, oN, 1))   // S = oA . oB oC
            val s6 = state(RP(b, oN, ER))      // b
            val s7 = state(RP(eoB, oN, ER))    // empty-oB
            val s8 = state(RP(oB, o0, ER))     // oB = b .
            val s9 = state(RP(oB, o1, ER))     // oB = .
            val s10 = state(RP(S, oN, 2))   // S = oA oB . oC
            val s11 = state(RP(c, oN, ER))      // c
            val s12 = state(RP(eoC, oN, ER))    // empty-oC
            val s13 = state(RP(oC, o0, ER))     // oC = c .
            val s14 = state(RP(oC, o1, ER))     // oC = .
            val s15 = state(RP(S, oN, ER))      // S = oA oB oC .
            val s16 = state(RP(G, oN, ER))      // G = S .

            transition(s0, s0, s1, WIDTH, setOf(b, c, EOT), setOf(), null)
            transition(s0, s0, s2, WIDTH, setOf(b, c, EOT), setOf(), null)
            transition(s0, s1, s3, HEIGHT, setOf(b, c, EOT), setOf(setOf(b, c, EOT)), setOf(RP(oA, o0, SOR)))
            transition(s0, s2, s4, HEIGHT, setOf(b, c, EOT), setOf(setOf(b, c, EOT)), setOf(RP(oA, o1, SOR)))
            transition(s0, s3, s5, HEIGHT, setOf(b, c, EOT), setOf(setOf(EOT)), setOf(RP(S, oN, 0)))
            transition(s0, s4, s5, HEIGHT, setOf(b, c, EOT), setOf(setOf(EOT)), setOf(RP(S, oN, 0)))
            transition(s0, s5, s6, WIDTH, setOf(c, EOT), setOf(), null)
            transition(s0, s5, s7, WIDTH, setOf(c, EOT), setOf(), null)
            transition(s5, s6, s8, HEIGHT, setOf(c, EOT), setOf(setOf(EOT, c)), setOf(RP(oB, o0, SOR)))
            transition(s5, s7, s9, HEIGHT, setOf(c, EOT), setOf(setOf(EOT, c)), setOf(RP(oB, o1, SOR)))
            transition(s5, s8, s10, GRAFT, setOf(c, EOT), setOf(setOf(EOT)), setOf(RP(S, oN, 1)))
            transition(s5, s9, s10, GRAFT, setOf(c, EOT), setOf(setOf(EOT)), setOf(RP(S, oN, 1)))
            transition(s0, s10, s11, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s10, s12, WIDTH, setOf(EOT), setOf(), null)
            transition(s10, s11, s13, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(oC, o0, SOR)))
            transition(s10, s12, s14, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(oC, o1, SOR)))
            transition(s10, s13, s15, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, oN, 2)))
            transition(s10, s14, s15, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, oN, 2)))
            transition(s0, s15, s16, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, oN, 0)))
        }

        AutomatonTest.assertEquals(expected, actual)

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
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
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
        val s0 = SM.startState
        val G = s0.runtimeRules.first()

        //val actual = SM.build()
        //println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, oN, SOR))      // G = . S


        }

        //AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "abc",
            "abc"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
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
            val s0 = state(RP(G, oN, SOR))      // G = . S


        }

        //AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "abc",
            "abd"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
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
            val s0 = state(RP(G, oN, SOR))      // G = . S


        }

        //AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "abc",
            "xabd"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
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
            val s0 = state(RP(G, oN, SOR))      // G = . S


        }

        //AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "aaaa"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
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
            val s0 = state(RP(G, oN, SOR))      // G = . S


        }

        //AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "a",
            "aa",
            "aaa",
            "aaaa",
            "aaaaa",
            "aaaaaa",
            "aaaaaaa"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
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
            val s0 = state(RP(G, oN, SOR))      // G = . S


        }

        //AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "a",
            "aa",
            "aaa",
            "aaaa",
            "aaaaa",
            "aaaaaa",
            "aaaaaaa"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
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
        val S = rrs.findRuntimeRule("E")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, oN, SOR))      // G = . S


        }

        //AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "id",
            "(id)",
            "..."
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
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
            val s0 = state(RP(G, oN, SOR))      // G = . S


        }

        //AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "da",
            "bdc",
            "dc",
            "bda"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
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
            val s0 = state(RP(G, oN, SOR))      // G = . S


        }

        //AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "a",
            "aa",
            "aaa",
            "aaaa"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
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

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, oN, SOR))      // G = . S
            val s1 = state(RP(a, oN, ER))      // a
            val s2 = state(RP(S, oN, ER))      // S = a .
            val s3 = state(RP(G, oN, SOR))      // G = . S


        }

        //AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "a",
            "aa",
            "aaa",
            "aaaa"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
        }
        println(rrs.usedAutomatonToString("S"))
    }
}