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

package net.akehurst.language.parser.scanondemand.listSeparated

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.parser.scanondemand.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class test_literal_a01 : test_LeftCornerParserAbstract() {

    // S = ['a' / ',']?
    private companion object {
        val rrs = runtimeRuleSet {
            sList("S", 0, 1, "'a'", "','")
            literal("','", ",")
            literal("'a'", "a")
        }
        val goal = "S"
    }

    @Test
    fun empty() {
        val sentence = ""

        val expected = "S|1 { <EMPTY_LIST> }"

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun a() {
        val sentence = "a"

        val expected = "S {'a'}"

        super.test(rrs, goal, sentence, 1, expected)
    }

    @Test
    fun aa_fails() {
        val sentence = "aa"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1), "a^a", setOf("<EOT>"))
            ), issues.errors
        )
    }

    @Test
    fun ac_fails() {
        val sentence = "a,"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1), "a^,", setOf("<EOT>"))
            ), issues.errors
        )

    }

    @Test
    fun aca_fails() {
        val sentence = "a,a"

        val (sppt, issues) = super.testFail(rrs, goal, sentence, 1)
        assertNull(sppt)
        assertEquals(
            listOf(
                parseError(InputLocation(1, 2, 1, 1), "a^,a", setOf("<EOT>"))
            ), issues.errors
        )

    }

}