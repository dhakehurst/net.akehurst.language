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

package net.akehurst.language.parser.scannerless

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail

class test_ScannerlessParser_expectedAt {

    fun concat_a() : RuntimeRuleSet {
        val rrb = RuntimeRuleSetBuilder()
        val r0 = rrb.literal("a")
        val r1 = rrb.rule("S").concatenation(r0)
        return rrb.ruleSet()
    }

    @Test
    fun concat_a_empty_0() {
        val rs = concat_a()
        val sp = ScannerlessParser(rs)

        val actual =  sp.expectedAt("S","",0)
        assertNotNull(actual)

        assertEquals(1, actual.size)
        assertEquals(true, actual[0].isTerminal)
        assertEquals("a", actual[0].value)
    }

    @Test
    fun concat_a_a_0() {
        val rs = concat_a()
        val sp = ScannerlessParser(rs)

        val actual =  sp.expectedAt("S","a",0)
        assertNotNull(actual)

        assertEquals(1, actual.size)
        assertEquals(true, actual[0].isTerminal)
        assertEquals("a", actual[0].value)
    }

    @Test
    fun concat_a_a_1() {
        val rs = concat_a()
        val sp = ScannerlessParser(rs)

        val actual =  sp.expectedAt("S","a",1)
        assertNotNull(actual)

        assertEquals(0, actual.size)
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
        val sp = ScannerlessParser(rs)

        val actual =  sp.expectedAt("S","",0)
        assertNotNull(actual)

        assertEquals(1, actual.size)
        assertEquals(true, actual[0].isTerminal)
        assertEquals("a", actual[0].value)
    }

    @Test
    fun concat_ab_a_0() {
        val rs = concat_ab()
        val sp = ScannerlessParser(rs)

        val actual =  sp.expectedAt("S","a",0)
        assertNotNull(actual)

        assertEquals(1, actual.size)
        assertEquals(true, actual[0].isTerminal)
        assertEquals("a", actual[0].value)
    }

    @Test
    fun concat_ab_a_1() {
        val rs = concat_ab()
        val sp = ScannerlessParser(rs)

        val actual =  sp.expectedAt("S","a",1)
        assertNotNull(actual)

        assertEquals(1, actual.size)
        assertEquals(true, actual[0].isTerminal)
        assertEquals("b", actual[0].value)
    }

    @Test
    fun concat_ab_ab_1() {
        val rs = concat_ab()
        val sp = ScannerlessParser(rs)

        val actual =  sp.expectedAt("S","ab",1)
        assertNotNull(actual)

        assertEquals(1, actual.size)
        assertEquals(true, actual[0].isTerminal)
        assertEquals("b", actual[0].value)
    }
    @Test
    fun concat_ab_ab_2() {
        val rs = concat_ab()
        val sp = ScannerlessParser(rs)

        val actual =  sp.expectedAt("S","ab",2)
        assertNotNull(actual)

        assertEquals(0, actual.size)
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
        val sp = ScannerlessParser(rs)

        val actual =  sp.expectedAt("S","",0)
        assertNotNull(actual)

        assertEquals(2, actual.size)
        assertEquals(true, actual[0].isTerminal)
        assertEquals("a", actual[0].value)
        assertEquals(true, actual[1].isTerminal)
        assertEquals("b", actual[1].value)
    }

    @Test
    fun choiceEqual_ab_a_1() {
        val rs = choiceEqual_ab()
        val sp = ScannerlessParser(rs)

        val actual =  sp.expectedAt("S","a",1)
        assertNotNull(actual)

        assertEquals(0, actual.size)
    }

    @Test
    fun choiceEqual_ab_b_1() {
        val rs = choiceEqual_ab()
        val sp = ScannerlessParser(rs)

        val actual =  sp.expectedAt("S","b",1)
        assertNotNull(actual)

        assertEquals(0, actual.size)
    }
}