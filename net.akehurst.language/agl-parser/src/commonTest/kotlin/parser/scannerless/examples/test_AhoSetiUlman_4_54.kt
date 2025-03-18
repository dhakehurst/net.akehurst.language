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

package net.akehurst.language.parser.leftcorner.examples

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

class test_AhoSetiUlman_4_54 : test_LeftCornerParserAbstract() {

    // S = CC ;
    // C = cC | d ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("C"); ref("C") }
            choice("C", RuntimeRuleChoiceKind.PRIORITY_LONGEST) {
                ref("cC")
                literal("d")
            }
            concatenation("cC") { literal("c"); ref("C") }
        }
        val goal = "S"
    }

    @Test
    fun c_fails() {
        val sentence = "c"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1, null), sentence, setOf("<GOAL>"), setOf("'c'", "'d'"))
            ), issues.errors
        )
    }

    @Test
    fun d_fails() {
        val sentence = "d"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1, null), sentence, setOf("<GOAL>"), setOf("'c'", "'d'"))
            ), issues.errors
        )
    }


    @Test
    fun dd() {
        val sentence = "dd"

        val expected = """
            S { C|1 { 'd' } C|1 { 'd' } }
        """.trimIndent()

        super.test_pass(rrs, goal, sentence, 1, expected)

    }


    @Test
    fun dcd() {
        fail("when converting to String get java.lang.OutOfMemoryError: Java heap space: failed reallocation of scalar replaced objects")
        val sentence = "dcd"

        val expected = """
            S { C { 'd' } C{ C|1 { 'c' C { 'd' } } } }
        """.trimIndent()

        super.test_pass(rrs, goal, sentence, 1, expected)
    }

}