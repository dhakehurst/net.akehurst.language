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

import net.akehurst.language.parser.api.OptionNum
import net.akehurst.language.parser.api.RulePosition
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_RulePosition_next {

    private companion object {
        val EOT = RuntimeRuleSet.END_OF_TEXT
        val RT = RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD

        val EOR = RulePosition.END_OF_RULE
        val SOR = RulePosition.START_OF_RULE

        val oN = RulePosition.OPTION_NONE
        val o1 = OptionNum(1)
        val o2 =  OptionNum(2)
        val OMI = RulePosition.OPTION_MULTI_ITEM
        val OME = RulePosition.OPTION_MULTI_EMPTY
        val OLE = RulePosition.OPTION_SLIST_EMPTY
        val OLI = RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR
        val OLS = RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR

        val PMI = RulePosition.POSITION_MULIT_ITEM
        val PLI = RulePosition.POSITION_SLIST_ITEM
        val PLS = RulePosition.POSITION_SLIST_SEPARATOR

        fun RP(rr: RuntimeRule, opt: OptionNum, pos: Int): RulePositionRuntime = RulePositionRuntime(rr, opt, pos)
    }

    //empty
    // S =  ;
    @Test
    fun empty__S_0_0() {
        //val rb = RuntimeRuleSetBuilder()
        //val r_S = rb.rule("S").empty()
        //val sut = rb.ruleSet()
        //val gr = RuntimeRuleSet.createGoalRule(r_S)

        val rrs = runtimeRuleSet {
            concatenation("S") { empty() }
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, oN, SOR).next()
        val expected: Set<RulePositionRuntime> = setOf(RP(r_S, oN, EOR))

        assertEquals(expected, actual)
    }

    // concatenation
    // S = a b c
    @Test
    fun concat__S_0_0() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a");literal("b"); literal("c") }
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, oN, SOR).next()
        val expected: Set<RulePositionRuntime> = setOf(
            RP(r_S, oN, 1)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun concat__S_0_1() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a");literal("b"); literal("c") }
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, oN,1).next()
        val expected: Set<RulePositionRuntime> = setOf(
            RP(r_S, oN,2)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun concat__S_0_2() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a");literal("b"); literal("c") }
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, oN, 2).next()
        val expected: Set<RulePositionRuntime> = setOf(
            RP(r_S, oN, EOR)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun concat__S_0_EOR() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a");literal("b"); literal("c") }
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, oN, EOR).next()
        val expected: Set<RulePositionRuntime> = setOf()

        assertEquals(expected, actual)
    }

    // choice equal
    // S = 'a' | 'b' 'x' | 'c' 'y' 'z';
    @Test
    fun choiceEquals__S_0_0() {
        val rrs = runtimeRuleSet {
            choiceLongest("S") {
                concatenation { literal("a") }
                concatenation { literal("b"); literal("x") }
                concatenation { literal("c"); literal("y"); literal("z") }
            }
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, oN, SOR).next()
        val expected: Set<RulePositionRuntime> = setOf(
            RP(r_S, oN, EOR)
        )

        assertEquals(expected, actual)
    }

    // S = 'a' | 'b' | 'c';
    @Test
    fun choiceEquals__S_1_0() {
        val rrs = runtimeRuleSet {
            choiceLongest("S") {
                concatenation { literal("a") }
                concatenation { literal("b"); literal("x") }
                concatenation { literal("c"); literal("y"); literal("z") }
            }
        }

        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, o1, SOR).next()
        val expected: Set<RulePositionRuntime> = setOf(RP(r_S, o1, 1))

        assertEquals(expected, actual)
    }

    // S = 'a' | 'b' | 'c';
    @Test
    fun choiceEquals__S_2_0() {
        val rrs = runtimeRuleSet {
            choiceLongest("S") {
                concatenation { literal("a") }
                concatenation { literal("b"); literal("x") }
                concatenation { literal("c"); literal("y"); literal("z") }
            }
        }

        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, o2, SOR).next()
        val expected: Set<RulePositionRuntime> = setOf(RP(r_S, o2, 1))

        assertEquals(expected, actual)
    }

