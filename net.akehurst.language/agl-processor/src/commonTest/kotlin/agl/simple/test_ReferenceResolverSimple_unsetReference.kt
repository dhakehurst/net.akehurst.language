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

package net.akehurst.language.agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.AsmTransformString
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.TypesString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A referring property that is not set (an optional reference the sentence does not give) has nothing to resolve:
 * it gives no error. The name of a reference is put by the asm-transform into the (typed) reference property itself.
 */
class test_ReferenceResolverSimple_unsetReference {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
              skip leaf WS = "\s+" ;
              unit = decl* ;
              decl = top | sub ;
              top = 'def' ID ;
              sub = 'def' ID ':' ID ;
              leaf ID = "[a-z]+" ;
            }
            """.trimIndent()
        )
        val typesStr = TypesString(
            """
            namespace test.Test
              data Unit { cmp var items: List<Def> }
              data Def { var name: String?  ref var general: Def? }
            """.trimIndent()
        )
        val transformStr = AsmTransformString(
            """
            namespace test
            asm-transform Test {
              unit: Unit() { items := children }
              decl: child[0] as Def
              top: Def() { name := child[1] }
              sub: Def() { name := child[1]  general := child[3] }
            }
            """.trimIndent()
        )
        val referenceStr = CrossReferenceString(
            """
            namespace test.Test
              identify Def by name
              references {
                in Def { property general refers-to Def }
              }
            """.trimIndent()
        )
        val processor = Agl.processorFromStringSimple(grammarStr, typesStr, transformStr, referenceStr).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun test(sentence: String, expectedErrors: List<String>, expectedTargets: List<String>) {
            val result = processor.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
            assertTrue(result.allIssues.errors.none { it.phase.name != "SEMANTIC_ANALYSIS" }, result.allIssues.toString())
            assertEquals(expectedErrors, result.allIssues.errors.map { it.message })
            assertEquals(expectedTargets, result.semanticAnalysis?.resolvedReferences?.map { it.target.toString() } ?: emptyList())
        }
    }

    @Test
    fun unset_reference_gives_no_error() {
        test("def a", emptyList(), emptyList())
    }

    @Test
    fun set_reference_is_resolved() {
        test("def a def b : a", emptyList(), listOf(":Def[a]"))
    }

    @Test
    fun set_reference_not_found_is_an_error() {
        test("def a def c : x", listOf("Reference 'x' not resolved, to type(s) [Def] in scope '/'"), emptyList())
    }
}
