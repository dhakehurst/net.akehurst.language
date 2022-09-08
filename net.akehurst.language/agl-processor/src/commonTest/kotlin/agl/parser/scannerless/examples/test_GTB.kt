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

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

internal class test_GTB : test_ScanOnDemandParserAbstract() {

    /*
     * from [https://ac.els-cdn.com/S1571066104052211/1-s2.0-S1571066104052211-main.pdf?_tid=ebfa8627-2763-446d-b750-084833f9dd4c&acdnat=1548755247_c9590c54393a9cf75f34499780c7b400]
     * The Grammar Tool Box: A Case Study Comparing GLR Parsing Algorithms, Adrian Johnstone, Elizabeth Scott, Giorgios Economopoulos
     *
     * S = 'a' | A B | A 'z' ;
     * A = 'a' ;
     * B = 'b' | <empty> ;
     *
     */

    private companion object {
        val rrs = runtimeRuleSet {
            choice("S",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
                ref("S2")
            }
            concatenation("S1") { ref("A"); ref("B") }
            concatenation("S2") { ref("A"); literal("z") }
            concatenation("A") { literal("a") }
            choice("B",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("b")
                ref("be")
            }
            //empty("be")
            concatenation("be") { empty() }
        }

        val goal = "S"
    }

    @Test
    fun a() {
        val sentence = "a"

        // will parse with S = A B rather than S= 'a'
        // because of priorities, choice 'AB' is higher than choice 'a'
        // S = 'a' | A B | A 'z'
        // if reorganise to be
        // S = A B | 'a' | A 'z'
        // then result is S { 'a' }

        val expected = """
        S { S1 {
          A { 'a' }
          B { be { §empty } }
        } }
        """.trimIndent()

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 2,
                expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun az() {
        val sentence = "az"

        val expected = """
            S|2 {
              S2 { A { 'a' } 'z' }
            }
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
    fun ab() {
        val sentence = "ab"

        val expected = """
            S|1 {
              S1 { A{'a'} B { 'b' } }
            }
        """.trimIndent()

        super.test(
                rrs = rrs,
                goal = goal,
                sentence = sentence,
                expectedNumGSSHeads = 1,
                expectedTrees = arrayOf(expected)
        )
    }
}