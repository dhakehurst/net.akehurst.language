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

package net.akehurst.language.agl.default

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.asm.asmSimple
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_SemanticAnalyserDefault {

    private companion object {

        fun test(grammarStr: String, sentence: String, expected: AsmSimple) {
            val processor = Agl.processorFromStringDefault(
                grammarStr
            ).processor!!

            val result = processor.process(sentence, Agl.options {
                semanticAnalysis {
                    context(ContextSimple())
                }
            })
            assertTrue(result.issues.isEmpty(), result.issues.toString())
            assertNotNull(result.asm)
            assertEquals(expected.asString("  ", ""), result.asm!!.asString("  ", ""))
        }
    }

    @Test
    fun listOfItems() {
        val grammarStr = """
            namespace test
            grammar test {
                S = L;
                L = I* ;
                I = 'a' | 'b' ;
            }
        """.trimIndent()
        val sentence = """
            aabba
        """.trimIndent()

        val expected = asmSimple {
            element("S") {
                propertyListOfString("l", listOf("a", "a", "b", "b", "a"))
            }
        }

        test(grammarStr, sentence, expected)
    }

    @Test
    fun listOfEmpty() {
        val grammarStr = """
            namespace test
            grammar test {
                S = L;
                L = I* ;
                I =  ;
            }
        """.trimIndent()
        val sentence = ""

        val expected = asmSimple {
            element("S") {
                propertyListOfString("l", listOf())
            }
        }

        test(grammarStr, sentence, expected)
    }

}