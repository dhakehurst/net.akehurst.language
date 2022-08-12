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

package net.akehurst.language.parser.scanondemand.whitespace

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test

internal class test_ab_WS_COMMENT : test_ScanOnDemandParserAbstract() {

    // skip WS = "\s+" ;
    // skip COMMENT = "//[^\n]*"
    // S = 'a' 'b' ;

    private companion object {
        val rrs = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            pattern("COMMENT", "//[^\\n\\r]*", true)
            concatenation("S") { literal("a"); literal("b") }
        }
        val goal = "S"
    }

    @Test
    fun ab() {
        val sentence = "ab"

        val expected = """
            S { 'a' 'b' }
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
    fun WS_ab() {
        val goal = "S"
        val sentence = " ab"

        val expected = """
            S { WS : ' ' 'a' 'b' }
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
    fun COMMENT_WS_ab() {
        val goal = "S"
        val sentence = """
            // comment
            ab
        """.trimIndent()

        val expected = """
            S {
                COMMENT : '// comment'
                WS : '⏎'
                'a' 'b'
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
    fun ab_WS() {
        val goal = "S"
        val sentence = "ab "

        val expected = """
            S { 'a' 'b' WS : ' ' }
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
    fun WS_ab_WS() {
        val goal = "S"
        val sentence = " ab "

        val expected = """
            S { WS : ' ' 'a' 'b' WS : ' ' }
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
    fun a_WS_b() {
        val goal = "S"
        val sentence = "a b"

        val expected = """
            S { 'a' WS : ' ' 'b' }
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
    fun a_WS_COMMENT_WS_b() {
        val goal = "S"
        val sentence = """
            a //comment
            b
        """.trimIndent()

        val expected = """
            S {
              'a'
              WS : ' '
              COMMENT : '//comment'
              WS : '⏎'
              'b'
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
    fun ab_WS_COMMENT() {
        val goal = "S"
        val sentence = "ab //comment"

        val expected = """
            S {
              'a' 'b'
              WS : ' '
              COMMENT : '//comment'
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