// - multi

    // -- S = 'a'? ;
    @Test
    fun multi01__S_Empty_start() {
        val rrs = runtimeRuleSet {
            multi("S", 0, 1, "'a'")
            literal("'a'", "a")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, OME, SOR).next()
        val expected: Set<RulePositionRuntime> = setOf(RP(r_S, OME, EOR))

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Empty_end() {
        val rrs = runtimeRuleSet {
            multi("S", 0, 1, "'a'")
            literal("'a'", "a")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, OME, EOR).next()
        val expected: Set<RulePositionRuntime> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Item_start() {
        val rrs = runtimeRuleSet {
            multi("S", 0, 1, "'a'")
            literal("'a'", "a")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, OMI, SOR).next()
        val expected: Set<RulePositionRuntime> = setOf(RP(r_S, OMI, EOR))

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Item_mid() {
        val rrs = runtimeRuleSet {
            multi("S", 0, 1, "'a'")
            literal("'a'", "a")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, OMI, PMI).next()
        val expected: Set<RulePositionRuntime> = setOf(
            RP(r_S, OMI, PMI),
            RP(r_S, OMI, EOR)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Item_end() {
        val rrs = runtimeRuleSet {
            multi("S", 0, 1, "'a'")
            literal("'a'", "a")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, OMI, EOR).next()
        val expected: Set<RulePositionRuntime> = setOf()

        assertEquals(expected, actual)
    }

// - sList

    // -- S = ['a' / ',']* ;
    @Test
    fun sList0n__S_Empty_start() {
        val rrs = runtimeRuleSet {
            sList("S", 0, -1, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, OLE, SOR).next()
        val expected: Set<RulePositionRuntime> = setOf(RP(r_S, OLE, EOR))

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Empty_end() {
        val rrs = runtimeRuleSet {
            sList("S", 0, -1, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, OLE, EOR).next()
        val expected: Set<RulePositionRuntime> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Item_start() {
        val rrs = runtimeRuleSet {
            sList("S", 0, -1, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, OLI, SOR).next()
        val expected: Set<RulePositionRuntime> = setOf(
            RP(r_S, oN, EOR),
            RP(r_S, oN, PLS)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Sep_sep() {
        val rrs = runtimeRuleSet {
            sList("S", 0, -1, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, OLS, PLS).next()
        val expected: Set<RulePositionRuntime> = setOf(RP(r_S, OLS, PLI))

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Item_end() {
        val rrs = runtimeRuleSet {
            sList("S", 0, -1, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, OLI, EOR).next()
        val expected: Set<RulePositionRuntime> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Sep_end() {
        val rrs = runtimeRuleSet {
            sList("S", 0, -1, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RulePositionRuntime> = RP(r_S, OLS, EOR).next()
        val expected: Set<RulePositionRuntime> = setOf()
//TODO: shoud this throw IllegalState ?
        assertEquals(expected, actual)
    }

// -- S = ['a' / ',']+ ;


    @Test
    fun sList1n__S_Empty_start() {
        val sList1n = runtimeRuleSet {
            sList("S", 1, -1, "'a'", "c")
            literal("c", ",")
            literal("'a'", "a")
        }
        val S = sList1n.findRuntimeRule("S")
        //val SM = sList1n.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        //val G = SM.startState

        val actual = RP(S, OLE, SOR).next()
        val expected: Set<RulePositionRuntime> = emptySet() // S should never be empty

        assertEquals(expected, actual)
    }

    @Test
    fun sList1n__S_Empty_end() {
        val sList1n = runtimeRuleSet {
            sList("S", 1, -1, "'a'", "c")
            literal("c", ",")
            literal("'a'", "a")
        }
        val S = sList1n.findRuntimeRule("S")
        //val SM = sList1n.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        //val G = SM.startState

        val actual = RP(S, OLE, EOR).next()
        val expected: Set<RulePositionRuntime> = emptySet() // S should never be empty

        assertEquals(expected, actual)
    }

    @Test
    fun sList1n__S_Item_start() {
        val sList1n = runtimeRuleSet {
            sList("S", 1, -1, "'a'", "c")
            literal("c", ",")
            literal("'a'", "a")
        }
        val S = sList1n.findRuntimeRule("S")
        //val SM = sList1n.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        //val G = SM.startState

        val rp = RP(S, oN, SOR)
        val actual = rp.next()
        val expected: Set<RulePositionRuntime> = setOf(
            RP(S, OLI, PLS),
            RP(S, OLI, EOR)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList1n__S_Sep_sep() {
        val sList1n = runtimeRuleSet {
            sList("S", 1, -1, "'a'", "c")
            literal("c", ",")
            literal("'a'", "a")
        }
        val S = sList1n.findRuntimeRule("S")
        //val SM = sList1n.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val rp = RP(S, OLS, PLS)
        val actual = rp.next()
        val expected: Set<RulePositionRuntime> = setOf(
            RP(S, OLS, PLI)
        )

        assertEquals(expected, actual)
    }

}