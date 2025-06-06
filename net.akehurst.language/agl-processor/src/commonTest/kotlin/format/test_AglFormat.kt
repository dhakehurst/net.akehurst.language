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

package net.akehurst.language.format.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.format.test.FormatModelTest
import net.akehurst.language.formatter.api.AglFormatModel
import net.akehurst.language.grammarTypemodel.builder.grammarTypeModel
import net.akehurst.language.typemodel.test.TypeModelTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_AglFormat {

    private companion object {
        data class TestData(
            val sentence: String,
            val expectedAsm: AglFormatModel? = null
        )

        val testData = listOf(
            TestData(
                sentence = """
                    namespace test
                    format Test {
                        Type -> ''
                    }
                """.trimIndent()
            ),
            TestData(
                sentence = """
                    // single line comment
                    namespace test
                    format Test {
                        Type -> ''
                    }
                """
            ),
            TestData(
                sentence = """
                    /* multi
                       line
                       comment
                    */
                    namespace test
                    format Test {
                        Type -> ''
                    }
                """.trimIndent()
            ),
            TestData(
                sentence = """
                    namespace test
                    format Test {
                        Type1 -> ''
                        Type2 -> ''
                        Type3 -> ''
                    }
                """.trimIndent()
            ),
            TestData(
                sentence = $$"""
                    namespace test
                    format Test {
                        Type1 -> when {
                          true -> ''
                          false -> ''
                          else -> $nothing
                        }
                    }
                """.trimIndent()
            ),
            TestData(
                sentence = """
                    namespace test
                    format Test {
                        Type1 -> ""
                    }
                """.trimIndent()
            ),
            TestData(
                sentence = """
                    namespace test
                    format Test {
                        Type1 -> "He said \"boo\" to me!"
                    }
                """.trimIndent()
            ),
            TestData(
                sentence = """
                    namespace test
                    format Test {
                        Type1 -> "§prop"
                    }
                """.trimIndent().replace("§", "\$")
            ),
            TestData(
                sentence = """
                    namespace test
                    format Test {
                        Type1 -> "§prop §prop §prop"
                    }
                """.trimIndent().replace("§", "\$")
            ),
            TestData(
                sentence = """
                    namespace test
                    format Test {
                        Type1 -> "§{prop} §{prop.re.sdga}"
                    }
                """.trimIndent().replace("§", "\$")
            )
        )

        private fun test_process(data: TestData) {
            val result = Agl.registry.agl.format.processor!!.process(data.sentence)
            assertNotNull(result.asm, result.allIssues.toString())
            assertTrue(result.allIssues.errors.isEmpty(), "'${data.sentence}'\n${result.allIssues}")
            data.expectedAsm?.let {
                FormatModelTest.assertEqual(it, result.asm)
            }
        }
    }

    @Test
    fun check_grammar() {
        val proc = Agl.registry.agl.format.processor
        assertTrue(Agl.registry.agl.format.issues.errors.isEmpty(), Agl.registry.agl.format.issues.toString())
        assertNotNull(proc)
    }

    @Test
    fun check_typeModel() {
        val actual = Agl.registry.agl.format.processor!!.typesModel
        val expected = grammarTypeModel("net.akehurst.language.agl.AglFormat", "AglFormat") {
            //unit = ruleList ;
            //ruleList = [formatRule]* ;
            //formatRule = typeReference '->' formatExpression ;
            //formatExpression
            // = stringExpression
            // | whenExpression
            // ;
        }

        TypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test
    fun parse() {
        val processor = Agl.registry.agl.format.processor!!
        for (td in testData) {
            println("Parsing '${td.sentence}'")
            val result = processor.parse(td.sentence)
            assertTrue(result.issues.errors.isEmpty(), "'${td.sentence}'\n${result.issues}")
        }
    }

    @Test
    fun process() {
        for (td in testData) {
            println("Processing '${td.sentence}'")
            test_process(td)
        }
    }

}