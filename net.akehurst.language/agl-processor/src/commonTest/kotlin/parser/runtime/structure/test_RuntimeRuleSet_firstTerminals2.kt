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
        val actual = sut.firstTerminals2[RulePosition(S,0,0)] ?: setOf()
        val expected = setOf(r_a)

        assertEquals<Set<RuntimeRule>>(expected, actual)
    }

    @Test
    fun g1_firstTerminals2_Se() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val S = rb.rule("S").concatenation(r_a)
        val sut = rb.ruleSet()
        val actual = sut.firstTerminals2[RulePosition(S,0,-1)] ?: setOf()
        val expected = emptySet<RuntimeRule>()

        assertEquals<Set<RuntimeRule>>(expected, actual)
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
        val r = rb.rule("r").choicePriority(Aa, Bb)

        val sut = rb.ruleSet()
        val actual = sut.firstTerminals2[RulePosition(r,0,0)]
        val expected = setOf(A, B)

        assertEquals(expected, actual)
    }




}