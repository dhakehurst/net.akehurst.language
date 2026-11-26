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
 * A collection property that is not set (e.g. of multiplicity 0..* in a metamodel, and the sentence gives no elements) reads as
 * an empty collection, also when its declared type is not nullable: no error. Here 'tags' is never set by the asm-transform.
 */
class test_ReferenceResolverSimple_unsetCollection {

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
              import std
              data Unit { cmp var items: List<Def> }
              data Def { var name: String?  ref var general: Def  cmp var tags: List<String>  cmp var marks: Set<String>  cmp var ordered: OrderedSet<String> }
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
                in Def { property general refers-to Def when (tags.size == 0) and (marks.size == 0) and (marks.filter({ x -> true }).size == 0) and (ordered.filter({ x -> true }).size == 0) and (ordered.size == 0) }
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
    fun unset_collections_are_empty__no_error() {
        test("def a", emptyList(), emptyList())
    }

    @Test
    fun unset_collections_are_empty__condition_holds_and_reference_resolved() {
        test("def a def b : a", emptyList(), listOf(":Def[a]"))
    }

    @Test
    fun reference_not_found_is_an_error() {
        test("def a def c : x", listOf("Reference 'x' not resolved, to type(s) [Def] in scope '/'"), emptyList())
    }
}
