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

import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_ScanOnDemandParser {

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
    fun parse() {
        val rrb = RuntimeRuleSetBuilder()
        val r0 = rrb.literal("a")
        val r1 = rrb.rule("a").concatenation(r0)
        val sp = ScanOnDemandParser(rrb.ruleSet())

        val result = sp.parseForGoal("a", "a", AutomatonKind.LOOKAHEAD_1)
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

    }


}