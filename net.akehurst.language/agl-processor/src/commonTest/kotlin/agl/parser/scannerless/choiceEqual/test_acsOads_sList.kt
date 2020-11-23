/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.parser.scanondemand.choiceEqual

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetBuilder
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.scanondemand.test_ScanOnDemandParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class test_acsOads_sList : test_ScanOnDemandParserAbstract() {

    // S = acs | ads
    // acs = [a / 'c' ]+
    // ads = [a / 'd' ]+
    private val rrs = runtimeRuleSet {
        choice("S",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("acs")
            ref("ads")
        }
        sList("acs",1,-1,"'a'","'c'")
        sList("ads",1,-1,"'a'","'d'")
        literal("'a'","a")
        literal("'c'","c")
        literal("'d'","d")
    }

    @Test
    fun empty_fails() {
        val goal = "S"
        val sentence = ""

        val ex = assertFailsWith(ParseFailedException::class) {
            super.test(rrs, goal, sentence)
        }
        assertEquals(1, ex.location.line)
        assertEquals(1, ex.location.column)
        assertEquals(setOf("'a'"), ex.expected)
    }

    @Test
    fun aca() {
        val goal = "S"
        val sentence = "aca"

        val expected = """
            S {
                acs { 'a' 'c' 'a' }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, expected)
    }

    @Test
    fun ada() {
        val goal = "S"
        val sentence = "ada"

        val expected = """
            S|1 {
                ads { 'a' 'd' 'a' }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, expected)
    }

    @Test
    fun a() {
        val goal = "S"
        val sentence = "a"

        val expected = """
            S|1 {
                ads { 'a' }
            }
        """.trimIndent()

        super.test(rrs, goal, sentence, expected)
    }
}