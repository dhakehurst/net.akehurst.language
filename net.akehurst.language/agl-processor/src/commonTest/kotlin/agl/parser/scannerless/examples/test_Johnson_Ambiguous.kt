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

package net.akehurst.language.parser.scannerless.examples

import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItemKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.parser.scannerless.test_ScannerlessParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.time.*

class test_Johnson_Ambiguous : test_ScannerlessParserAbstract() {
    /**
     * S = S S S || S S || 'a' ;
     */
    /**
     * S = S1 || S2 || 'a' ;
     * S1 = S S S ;
     * S2 = S S ;
     */
    private fun S(): RuntimeRuleSetBuilder {
        val rrb = RuntimeRuleSetBuilder()
        val ra = rrb.literal("a")
        val rS = rrb.rule("S").build()
        val rS1 = rrb.rule("S1").concatenation(rS, rS, rS)
        val rS2 = rrb.rule("S2").concatenation(rS, rS)
        rS.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CHOICE, RuntimeRuleChoiceKind.AMBIGUOUS, -1, 0, arrayOf(rS1, rS2, ra))
        return rrb
    }

    @Test
    fun empty() {
        val rrb = this.S()
        val goal = "S"
        val sentence = ""

        val e = assertFailsWith(ParseFailedException::class) {
            super.test(rrb, goal, sentence)
        }
        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun a() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a"

        val expected1 = """
            S|2 { 'a' }
        """.trimIndent()

        super.test(rrb.ruleSet(), goal, sentence, expected1)

        val sm = rrb.ruleSet().printUsedAutomaton(goal)
        println(sm)
    }

    @Test
    fun aa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aa"

        val expected1 = """
            S|1 {
              S2 {
                S|2 { 'a' }
                S|2 { 'a' }
              }
            }
        """.trimIndent()

        super.test(rrb, goal, sentence, expected1)
    }

    @Test
    fun aaa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aaa"

        val expected1 = """
            S {
              S1 {
                S|2 { 'a' }
                S|2 { 'a' }
                S|2 { 'a' }
              }
            }
        """.trimIndent()


        super.testStringResult(rrb, goal, sentence, expected1)
    }

    @Test
    fun aaaa() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "aaaa"

        val expected1 = """
         S|1 { S2 {
            S|1 { S2 {
                S|2 { 'a' }
                S|2 { 'a' }
              } }
            S|1 { S2 {
                S|2 { 'a' }
                S|2 { 'a' }
              } }
          } }
        """.trimIndent()


        super.testStringResult(rrb, goal, sentence, expected1)
    }

    @Test
    fun a10() {
        val rrb = this.S()
        val goal = "S"
        val sentence = "a".repeat(10)

        val expected1 = """
            S {
              S1 {
                S { 'a' }
                S { 'a' }
                S { 'a' }
              }
            }
        """.trimIndent()


       // super.testStringResult(rrb, goal, sentence, expected1)
        val p = ScanOnDemandParser(rrb.ruleSet())
        p.parse(goal, sentence)
        val sm = rrb.ruleSet().printUsedAutomaton(goal)
        println(sm)

    }

    @ExperimentalTime
    @Test
    fun time() {
        val parser = ScanOnDemandParser(this.S().ruleSet())
        val times = mutableListOf<Duration>()
        val goal = "S"

        for (i in 1..25) {
            val text = "a".repeat(i)
            //warm up
            parser.parse(goal, text)
            //time it
            val time = TimeSource.Monotonic.measureTime {
                parser.parse(goal, text)
            }
            times.add(time)
        }

        times.forEach {
            println(it.inMilliseconds.toInt())
        }
    }
}