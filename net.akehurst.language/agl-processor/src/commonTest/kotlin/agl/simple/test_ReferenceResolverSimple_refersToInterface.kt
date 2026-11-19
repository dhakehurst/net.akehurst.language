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
 * 'refers-to' an interface type (e.g. a metamodel's abstract class 'Element'): every object whose type conforms to the interface
 * (a data type that implements it) is a possible target; an object of another type is not.
 */
class test_ReferenceResolverSimple_refersToInterface {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
              skip leaf WS = "\s+" ;
              unit = decl* ;
              decl = top | sub | kind | other ;
              top = 'def' ID ;
              sub = 'def' ID ':' ID ;
              kind = 'kind' ID ;
              other = 'other' ID ;
              leaf ID = "[a-z]+" ;
            }
            """.trimIndent()
        )
        val typesStr = TypesString(
            """
            namespace test.Test
              interface Decl
              interface Thing : Decl
              data Unit { cmp var items: List<Decl> }
              data Def : Thing { var name: String  ref var general: Thing }
              data Kind : Thing { var name: String }
              data Other : Decl { var name: String }
            """.trimIndent()
        )
        val transformStr = AsmTransformString(
            """
            namespace test
            asm-transform Test {
              unit: Unit() { items := children }
              decl: child[0] as Decl
              top: Def() { name := child[1] }
              sub: Def() { name := child[1]  general := child[3] }
              kind: Kind() { name := child[1] }
              other: Other() { name := child[1] }
            }
            """.trimIndent()
        )
        val referenceStr = CrossReferenceString(
            """
            namespace test.Test
              identify Def by name
              identify Kind by name
              identify Other by name
              references {
                in Def { property general refers-to Thing }
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
    fun refers_to_interface__target_of_one_implementing_type() {
        test("def a def b : a", emptyList(), listOf(":Def[a]"))
    }

    @Test
    fun refers_to_interface__target_of_another_implementing_type() {
        test("kind k def b : k", emptyList(), listOf(":Kind[k]"))
    }

    @Test
    fun refers_to_interface__target_of_a_type_that_does_not_implement_it_is_not_found() {
        test("other o def b : o", listOf("Reference 'o' not resolved, to type(s) [Thing] in scope '/'"), emptyList())
    }
}
