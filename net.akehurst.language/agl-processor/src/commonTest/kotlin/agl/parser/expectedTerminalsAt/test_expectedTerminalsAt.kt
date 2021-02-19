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

package net.akehurst.language.parser.expectedTerminalsAt

import net.akehurst.language.agl.automaton.AutomatonKind
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_expectedTerminalsAt {

    fun concat_a() : RuntimeRuleSet {
        val rrb = RuntimeRuleSetBuilder()
        val r0 = rrb.literal("a")
        val r1 = rrb.rule("S").concatenation(r0)
        return rrb.ruleSet()
    }

    @Test
    fun concat_a_empty_0() {
        val rs = concat_a()
        val sp = ScanOnDemandParser(rs)

        val actual =  sp.expectedTerminalsAt("S","",0, AutomatonKind.LC1).toList() //to list to make assertions easier
        assertNotNull(actual)

        assertEquals(1, actual.size)
        assertEquals(RuntimeRuleKind.TERMINAL, actual[0].kind)
        assertEquals("a", actual[0].value)
    }

    @Test
    fun concat_a_a_0() {
        val rs = concat_a()
        val sp = ScanOnDemandParser(rs)

        val actual =  sp.expectedTerminalsAt("S","a",0, AutomatonKind.LC1).toList() //to list to make assertions easier
        assertNotNull(actual)

        assertEquals(1, actual.size)
        assertEquals(RuntimeRuleKind.TERMINAL, actual[0].kind)
        assertEquals("a", actual[0].value)
    }

    @Test
    fun concat_a_a_1() {
        val rs = concat_a()
        val sp = ScanOnDemandParser(rs)

        val actual =  sp.expectedTerminalsAt("S","a",1, AutomatonKind.LC1)
        assertNotNull(actual)

        assertEquals(setOf(RuntimeRuleSet.END_OF_TEXT), actual)
    }

    fun concat_ab() : RuntimeRuleSet {
        val rrb = RuntimeRuleSetBuilder()
        val a = rrb.literal("a")
        val b = rrb.literal("b")
        val r1 = rrb.rule("S").concatenation(a,b)
        return rrb.ruleSet()
    }

    @Test
    fun concat_ab_empty_0() {
        val rs = concat_ab()
        val sp = ScanOnDemandParser(rs)

        val actual =  sp.expectedTerminalsAt("S","",0, AutomatonKind.LC1).toList() //to list to make assertions easier
        assertNotNull(actual)

        assertEquals(1, actual.size)
        assertEquals(RuntimeRuleKind.TERMINAL, actual[0].kind)
        assertEquals("a", actual[0].value)
    }

    @Test
    fun concat_ab_a_0() {
        val rs = concat_ab()
        val sp = ScanOnDemandParser(rs)

        val actual =  sp.expectedTerminalsAt("S","a",0, AutomatonKind.LC1).toList() //to list to make assertions easier
        assertNotNull(actual)

        assertEquals(1, actual.size)
        assertEquals(RuntimeRuleKind.TERMINAL, actual[0].kind)
        assertEquals("a", actual[0].value)
    }

    @Test
    fun concat_ab_a_1() {
        val rs = concat_ab()
        val sp = ScanOnDemandParser(rs)

        val actual =  sp.expectedTerminalsAt("S","a",1, AutomatonKind.LC1).toList() //to list to make assertions easier
        assertNotNull(actual)

        assertEquals(1, actual.size)
        assertEquals(RuntimeRuleKind.TERMINAL, actual[0].kind)
        assertEquals("b", actual[0].value)
    }

    @Test
    fun concat_ab_ab_1() {
        val rs = concat_ab()
        val sp = ScanOnDemandParser(rs)

        val actual =  sp.expectedTerminalsAt("S","ab",1, AutomatonKind.LC1).toList() //to list to make assertions easier
        assertNotNull(actual)

        assertEquals(1, actual.size)
        assertEquals(RuntimeRuleKind.TERMINAL, actual[0].kind)
        assertEquals("b", actual[0].value)
    }
    @Test
    fun concat_ab_ab_2() {
        val rs = concat_ab()
        val sp = ScanOnDemandParser(rs)

        val actual =  sp.expectedTerminalsAt("S","ab",2, AutomatonKind.LC1)
        assertNotNull(actual)

        assertEquals(setOf(RuntimeRuleSet.END_OF_TEXT), actual)
    }

    fun choiceEqual_ab() : RuntimeRuleSet {
        val rrb = RuntimeRuleSetBuilder()
        val a = rrb.literal("a")
        val b = rrb.literal("b")
        val r1 = rrb.rule("S").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY,a,b)
        return rrb.ruleSet()
    }

    @Test
    fun choiceEqual_ab_empty_0() {
        val rs = choiceEqual_ab()
        val sp = ScanOnDemandParser(rs)

        val actual =  sp.expectedTerminalsAt("S","",0, AutomatonKind.LC1).toList() //to list to make assertions easier
        assertNotNull(actual)

        assertEquals(2, actual.size)
        assertEquals(RuntimeRuleKind.TERMINAL, actual[0].kind)
        assertEquals("a", actual[0].value)
        assertEquals(RuntimeRuleKind.TERMINAL, actual[1].kind)
        assertEquals("b", actual[1].value)
    }

    @Test
    fun choiceEqual_ab_a_1() {
        val rs = choiceEqual_ab()
        val sp = ScanOnDemandParser(rs)

        val actual =  sp.expectedTerminalsAt("S","a",1, AutomatonKind.LC1)
        assertNotNull(actual)

        assertEquals(setOf(RuntimeRuleSet.END_OF_TEXT), actual)
    }

    @Test
    fun choiceEqual_ab_b_1() {
        val rs = choiceEqual_ab()
        val sp = ScanOnDemandParser(rs)

        val actual =  sp.expectedTerminalsAt("S","b",1, AutomatonKind.LC1)
        assertNotNull(actual)

        assertEquals(setOf(RuntimeRuleSet.END_OF_TEXT), actual)
    }
}