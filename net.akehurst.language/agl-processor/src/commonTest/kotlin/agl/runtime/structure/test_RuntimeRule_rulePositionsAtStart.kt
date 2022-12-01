/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

class test_RuntimeRule_rulePositionsAtStart {

    @Test
    fun literal() {
        // 'a'

        //given
        val rrs = runtimeRuleSet {
            literal("'a'", "a")
        }
        val r = rrs.findRuntimeRule("'a'")

        //when
        val actual = r.rulePositionsAtStart

        //then
        val expected = setOf<RulePosition>()
        assertEquals(expected, actual)
    }

    @Test
    fun pattern() {
        // 'a'

        //given
        val rrs = runtimeRuleSet {
            pattern("P", "[a-c]")
        }
        val r = rrs.findRuntimeRule("P")

        //when
        val actual = r.rulePositionsAtStart

        //then
        val expected = setOf<RulePosition>()
        assertEquals(expected, actual)
    }

    @Test
    fun S_empty() {
        // S = ;

        //given
        val rrs = runtimeRuleSet {
            concatenation("S") { empty() }
        }
        val r = rrs.findRuntimeRule("S")

        //when
        val actual = r.rulePositionsAtStart

        //then
        val expected = setOf<RulePosition>(
            RulePosition(r, 0, RulePosition.START_OF_RULE)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun S_concat_a_b_c() {
        // S = a b c;

        //given
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a"); literal("b"); literal("c") }
        }
        val r = rrs.findRuntimeRule("S")

        //when
        val actual = r.rulePositionsAtStart

        //then
        val expected = setOf<RulePosition>(
            RulePosition(r, 0, RulePosition.START_OF_RULE)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun S_choice_a_b_c() {
        // S = a | b | c;

        //given
        val rrs = runtimeRuleSet {
            choiceLongest("S") {
                literal("a")
                literal("b")
                literal("c")
            }
        }
        val r = rrs.findRuntimeRule("S")


        //when
        val actual = r.rulePositionsAtStart

        //then
        val expected = setOf<RulePosition>(
            RulePosition(r, 0, RulePosition.START_OF_RULE),
            RulePosition(r, 1, RulePosition.START_OF_RULE),
            RulePosition(r, 2, RulePosition.START_OF_RULE)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun S_multi_01() {
        // S = a?;

        //given
        val rrs = runtimeRuleSet {
            multi("S", 0, 1, "'a'")
            literal("'a'", "a")
        }
        val r = rrs.findRuntimeRule("S")

        //when
        val actual = r.rulePositionsAtStart

        //then
        val expected = setOf<RulePosition>(
            RulePosition(r, RulePosition.OPTION_MULTI_EMPTY, RulePosition.START_OF_RULE),
            RulePosition(r, RulePosition.OPTION_MULTI_ITEM, RulePosition.START_OF_RULE)

        )
        assertEquals(expected, actual)
    }

    @Test
    fun S_multi_24() {
        // S = a{2..4};

        //given
        val rrs = runtimeRuleSet {
            multi("S", 2, 4, "'a'")
            literal("'a'", "a")
        }
        val r = rrs.findRuntimeRule("S")

        //when
        val actual = r.rulePositionsAtStart

        //then
        val expected = setOf<RulePosition>(
            RulePosition(r, RulePosition.OPTION_MULTI_ITEM, RulePosition.START_OF_RULE)

        )
        assertEquals(expected, actual)
    }

    @Test
    fun S_slist_4() {
        // S = [a/b]4;

        //given
        val rrs = runtimeRuleSet {
            sList("S", 4, 4, "'a'", "'b'")
            literal("'a'", "a")
            literal("'b'", "b")
        }
        val r = rrs.findRuntimeRule("S")

        //when
        val actual = r.rulePositionsAtStart

        //then
        val expected = setOf<RulePosition>(
            RulePosition(r, RulePosition.OPTION_MULTI_ITEM, RulePosition.START_OF_RULE)

        )
        assertEquals(expected, actual)
    }

    @Test
    fun S_slist_0N() {
        // S = a*;

        //given
        val rrs = runtimeRuleSet {
            multi("S", 0, -1, "'a'")
            literal("'a'", "a")
        }
        val r = rrs.findRuntimeRule("S")

        //when
        val actual = r.rulePositionsAtStart

        //then
        val expected = setOf<RulePosition>(
            RulePosition(r, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE),
            RulePosition(r, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE)

        )
        assertEquals(expected, actual)
    }
}