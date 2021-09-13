/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
import net.akehurst.language.agl.regex.regexMatcher
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.*

internal class test_Character_vs_RegEx : test_ScanOnDemandParserAbstract() {


    private companion object {

        val kotlinRegEx = Regex("a*")
        val aglRegex = regexMatcher("a*")

        val regExParser = ScanOnDemandParser(
                runtimeRuleSet {
                    pattern("S", "a*")
                })
        val charParser = ScanOnDemandParser(
                runtimeRuleSet {
                    multi("S", 0, -1, "'a'")
                    literal("'a'", "a")
                })
    }


    @Test
    fun forProfiling() {
        val text = "a".repeat(100000)
        aglRegex.match(text)
    }

    @ExperimentalTime
    @Test
    fun a10() {

        val goal = "S"
        val text = "a".repeat(10)

        // warm up the processors
        regExParser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
        charParser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)

        // measure
        val timeRegEx = TimeSource.Monotonic.measureTimedValue {
            regExParser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
        }
        val timeChar = TimeSource.Monotonic.measureTimedValue {
            charParser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
        }

        assertEquals("S", timeRegEx.value.root.name)
        assertEquals("S", timeChar.value.root.name)

        println("- 10 -")
        println("regEx = ${timeRegEx.duration}")
        println("char = ${timeChar.duration}")
    }

    @ExperimentalTime
    @Test
    fun a100() {

        val goal = "S"
        val text = "a".repeat(100)

        // warm up the processors
        regExParser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
        charParser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)

        val timeRegEx = TimeSource.Monotonic.measureTimedValue {
            regExParser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
        }
        val timeChar = TimeSource.Monotonic.measureTimedValue {
            charParser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
        }

        assertEquals("S", timeRegEx.value.root.name)
        assertEquals("S", timeChar.value.root.name)

        println("- 100 -")
        println("regEx = ${timeRegEx.duration}")
        println("char = ${timeChar.duration}")
    }

    @ExperimentalTime
    @Test
    fun a1000() {

        val goal = "S"
        val text = "a".repeat(1000)

        // warm up the processors
        regExParser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
        charParser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)

        val timeRegEx = TimeSource.Monotonic.measureTimedValue {
            regExParser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
        }
        val timeChar = TimeSource.Monotonic.measureTimedValue {
            charParser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
        }

        assertEquals("S", timeRegEx.value.root.name)
        assertEquals("S", timeChar.value.root.name)

        println("- 1000 -")
        println("regEx = ${timeRegEx.duration}")
        println("char = ${timeChar.duration}")
    }

    @ExperimentalTime
    @Test
    fun a10000() {

        val goal = "S"
        val text = "a".repeat(10000)

        // warm up the processors
        println("warmup")
        for (i in 0 until 20) {
            kotlinRegEx.matches(text)
            aglRegex.match(text, 0)
            regExParser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
            charParser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
        }

        val timeRegExKotlinList = mutableListOf<Duration>()
        val timeRegExAglList = mutableListOf<Duration>()
        val timeRegExParserList = mutableListOf<Duration>()
        val timeCharParserList = mutableListOf<Duration>()
        val count = 50
        println("measure")
        for (i in 0 until count) {
            val timeRegExKotlin = TimeSource.Monotonic.measureTimedValue {
                kotlinRegEx.matches(text)
            }
            val timeRegExAgl = TimeSource.Monotonic.measureTimedValue {
                aglRegex.match(text, 0)
            }
            val timeRegExParser = TimeSource.Monotonic.measureTimedValue {
                regExParser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
            }
            val timeCharParser = TimeSource.Monotonic.measureTimedValue {
                charParser.parseForGoal(goal, text, AutomatonKind.LOOKAHEAD_1)
            }
            assertEquals("S", timeRegExParser.value.root.name)
            assertEquals("S", timeCharParser.value.root.name)
            timeRegExKotlinList.add(timeRegExKotlin.duration)
            timeRegExAglList.add(timeRegExAgl.duration)
            timeRegExParserList.add(timeRegExParser.duration)
            timeCharParserList.add(timeCharParser.duration)
        }

        println("- 10000 -")
        println("regExKotlinList = ${timeRegExKotlinList}")
        println("regExAglList = ${timeRegExAglList}")
        println("regExParser = ${timeRegExParserList}")
        println("charParser = ${timeCharParserList}")

        println("regExKotlin = ${timeRegExKotlinList.sumBy { it.toInt(DurationUnit.MICROSECONDS) }/count}")
        println("regExAgl = ${timeRegExAglList.sumBy { it.toInt(DurationUnit.MICROSECONDS) }/count}")
        println("regExParser = ${timeRegExParserList.sumBy { it.toInt(DurationUnit.MICROSECONDS) }/count}")
        println("charParser = ${timeCharParserList.sumBy { it.toInt(DurationUnit.MICROSECONDS) }/count}")
    }
}