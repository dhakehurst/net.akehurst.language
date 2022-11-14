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

import kotlin.test.Test
import kotlin.test.assertEquals

class test_RuntimeRuleSet_firstTerminals2 {

    // S = 'a' ;
    @Test
    fun g1_firstTerminals2_S0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val S = rb.rule("S").concatenation(r_a)
        val sut = rb.ruleSet()
        val actual = sut.firstTerminals2[RuleOptionPosition(S,0,0)]
        val expected = listOf(r_a)

        assertEquals(expected, actual)
    }

    @Test
    fun g1_firstTerminals2_Se() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val S = rb.rule("S").concatenation(r_a)
        val sut = rb.ruleSet()
        val actual = sut.firstTerminals2[RuleOptionPosition(S,0,-1)]
        val expected = emptyList<RuntimeRule>()

        assertEquals(expected, actual)
    }

    @Test
    fun firstTerminals2() {
        val rb = RuntimeRuleSetBuilder()
        val A = rb.literal("A")
        val a = rb.literal("a")
        val B = rb.literal("B")
        val b = rb.literal("b")
        val Aa = rb.rule("Aa").concatenation(A, a)
        val Bb = rb.rule("Bb").concatenation(B, b)
        val r_S = rb.rule("S").choice(RuntimeRuleChoiceKind.PRIORITY_LONGEST,Aa, Bb)

        val sut = rb.ruleSet()
        val actual1 = sut.firstTerminals2[RuleOptionPosition(r_S,0,0)]
        assertEquals(listOf(A), actual1)
        val actual2 = sut.firstTerminals2[RuleOptionPosition(r_S,1,0)]
        assertEquals(listOf(B), actual2)
    }

    // S = P | 'a' ;
    // P =  S*;
    @Test
    fun G2_S_0_0() {
        val b = RuntimeRuleSetBuilder()
        val r_a = b.literal("a")
        val r_S = b.rule("S").build()
        val r_P = b.rule("P").multi(0,-1,r_S)
        b.rule(r_S).choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY,r_P, r_a)
        val sut = b.ruleSet()

        val actual = sut.firstTerminals2[RuleOptionPosition(r_S,0,0)]
        val expected = listOf(r_P.emptyRuleItem, r_a)

        assertEquals(expected, actual)
    }


}