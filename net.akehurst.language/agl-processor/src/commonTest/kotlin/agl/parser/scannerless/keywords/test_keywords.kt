/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.parser.scanondemand.keywords

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.ScannerKind
import net.akehurst.language.parser.scanondemand.test_LeftCornerParserAbstract
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class test_keywords : test_LeftCornerParserAbstract() {

    private companion object {
        val rrs = runtimeRuleSet {
            pattern("WS", "\\s+", true)
            concatenation("S") { literal("class"); ref("NAME"); literal(";") }
            pattern("NAME", "[a-zA-Z]+")
        }
        val goal = "S"
    }

    @Test
    fun scanOnDemand_class_A() {
        val sentence = "class A;"

        val expected = """
            S {
             'class' WS:' '
             NAME:'A'
             ';'
            }
        """.trimIndent()

        testWithOptions(
            rrs = rrs,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            options = Agl.options {
                parse { goalRuleName(goal) }
            },
            scannerKind = ScannerKind.OnDemand,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun scanOnDemand_class_class() {
        val sentence = "class class;"

        val expected = """
            S {
             'class' WS:' '
             NAME:'class'
             ';'
            }
        """.trimIndent()

        testWithOptions(
            rrs = rrs,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            options = Agl.options {
                parse { goalRuleName(goal) }
            },
            scannerKind = ScannerKind.OnDemand,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun scanClassic_class_A() {
        val sentence = "class A;"

        val expected = """
            S {
             'class' WS:' '
             NAME:'A'
             ';'
            }
        """.trimIndent()

        testWithOptions(
            rrs = rrs,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            options = Agl.options {
                parse { goalRuleName(goal) }
            },
            scannerKind = ScannerKind.Classic,
            expectedTrees = arrayOf(expected)
        )
    }

    @Test
    fun scanClassic_class_class__fails() {
        val sentence = "class class;"

        val expectedIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, //TODO: should this be SCAN ?
                InputLocation(6, 7, 1, 1),
                "class ^class;", setOf("NAME")
            )
        )

        val result = testFailWithOptions(
            rrs = rrs,
            sentence = sentence,
            expectedNumGSSHeads = 1,
            options = Agl.options {
                parse { goalRuleName(goal) }
            },
            scannerKind = ScannerKind.Classic,
        )

        assertTrue(result.issues.errors.isNotEmpty(), result.issues.toString())
        assertEquals(expectedIssues, result.issues.all.toList(), result.issues.toString())
    }

}