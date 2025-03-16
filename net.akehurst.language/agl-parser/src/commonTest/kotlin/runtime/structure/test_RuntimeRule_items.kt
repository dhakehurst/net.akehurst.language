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

import net.akehurst.language.automaton.leftcorner.test_AutomatonUtilsAbstract
import kotlin.test.Test
import kotlin.test.assertEquals

class test_RuntimeRule_items : test_AutomatonUtilsAbstract() {

    fun check(rp:RulePositionRuntime, expected:Set<RuntimeRule>) {
        val actual = rp.items
        assertEquals(expected, actual)
    }

    @Test
    fun rulePositions_literal() {
        // 'a'

        //given
        val rrs = runtimeRuleSet {
            literal("'a'", "a")
        }
        val a = rrs.findRuntimeRule("'a'")
        val G = rrs.goalRuleFor[a]

        //then
        assertEquals(2, G.rulePositions.size)
        check(RP(G,oN, SOR),setOf(a))
        check(RP(G,oN, EOR),setOf())
        assertEquals(0, a.rulePositions.size)
    }

    @Test
    fun rulePositions_concat_literal() {
        // S = 'a'

        //given
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val S = rrs.findRuntimeRule("S")
        val G = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")

        //then
        assertEquals(2, G.rulePositions.size)
        check(RP(G,oN, SOR),setOf(S))
        check(RP(G,oN, EOR),setOf())
        assertEquals(2, S.rulePositions.size)
        check(RP(S,oN, SOR),setOf(a))
        check(RP(S,oN, EOR),setOf())
        assertEquals(0, a.rulePositions.size)
    }

    @Test
    fun rulePositions_list_simple_0_0() {
        // S = 'a'{0}

        //given
        val rrs = runtimeRuleSet {
            multi("S", 0, 0, "'a'")
            literal("'a'", "a")
        }
        val S = rrs.findRuntimeRule("S")
        val G = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")


        //then
        assertEquals(2, G.rulePositions.size)
        check(RP(G,oN, SOR),setOf(S))
        check(RP(G,oN, EOR),setOf())
        assertEquals(2, S.rulePositions.size)
        check(RP(S, oLE, SOR),setOf(EMPTY_LIST))
        check(RP(S,oLE, EOR),setOf())
        assertEquals(0, a.rulePositions.size)
    }

    @Test
    fun rulePositions_list_simple_0_1() {
        // S = 'a'?

        //given
        val rrs = runtimeRuleSet {
            multi("S", 0, 1, "'a'")
            literal("'a'", "a")
        }
        val S = rrs.findRuntimeRule("S")
        val G = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")

        //then
        assertEquals(2, G.rulePositions.size)
        check(RP(G,oN, SOR),setOf(S))
        check(RP(G,oN, EOR),setOf())
        assertEquals(4, S.rulePositions.size)
        check(RP(S, oLE, SOR),setOf(EMPTY_LIST))
        check(RP(S,oLE, EOR),setOf())
        check(RP(S, oLI, SOR),setOf(a))
        check(RP(S,oLI, EOR),setOf())
        assertEquals(0, a.rulePositions.size)
    }

