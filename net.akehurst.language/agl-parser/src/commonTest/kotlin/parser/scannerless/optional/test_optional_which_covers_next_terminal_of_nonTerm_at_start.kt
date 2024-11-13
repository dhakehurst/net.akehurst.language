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

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.test_LeftCornerParserAbstract
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class test_optional_which_covers_next_terminal_of_nonTerm_at_start : test_LeftCornerParserAbstract() {

    // S = 'a'? as ; as = 'a' | 'a' as ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("oa"); ref("as") }
            optional("oa", "'a'")
            choiceLongest("as") {
                ref("'a'")
                ref("aas")
            }
            concatenation("aas") {  ref("'a'"); ref("as") }
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
            parseError(InputLocation(0,1,1,1),sentence, setOf("<GOAL>"),setOf("'a'"))
        ),issues.errors)
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = """
            S { oa{<EMPTY>} as {'a'} }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun aa() {
        val sentence = "aa"

        val expected = """
            S { oa{ 'a' } as {'a'} }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, true, expected)
    }

    @Test
    fun aaa() {
        val sentence = "aaa"

        val expected = """
            S { oa{ 'a' } as { aas { 'a' as { 'a' } } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun aaaa() {
        val sentence = "aaaa"

        val expected = """
            S { oa{ 'a' } as { aas { 'a' as { aas { 'a' as { 'a' } } } } } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

}