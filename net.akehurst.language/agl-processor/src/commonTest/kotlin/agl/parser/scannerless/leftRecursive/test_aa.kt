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

package net.akehurst.language.parser.scanondemand.leftRecursive

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.ParserTerminatedException
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class test_aa : test_ScanOnDemandParserAbstract() {

    // S  = P | 'a' ;
    // P  = P1 | S ;  // S*
    // P1 = P S ;    // S*; try right recursive also
    private companion object {
        val rrs = runtimeRuleSet {
            choice("S",RuntimeRuleChoiceKind.LONGEST_PRIORITY){
                ref("P")
                literal("a")
            }
            choice("P",RuntimeRuleChoiceKind.LONGEST_PRIORITY){
                ref("P1")
                ref("S")
            }
            concatenation("P1") { ref("P"); ref("S") }
        }
        val goal = "S"
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S { 'a' }
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
    fun aa() {
        val sentence = "aa"

        val expected = """
            S { P { P1 {
                P { S { 'a' } }
                S { 'a' }
            } } }
        """.trimIndent()

        assertFailsWith<ParserTerminatedException> {
            super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
            )
        }
    }

    @Test
    fun aaa() {
        val sentence = "aaa"

        val expected = """
            S { P { P1 {
              P { S { 'a' } }
              S { P { P1 {
                P { S { 'a' } }
                S { 'a' }
              } } }
            } } }
        """.trimIndent()

        assertFailsWith<ParserTerminatedException> {
            super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
            )
        }
    }
}