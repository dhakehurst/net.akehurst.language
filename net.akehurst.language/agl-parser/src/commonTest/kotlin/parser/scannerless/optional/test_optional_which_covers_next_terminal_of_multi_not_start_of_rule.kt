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

package net.akehurst.language.parser.leftcorner.multi

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_optional_which_covers_next_terminal_of_multi_not_start_of_rule : test_LeftCornerParserAbstract() {

    // S = 'b' 'a'? As ; As = 'a'+
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("b"); ref("oa"); ref("as") }
            optional("oa", "'a'")
            multi("as",1,-1, "'a'")
            literal( "a")
        }
        val goal = "S"
    }

    @Test
    fun empty() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0, 1, 1, 1, null),sentence, setOf("<GOAL>"),setOf("'b'"))
        ),issues.errors)
    }

    @Test
    fun ba() {
        val sentence = "ba"

        val expected = """
            S { 'b' oa{<EMPTY>} as {'a'} }
        """.trimIndent()

        super.test_pass(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun baa() {
        val sentence = "baa"

        val expected = """
            S { 'b' oa{ 'a' } as {'a'} }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, true, expected)
    }

    @Test
    fun baaa() {
        val sentence = "baaa"

        val expected = """
            S { 'b' oa{ 'a' } as {'a' 'a'} }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, true, expected)
    }

}