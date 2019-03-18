/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.runtime.structure

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.parser.ParseException
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.parser.scannerless.ScannerlessParser
import parser.runtime.structure.RuntimeRuleSet.nextPossibleRulePositionStates.assertDeepEquals
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_misc {

    // S = P | 'a' ;
    // P =  S*;
    @Test
    fun G2_S_0_0() {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_S = b.rule("S").build()
        val r_P = b.rule("P").multi(0,-1,r_S)
        b.rule(r_S).choiceEqual(r_P, r_a)
        val sut = b.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val s1 = RulePositionState(1, RulePosition(r_a, 0, 2), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))

        val actual = sut.nextPossibleRulePositionStates(gr, s1, s1.graftLookahead)
        val expected = setOf(
            RulePositionState(1, RulePosition(r_a, 0, 0), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))
        )
        assertDeepEquals(expected, actual)
    }

    // S =  'a' | P | I ;
    // P = S 'p' 'a' ;
    // I = [S / 'o']2+ ;
    @Test
    fun aPI_lookahead__S_0_0() {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_p = b.literal("p")
        val r_o = b.literal("o")
        val r_S = b.rule("S").build()
        val r_P = b.rule("P").concatenation(r_S, r_p, r_a)
        val r_I = b.rule("I").separatedList(2, -1, r_o, r_S)
        b.rule(r_S).choiceEqual(r_a, r_P, r_I)
        val sut = b.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val s1 = RulePositionState(1, RulePosition(r_a, 0, 2), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))

        val actual = sut.nextPossibleRulePositionStates(gr, s1, s1.graftLookahead)
        val expected = setOf(
            RulePositionState(1, RulePosition(r_a, 0, 0), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))
        )
        assertDeepEquals(expected, actual)
    }

    // S =  'a' | P | I ;
    // P = S 'p' 'a' ;
    // I = [S / 'o']2+ ;
    @Test
    fun sPI_lookahead__S_1_0() {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_p = b.literal("p")
        val r_o = b.literal("o")
        val r_S = b.rule("S").build()
        val r_P = b.rule("P").concatenation(r_S, r_p, r_a)
        val r_I = b.rule("I").separatedList(2, -1, r_o, r_S)
        b.rule(r_S).choiceEqual(r_a, r_P, r_I)
        val sut = b.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val s1 = RulePositionState(1, RulePosition(r_a, 0, 2), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))

        val actual = sut.nextPossibleRulePositionStates(gr, s1, s1.graftLookahead)
        val expected = setOf(
            RulePositionState(1, RulePosition(r_a, 0, 0), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))
        )
        assertDeepEquals(expected, actual)
    }

    // S =  n | P | I ;      //  infix < propertyCall < name
    // n = 'a' ;             // "[a-z]+"
    // P = S 'p' n ;         // S '.' name
    // I = [S / 'o']2+ ;         // [S / '+']2+
    @Test
    fun SnPI_lookahead__S_0_0() {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_p = b.literal("p")
        val r_o = b.literal("o")
        val r_n = b.rule("n").concatenation(r_a)
        val r_S = b.rule("S").build()
        val r_P = b.rule("P").concatenation(r_S, r_p, r_n)
        val r_I = b.rule("I").separatedList(2, -1, r_o, r_S)
        b.rule(r_S).choiceEqual(r_n, r_P, r_I)
        val sut = b.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val s1 = RulePositionState(1, RulePosition(r_a, 0, 2), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))

        val actual = sut.nextPossibleRulePositionStates(gr, s1, s1.graftLookahead)
        val expected = setOf(
            RulePositionState(1, RulePosition(r_a, 0, 0), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))
        )
        assertDeepEquals(expected, actual)
    }

    // S =  n | P | I ;      //  infix < propertyCall < name
    // n = 'a' ;             // "[a-z]+"
    // P = S 'p' n ;         // S '.' name
    // I = [S / 'o']2+ ;         // [S / '+']2+
    @Test
    fun SnPI_lookahead__S_1_0() {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_p = b.literal("p")
        val r_o = b.literal("o")
        val r_n = b.rule("n").concatenation(r_a)
        val r_S = b.rule("S").build()
        val r_P = b.rule("P").concatenation(r_S, r_p, r_n)
        val r_I = b.rule("I").separatedList(2, -1, r_o, r_S)
        b.rule(r_S).choiceEqual(r_n, r_P, r_I)
        val sut = b.ruleSet()
        val s0 = sut.startingRulePositionState(r_S)
        val gr = s0.rulePosition.runtimeRule

        val s1 = RulePositionState(1, RulePosition(r_a, 0, 2), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))

        val actual = sut.nextPossibleRulePositionStates(gr, s1, s1.graftLookahead)
        val expected = setOf(
            RulePositionState(1, RulePosition(r_a, 0, 0), emptySet(),setOf(RuntimeRuleSet.END_OF_TEXT))
        )
        assertDeepEquals(expected, actual)
    }

    fun g3(): RuntimeRuleSet {
        val rb = RuntimeRuleSetBuilder()
        val A = rb.literal("A")
        val a = rb.literal("a")
        val B = rb.literal("B")
        val b = rb.literal("b")
        val Aa = rb.rule("Aa").concatenation(A, a)
        val Bb = rb.rule("Bb").concatenation(B, b)
        val r = rb.rule("r").choicePriority(Aa, Bb)

        return rb.ruleSet()
    }

}