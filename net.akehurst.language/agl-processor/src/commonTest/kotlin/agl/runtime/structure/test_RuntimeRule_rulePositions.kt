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

class test_RuntimeRule_rulePositions {

    @Test
    fun rulePositions_literal_0_0() {
        // 'a' ?

        //given
        val rrs = runtimeRuleSet {
            literal("'a'", "a")
        }
        val ta = rrs.findRuntimeRule("'a'")

        val expected = setOf<RulePosition>()

        //when
        val actual = ta.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_concat_literal_0_0() {
        // S = 'a' ?

        //given
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, 0, RulePosition.END_OF_RULE)
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_list_separated_0_0() {
        // S = ['a'/',']?

        //given
        val rrs = runtimeRuleSet {
            sList("S", 0, 0, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_EMPTY, RulePosition.END_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_list_separated_0_1() {
        // S = ['a'/',']?

        //given
        val rrs = runtimeRuleSet {
            sList("S", 0, 1, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_EMPTY, RulePosition.END_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_list_separated_0_2() {
        // S = ['a'/',']?

        //given
        val rrs = runtimeRuleSet {
            sList("S", 0, 2, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_EMPTY, RulePosition.END_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_list_separated_0_5() {
        // S = ['a'/',']0..5

        //given
        val rrs = runtimeRuleSet {
            sList("S", 0, 5, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_EMPTY, RulePosition.END_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_list_separated_0_n() {
        // S = ['a'/',']*

        //given
        val rrs = runtimeRuleSet {
            sList("S", 0, RuntimeRuleRhsList.MULTIPLICITY_N, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, RulePosition.OPTION_SLIST_EMPTY, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_EMPTY, RulePosition.END_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_list_separated_1_0() {
        // S = ['a'/',']1..1

        //given
        val rrs = runtimeRuleSet {
            sList("S", 1, 0, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_list_separated_1_1() {
        // S = ['a'/',']1..1

        //given
        val rrs = runtimeRuleSet {
            sList("S", 1, 1, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_list_separated_1_2() {
        // S = ['a'/',']1..1

        //given
        val rrs = runtimeRuleSet {
            sList("S", 1, 2, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_list_separated_1_5() {
        // S = ['a'/',']1..5

        //given
        val rrs = runtimeRuleSet {
            sList("S", 1, 5, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_list_separated_1_n() {
        // S = ['a'/',']+

        //given
        val rrs = runtimeRuleSet {
            sList("S", 1, RuntimeRuleRhsList.MULTIPLICITY_N, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_list_separated_2_2() {
        // S = ['a'/',']2..5

        //given
        val rrs = runtimeRuleSet {
            sList("S", 2, 2, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_list_separated_2_5() {
        // S = ['a'/',']2..5

        //given
        val rrs = runtimeRuleSet {
            sList("S", 2, 5, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_list_separated_2_n() {
        // S = ['a'/',']2+

        //given
        val rrs = runtimeRuleSet {
            sList("S", 2, RuntimeRuleRhsList.MULTIPLICITY_N, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_list_separated_5_5() {
        // S = ['a'/',']2..5

        //given
        val rrs = runtimeRuleSet {
            sList("S", 5, 5, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_list_separated_5_7() {
        // S = ['a'/',']2..5

        //given
        val rrs = runtimeRuleSet {
            sList("S", 5, 7, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_list_separated_5_n() {
        // S = ['a'/',']2+

        //given
        val rrs = runtimeRuleSet {
            sList("S", 5, RuntimeRuleRhsList.MULTIPLICITY_N, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")

        val expected = setOf(
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.START_OF_RULE),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_ITEM),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.POSITION_SLIST_SEPARATOR),
            RulePosition(S, RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR, RulePosition.END_OF_RULE),
        )

        //when
        val actual = S.rulePositionsNotAtStart

        //then
        assertEquals(expected, actual)
    }

}