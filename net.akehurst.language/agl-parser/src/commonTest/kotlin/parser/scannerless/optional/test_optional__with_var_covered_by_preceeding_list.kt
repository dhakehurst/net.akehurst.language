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

class test_optional__with_var_covered_by_preceeding_list : test_LeftCornerParserAbstract() {

    // S = 'b' vList optV ;
    // vList = v*;
    // optV = v?;
    // v = [a-z]
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { literal("b"); ref("vList"); ref("kvlOpt") }
            optional("kvlOpt", "kvl")
            concatenation("kvl") { literal("k"); ref("vList")  }
            multi("vList",0,-1,"v")
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
            parseError(InputLocation(0, 1, 1, 1, null),sentence, setOf("<GOAL>"),setOf("'b'"))
        ),issues.errors)
    }

    @Test
    fun b__pass() {
        val sentence = "b"

        val expected = """
            S { 'b' vList {<EMPTY_LIST>} kvlOpt{<EMPTY>} }
        """.trimIndent()

        super.test_pass(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun ba() {
        val sentence = "ba"

        val expected = """
            S { 'b' vList {v:'a'} kvlOpt{<EMPTY>} }
        """.trimIndent()

        super.test_pass(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun baa() {
        val sentence = "baa"

        val expected = """
            S { 'b' vList {v:'a' v:'a'} kvlOpt{<EMPTY>} }
        """.trimIndent()

        super.test_pass(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun bka() {
        val sentence = "bka"

        val expected = """
            S { 'b' vList {<EMPTY_LIST>} kvlOpt{ 'k' v:'a'} }
        """.trimIndent()

        super.test_pass(rrs, goal, sentence, 2, expected)
    }

    @Test
    fun baka() {
        val sentence = "baka"

        val expected = """
             S { 'b'  vList {v:'a'} kvlOpt{ kvl{ 'k' vList { v:'a' } } } }
        """.trimIndent()

        super.test_pass(rrs, goal, sentence, 2, expected)
    }

    @Test
    fun baakaa() {
        val sentence = "baakaa"

        val expected = "S { 'b'  vList {v:'a' v:'a'} kvlOpt{ kvl{ 'k'vList { v:'a' v:'a' } } } }"

        super.test_pass(rrs, goal, sentence, 2, expected)
    }

}