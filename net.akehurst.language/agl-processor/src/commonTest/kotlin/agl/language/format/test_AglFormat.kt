/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.agl.grammar.format

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.format.test.FormatModelTest
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.formatter.formatModel
import net.akehurst.language.typemodel.api.typeModel
import net.akehurst.language.typemodel.test.TypeModelTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_AglFormat {

    private companion object {
        val aglProc = Agl.registry.agl.formatter.processor!!
    }

    private fun test(sentence: String, expected: AglFormatterModel) {
        val result = aglProc.process(sentence)
        assertNotNull(result.asm, result.issues.toString())
        assertTrue(result.issues.isEmpty())
        FormatModelTest.assertEqual(expected, result.asm)
    }

    @Test
    fun typeModel() {
        val actual = aglProc.typeModel
        val expected = typeModel("AglFormat", true) {
            namespace("net.akehurst.language.agl") {
                //unit = ruleList ;
                //ruleList = [formatRule]* ;
                //formatRule = typeReference '->' formatExpression ;
                //formatExpression
                // = stringExpression
                // | whenExpression
                // ;
            }
        }

        TypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test
    fun single_line_comment() {

        val sentence = """
            // single line comment
        """.trimIndent()

        val expected = formatModel {
        }

        test(sentence, expected)

    }

    @Test
    fun multi_line_comment() {

        val sentence = """
            /* multi
               line
               comment
            */
        """.trimIndent()

        val expected = formatModel {
        }

        test(sentence, expected)
    }

    @Test
    fun one_rule_literal_empty() {
        val sentence = """
            Type -> ''
        """
        val expected = formatModel {
            rule("Type") {
                literalString("")
            }
        }

        test(sentence, expected)
    }

}