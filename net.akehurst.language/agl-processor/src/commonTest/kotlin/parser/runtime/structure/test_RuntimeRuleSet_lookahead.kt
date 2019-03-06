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
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_RuntimeRuleSet_lookahead {

    // S = 'a' ;
    @Test
    fun simple_lookahead__S_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").concatenation(r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual = sut.lookahead(RulePosition(r_S,0,0),gr)
        val expected = setOf(RuntimeRuleSet.END_OF_TEXT)

        assertEquals(expected, actual)
    }

    // S = 'a' | S1 ;
    // S1 = S 'a' ;
    @Test
    fun simple_LRecursive_lookahead__S_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").build()
        val r_S1 = rb.rule("S1").concatenation(r_S, r_a)
        rb.rule(r_S).choiceEqual(r_a, r_S1)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual = sut.lookahead(RulePosition(r_S,0,0),gr)
        val expected = setOf(r_a, RuntimeRuleSet.END_OF_TEXT)

        assertEquals(expected, actual)
    }


    // S = 'a' | S1 ;
    // S1 = S 'a' ;
    @Test
    fun simple_LRecursive_lookahead__S1_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").build()
        val r_S1 = rb.rule("S1").concatenation(r_S, r_a)
        rb.rule(r_S).choiceEqual(r_a, r_S1)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual = sut.lookahead(RulePosition(r_S1,0,0),gr)
        val expected = setOf(r_a)

        assertEquals<Set<RuntimeRule>>(expected, actual)
    }

    // S = 'a' | S1 ;
    // S1 = S 'a' ;
    @Test
    fun simple_LRecursive_lookahead__S1_0_1() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").build()
        val r_S1 = rb.rule("S1").concatenation(r_S, r_a)
        rb.rule(r_S).choiceEqual(r_a, r_S1)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual = sut.lookahead(RulePosition(r_S1,0,1),gr)
        val expected = setOf(RuntimeRuleSet.END_OF_TEXT, r_a)

        assertEquals<Set<RuntimeRule>>(expected, actual)
    }

    // S = 'a' | S1 ;
    // S1 = 'a' S ;
    @Test
    fun simple_RRecursive_lookahead__S_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").build()
        val r_S1 = rb.rule("S1").concatenation(r_a, r_S)
        rb.rule(r_S).choiceEqual(r_a, r_S1)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual = sut.lookahead(RulePosition(r_S,0,0),gr)
        val expected = setOf(RuntimeRuleSet.END_OF_TEXT)

        assertEquals<Set<RuntimeRule>>(expected, actual)
    }

    // S = 'a' | S1 ;
    // S1 = 'a' S ;
    @Test
    fun simple_RRecursive_lookahead__S1_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").build()
        val r_S1 = rb.rule("S1").concatenation(r_a, r_S)
        rb.rule(r_S).choiceEqual(r_a, r_S1)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual = sut.lookahead(RulePosition(r_S1,0,0),gr)
        val expected = setOf(r_a)

        assertEquals<Set<RuntimeRule>>(expected, actual)
    }

    // S = 'a' | S1 ;
    // S1 = 'a' S ;
    @Test
    fun simple_RRecursive_lookahead__S1_0_1() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_S = rb.rule("S").build()
        val r_S1 = rb.rule("S1").concatenation(r_a, r_S)
        rb.rule(r_S).choiceEqual(r_a, r_S1)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual = sut.lookahead(RulePosition(r_S1,0,1),gr)
        val expected = setOf(RuntimeRuleSet.END_OF_TEXT)

        assertEquals<Set<RuntimeRule>>(expected, actual)
    }

    //TODO: multi and sList lookahead


    // S = ['a' / ',']*
    @Test
    fun sList_lookahead__S_empty_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual = sut.lookahead(RulePosition(r_S, RuntimeRuleItem.SLIST__EMPTY_RULE,0),gr)
        val expected = setOf(RuntimeRuleSet.END_OF_TEXT)

        assertEquals<Set<RuntimeRule>>(expected, actual)
    }

    // S = ['a' / ',']*
    @Test
    fun sList_lookahead__S_item_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual = sut.lookahead(RulePosition(r_S, RuntimeRuleItem.SLIST__ITEM,0),gr)
        val expected = setOf(r_c, RuntimeRuleSet.END_OF_TEXT)

        assertEquals<Set<RuntimeRule>>(expected, actual)
    }

    // S = ['a' / ',']*
    @Test
    fun sList_lookahead__S_separator_1() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val actual = sut.lookahead(RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR,1),gr)
        val expected = setOf(r_a)

        assertEquals<Set<RuntimeRule>>(expected, actual)
    }



    // S = ['a' / ',']*
    @Test
    fun sList_lookahead__S_item_1() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val ex = assertFailsWith(ParseException::class) {
            sut.lookahead(RulePosition(r_S,RuntimeRuleItem.SLIST__ITEM,1),gr)
        }

    }

    // S = ['a' / ',']*
    @Test
    fun sList_lookahead__S_separator_2() {
        val rb = RuntimeRuleSetBuilder()
        val r_a = rb.literal("a")
        val r_c = rb.literal(",")
        val r_S = rb.rule("S").separatedList(0, -1, r_c, r_a)
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoal(r_S)

        val ex = assertFailsWith(ParseException::class) {
            val actual = sut.lookahead(RulePosition(r_S, RuntimeRuleItem.SLIST__SEPARATOR, 2), gr)
        }

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