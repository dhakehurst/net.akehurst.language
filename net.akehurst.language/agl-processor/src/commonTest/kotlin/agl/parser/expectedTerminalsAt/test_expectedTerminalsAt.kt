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
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.runtime.structure.*
import kotlin.test.Test
import kotlin.test.assertEquals

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
        val sentence = ""
        val goal = "S"
        val position = 0

        val actual = sp.expectedTerminalsAt(sentence, position, Agl.parseOptions { goalRuleName(goal) })
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.patternUnescaped
                is RuntimeRuleRhsLiteral -> rhs.literalUnescaped
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
        val sentence = "a"
        val goal = "S"
        val position = 0

        val actual = sp.expectedTerminalsAt(sentence, position, Agl.parseOptions { goalRuleName(goal) })
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.patternUnescaped
                is RuntimeRuleRhsLiteral -> rhs.literalUnescaped
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
        val sentence = "a"
        val goal = "S"
        val position = 1

        val actual = sp.expectedTerminalsAt(sentence, position, Agl.parseOptions { goalRuleName(goal) })

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
        val sentence = ""
        val goal = "S"
        val position = 0

        val actual = sp.expectedTerminalsAt(sentence, position, Agl.parseOptions { goalRuleName(goal) })
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.patternUnescaped
                is RuntimeRuleRhsLiteral -> rhs.literalUnescaped
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
        val sentence = "a"
        val goal = "S"
        val position = 0

        val actual = sp.expectedTerminalsAt(sentence, position, Agl.parseOptions { goalRuleName(goal) })
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.patternUnescaped
                is RuntimeRuleRhsLiteral -> rhs.literalUnescaped
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
        val sentence = "a"
        val goal = "S"
        val position = 1

        val actual = sp.expectedTerminalsAt(sentence, position, Agl.parseOptions { goalRuleName(goal) })
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.patternUnescaped
                is RuntimeRuleRhsLiteral -> rhs.literalUnescaped
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
        val sentence = "ab"
        val goal = "S"
        val position = 1

        val actual = sp.expectedTerminalsAt(sentence, position, Agl.parseOptions { goalRuleName(goal) })
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.patternUnescaped
                is RuntimeRuleRhsLiteral -> rhs.literalUnescaped
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
        val sentence = "ab"
        val goal = "S"
        val position = 2

        val actual = sp.expectedTerminalsAt(sentence, position, Agl.parseOptions { goalRuleName(goal) })
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.patternUnescaped
                is RuntimeRuleRhsLiteral -> rhs.literalUnescaped
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
        val sentence = ""
        val goal = "S"
        val position = 0

        val actual = sp.expectedTerminalsAt(sentence, position, Agl.parseOptions { goalRuleName(goal) })
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.patternUnescaped
                is RuntimeRuleRhsLiteral -> rhs.literalUnescaped
                else -> it.tag
            }
        }.toSet()

        assertEquals(2, actual.size)
        assertEquals(true, actual.all { it.isTerminal })
        assertEquals(setOf("a", "b"), actStr)
    }

    @Test
    fun choiceEqual_ab_a_1() {
        val rs = choiceEqual_ab()
        val sp = ScanOnDemandParser(rs)
        val sentence = "a"
        val goal = "S"
        val position = 1

        val actual = sp.expectedTerminalsAt(sentence, position, Agl.parseOptions { goalRuleName(goal) })
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.patternUnescaped
                is RuntimeRuleRhsLiteral -> rhs.literalUnescaped
                else -> it.tag
            }
        }.toSet()

        assertEquals(setOf(), actStr)
    }

    @Test
    fun choiceEqual_ab_b_1() {
        val rs = choiceEqual_ab()
        val sp = ScanOnDemandParser(rs)
        val sentence = "b"
        val goal = "S"
        val position = 1

        val actual = sp.expectedTerminalsAt(sentence, position, Agl.parseOptions { goalRuleName(goal) })
        val actStr = actual.map {
            val rhs = it.rhs
            when (rhs) {
                is RuntimeRuleRhsPattern -> rhs.patternUnescaped
                is RuntimeRuleRhsLiteral -> rhs.literalUnescaped
                else -> it.tag
            }
        }.toSet()

        assertEquals(setOf(), actStr)
    }
}