    @Test
    fun rulePositions_list_simple_0_n() {
        // S = 'a'*

        //given
        val rrs = runtimeRuleSet {
            multi("S", 0, -1, "'a'")
            literal("'a'", "a")
        }
        val S = rrs.findRuntimeRule("S")
        val G = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")

        //then
        assertEquals(2, G.rulePositions.size)
        check(RP(G,oN, SOR),setOf(S))
        check(RP(G,oN, EOR),setOf())
        assertEquals(5, S.rulePositions.size)
        check(RP(S, oLE, SOR),setOf(EMPTY_LIST))
        check(RP(S,oLE, EOR),setOf())
        check(RP(S, oLI, SOR),setOf(a))
        check(RP(S,oLI, PMI),setOf(a))
        check(RP(S,oLI, EOR),setOf())
        assertEquals(0, a.rulePositions.size)
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
        val G = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")
        val c = rrs.findRuntimeRule("','")

        //then
        assertEquals(2, G.rulePositions.size)
        check(RP(G,oN, SOR),setOf(S))
        check(RP(G,oN, EOR),setOf())
        assertEquals(4, S.rulePositions.size)
        check(RP(S, oSE, SOR),setOf(EMPTY_LIST))
        check(RP(S,oSE, EOR),setOf())
        check(RP(S, oSI, SOR),setOf(a))
        check(RP(S,oSI, EOR),setOf())
        assertEquals(0, a.rulePositions.size)
        assertEquals(0, c.rulePositions.size)
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
        val G = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")
        val c = rrs.findRuntimeRule("','")

        //then
        assertEquals(2, G.rulePositions.size)
        check(RP(G,oN, SOR),setOf(S))
        check(RP(G,oN, EOR),setOf())
        assertEquals(6, S.rulePositions.size)
        check(RP(S, oSE, SOR),setOf(EMPTY_LIST))
        check(RP(S,oSE, EOR),setOf())
        check(RP(S, oSI, SOR),setOf(a))
        check(RP(S, oSI, PLI),setOf(a))
        check(RP(S, oSS, PLS),setOf(c))
        check(RP(S,oSI, EOR),setOf())
        assertEquals(0, a.rulePositions.size)
        assertEquals(0, c.rulePositions.size)
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
        val G = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")
        val c = rrs.findRuntimeRule("','")

        //then
        assertEquals(2, G.rulePositions.size)
        check(RP(G,oN, SOR),setOf(S))
        check(RP(G,oN, EOR),setOf())
        assertEquals(6, S.rulePositions.size)
        check(RP(S, oSE, SOR),setOf(EMPTY_LIST))
        check(RP(S,oSE, EOR),setOf())
        check(RP(S, oSI, SOR),setOf(a))
        check(RP(S, oSI, PLI),setOf(a))
        check(RP(S, oSS, PLS),setOf(c))
        check(RP(S,oSI, EOR),setOf())
        assertEquals(0, a.rulePositions.size)
        assertEquals(0, c.rulePositions.size)
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
        val G = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")
        val c = rrs.findRuntimeRule("','")

        //then
        assertEquals(2, G.rulePositions.size)
        check(RP(G,oN, SOR),setOf(S))
        check(RP(G,oN, EOR),setOf())
        assertEquals(2, S.rulePositions.size)
        check(RP(S, oSI, SOR),setOf(a))
        check(RP(S,oSI, EOR),setOf())
        assertEquals(0, a.rulePositions.size)
        assertEquals(0, c.rulePositions.size)
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
        val G = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")
        val c = rrs.findRuntimeRule("','")

        //then
        assertEquals(2, G.rulePositions.size)
        check(RP(G,oN, SOR),setOf(S))
        check(RP(G,oN, EOR),setOf())
        assertEquals(4, S.rulePositions.size)
        check(RP(S, oSI, SOR),setOf(a))
        check(RP(S, oSI, PLI),setOf(a))
        check(RP(S, oSS, PLS),setOf(c))
        check(RP(S,oSI, EOR),setOf())
        assertEquals(0, a.rulePositions.size)
        assertEquals(0, c.rulePositions.size)
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
        val G = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")
        val c = rrs.findRuntimeRule("','")

        //then
        assertEquals(2, G.rulePositions.size)
        check(RP(G,oN, SOR),setOf(S))
        check(RP(G,oN, EOR),setOf())
        assertEquals(4, S.rulePositions.size)
        check(RP(S, oSI, SOR),setOf(a))
        check(RP(S, oSI, PLI),setOf(a))
        check(RP(S, oSS, PLS),setOf(c))
        check(RP(S,oSI, EOR),setOf())
        assertEquals(0, a.rulePositions.size)
        assertEquals(0, c.rulePositions.size)
    }

    @Test
    fun rulePositions_list_separated_5_n() {
        // S = ['a'/',']5+

        //given
        val rrs = runtimeRuleSet {
            sList("S", 5, RuntimeRuleRhsList.MULTIPLICITY_N, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")
        val G = rrs.goalRuleFor[S]
        val a = rrs.findRuntimeRule("'a'")

        //then
        assertEquals(2, G.rulePositions.size)
        check(RP(G,oN, SOR),setOf(S))
        check(RP(G,oN, EOR),setOf())
        assertEquals(5, S.rulePositions.size)
        check(RP(S, oLE, SOR),setOf(EMPTY))
        check(RP(S,oLE, EOR),setOf())
        check(RP(S, oLI, SOR),setOf(a))
        check(RP(S,oLI, PMI),setOf(a))
        check(RP(S,oLI, EOR),setOf())
        assertEquals(0, a.rulePositions.size)
    }

}