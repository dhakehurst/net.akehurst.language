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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.ruleSet
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
        val rG = SM.startState.runtimeRules.first()
        val a = rrs.findRuntimeRule("'a'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(rG, oN, SOR)      // G = . S
            val s1 = state(rG, oN, EOR)      // G = S .
            val s2 = state(S, oN, EOR)      // S = 'a' .
            val s3 = state(a, oN, EOR)      // 'a'

            trans(WIDTH) { src(rG, oN, SOR); tgt(a); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(S); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
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
        val rG = s0.runtimeRules.first()
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val d = rrs.findRuntimeRule("'d'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(rG, oN, SOR)      // G = . S
            val s1 = state(a, oN, EOR)      // a
            val s2 = state(S, oN, 1)   // S = a . b c d
            val s3 = state(b, oN, EOR)      // b
            val s4 = state(S, oN, 2)   // S = a b . c d
            val s5 = state(c, oN, EOR)      // c
            val s6 = state(S, oN, 3)   // S = a b c . d
            val s7 = state(d, oN, EOR)      // d
            val s8 = state(S, oN, EOR)      // S = a b c d .
            val s9 = state(rG, oN, EOR)      // G = S .

            trans(WIDTH) { src(rG, oN, SOR); tgt(a); lhg(setOf(b)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(S, oN, 1); lhg(setOf(b), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, 1); tgt(b); lhg(setOf(c)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(b); tgt(S, oN, 2); lhg(setOf(c)); ctx(S, oN, 1) }
            trans(WIDTH) { src(S, oN, 2); tgt(c); lhg(setOf(d)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(c); tgt(S, oN, 3); lhg(setOf(d)); ctx(S, oN, 2) }
            trans(WIDTH) { src(S, oN, 3); tgt(d); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(d); tgt(S); lhg(setOf(EOT)); ctx(S, oN, 3) }
            trans(GOAL) { src(S); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
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
        val rG = s0.runtimeRules.first()
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val d = rrs.findRuntimeRule("'d'")

        val actual = SM.build()

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(rG, oN, SOR)      // G = . S
            val s1 = state(a, oN, ER)      // a
            val s2 = state(b, oN, ER)      // b
            val s3 = state(c, oN, ER)      // c
            val s4 = state(d, oN, ER)      // d
            val s5 = state(S, o0, ER)      // S = a .
            val s6 = state(S, o1, ER)      // S = b .
            val s7 = state(S, o2, ER)      // S = c .
            val s8 = state(S, o3, ER)      // S = d .
            val s9 = state(rG, oN, ER)      // G = S .

            trans(WIDTH) { src(rG, oN, SOR); tgt(a); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(b); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(c); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(d); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(S, o0, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(b); tgt(S, o1, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(c); tgt(S, o2, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(d); tgt(S, o3, ER); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o0, ER); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o1, ER); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o2, ER); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o3, ER); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
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
        val rG = s0.runtimeRules.first()
        val A = rrs.findRuntimeRule("A")
        val B = rrs.findRuntimeRule("B")
        val C = rrs.findRuntimeRule("C")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")

        val actual = SM.build()

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(rG, oN, SOR)      // G = . S
            val s1 = state(a, oN, ER)      // a
            val s2 = state(A, oN, ER)      // A = a .
            val s3 = state(S, oN, 1)   // S = A . B C
            val s4 = state(b, oN, ER)      // b
            val s5 = state(B, oN, ER)      // B = b .
            val s6 = state(S, oN, 2)   // S = A B . C
            val s7 = state(c, oN, ER)      // c
            val s8 = state(C, oN, ER)      // C = c .
            val s9 = state(S, oN, ER)      // S = A B C .
            val s10 = state(rG, oN, ER)      // G = S .

            trans(WIDTH) { src(rG, oN, SOR); tgt(a); lhg(setOf(b)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(A); lhg(setOf(b), setOf(b)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(A); tgt(S, oN, 1); lhg(setOf(b), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, 1); tgt(b); lhg(setOf(c)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(b); tgt(B); lhg(setOf(c), setOf(c)); ctx(S, oN, 1) }
            trans(GRAFT) { src(B); tgt(S, oN, 2); lhg(setOf(c)); ctx(S, oN, 1) }
            trans(WIDTH) { src(S, oN, 2); tgt(c); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(c); tgt(C); lhg(setOf(EOT), setOf(EOT)); ctx(S, oN, 2) }
            trans(GRAFT) { src(C); tgt(S); lhg(setOf(EOT)); ctx(S, oN, 2) }
            trans(GOAL) { src(S); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
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
        val rG = s0.runtimeRules.first()
        val eS = EMPTY

        val actual = SM.build()

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(rG, oN, SOR)      // G = . S
            val s1 = state(eS, oN, ER)      // eS
            val s2 = state(S, oN, ER)      // S = eS .
            val s3 = state(rG, oN, ER)      // G = S .

            trans(WIDTH) { src(rG, oN, SOR); tgt(eS); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(eS); tgt(S); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
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
        val rG = s0.runtimeRules.first()
        val oA = rrs.findRuntimeRule("oA")
        val oB = rrs.findRuntimeRule("oB")
        val oC = rrs.findRuntimeRule("oC")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(rG, oN, SOR)      // G = . S
            state(rG, oN, ER)               // G = S .
            state(S, oN, 1)                // S = oA . oB oC
            state(S, oN, 2)                // S = oA oB . oC
            state(S, oN, ER)               // S = oA oB oC .
            state(oA, o0, ER)              // oA = a .
            state(oB, o0, ER)              // oB = b .
            state(oC, o0, ER)              // oC = c .
            state(a, oN, ER)               // a
            state(b, oN, ER)               // b
            state(c, oN, ER)               // c

            trans(WIDTH) { src(rG, oN, SOR); tgt(a); lhg(setOf(b)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, 1); tgt(b); lhg(setOf(c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, 2); tgt(c); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(oA, o0, ER); tgt(S, oN, 1); lhg(setOf(b), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(oB, o0, ER); tgt(S, oN, 2); lhg(setOf(c)); ctx(S, oN, 1) }
            trans(GRAFT) { src(oC, o0, ER); tgt(S); lhg(setOf(EOT)); ctx(S, oN, 2) }
            trans(HEIGHT) { src(a); tgt(oA, o0, ER); lhg(setOf(b), setOf(b)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(b); tgt(oB, o0, ER); lhg(setOf(c), setOf(c)); ctx(S, oN, 1) }
            trans(HEIGHT) { src(c); tgt(oC, o0, ER); lhg(setOf(EOT), setOf(EOT)); ctx(S, oN, 2) }
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
        val rG = s0.runtimeRules.first()

        //val actual = SM.build()
        //println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(rG, oN, SOR)      // G = . S


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
        val rG = s0.runtimeRules.first()
        val ABC = rrs.findRuntimeRule("ABC")
        val ABD = rrs.findRuntimeRule("ABD")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val d = rrs.findRuntimeRule("'d'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            state(rG, oN, SOR)
            state(rG, oN, EOR)
            state(S, o0, EOR)
            state(S, o1, EOR)
            state(ABC, oN, 1)
            state(ABC, oN, 2)
            state(ABC, oN, EOR)
            state(ABD, oN, 1)
            state(ABD, oN, 2)
            state(ABD, oN, EOR)
            state(a, oN, EOR)
            state(b, oN, EOR)
            state(c, oN, EOR)
            state(d, oN, EOR)

            trans(WIDTH) { src(rG, oN, SOR); tgt(a); lhg(setOf(b)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(ABC, oN, 1); tgt(b); lhg(setOf(c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(ABD, oN, 1); tgt(b); lhg(setOf(d)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(ABC, oN, 2); tgt(c); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(ABD, oN, 2); tgt(d); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o0, EOR); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o1, EOR); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(ABC, oN, 1); lhg(setOf(b), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(b); tgt(ABC, oN, 2); lhg(setOf(c)); ctx(ABC, oN, 1) }
            trans(GRAFT) { src(c); tgt(ABC, oN, EOR); lhg(setOf(EOT)); ctx(ABC, oN, 2) }
            trans(HEIGHT) { src(a); tgt(ABD, oN, 1); lhg(setOf(b), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(b); tgt(ABD, oN, 2); lhg(setOf(d)); ctx(ABD, oN, 1) }
            trans(GRAFT) { src(d); tgt(ABD, oN, EOR); lhg(setOf(EOT)); ctx(ABD, oN, 2) }
            trans(HEIGHT) { src(ABC, oN, EOR); tgt(S, o0, EOR); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(ABD, oN, EOR); tgt(S, o1, EOR); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
        }

        AutomatonTest.assertEquals(expected, actual)

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
        val rG = s0.runtimeRules.first()

        val ABC = rrs.findRuntimeRule("ABC")
        val XABD = rrs.findRuntimeRule("XABD")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val d = rrs.findRuntimeRule("'d'")
        val x = rrs.findRuntimeRule("'x'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            state(rG, oN, SOR)
            state(rG, oN, EOR)
            state(S, o0, EOR)
            state(S, o1, EOR)
            state(ABC, oN, 1)
            state(ABC, oN, 2)
            state(ABC, oN, EOR)
            state(XABD, oN, 1)
            state(XABD, oN, 2)
            state(XABD, oN, 3)
            state(XABD, oN, EOR)
            state(a, oN, EOR)
            state(b, oN, EOR)
            state(c, oN, EOR)
            state(d, oN, EOR)
            state(x, oN, EOR)

            trans(WIDTH) { src(rG, oN, SOR); tgt(a); lhg(setOf(b)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(XABD, oN, 1); tgt(a); lhg(setOf(b)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(ABC, oN, 1); tgt(b); lhg(setOf(c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(XABD, oN, 2); tgt(b); lhg(setOf(d)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(ABC, oN, 2); tgt(c); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(XABD, oN, 3); tgt(d); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(x); lhg(setOf(a)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o0, EOR); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o1, EOR); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(ABC, oN, 1); lhg(setOf(b), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(b); tgt(ABC, oN, 2); lhg(setOf(c)); ctx(ABC, oN, 1) }
            trans(GRAFT) { src(c); tgt(ABC, oN, EOR); lhg(setOf(EOT)); ctx(ABC, oN, 2) }
            trans(HEIGHT) { src(ABC, oN, EOR); tgt(S, o0, EOR); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(XABD, oN, EOR); tgt(S, o1, EOR); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(x); tgt(XABD, oN, 1); lhg(setOf(a), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(a); tgt(XABD, oN, 2); lhg(setOf(b)); ctx(XABD, oN, 1) }
            trans(GRAFT) { src(b); tgt(XABD, oN, 3); lhg(setOf(d)); ctx(XABD, oN, 2) }
            trans(GRAFT) { src(d); tgt(XABD, oN, EOR); lhg(setOf(EOT)); ctx(XABD, oN, 3) }
        }

        AutomatonTest.assertEquals(expected, actual)

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
        val rG = s0.runtimeRules.first()

        val AAAA = rrs.findRuntimeRule("AAAA")
        val AAAAm = rrs.findRuntimeRule("AAAAm")
        val a = rrs.findRuntimeRule("'a'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            state(rG, oN, SOR)
            state(rG, oN, EOR)
            state(S, o0, EOR)
            state(S, o1, EOR)
            state(AAAA, oN, 1)
            state(AAAA, oN, 2)
            state(AAAA, oN, 3)
            state(AAAA, oN, EOR)
            state(AAAAm, oLI, PMI)
            state(AAAAm, oLI, EOR)
            state(a, oN, EOR)

            trans(WIDTH) { src(rG, oN, SOR); tgt(a); lhg(setOf(a)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(AAAA, oN, 1); tgt(a); lhg(setOf(a)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(AAAA, oN, 2); tgt(a); lhg(setOf(a)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(AAAA, oN, 3); tgt(a); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(AAAAm, oLI, PMI); tgt(a); lhg(setOf(EOT, a)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o0, EOR); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o1, EOR); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(AAAA, oN, 1); lhg(setOf(a), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(a); tgt(AAAA, oN, 2); lhg(setOf(a)); ctx(AAAA, oN, 1) }
            trans(GRAFT) { src(a); tgt(AAAA, oN, 3); lhg(setOf(a)); ctx(AAAA, oN, 2) }
            trans(GRAFT) { src(a); tgt(AAAA, oN, EOR); lhg(setOf(EOT)); ctx(AAAA, oN, 3) }
            trans(GRAFT) { src(a); tgt(AAAAm, oLI, EOR); lhg(setOf(EOT)); ctx(AAAAm, oLI, PMI) }
            trans(HEIGHT) { src(a); tgt(AAAAm, oLI, PMI); lhg(setOf(a), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(a); tgt(AAAAm, oLI, PMI); lhg(setOf(a)); ctx(AAAAm, oLI, PMI) }
            trans(HEIGHT) { src(AAAA, oN, EOR); tgt(S, o0, EOR); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(AAAAm, oLI, EOR); tgt(S, o1, EOR); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
        }

        AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "aaaa"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
        }
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
        val rG = s0.runtimeRules.first()

        val AAAA = rrs.findRuntimeRule("AAAA")
        val AAAAm = rrs.findRuntimeRule("AAAAm")
        val a = rrs.findRuntimeRule("'a'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            state(rG, oN, SOR)
            state(rG, oN, EOR)
            state(S, o0, EOR)
            state(S, o1, EOR)
            state(AAAA, oN, 1)
            state(AAAA, oN, 2)
            state(AAAA, oN, 3)
            state(AAAA, oN, EOR)
            state(AAAAm, oLE, EOR)
            state(AAAAm, oLI, PMI)
            state(AAAAm, oLI, EOR)
            state(EMPTY_LIST, oN, EOR)
            state(a, oN, EOR)

            trans(WIDTH) { src(rG, oN, SOR); tgt(EMPTY_LIST); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(a); lhg(setOf(EOT, a)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(AAAA, oN, 1); tgt(a); lhg(setOf(a)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(AAAA, oN, 2); tgt(a); lhg(setOf(a)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(AAAA, oN, 3); tgt(a); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(AAAAm, oLI, PMI); tgt(a); lhg(setOf(EOT, a)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o0, EOR); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o1, EOR); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(AAAA, oN, 1); lhg(setOf(a), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(a); tgt(AAAA, oN, 2); lhg(setOf(a)); ctx(AAAA, oN, 1) }
            trans(GRAFT) { src(a); tgt(AAAA, oN, 3); lhg(setOf(a)); ctx(AAAA, oN, 2) }
            trans(GRAFT) { src(a); tgt(AAAA, oN, EOR); lhg(setOf(EOT)); ctx(AAAA, oN, 3) }
            trans(HEIGHT) { src(EMPTY_LIST); tgt(AAAAm, oLE, EOR); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(AAAAm, oLI, EOR); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(a); tgt(AAAAm, oLI, EOR); lhg(setOf(EOT)); ctx(AAAAm, oLI, PMI) }
            trans(HEIGHT) { src(a); tgt(AAAAm, oLI, PMI); lhg(setOf(a), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(a); tgt(AAAAm, oLI, PMI); lhg(setOf(a)); ctx(AAAAm, oLI, PMI) }
            trans(HEIGHT) { src(AAAA, oN, EOR); tgt(S, o0, EOR); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(AAAAm, oLE, EOR); tgt(S, o1, EOR); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(AAAAm, oLI, EOR); tgt(S, o1, EOR); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
        }

        AutomatonTest.assertEquals(expected, actual)

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
        val rG = s0.runtimeRules.first()

        val AAAAm1 = rrs.findRuntimeRule("AAAAm1")
        val AAAAm2 = rrs.findRuntimeRule("AAAAm2")
        val a = rrs.findRuntimeRule("'a'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            state(rG, oN, SOR)
            state(rG, oN, EOR)
            state(S, o0, EOR)
            state(S, o1, EOR)
            state(AAAAm1, oLI, PMI)
            state(AAAAm1, oLI, EOR)
            state(AAAAm2, oLI, PMI)
            state(AAAAm2, oLI, EOR)
            state(a, oN, EOR)

            trans(WIDTH) { src(rG, oN, SOR); tgt(a); lhg(setOf(EOT, a)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(AAAAm1, oLI, PMI); tgt(a); lhg(setOf(EOT, a)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(AAAAm2, oLI, PMI); tgt(a); lhg(setOf(EOT, a)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o0, EOR); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o1, EOR); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(AAAAm1, oLI, EOR); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(a); tgt(AAAAm1, oLI, EOR); lhg(setOf(EOT)); ctx(AAAAm1, oLI, PMI) }
            trans(HEIGHT) { src(a); tgt(AAAAm1, oLI, PMI); lhg(setOf(a), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(a); tgt(AAAAm1, oLI, PMI); lhg(setOf(a)); ctx(AAAAm1, oLI, PMI) }
            trans(GRAFT) { src(a); tgt(AAAAm2, oLI, EOR); lhg(setOf(EOT)); ctx(AAAAm2, oLI, PMI) }
            trans(HEIGHT) { src(a); tgt(AAAAm2, oLI, PMI); lhg(setOf(a), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(a); tgt(AAAAm2, oLI, PMI); lhg(setOf(a)); ctx(AAAAm2, oLI, PMI) }
            trans(HEIGHT) { src(AAAAm1, oLI, EOR); tgt(S, o0, EOR); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(AAAAm2, oLI, EOR); tgt(S, o1, EOR); lhg(setOf(EOT), setOf(EOT)); ctx(rG, oN, SOR) }
        }

        AutomatonTest.assertEquals(expected, actual)

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
    }

    @Test
    fun AhoSetiUlman_Grm_4_1() {
        val rrs = ruleSet("Test") {
            choiceLongest("E") {
                ref("E1")
                ref("E2")
            }
            concatenation("E1") { ref("E"); literal("+"); ref("T") }
            concatenation("E2") { ref("T"); }
            choiceLongest("T") {
                ref("T1")
                ref("T2")
            }
            concatenation("T1") { ref("T"); literal("*"); ref("F") }
            concatenation("T2") { ref("F") }
            choiceLongest("F") {
                ref("F1")
                ref("F2")
            }
            concatenation("F1") { literal("("); literal("id"); literal(")") }
            concatenation("F2") { literal("id"); }
        } as RuntimeRuleSet

        val E = rrs.rule[0]  // E
        val _t1 = rrs.rule[1]  // '+'
        val E1 = rrs.rule[2]  // E1
        val E2 = rrs.rule[3]  // E2
        val T = rrs.rule[4]  // T
        val _t5 = rrs.rule[5]  // '*'
        val T1 = rrs.rule[6]  // T1
        val T2 = rrs.rule[7]  // T2
        val F = rrs.rule[8]  // F
        val _t9 = rrs.rule[9]  // '('
        val _t10 = rrs.rule[10]  // 'id'
        val _t11 = rrs.rule[11]  // ')'
        val F1 = rrs.rule[12]  // F1
        val F2 = rrs.rule[13]  // F2
        val rG = rrs.goalRuleFor[E]

        val actual = rrs.fetchStateSetFor(E, automatonKind).build()
        println(rrs.usedAutomatonToString("E"))

        val expected = automaton(rrs, automatonKind, "E", false) {
            state(rG, oN, SR)   // <GOAL> =  . E
            state(rG, oN, ER)   // <GOAL> = E .
            state(E, o0, ER)   // E = E1 .
            state(E, o1, ER)   // E = E2 .
            state(E1, oN, 1)   // E1 = E . '+' T
            state(E1, oN, 2)   // E1 = E '+' . T
            state(E1, oN, ER)   // E1 = E '+' T .
            state(T, o0, ER)   // T = T1 .
            state(T, o1, ER)   // T = T2 .
            state(T1, oN, 1)   // T1 = T . '*' F
            state(T1, oN, 2)   // T1 = T '*' . F
            state(T1, oN, ER)   // T1 = T '*' F .
            state(F, o0, ER)   // F = F1 .
            state(F, o1, ER)   // F = F2 .
            state(F1, oN, 1)   // F1 = '(' . 'id' ')'
            state(F1, oN, 2)   // F1 = '(' 'id' . ')'
            state(F1, oN, ER)   // F1 = '(' 'id' ')' .
            state(F2, oN, ER)   // F2 = 'id' .
            state(T2, oN, ER)   // T2 = F .
            state(E2, oN, ER)   // E2 = T .
            state(_t1, oN, ER)   // '+'
            state(_t5, oN, ER)   // '*'
            state(_t9, oN, ER)   // '('
            state(_t10, oN, ER)   // 'id'
            state(_t11, oN, ER)   // ')'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t9, oN, ER); lhg(_t10); ctx(rG, oN, SR) }
            trans(WIDTH) { src(rG, oN, SR); tgt(_t10, oN, ER); lhg(setOf(EOT, _t5, _t1)); ctx(rG, oN, SR) }
            trans(HEIGHT) {
                src(E, o0, ER); tgt(E1, oN, 1); lhg(setOf(_t1), setOf(EOT, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(GOAL) {
                src(E, o0, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(HEIGHT) {
                src(E, o1, ER); tgt(E1, oN, 1); lhg(setOf(_t1), setOf(EOT, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(GOAL) {
                src(E, o1, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(WIDTH) { src(E1, oN, 1); tgt(_t1, oN, ER); lhg(setOf(_t9, _t10)); ctx(rG, oN, SR) }
            trans(WIDTH) { src(E1, oN, 2); tgt(_t9, oN, ER); lhg(_t10); ctx(rG, oN, SR) }
            trans(WIDTH) { src(E1, oN, 2); tgt(_t10, oN, ER); lhg(setOf(EOT, _t5, _t1)); ctx(rG, oN, SR) }
            trans(HEIGHT) {
                src(E1, oN, ER); tgt(E, o0, ER); lhg(setOf(EOT, _t1), setOf(EOT, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(HEIGHT) {
                src(T, o0, ER); tgt(E2, oN, ER); lhg(setOf(EOT, _t1), setOf(EOT, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(HEIGHT) {
                src(T, o0, ER); tgt(T1, oN, 1); lhg(setOf(_t5), setOf(EOT, _t5, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(E1, oN, 2)))
            }
            trans(GRAFT) {
                src(T, o0, ER); tgt(E1, oN, ER); lhg(setOf(EOT, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(E1, oN, 2)))
            }
            trans(HEIGHT) {
                src(T, o1, ER); tgt(E2, oN, ER); lhg(setOf(EOT, _t1), setOf(EOT, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(HEIGHT) {
                src(T, o1, ER); tgt(T1, oN, 1); lhg(setOf(_t5), setOf(EOT, _t5, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(E1, oN, 2)))
            }
            trans(GRAFT) {
                src(T, o1, ER); tgt(E1, oN, ER); lhg(setOf(EOT, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(E1, oN, 2)))
            }
            trans(WIDTH) { src(T1, oN, 1); tgt(_t5, oN, ER); lhg(setOf(_t9, _t10)); ctx(RP(E1, oN, 2), RP(rG, oN, SR)) }
            trans(WIDTH) { src(T1, oN, 2); tgt(_t9, oN, ER); lhg(_t10); ctx(RP(E1, oN, 2), RP(rG, oN, SR)) }
            trans(WIDTH) { src(T1, oN, 2); tgt(_t10, oN, ER); lhg(setOf(EOT, _t5, _t1)); ctx(RP(E1, oN, 2), RP(rG, oN, SR)) }
            trans(HEIGHT) {
                src(T1, oN, ER); tgt(T, o0, ER); lhg(setOf(EOT, _t5, _t1), setOf(EOT, _t5, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(E1, oN, 2)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(HEIGHT) {
                src(F, o0, ER); tgt(T2, oN, ER); lhg(setOf(EOT, _t5, _t1), setOf(EOT, _t5, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(E1, oN, 2)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(GRAFT) {
                src(F, o0, ER); tgt(T1, oN, ER); lhg(setOf(EOT, _t5, _t1));
                prevPair(setOf(RP(E1, oN, 2)), setOf(RP(T1, oN, 2)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(T1, oN, 2)))
            }
            trans(HEIGHT) {
                src(F, o1, ER); tgt(T2, oN, ER); lhg(setOf(EOT, _t5, _t1), setOf(EOT, _t5, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(E1, oN, 2)))
            }
            trans(GRAFT) {
                src(F, o1, ER); tgt(T1, oN, ER); lhg(setOf(EOT, _t5, _t1));
                prevPair(setOf(RP(E1, oN, 2)), setOf(RP(T1, oN, 2)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(T1, oN, 2)))
            }
            trans(WIDTH) { src(F1, oN, 1); tgt(_t10, oN, ER); lhg(_t11); ctx(RP(T1, oN, 2), RP(E1, oN, 2), RP(rG, oN, SR)) }
            trans(WIDTH) { src(F1, oN, 2); tgt(_t11, oN, ER); lhg(setOf(EOT, _t5, _t1)); ctx(RP(rG, oN, SR), RP(T1, oN, 2), RP(E1, oN, 2)) }
            trans(HEIGHT) {
                src(F1, oN, ER); tgt(F, o0, ER); lhg(setOf(EOT, _t5, _t1), setOf(EOT, _t5, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(E1, oN, 2)))
                prevPair(setOf(RP(E1, oN, 2)), setOf(RP(T1, oN, 2)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(T1, oN, 2)))
            }
            trans(HEIGHT) {
                src(F2, oN, ER); tgt(F, o1, ER); lhg(setOf(EOT, _t5, _t1), setOf(EOT, _t5, _t1));
                prevPair(setOf(RP(E1, oN, 2)), setOf(RP(T1, oN, 2)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(T1, oN, 2)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(E1, oN, 2)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(HEIGHT) {
                src(T2, oN, ER); tgt(T, o1, ER); lhg(setOf(EOT, _t5, _t1), setOf(EOT, _t5, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(E1, oN, 2)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(HEIGHT) {
                src(E2, oN, ER); tgt(E, o1, ER); lhg(setOf(EOT, _t1), setOf(EOT, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(GRAFT) {
                src(_t1, oN, ER); tgt(E1, oN, 2); lhg(setOf(_t9, _t10));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(E1, oN, 1)))
            }
            trans(GRAFT) {
                src(_t5, oN, ER); tgt(T1, oN, 2); lhg(setOf(_t9, _t10));
                prevPair(setOf(RP(E1, oN, 2)), setOf(RP(T1, oN, 1)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(T1, oN, 1)))
            }
            trans(HEIGHT) {
                src(_t9, oN, ER); tgt(F1, oN, 1); lhg(setOf(_t10), setOf(EOT, _t5, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(E1, oN, 2)))
                prevPair(setOf(RP(E1, oN, 2)), setOf(RP(T1, oN, 2)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(T1, oN, 2)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(HEIGHT) {
                src(_t10, oN, ER); tgt(F2, oN, ER); lhg(setOf(EOT, _t5, _t1), setOf(EOT, _t5, _t1));
                prevPair(setOf(RP(E1, oN, 2)), setOf(RP(T1, oN, 2)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(T1, oN, 2)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(E1, oN, 2)))
            }
            trans(GRAFT) {
                src(_t10, oN, ER); tgt(F1, oN, 2); lhg(_t11);
                prevPair(setOf(RP(T1, oN, 2)), setOf(RP(F1, oN, 1)))
                prevPair(setOf(RP(E1, oN, 2)), setOf(RP(F1, oN, 1)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(F1, oN, 1)))
            }
            trans(GRAFT) {
                src(_t11, oN, ER); tgt(F1, oN, ER); lhg(setOf(EOT, _t5, _t1));
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(F1, oN, 2)))
                prevPair(setOf(RP(T1, oN, 2)), setOf(RP(F1, oN, 2)))
                prevPair(setOf(RP(E1, oN, 2)), setOf(RP(F1, oN, 2)))
            }
        }

        AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "id",
            "(id)",
            "..."
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("E", sentence)
        }
        println(rrs.usedAutomatonToString("E"))
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
        val rG = s0.runtimeRules.first()

        //val actual = SM.build()
        //println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(rG, oN, SOR)      // G = . S


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
        val rG = s0.runtimeRules.first()

        //val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(rG, oN, SOR)      // G = . S


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
        val rrs = ruleSet("Test") {
            choiceLongest("S") {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { literal("a"); ref("S") }
        } as RuntimeRuleSet

        val _t0 = rrs.rule[0]  // 'a'
        val S = rrs.rule[1]  // S
        val S1 = rrs.rule[2]  // S1
        val rG = rrs.goalRuleFor[S]

        val actual = rrs.fetchStateSetFor(S, automatonKind).build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            state(rG, oN, SR)   // <GOAL> =  . S
            state(rG, oN, ER)   // <GOAL> = S .
            state(S, o0, ER)   // S = 'a' .
            state(S, o1, ER)   // S = S1 .
            state(S1, oN, 1)   // S1 = 'a' . S
            state(S1, oN, ER)   // S1 = 'a' S .
            state(_t0, oN, ER)   // 'a'

            trans(WIDTH) { src(rG, oN, SR); tgt(_t0, oN, ER); lhg(setOf(EOT, _t0)); ctx(rG, oN, SR) }
            trans(GRAFT) {
                src(S, o0, ER); tgt(S1, oN, ER); lhg(EOT);
                prevPair(setOf(RP(S1, oN, 1)), setOf(RP(S1, oN, 1)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(S1, oN, 1)))
            }
            trans(GOAL) {
                src(S, o0, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(GRAFT) {
                src(S, o1, ER); tgt(S1, oN, ER); lhg(EOT);
                prevPair(setOf(RP(S1, oN, 1)), setOf(RP(S1, oN, 1)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(S1, oN, 1)))
            }
            trans(GOAL) {
                src(S, o1, ER); tgt(rG, oN, ER); lhg(EOT);
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(WIDTH) { src(S1, oN, 1); tgt(_t0, oN, ER); lhg(setOf(EOT, _t0)); ctx(RP(S1, oN, 1), RP(rG, oN, SR)) }
            trans(HEIGHT) {
                src(S1, oN, ER); tgt(S, o1, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(setOf(RP(S1, oN, 1)), setOf(RP(S1, oN, 1)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(S1, oN, 1)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(HEIGHT) {
                src(_t0, oN, ER); tgt(S, o0, ER); lhg(setOf(EOT), setOf(EOT));
                prevPair(setOf(RP(S1, oN, 1)), setOf(RP(S1, oN, 1)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(S1, oN, 1)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
            trans(HEIGHT) {
                src(_t0, oN, ER); tgt(S1, oN, 1); lhg(setOf(_t0), setOf(EOT));
                prevPair(setOf(RP(S1, oN, 1)), setOf(RP(S1, oN, 1)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(S1, oN, 1)))
                prevPair(setOf(RP(rG, oN, SR)), setOf(RP(rG, oN, SR)))
            }
        }

        AutomatonTest.assertEquals(expected, actual)

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