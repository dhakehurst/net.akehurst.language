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

class test_optional_covered_by_list_nonTerm_not_at_start_of_rule : test_LeftCornerParserAbstract() {

    // S = 'b' as 'a'? ; vs = v | v vs ; v = [a-z]
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("b");  ref("vs"); ref("oa") }
            optional("oa", "'a'")
            choiceLongest("vs") {
                ref("v")
                ref("vvs")
            }
            concatenation("vvs") {  ref("v"); ref("vs") }
            literal( "a")
            pattern("v","[a-z]")
        }
        val goal = "S"
    }

    @Test
    fun empty__fails() {
        val sentence = ""

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(0,1,1,1),sentence, setOf("<GOAL>"),setOf("'b'"))
        ),issues.errors)
    }

    @Test
    fun b__fails() {
        val sentence = "b"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, expectedNumGSSHeads = 1)
        assertNull(sppt)
        assertEquals(listOf(
            parseError(InputLocation(1,2,1,1),sentence, setOf("<GOAL>"),setOf("v"))
        ),issues.errors)
    }

    @Test
    fun ba() {
        val sentence = "ba"

        val expected = """
            S { 'b' vs {v:'a'} oa{<EMPTY>} }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun baa() {
        val sentence = "baa"

        val expected = """
            S { 'b' vs { vvs { v:'a' vs { v:'a' } } } oa{<EMPTY>} }
        """.trimIndent()

        super.test(rrs, goal, sentence, 2, expected)
    }

    @Test
    fun bxa() {
        val sentence = "bxa"

        // result is based on dynamic-prio
        // prio of vs is tested before prio of oa
        // The dynamic-prio of vs is based on order of choices and vss it higher than v
        // Thus the S-alternative with vs being vss is chosen over the option of oa being non-empty
        val expected = """
            S { 'b' vs { vvs { v:'x' vs { v:'a' } } } oa { <EMPTY> } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 2, expected)
    }

    @Test
    fun bxxa() {
        val sentence = "bxxa"

        // result is based on dynamic-prio
        // prio of vs is tested before prio of oa
        // The dynamic-prio of vs is based on order of choices and vss it higher than v
        // Thus the option with vs being vss is chosen over the option of oa being non-empty
        val expected = """
             S { 'b' vs { vvs { v:'x' vs { v:'x' } } } oa { 'a' } }
        """.trimIndent()

        super.test(rrs, goal, sentence, 2, expected)
    }

    @Test
    fun bxxxa() {
        val sentence = "bxxxa"

        val expected = """
            S { 'b' vs { vvs { v:'x' vs { vvs { v:'x' vs { v:'a' } } } } } oa{<EMPTY>} }
        """.trimIndent()

        super.test(rrs, goal, sentence, 1, expected)
    }

}