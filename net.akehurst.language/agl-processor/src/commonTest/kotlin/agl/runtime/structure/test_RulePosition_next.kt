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

import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_RulePosition_next {

    private companion object {
        val EOT = RuntimeRuleSet.END_OF_TEXT
        val RT = RuntimeRuleSet.USE_RUNTIME_LOOKAHEAD

        val EOR = RuleOptionPosition.END_OF_RULE
        val SOR = RuleOptionPosition.START_OF_RULE

        val OMI = RuleOptionPosition.OPTION_MULTI_ITEM
        val OME = RuleOptionPosition.OPTION_MULTI_EMPTY
        val OLE = RuleOptionPosition.OPTION_SLIST_EMPTY
        val OLI = RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR
        val OLS = RuleOptionPosition.OPTION_SLIST_ITEM_OR_SEPERATOR

        val PMI = RuleOptionPosition.POSITION_MULIT_ITEM
        val PLI = RuleOptionPosition.POSITION_SLIST_ITEM
        val PLS = RuleOptionPosition.POSITION_SLIST_SEPARATOR

        fun RP(rr: RuntimeRule, opt: Int, pos: Int): RuleOptionPosition = RuleOptionPosition(rr, opt, pos)
    }

    //empty
    // S =  ;
    @Test
    fun empty__S_0_0() {
        val rb = RuntimeRuleSetBuilder()
        val r_S = rb.rule("S").empty()
        val sut = rb.ruleSet()
        val gr = RuntimeRuleSet.createGoalRule(r_S)

        val actual: Set<RuleOptionPosition> = RP(r_S, 0, SOR).next()
        val expected: Set<RuleOptionPosition> = setOf(RP(r_S, 0, EOR))

        assertEquals(expected, actual)
    }

    // choice equal
    // S = 'a' | 'b' | 'c';
    @Test
    fun choiceEquals__S_0_0() {
        val rrs = runtimeRuleSet {
            choice("S",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                literal("b")
                literal("c")
            }
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RuleOptionPosition> = RP(r_S, 0, SOR).next()
        val expected: Set<RuleOptionPosition> = setOf(RP(r_S, 0, EOR))

        assertEquals(expected, actual)
    }

    // S = 'a' | 'b' | 'c';
    @Test
    fun choiceEquals__S_1_0() {
        val rrs = runtimeRuleSet {
            choice("S",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                literal("b")
                literal("c")
            }
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RuleOptionPosition> = RP(r_S, 1, SOR).next()
        val expected: Set<RuleOptionPosition> = setOf(RP(r_S, 1, EOR))

        assertEquals(expected, actual)
    }

    // S = 'a' | 'b' | 'c';
    @Test
    fun choiceEquals__S_2_0() {
        val rrs = runtimeRuleSet {
            choice("S",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                literal("b")
                literal("c")
            }
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RuleOptionPosition> = RP(r_S, 2, SOR).next()
        val expected: Set<RuleOptionPosition> = setOf(RP(r_S, 2, EOR))

        assertEquals(expected, actual)
    }

    // choice priority

    // concatenation

    // - multi

    // -- S = 'a'? ;
    @Test
    fun multi01__S_Empty_start() {
        val rrs = runtimeRuleSet {
            multi("S",0,1,"'a'")
            literal("'a'","a")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RuleOptionPosition> = RP(r_S, OME, SOR).next()
        val expected: Set<RuleOptionPosition> = setOf(RP(r_S, OME, EOR))

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Empty_end() {
        val rrs = runtimeRuleSet {
            multi("S",0,1,"'a'")
            literal("'a'","a")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RuleOptionPosition> = RP(r_S, OME, EOR).next()
        val expected: Set<RuleOptionPosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Item_start() {
        val rrs = runtimeRuleSet {
            multi("S",0,1,"'a'")
            literal("'a'","a")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RuleOptionPosition> = RP(r_S, OMI, SOR).next()
        val expected: Set<RuleOptionPosition> = setOf(RP(r_S, OMI, EOR))

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Item_mid() {
        val rrs = runtimeRuleSet {
            multi("S",0,1,"'a'")
            literal("'a'","a")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RuleOptionPosition> = RP(r_S, OMI, PMI).next()
        val expected: Set<RuleOptionPosition> = setOf(
                RP(r_S, OMI, PMI),
                RP(r_S, OMI, EOR)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun multi01__S_Item_end() {
        val rrs = runtimeRuleSet {
            multi("S",0,1,"'a'")
            literal("'a'","a")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RuleOptionPosition> = RP(r_S, PMI, EOR).next()
        val expected: Set<RuleOptionPosition> = setOf()

        assertEquals(expected, actual)
    }

    // - sList

    // -- S = ['a' / ',']* ;
    @Test
    fun sList0n__S_Empty_start() {
        val rrs = runtimeRuleSet {
            sList("S",0,-1,"'a'","','")
            literal("'a'","a")
            literal("','",",")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RuleOptionPosition> = RP(r_S, OLE, SOR).next()
        val expected: Set<RuleOptionPosition> = setOf(RP(r_S, OLE, EOR))

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Empty_end() {
        val rrs = runtimeRuleSet {
            sList("S",0,-1,"'a'","','")
            literal("'a'","a")
            literal("','",",")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RuleOptionPosition> = RP(r_S, OLE, EOR).next()
        val expected: Set<RuleOptionPosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Item_start() {
        val rrs = runtimeRuleSet {
            sList("S",0,-1,"'a'","','")
            literal("'a'","a")
            literal("','",",")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RuleOptionPosition> = RP(r_S, OLI, SOR).next()
        val expected: Set<RuleOptionPosition> = setOf(
                RP(r_S, OLI, EOR),
                RP(r_S, OLS, PLS)
        )

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Sep_sep() {
        val rrs = runtimeRuleSet {
            sList("S",0,-1,"'a'","','")
            literal("'a'","a")
            literal("','",",")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RuleOptionPosition> = RP(r_S, OLS, PLS).next()
        val expected: Set<RuleOptionPosition> = setOf(RP(r_S, OLI, PLI))

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Item_end() {
        val rrs = runtimeRuleSet {
            sList("S",0,-1,"'a'","','")
            literal("'a'","a")
            literal("','",",")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RuleOptionPosition> = RP(r_S, OLI, EOR).next()
        val expected: Set<RuleOptionPosition> = setOf()

        assertEquals(expected, actual)
    }

    @Test
    fun sList0n__S_Sep_end() {
        val rrs = runtimeRuleSet {
            sList("S",0,-1,"'a'","','")
            literal("'a'","a")
            literal("','",",")
        }
        val r_S = rrs.findRuntimeRule("S")

        val actual: Set<RuleOptionPosition> = RP(r_S, OLS, EOR).next()
        val expected: Set<RuleOptionPosition> = setOf()
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
        val SM = sList1n.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState

        val actual = RP(S, OLE, SOR).next()
        val expected: Set<RuleOptionPosition> = emptySet() // S should never be empty

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
        val SM = sList1n.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState

        val actual = RP(S, OLE, EOR).next()
        val expected: Set<RuleOptionPosition> = emptySet() // S should never be empty

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
        val SM = sList1n.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState

        val rp = RP(S, OLI, SOR)
        val actual = rp.next()
        val expected: Set<RuleOptionPosition> = setOf(
                RP(S, OLS, PLS),
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
        val SM = sList1n.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val rp = RP(S, OLS, PLS)
        val actual = rp.next()
        val expected: Set<RuleOptionPosition> = setOf(
                RP(S, OLI, PLI)
        )

        assertEquals(expected, actual)
    }

}