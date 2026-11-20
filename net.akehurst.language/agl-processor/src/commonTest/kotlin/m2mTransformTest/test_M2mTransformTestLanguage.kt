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

package net.akehurst.language.m2mTransformTest.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.contextFromRegistryGrammars
import net.akehurst.language.base.processor.AglBase
import net.akehurst.language.expressions.processor.AglExpressions
import net.akehurst.language.m2mTransform.api.DomainReference
import net.akehurst.language.m2mTransform.api.M2mTransformDomain
import net.akehurst.language.base.api.SimpleName
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_M2mTransformTestLanguage {

    private companion object {
        val m2m: M2mTransformDomain by lazy {
            // parsed only (no semantic analysis), the tests only need the transform names and domains
            val res = Agl.registry.agl.m2mTransform.processor!!.process(
                """
                namespace example
                transform ShapesToDrawing(s:Shapes, d:Drawing) {
                }
                """.trimIndent(),
                Agl.options { semanticAnalysis { enabled(false) } }
            )
            assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
            res.asm!!
        }

        fun process(text: String) = Agl.registry.agl.m2mTransformTest.processor!!.process(
            text,
            Agl.options { semanticAnalysis { sentenceContext(contextFromM2mTransformDomain(m2m)) } }
        )
    }

    @Test
    fun identity() {
        assertEquals("net.akehurst.language.M2mTransformTest", M2mTransformTest.identity.value)
    }

    @Test
    fun process_grammarString_EQ_grammarModel() {
        val res = Agl.registry.agl.grammar.processor!!.process(
            AglBase.grammarString + "\n" + AglExpressions.grammarString + "\n" + M2mTransformTest.grammarString,
            Agl.options {
                semanticAnalysis {
                    sentenceContext(contextFromRegistryGrammars(Agl.registry))
                }
            }
        )
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val actual = res.asm!!.findDefinitionByQualifiedNameOrNull(M2mTransformTest.defaultTargetGrammar.qualifiedName)!!.asString()
        val expected = M2mTransformTest.defaultTargetGrammar.asString()
        assertEquals(expected, actual)
    }

    @Test
    fun transform_resolved_qualified() {
        val res = process(
            """
            namespace example.test
            transform-test ShapesToDrawingTest for example.ShapesToDrawing {
              test-case OneCircle {
                domain s := Circle() { name := 'c' }
                domain d := Disc() { label := 'c' }
              }
            }
            """.trimIndent()
        )
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val test = res.asm!!.allTransformTest.single()
        assertEquals("example.test.ShapesToDrawingTest", test.qualifiedName.value)
        assertEquals("example.ShapesToDrawing", test.transform.resolved?.qualifiedName?.value)
        assertEquals(setOf(DomainReference("s"), DomainReference("d")), test.domainParameters.keys)
        assertEquals(setOf(DomainReference("s"), DomainReference("d")), test.testCase[SimpleName("OneCircle")]!!.domain.keys)
    }

    @Test
    fun transform_resolved_simple_name_by_import() {
        val res = process(
            """
            namespace example.test
              import example
            transform-test ShapesToDrawingTest for ShapesToDrawing {
            }
            """.trimIndent()
        )
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        assertEquals("example.ShapesToDrawing", res.asm!!.allTransformTest.single().transform.resolved?.qualifiedName?.value)
    }

    @Test
    fun transform_not_found() {
        val res = process(
            """
            namespace example.test
            transform-test T for Unknown {
            }
            """.trimIndent()
        )
        assertEquals(listOf("Transform 'Unknown' not found for transform-test 'T'."), res.allIssues.errors.map { it.message })
    }

    @Test
    fun domain_not_of_transform() {
        val res = process(
            """
            namespace example.test
            transform-test T for example.ShapesToDrawing {
              test-case C {
                domain s := 'a'
                domain x := 'b'
              }
            }
            """.trimIndent()
        )
        assertEquals(
            listOf("In test-case 'C' of transform-test 'T', 'x' is not a domain of transform 'example.ShapesToDrawing'."),
            res.allIssues.errors.map { it.message }
        )
    }

    @Test
    fun duplicates() {
        val res = process(
            """
            namespace example.test
            transform-test T for example.ShapesToDrawing {
              test-case C {
                domain s := 'a'
                domain s := 'b'
              }
              test-case C {
                domain s := 'a'
                domain d := 'b'
              }
            }
            transform-test T for example.ShapesToDrawing {
            }
            """.trimIndent()
        )
        assertEquals(
            setOf(
                "test-case 'C' gives domain 's' more than once.",
                "transform-test 'T' already contains a test-case named 'C'.",
                "namespace 'example.test' already contains a transform-test named 'T'."
            ),
            res.allIssues.errors.map { it.message }.toSet()
        )
    }

    @Test
    fun asString() {
        val text = """
            transform-test T for example.ShapesToDrawing {
              test-case C {
                domain s := 'a'
                domain d := 'b'
              }
            }
        """.trimIndent()
        val res = process("namespace example.test\n" + text)
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        assertEquals(text, res.asm!!.allTransformTest.single().asString())
    }
}
