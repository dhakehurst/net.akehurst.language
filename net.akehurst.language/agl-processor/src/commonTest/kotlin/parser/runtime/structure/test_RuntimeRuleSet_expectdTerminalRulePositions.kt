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

class test_RuntimeRuleSet_expectdTerminalRulePositions {

    // S = 'a' ;
    @Test
    fun expectedTerminalRulePositions1() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val S = rb.rule("S").concatenation(r_a)
        val sut = rb.ruleSet()

        val goal = RuntimeRuleSet.createGoal(S)
        val actual = sut.expectedTerminalRulePositions[RulePosition(goal,0,0)] ?: arrayOf()
        val expected = arrayOf(RulePosition(S,0,0))

        assertEquals(expected.toList(), actual.toList())
    }

    // S = 'a' | S1 ;
    // S1 = S 'a' ;
    @Test
    fun expectedTerminalRulePositions2() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").build()
        val r_S1 = rb.rule("S1").concatenation(r_S, r_a)
        rb.rule(r_S).choiceEqual(r_a, r_S1)
        val sut = rb.ruleSet()

        val goal = RuntimeRuleSet.createGoal(r_S)
        val actual = sut.expectedTerminalRulePositions[RulePosition(goal,0,0)] ?: arrayOf()
        val expected = arrayOf(RulePosition(r_S,0,0))

        assertEquals(expected.toList(), actual.toList())
    }

    // S = 'a' | S1 ;
    // S1 = 'a' S ;
    @Test
    fun expectedTerminalRulePositions3() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").build()
        val r_S1 = rb.rule("S1").concatenation(r_a, r_S)
        rb.rule(r_S).choiceEqual(r_a, r_S1)
        val sut = rb.ruleSet()

        val goal = RuntimeRuleSet.createGoal(r_S)
        val actual = sut.expectedTerminalRulePositions[RulePosition(goal,0,0)] ?: arrayOf()
        val expected = arrayOf(RulePosition(r_S,0,0), RulePosition(r_S1,0,0))

        assertEquals(expected.toList(), actual.toList())
    }

    //TODO: more tests
}