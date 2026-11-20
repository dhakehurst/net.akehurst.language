/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 'import members-of ref' and 'inherit members-of ref' without 'forall': the same as 'forall $self'.
 */
class test_ReferenceResolverSimple_importWithoutForall {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = element* ;
                element = def | sub | use ;
                def = 'def' ID body? ;
                sub = 'sub' ID 'of' ID body? ;
                body = '{' element* '}' ;
                use = 'use' ID ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
            """.trimIndent()
        )

        fun processor(scopes: String) = Agl.processorFromStringSimple(
            grammarDefinitionStr = grammarStr, referenceStr = CrossReferenceString(
                """
                namespace test.Test
                    identify Def by id
                    identify Sub by id
                    $scopes
                    references {
                        in Sub { property id2 refers-to Def }
                        in Use { property id refers-to Def }
                    }
                """.trimIndent()
            )
        ).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        val inheritWithout = processor("scope Def { }\n    scope Sub { inherit members-of id2 }")
        val inheritWith = processor("scope Def { }\n    scope Sub { inherit forall ${'$'}self members-of id2 }")
        val importWithout = processor("scope Def { }\n    scope Sub { import members-of id2 }")
        val importWith = processor("scope Def { }\n    scope Sub { import forall ${'$'}self members-of id2 }")

        fun errors(p: Int, sentence: String): List<String> {
            val proc = listOf(inheritWithout, inheritWith, importWithout, importWith)[p]
            return proc.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } }).allIssues.errors.map { it.message }
        }

        const val GOOD = "def A { def M } sub B of A { use M }"
        const val BAD = "def A { def M } def C { def N } sub B of A { use N }"
    }

    @Test
    fun inherit_without_forall() {
        assertEquals(errors(1, GOOD), errors(0, GOOD))
        assertEquals(emptyList(), errors(0, GOOD))
        assertEquals(1, errors(0, BAD).size, errors(0, BAD).toString())
        assertEquals(errors(1, GOOD), errors(0, GOOD))
        assertEquals(errors(1, BAD), errors(0, BAD))
    }

    @Test
    fun import_without_forall() {
        assertEquals(emptyList(), errors(2, GOOD))
        assertEquals(1, errors(2, BAD).size, errors(2, BAD).toString())
        assertEquals(errors(3, GOOD), errors(2, GOOD))
        assertEquals(errors(3, BAD), errors(2, BAD))
    }

    @Test
    fun asString_omits_forall_self() {
        val str = inheritWith.crossReferenceDomain.asString()
        assertTrue(str.contains("inherit members-of id2"), str)
        assertTrue(importWith.crossReferenceDomain.asString().contains("import members-of id2"), importWith.crossReferenceDomain.asString())
        val again = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = CrossReferenceString(str))
        assertTrue(again.issues.errors.isEmpty(), again.issues.toString())
    }
}
