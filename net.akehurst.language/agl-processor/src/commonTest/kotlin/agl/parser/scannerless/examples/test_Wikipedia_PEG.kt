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

package net.akehurst.language.parser.scanondemand.examples

import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.AutomatonKind
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource
import kotlin.time.measureTime

internal class test_Wikipedia_PEG : test_ScanOnDemandParserAbstract() {

    /**
     * S = x S x | x ;
     */

    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { literal("x"); ref("S"); literal("x") }
                concatenation { literal("x") }
            }
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),"^",setOf("'x'"))
        ),issues.error)
    }

    @Test
    fun x() {
        val sentence = "x"

        val expected = """
            S { 'x' }
        """.trimIndent()

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun xx_fails() {
        val sentence = "xx"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(2,3,1,1),"xx^",setOf("'x'"))
        ),issues.error)
    }

    @Test
    fun xxx() {
        val sentence = "xxx"

        val expected = """
            S { 'x' S { 'x' } 'x' }
        """.trimIndent()

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun xxxx() {
        val sentence = "xxxx"

        val (sppt,issues)=super.testFail(rrs, goal, sentence,1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(4,5,1,1),"xxxx^",setOf("'x'"))
        ),issues.error)
    }

    @Test
    fun a9() {
        val sentence = "x".repeat(9)

        val expected = "S { 'x' ".repeat(5) + "} 'x' ".repeat(4) + "}"

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }

    @ExperimentalTime
    //@Test
    fun time() {
        val parser = ScanOnDemandParser(rrs)
        val times = mutableListOf<Duration>()

        for (i in 1..25) {
            val text = "a".repeat(i)
            //warm up
            parser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
            //time it
            val time = TimeSource.Monotonic.measureTime {
                parser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
            }
            times.add(time)
        }

        times.forEach {
            println(it.inWholeMilliseconds.toInt())
        }
    }
}