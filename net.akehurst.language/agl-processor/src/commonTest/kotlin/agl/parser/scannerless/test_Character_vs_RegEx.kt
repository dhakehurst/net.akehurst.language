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
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime
import kotlin.time.MonoClock
import kotlin.time.measureTimedValue

class test_Character_vs_RegEx : test_ScanOnDemandParserAbstract() {


    companion object {
        val regExGrammarStr = """
            namespace test
            grammar regEx {
               S = "[a]*" ;
            }
        """;
        val charGrammarStr = """
            namespace test
            grammar regEx {
               S = 'a'* ;
            }
        """;

        val regExParser = ScanOnDemandParser(
                runtimeRuleSet {
                    pattern("S", "[a]*")
                })
        val charParser = ScanOnDemandParser(
                runtimeRuleSet {
                    multi("S", 0, -1,"'a'")
                    literal("'a'","a")
                })
    }

    @ExperimentalTime
    @Test
    fun a10() {

        val goal = "S"
        val text = "a".repeat(10)

        // warm up the processors
        regExParser.parse(goal, text)
        charParser.parse(goal, text)

        // measure
        val timeRegEx = MonoClock.measureTimedValue {
            regExParser.parse(goal, text)
        }
        val timeChar = MonoClock.measureTimedValue {
            charParser.parse(goal, text)
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
        regExParser.parse(goal, text)
        charParser.parse(goal, text)

        val timeRegEx = MonoClock.measureTimedValue {
            regExParser.parse(goal, text)
        }
        val timeChar = MonoClock.measureTimedValue {
            charParser.parse(goal, text)
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
        regExParser.parse(goal, text)
        charParser.parse(goal, text)

        val timeRegEx = MonoClock.measureTimedValue {
            regExParser.parse(goal, text)
        }
        val timeChar = MonoClock.measureTimedValue {
            charParser.parse(goal, text)
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
        regExParser.parse(goal, text)
        charParser.parse(goal, text)

        val timeRegEx = MonoClock.measureTimedValue {
            regExParser.parse(goal, text)
        }
        val timeChar = MonoClock.measureTimedValue {
            charParser.parse(goal, text)
        }

        assertEquals("S", timeRegEx.value.root.name)
        assertEquals("S", timeChar.value.root.name)

        println("- 10000 -")
        println("regEx = ${timeRegEx.duration}")
        println("char = ${timeChar.duration}")
    }
}