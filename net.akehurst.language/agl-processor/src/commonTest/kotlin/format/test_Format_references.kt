/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.format.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.api.processor.GrammarString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Formatting an ASM whose references have been resolved:
 * a reference is formatted as the text that refers (e.g. 'a'), not as the element it refers to (e.g. 'def a').
 */
class test_Format_references {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Refs {
                skip leaf WS = "\s+" ;
                unit = decl* ;
                decl = def | use | uses ;
                def = 'def' ID ;
                use = 'use' ref ;
                uses = 'uses' [target / ',']+ ;
                target = ref ;
                leaf ref = ID ;
                leaf ID = "[a-zA-Z]+" ;
            }
            """.trimIndent()
        )

        val referenceStr = CrossReferenceString(
            """
            namespace test.Refs
                identify Def by id
                references {
                    in Use {
                        property ref refers-to Def
                    }
                    in Uses {
                        forall target {
                            property ref refers-to Def
                        }
                    }
                }
            """.trimIndent()
        )

        fun processor(format: String) = Agl.processorFromStringSimple(
            grammarDefinitionStr = grammarStr,
            referenceStr = referenceStr,
            formatterStr = FormatString(format)
        ).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun test(format: String, sentence: String, expected: String = sentence, expectedResolved: Int = 1) {
            val processor = processor(format)
            val result = processor.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
            assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())
            assertEquals(expectedResolved, result.semanticAnalysis?.resolvedReferences?.size, "resolved references")
            val formatted = processor.formatAsm(result.asm!!)
            assertTrue(formatted.issues.errors.isEmpty(), formatted.issues.toString())
            assertEquals(expected, formatted.sentence)
        }
    }

    @Test
    fun property_reference() = test(
        $$"""
            namespace test
            format Refs {
                Unit -> "$[decl sep ' ']"
                Def -> "def $id"
                Use -> "use $ref"
            }
        """.trimIndent(),
        "def a use a"
    )

    @Test
    fun property_reference__embedded() = test(
        $$"""
            namespace test
            format Refs {
                Unit -> "$[decl sep ' ']"
                Def -> "def $id"
                Use -> "use ${ref}"
            }
        """.trimIndent(),
        "def a use a"
    )

    @Test
    fun property_reference__target_before_and_after() = test(
        $$"""
            namespace test
            format Refs {
                Unit -> "$[decl sep ' ']"
                Def -> "def $id"
                Use -> "use $ref"
            }
        """.trimIndent(),
        "use b def a use a def b",
        expectedResolved = 2
    )

    @Test
    fun references_in_list_elements() = test(
        $$"""
            namespace test
            format Refs {
                Unit -> "$[decl sep ' ']"
                Def -> "def $id"
                Use -> "use $ref"
                Uses -> "uses $[target sep ',']"
                Target -> "$ref"
            }
        """.trimIndent(),
        "def a def b uses a,b",
        expectedResolved = 2
    )
}
