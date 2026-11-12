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
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reference DSL: '$container' is the element that contains the current element in the ASM.
 * Here the visibility is written on a 'member' wrapper around the element (as in SysML),
 * so 'identify' takes it from the container.
 */
class test_ReferenceResolverSimple_container {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = member* ;
                member = visibility? element ;
                element = package | def | use ;
                package = 'package' ID '{' member* '}' ;
                def = 'def' ID params? ;
                params = '(' param* ')' ;
                param = 'in' ID ;
                use = 'use' ID ':' qname ;
                qname = [ID / '::']+ ;
                leaf visibility = 'private' | 'protected' | 'public' ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
            """.trimIndent()
        )
        val referenceStr = CrossReferenceString(
            """
            namespace test.Test
                identify Package by id visibility ${'$'}container.visibility
                identify Def by id visibility ${'$'}container.visibility
                identify Use by id visibility ${'$'}container.visibility
                identify Param by id

                scope Package { }
                scope Def { }

                references {
                    in Use { property qname refers-to Def|Param }
                }
            """.trimIndent()
        )
        val processorResult = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = referenceStr)
        val processor = processorResult.let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun test(sentence: String, expectedErrors: List<String> = emptyList()) {
            val result = processor.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
            assertTrue(result.allIssues.errors.none { it.phase.name != "SEMANTIC_ANALYSIS" }, result.allIssues.toString())
            assertEquals(expectedErrors, result.allIssues.errors.map { it.message })
        }
    }

    @Test
    fun public_member__visible_outside() = test("package A { def X } use u : A::X")

    @Test
    fun explicit_public_member__visible_outside() = test("package A { public def X } use u : A::X")

    @Test
    fun private_member__not_visible_outside() = test(
        "package A { private def X } use u : A::X",
        listOf("Reference 'A.X' not resolved, to type(s) [Def, Param] in scope '/'")
    )

    @Test
    fun private_member__visible_inside() = test("package A { private def X use u : X }")

    @Test
    fun private_package__not_visible_outside_its_owner() = test(
        "package A { private package B { def X } } use u : A::B::X",
        listOf("Reference 'A.B.X' not resolved, to type(s) [Def, Param] in scope '/'")
    )

    @Test
    fun element_without_container_property__public() = test("def F ( in p ) use u : F::p")
}
