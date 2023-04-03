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

import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_expectedTerminalsAt {

    fun concat_a(): RuntimeRuleSet {
        return runtimeRuleSet {
            concatenation("S") { literal("a") }
        }
        //val rrb = RuntimeRuleSetBuilder()
        //val r0 = rrb.literal("a")
        //val r1 = rrb.rule("S").concatenation(r0)
        //return rrb.ruleSet()
    }

    @Test
    fun concat_a_empty_0() {
        val rs = concat_a()
        val sp = ScanOnDemandParser(rs)

        val actual = sp.expectedTerminalsAt("S", "", 0, AutomatonKind.LOOKAHEAD_1)
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.pattern
                is RuntimeRuleRhsLiteral -> rhs.value
                else -> it.tag
            }
        }

        assertEquals(1, actual.size)
        assertEquals(true, actual.all { it.isTerminal })
        assertEquals("a", actStr[0])
    }

    @Test
    fun concat_a_a_0() {
        val rs = concat_a()
        val sp = ScanOnDemandParser(rs)

        val actual = sp.expectedTerminalsAt("S", "a", 0, AutomatonKind.LOOKAHEAD_1).toList() //to list to make assertions easier
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.pattern
                is RuntimeRuleRhsLiteral -> rhs.value
                else -> it.tag
            }
        }
        assertEquals(1, actual.size)
        assertEquals(true, actual.all { it.isTerminal })
        assertEquals("a", actStr[0])
    }

    @Test
    fun concat_a_a_1() {
        val rs = concat_a()
        val sp = ScanOnDemandParser(rs)

        val actual = sp.expectedTerminalsAt("S", "a", 1, AutomatonKind.LOOKAHEAD_1)

        assertEquals(true, actual.all { it.isTerminal })
        assertEquals(setOf(), actual)
    }

    fun concat_ab(): RuntimeRuleSet {
        return runtimeRuleSet {
            concatenation("S") { literal("a"); literal("b") }
        }
        //val rrb = RuntimeRuleSetBuilder()
        //val a = rrb.literal("a")
        //val b = rrb.literal("b")
        //val r1 = rrb.rule("S").concatenation(a,b)
        //return rrb.ruleSet()
    }

    @Test
    fun concat_ab_empty_0() {
        val rs = concat_ab()
        val sp = ScanOnDemandParser(rs)

        val actual = sp.expectedTerminalsAt("S", "", 0, AutomatonKind.LOOKAHEAD_1).toList() //to list to make assertions easier
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.pattern
                is RuntimeRuleRhsLiteral -> rhs.value
                else -> it.tag
            }
        }

        assertEquals(1, actual.size)
        assertEquals(true, actual.all { it.isTerminal })
        assertEquals("a", actStr[0])
    }

    @Test
    fun concat_ab_a_0() {
        val rs = concat_ab()
        val sp = ScanOnDemandParser(rs)

        val actual = sp.expectedTerminalsAt("S", "a", 0, AutomatonKind.LOOKAHEAD_1).toList() //to list to make assertions easier
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.pattern
                is RuntimeRuleRhsLiteral -> rhs.value
                else -> it.tag
            }
        }

        assertEquals(1, actual.size)
        assertEquals(true, actual.all { it.isTerminal })
        assertEquals("a", actStr[0])
    }

    @Test
    fun concat_ab_a_1() {
        val rs = concat_ab()
        val sp = ScanOnDemandParser(rs)

        val actual = sp.expectedTerminalsAt("S", "a", 1, AutomatonKind.LOOKAHEAD_1).toList() //to list to make assertions easier
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.pattern
                is RuntimeRuleRhsLiteral -> rhs.value
                else -> it.tag
            }
        }

        assertEquals(1, actual.size)
        assertEquals(true, actual.all { it.isTerminal })
        assertEquals("b", actStr[0])
    }

    @Test
    fun concat_ab_ab_1() {
        val rs = concat_ab()
        val sp = ScanOnDemandParser(rs)

        val actual = sp.expectedTerminalsAt("S", "ab", 1, AutomatonKind.LOOKAHEAD_1).toList() //to list to make assertions easier
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.pattern
                is RuntimeRuleRhsLiteral -> rhs.value
                else -> it.tag
            }
        }

        assertEquals(1, actual.size)
        assertEquals(true, actual.all { it.isTerminal })
        assertEquals("b", actStr[0])
    }

    @Test
    fun concat_ab_ab_2() {
        val rs = concat_ab()
        val sp = ScanOnDemandParser(rs)

        val actual = sp.expectedTerminalsAt("S", "ab", 2, AutomatonKind.LOOKAHEAD_1)
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.pattern
                is RuntimeRuleRhsLiteral -> rhs.value
                else -> it.tag
            }
        }

        assertEquals(setOf(), actual)
    }

    fun choiceEqual_ab(): RuntimeRuleSet {
        return runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                literal("b")
            }
        }
        //val rrb = RuntimeRuleSetBuilder()
        //val a = rrb.literal("a")
        //val b = rrb.literal("b")
        //val r1 = rrb.rule("S").choice(RuntimeRuleChoiceKind.LONGEST_PRIORITY,a,b)
        //return rrb.ruleSet()
    }

    @Test
    fun choiceEqual_ab_empty_0() {
        val rs = choiceEqual_ab()
        val sp = ScanOnDemandParser(rs)

        val actual = sp.expectedTerminalsAt("S", "", 0, AutomatonKind.LOOKAHEAD_1).toList() //to list to make assertions easier
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.pattern
                is RuntimeRuleRhsLiteral -> rhs.value
                else -> it.tag
            }
        }

        assertEquals(2, actual.size)
        assertEquals(true, actual.all { it.isTerminal })
        assertEquals("a", actStr[0])
        assertEquals(true, actual.all { it.isTerminal })
        assertEquals("b", actStr[1])
    }

    @Test
    fun choiceEqual_ab_a_1() {
        val rs = choiceEqual_ab()
        val sp = ScanOnDemandParser(rs)

        val actual = sp.expectedTerminalsAt("S", "a", 1, AutomatonKind.LOOKAHEAD_1)
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.pattern
                is RuntimeRuleRhsLiteral -> rhs.value
                else -> it.tag
            }
        }

        assertEquals(setOf(), actual)
    }

    @Test
    fun choiceEqual_ab_b_1() {
        val rs = choiceEqual_ab()
        val sp = ScanOnDemandParser(rs)

        val actual = sp.expectedTerminalsAt("S", "b", 1, AutomatonKind.LOOKAHEAD_1)
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.pattern
                is RuntimeRuleRhsLiteral -> rhs.value
                else -> it.tag
            }
        }

        assertEquals(setOf(), actual)
    }
}