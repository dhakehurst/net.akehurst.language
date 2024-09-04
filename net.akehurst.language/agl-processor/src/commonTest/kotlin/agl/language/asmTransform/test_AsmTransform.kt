/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.language.asmTransform

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.grammarTypeModel.grammarTypeModel
import net.akehurst.language.agl.language.asmTransform.test.AsmTransformModelTest
import net.akehurst.language.agl.language.typemodel.test.TypeModelTest
import net.akehurst.language.api.language.asmTransform.TransformModel
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_AsmTransform {

    companion object {
        data class TestData(
            val testName: String,
            val sentence: String,
            val expectedAsm: List<TransformModel> = emptyList()
        )

        val testData = listOf(
            TestData(
                testName = "single create rule no assignments",
                sentence = """
                    namespace test
                    transform Test {
                        rule1 : Type
                    }
                """.trimIndent()
            ),
            TestData(
                testName = "single line comment",
                sentence = """
                    // single line comment
                    namespace test
                    transform Test {
                        rule1 : Type
                    }
                """.trimIndent()
            ),
            TestData(
                testName = "multi line comment",
                sentence = """
                    /* multi
                       line
                       comment
                    */
                    namespace test
                    transform Test {
                        rule1 : Type
                    }
                """.trimIndent()
            ),
            TestData(
                testName = "multiple create rules",
                sentence = """
                    namespace test
                    transform Test {
                        rule1: Type1
                        rule2: Type2
                        rule3: Type3
                    }
                """.trimIndent()
            ),
            TestData(
                testName = "single modify rules, single assignment",
                sentence = """
                    namespace test
                    transform Test {
                        rule1: { Type1 -> prop := nonTerminal }
                    }
                """.trimIndent()
            ),
            TestData(
                testName = "multiple modify rules, single assignment",
                sentence = """
                    namespace test
                    transform Test {
                        rule1: { Type1 -> prop1 := nonTerminal }
                        rule2: { Type1 -> prop2 := nonTerminal }
                        rule3: { Type1 -> prop3 := nonTerminal }
                    }
                """.trimIndent()
            ),
            TestData(
                testName = "single modify rules, multiple assignment",
                sentence = """
                    namespace test
                    transform Test {
                        rule1: { Type1 ->
                          prop1 := nonTerminal
                          prop2 := nonTerminal
                          prop3 := nonTerminal
                        }
                    }
                """.trimIndent()
            ),
            TestData(
                testName = "single create rule with one assignment",
                sentence = """
                    namespace test
                    transform Test {
                        rule1 : Type { prop1 := nonTerminal }
                    }
                """.trimIndent()
            ),
        )

        private fun test_process(data: TestData) {
            val result = Agl.registry.agl.asmTransform.processor!!.process(data.sentence)
            assertNotNull(result.asm, result.issues.toString())
            assertTrue(result.issues.errors.isEmpty(), "'${data.sentence}'\n${result.issues}")
            data.expectedAsm.forEachIndexed { idx, it ->
                AsmTransformModelTest.trAssertEquals(it, result.asm!!)
            }
        }

    }

    @Test
    fun check_grammar() {
        val proc = Agl.registry.agl.asmTransform.processor
        assertTrue(Agl.registry.agl.asmTransform.issues.isEmpty(), Agl.registry.agl.asmTransform.issues.toString())
        assertNotNull(proc)
    }

    @Test
    fun check_typeModel() {
        val actual = Agl.registry.agl.asmTransform.processor!!.typeModel
        val expected = grammarTypeModel("net.akehurst.language.agl", "AsmTransform") {
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
        val processor = Agl.registry.agl.asmTransform.processor!!
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