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

package net.akehurst.language.parser.scanondemand

import net.akehurst.language.agl.automaton.AutomatonKind
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_RuntimeParser {

    @Test
    fun construct() {
        val rrb = RuntimeRuleSetBuilder()
        val sp = ScanOnDemandParser(rrb.ruleSet())

        assertNotNull(sp)
    }

    @Test
    fun build() {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        val sp = ScanOnDemandParser(rrs)
        sp.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        //TODO: how to test if build worked!
    }

    @Test
    fun expectedAt() {
        val rrb = RuntimeRuleSetBuilder()
        val r_a = rrb.literal("a")
        val r_S = rrb.rule("S").concatenation(r_a)

        val sp = ScanOnDemandParser(rrb.ruleSet())

        val actual = sp.expectedAt("S", "", 0, AutomatonKind.LOOKAHEAD_1).toList() //to list to make assertions easier

        val expected = listOf(r_S)

        assertEquals(expected, actual)
    }

    @Test
    fun expectedTerminalsAt() {
        val rrb = RuntimeRuleSetBuilder()
        val r_a = rrb.literal("a")
        val r_S = rrb.rule("S").concatenation(r_a)

        val sp = ScanOnDemandParser(rrb.ruleSet())

        val actual = sp.expectedTerminalsAt("S", "", 0, AutomatonKind.LOOKAHEAD_1).toList() //to list to make assertions easier

        val expected = listOf(r_a)

        assertEquals(expected, actual)
    }
}