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

package net.akehurst.language.parser.leftcorner

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_pattern__a_z : test_LeftCornerParserAbstract() {

    //  S = "a"
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { pattern("[a-c]") }
        }
        val goal = "S"
    }


    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S{ "[a-c]":'a' }
        """.trimIndent()

        super.test_pass(
            rrs = rrs,
            goal= goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expected
        )
    }

    @Test
    fun b() {
        val sentence = "b"

        val expected = """
            S{ "[a-c]":'b' }
        """.trimIndent()

        super.test_pass(
            rrs = rrs,
            goal= goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expected
        )
    }

    @Test
    fun pattern_a2c_a_c() {
        val sentence = "c"

        val expected = """
            S{ "[a-c]":'c' }
        """.trimIndent()

        super.test_pass(
            rrs = rrs,
            goal= goal,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            expected
        )
    }

    @Test
    fun pattern_a2c_a_d_fails() {
        val sentence = "d"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError( InputLocation(0,1,1,1),sentence, setOf("<GOAL>"), setOf("\"[a-c]\""))
        ),issues.errors)
    }
}