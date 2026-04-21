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
            val s0 = state(G, oN, SOR)      // G = . S
            val s1 = state(G, oN, EOR)      // G = S .
            val s2 = state(S, oN, EOR)      // S = 'a' .
            val s3 = state(a, oN, EOR)      // 'a'

            trans(WIDTH) { src(G, oN, SOR); tgt(a); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(S); lhg(setOf(ANY), setOf(ANY)); ctx(G, oN, SOR) }
            trans(GRAFT) { src(S); tgt(G); lhg(setOf(EOT), setOf(ANY)); ctx(G, oN, SOR) }
        }

        AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "a"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
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
        val G = SM.startState.runtimeRules.first()
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val d = rrs.findRuntimeRule("'d'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(G, oN, SOR)      // G = . S
            val s1 = state(a, oN, EOR)      // a
            val s2 = state(S, oN, 1)        // S = a . b c d
            val s3 = state(b, oN, EOR)      // b
            val s4 = state(S, oN, 2)         // S = a b . c d
            val s5 = state(c, oN, EOR)      // c
            val s6 = state(S, oN, 3)        // S = a b c . d
            val s7 = state(d, oN, EOR)      // d
            val s8 = state(S, oN, EOR)      // S = a b c d .
            val s9 = state(G, oN, EOR)      // G = S .

            trans(WIDTH) { src(G, oN, SOR); tgt(a); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(S, oN, 1); lhg(setOf(ANY), setOf(ANY)); ctx(G, oN, SOR) }
            trans(WIDTH) { src(S, oN, 1); tgt(b); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(GRAFT) { src(b); tgt(S, oN, 2); lhg(setOf(ANY), setOf(ANY)); ctx(S, oN, 1) }
            trans(WIDTH) { src(S, oN, 2); tgt(c); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(GRAFT) { src(c); tgt(S, oN, 3); lhg(setOf(ANY), setOf(ANY)); ctx(S, oN, 2) }
            trans(WIDTH) { src(S, oN, 3); tgt(d); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(GRAFT) { src(d); tgt(S); lhg(setOf(ANY), setOf(ANY)); ctx(S, oN, 3) }
            trans(GRAFT) { src(S); tgt(G); lhg(setOf(EOT), setOf(ANY)); ctx(G, oN, SOR) }
        }

        AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "abcd"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
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
        val G = SM.startState.runtimeRules.first()
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val d = rrs.findRuntimeRule("'d'")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(G, oN, SOR)      // G = . S
            val s1 = state(a, oN, ER)      // a
            val s2 = state(b, oN, ER)      // b
            val s3 = state(c, oN, ER)      // c
            val s4 = state(d, oN, ER)      // d
            val s5 = state(S, o0, ER)      // S = a .
            val s6 = state(S, o1, ER)      // S = b .
            val s7 = state(S, o2, ER)      // S = c .
            val s8 = state(S, o3, ER)      // S = d .
            val s9 = state(G, oN, ER)      // G = S .

            trans(WIDTH) { src(G, oN, SOR); tgt(a); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(WIDTH) { src(G, oN, SOR); tgt(b); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(WIDTH) { src(G, oN, SOR); tgt(c); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(WIDTH) { src(G, oN, SOR); tgt(d); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(S, o0, ER); lhg(setOf(ANY), setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(b); tgt(S, o1, ER); lhg(setOf(ANY), setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(c); tgt(S, o2, ER); lhg(setOf(ANY), setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(d); tgt(S, o3, ER); lhg(setOf(ANY), setOf(ANY)); ctx(G, oN, SOR) }
            trans(GRAFT) { src(S, o0, ER); tgt(G); lhg(setOf(EOT), setOf(ANY)); ctx(G, oN, SOR) }
            trans(GRAFT) { src(S, o1, ER); tgt(G); lhg(setOf(EOT), setOf(ANY)); ctx(G, oN, SOR) }
            trans(GRAFT) { src(S, o2, ER); tgt(G); lhg(setOf(EOT), setOf(ANY)); ctx(G, oN, SOR) }
            trans(GRAFT) { src(S, o3, ER); tgt(G); lhg(setOf(EOT), setOf(ANY)); ctx(G, oN, SOR) }

        }

        AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "a",
            "b",
            "c",
            "d"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
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
            val s0 = state(G, oN, SOR)      // G = . S
            val s1 = state(a, oN, ER)      // a
            val s2 = state(A, oN, ER)      // A = a .
            val s3 = state(S, oN, 1)   // S = A . B C
            val s4 = state(b, oN, ER)      // b
            val s5 = state(B, oN, ER)      // B = b .
            val s6 = state(S, oN, 2)   // S = A B . C
            val s7 = state(c, oN, ER)      // c
            val s8 = state(C, oN, ER)      // C = c .
            val s9 = state(S, oN, ER)      // S = A B C .
            val s10 = state(G, oN, ER)      // G = S .

            trans(WIDTH) { src(G, oN, SOR); tgt(a); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(A); lhg(setOf(ANY), setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(A); tgt(S, oN, 1); lhg(setOf(ANY), setOf(ANY)); ctx(G, oN, SOR) }
            trans(WIDTH) { src(S, oN, 1); tgt(b); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(b); tgt(B); lhg(setOf(ANY), setOf(ANY)); ctx(S, oN, 1) }
            trans(GRAFT) { src(B); tgt(S, oN, 2); lhg(setOf(ANY), setOf(ANY)); ctx(S, oN, 1) }
            trans(WIDTH) { src(S, oN, 2); tgt(c); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(c); tgt(C); lhg(setOf(ANY), setOf(ANY)); ctx(S, oN, 2) }
            trans(GRAFT) { src(C); tgt(S); lhg(setOf(ANY), setOf(ANY)); ctx(S, oN, 2) }
            trans(GRAFT) { src(S); tgt(G); lhg(setOf(EOT), setOf(ANY)); ctx(G, oN, SOR) }

        }

        AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "abc"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
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
        val eS = EMPTY

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(G, oN, SOR)      // G = . S
            val s1 = state(eS, oN, ER)      // eS
            val s2 = state(S, oN, ER)      // S = eS .
            val s3 = state(G, oN, ER)      // G = S .

            trans(WIDTH) { src(G, oN, SOR); tgt(eS); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(eS); tgt(S); lhg(setOf(ANY), setOf(ANY)); ctx(G, oN, SOR) }
            trans(GRAFT) { src(S); tgt(G); lhg(setOf(EOT), setOf(ANY)); ctx(G, oN, SOR) }

        }

        AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            ""
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
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
        val G = SM.startState.runtimeRules.first()
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
            val s0 = state(G, oN, SOR)      // G = . S
            val s1 = state(a, oN, ER)      // a
            val s2 = state(eoA, oN, ER)    // empty-oA
            val s3 = state(oA, o0, ER)     // oA = a .
            val s4 = state(oA, o1, ER)     // oA =  .
            val s5 = state(S, oN, 1)   // S = oA . oB oC
            val s6 = state(b, oN, ER)      // b
            val s7 = state(eoB, oN, ER)    // empty-oB
            val s8 = state(oB, o0, ER)     // oB = b .
            val s9 = state(oB, o1, ER)     // oB = .
            val s10 = state(S, oN, 2)   // S = oA oB . oC
            val s11 = state(c, oN, ER)      // c
            val s12 = state(eoC, oN, ER)    // empty-oC
            val s13 = state(oC, o0, ER)     // oC = c .
            val s14 = state(oC, o1, ER)     // oC = .
            val s15 = state(S, oN, ER)      // S = oA oB oC .
            val s16 = state(G, oN, ER)      // G = S .

            trans(WIDTH) { src(G, oN, SOR); tgt(a); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(WIDTH) { src(G, oN, SOR); tgt(eoA); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(oA, o0, ER); lhg(setOf(ANY), setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(eoA); tgt(oA, o1, ER); lhg(setOf(ANY), setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(oA, o0, ER); tgt(S, oN, 1); lhg(setOf(ANY), setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(oA, o1, ER); tgt(S, oN, 1); lhg(setOf(ANY), setOf(ANY)); ctx(G, oN, SOR) }
            trans(WIDTH) { src(S, oN, 1); tgt(b); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(WIDTH) { src(S, oN, 1); tgt(eoB); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(b); tgt(oB, o0, ER); lhg(setOf(ANY), setOf(ANY)); ctx(S, oN, 1) }
            trans(HEIGHT) { src(eoB); tgt(oB, o1, ER); lhg(setOf(ANY), setOf(ANY)); ctx(S, oN, 1) }
            trans(GRAFT) { src(oB, o0, ER); tgt(S, oN, 2); lhg(setOf(ANY), setOf(ANY)); ctx(S, oN, 1) }
            trans(GRAFT) { src(oB, o1, ER); tgt(S, oN, 2); lhg(setOf(ANY), setOf(ANY)); ctx(S, oN, 1) }
            trans(WIDTH) { src(S, oN, 2); tgt(c); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(WIDTH) { src(S, oN, 2); tgt(eoC); lhg(setOf(ANY)); ctx(G, oN, SOR) }
            trans(HEIGHT) { src(c); tgt(oC, o0, ER); lhg(setOf(ANY), setOf(ANY)); ctx(S, oN, 2) }
            trans(HEIGHT) { src(eoC); tgt(oC, o1, ER); lhg(setOf(ANY), setOf(ANY)); ctx(S, oN, 2) }
            trans(GRAFT) { src(oC, o0, ER); tgt(S); lhg(setOf(ANY), setOf(ANY)); ctx(S, oN, 2) }
            trans(GRAFT) { src(oC, o1, ER); tgt(S); lhg(setOf(ANY), setOf(ANY)); ctx(S, oN, 2) }
            trans(GRAFT) { src(S); tgt(G); lhg(setOf(EOT), setOf(ANY)); ctx(G, oN, SOR) }

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
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
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
        val G = SM.startState.runtimeRules.first()
        val ABC1 = rrs.findRuntimeRule("ABC1")
        val ABC2 = rrs.findRuntimeRule("ABC2")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")

        //val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(G, oN, SOR)                           // G = . S
            val s1 = state(a, oN, ER)                            // a
            val s2 = state(RP(ABC1, oN, 1), RP(ABC2, oN, 1))     // ABC1 = a . b c, ABC2 = a . b c
            val s3 = state(b, oN, SOR)                           // b
            val s4 = state(RP(ABC1, oN, 2), RP(ABC2, oN, 2))     // ABC1 = a b . c, ABC2 = a b . c
            val s5 = state(RP(c, oN, ER))                        // c
            val s6 = state(RP(ABC1, oN, ER), RP(ABC2, oN, ER))   // ABC1 = a b c ., ABC2 = a b c .
            val s7 = state(RP(S, oN, ER), RP(S, o1, ER))         // S = ABC1 ., S = ABC2 .
            val s8 = state(RP(G, oN, SOR))                       // G = S .

            transition(s0, s0, s1, WIDTH, setOf(ANY), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(ANY), setOf(setOf(ANY)), setOf(RP(ABC1, oN, SOR), RP(ABC2, oN, SOR)))
            transition(s0, s2, s3, WIDTH, setOf(ANY), setOf(setOf(ANY)), null)
            transition(s0, s3, s4, GRAFT, setOf(ANY), setOf(setOf(ANY)), setOf(RP(ABC1, oN, 1), RP(ABC2, oN, 1)))
            transition(s0, s4, s5, WIDTH, setOf(ANY), setOf(setOf(ANY)), null)
            transition(s0, s5, s6, GRAFT, setOf(ANY), setOf(setOf(ANY)), setOf(RP(ABC1, oN, 2), RP(ABC2, oN, 2)))
            transition(s0, s6, s7, HEIGHT, setOf(ANY), setOf(setOf(ANY)), setOf(RP(S, oN, SOR), RP(S, o1, SOR)))
            transition(s0, s7, s8, GRAFT, setOf(EOT), setOf(setOf(ANY)), setOf(RP(G, oN, SOR)))

        }

        //AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "abc",
            "abc"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
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
            val s0 = state(G, oN, SOR)      // G = . S


        }

        //AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "abc",
            "abd"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
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
            val s0 = state(G, oN, SOR)      // G = . S


        }

        //AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "abc",
            "xabd"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
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
            val s0 = state(G, oN, SOR)      // G = . S


        }

        //AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "aaaa"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
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
            val s0 = state(G, oN, SOR)      // G = . S


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
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
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
            val s0 = state(G, oN, SOR)      // G = . S


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
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
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
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, automatonKind)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(RP(G, oN, SOR))      // G = . S


        }

        //AutomatonTest..assertEquals(expected, actual)

        val sentences = listOf(
            "id",
            "(id)",
            "..."
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
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

        val actual = SM.build()
        //println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(G, oN, SOR)      // G = . S


        }

        AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "da",
            "bdc",
            "dc",
            "bda"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
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
            val s0 = state(G, oN, SOR)      // G = . S


        }

        //AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "a",
            "aa",
            "aaa",
            "aaaa"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
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
        val S1 = rrs.findRuntimeRule("S1")

        val actual = SM.build()
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, automatonKind, "S", false) {
            val s0 = state(G, oN, SOR)      // G = . S
            val s1 = state(a, oN, ER)      // a
            val s2 = state(S, oN, ER)      // S = a .
            val s3 = state(S1, oN, 1)  // S1 = a . S
            val s4 = state(G, oN, ER)      // G = S .
            val s5 = state(S1, oN, ER)     // S1 = a S .
            val s6 = state(S, o1, ER)      // S = S1 .

            transition(s0, s0, s1, WIDTH, setOf(ANY), emptySet(), null)
            transition(setOf(s0, s3), s1, s2, HEIGHT, setOf(ANY), setOf(setOf(ANY)), setOf(RP(S, oN, SOR)))
            transition(setOf(s0, s3), s1, s3, HEIGHT, setOf(ANY), setOf(setOf(ANY)), setOf(RP(S1, oN, SOR)))
            transition(s0, s2, s4, GRAFT, setOf(EOT), setOf(setOf(ANY)), setOf(RP(G, oN, SOR)))
            transition(s3, s2, s5, GRAFT, setOf(ANY), setOf(setOf(ANY)), setOf(RP(S1, oN, 1)))
            transition(setOf(s0, s3), s3, s1, WIDTH, setOf(ANY), emptySet(), null)
            transition(setOf(s0, s3), s5, s6, HEIGHT, setOf(ANY), setOf(setOf(ANY)), setOf(RP(S, o1, SOR)))
            transition(s3, s5, s6, HEIGHT, setOf(ANY), setOf(setOf(ANY)), setOf(RP(S, o1, SOR)))
            transition(s3, s6, s5, GRAFT, setOf(ANY), setOf(setOf(ANY)), setOf(RP(S1, oN, 1)))
            transition(s0, s6, s4, GRAFT, setOf(EOT), setOf(setOf(ANY)), setOf(RP(G, oN, SOR)))

        }

        AutomatonTest.assertEquals(expected, actual)

        val sentences = listOf(
            "a",
            "aa",
            "aaa",
            "aaaa"
        )
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)
        for (sentence in sentences) {
            parser.parseForGoal("S", sentence)
        }
        println(rrs.usedAutomatonToString("S"))
    }
}