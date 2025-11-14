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

package net.akehurst.language.parser.leftcorner.listSeparated

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_literal_a1n_followed_by_empty : test_LeftCornerParserAbstract() {

    // S = A B? ;
    // A = ['a'/'.']+ ;
    // B = 'b' ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("A"); ref("oB") }
            sList("A", 1, -1, "'a'", "'.'")
            optional("oB", "B")
            concatenation("B") { literal("b") }
            literal("'.'", ".")
            literal("'a'", "a")
        }
        val goal = "S"
    }

    @Test
    fun empty_fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(0, 1, 1, 1, null), sentence, setOf("<GOAL>"), setOf("'a'"))
            ), issues.errors
        )
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S {
                A{ 'a' }
                oB{ <EMPTY> }
            }
        """

        super.test_pass(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun aa_fails() {
        val sentence = "aa"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1, null), sentence, setOf("<GOAL>"), setOf("<EOT>", "'.'", "'b'"))
            ), issues.errors
        )
    }

    @Test
    fun ap_fails() {
        val sentence = "a."

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(2, 3, 1, 1, null), sentence, setOf("A"), setOf("'a'"))
            ), issues.errors
        )

    }

    @Test
    fun apa() {
        val sentence = "a.a"

        val expected = """
            S {
                A{ 'a' '.' 'a' }
                oB{ <EMPTY> }
            }
        """

        super.test_pass(rrs, goal, sentence, 1, expected)
    }

}