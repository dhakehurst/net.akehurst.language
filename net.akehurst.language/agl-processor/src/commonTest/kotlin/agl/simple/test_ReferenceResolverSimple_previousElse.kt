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
 * Reference DSL: 'from \$previous.p else <navigation>'.
 * In an expression chain 'f.h.k' the base 'f' is an expression and the rest is a list;
 * the first element of the list is looked up in the members of what the base refers to.
 */
class test_ReferenceResolverSimple_previousElse {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = def* ;
                def = 'def' ID '{' feat* '}' ;
                feat = 'feat' ID typing? value? ;
                typing = ':' qualifiedName ;
                value = '=' expr ;
                expr = chain | base ;
                chain = base '.' [qualifiedName / '.']+ ;
                base = ref | new ;
                ref = qualifiedName ;
                new = 'new' ID '(' ')' ;
                qualifiedName = name ;
                name = [ID / '::']+ ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
            """.trimIndent()
        )
        val referenceStr = CrossReferenceString(
            """
            namespace test.Test
                identify Def by id
                identify Feat by id

                scope Def { }
                scope Feat {
                    inherit forall typing.qualifiedName members-of name
                }

                references {
                    in QualifiedName { property name refers-to Def|Feat }
                    in Chain {
                        forall qualifiedName { property name refers-to Feat from ${'$'}previous.name else base.qualifiedName.name }
                    }
                }
            """.trimIndent()
        )
        val processorResult = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = referenceStr)
        val processor = processorResult.let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun test(sentence: String, expectedErrors: List<String> = emptyList()): List<String> {
            val result = processor.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
            assertTrue(result.allIssues.errors.none { it.phase.name != "SEMANTIC_ANALYSIS" }, result.allIssues.toString())
            assertEquals(expectedErrors, result.allIssues.errors.map { it.message })
            return result.semanticAnalysis?.resolvedReferences?.map { it.target.toString() } ?: emptyList()
        }
    }

    @Test
    fun reference_dsl_asString_has_else() {
        val asString = processor.crossReferenceDomain.asString()
        assertTrue(asString.contains("from \$previous.name else base.qualifiedName.name"), asString)
        val again = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = CrossReferenceString(asString))
        assertTrue(again.issues.errors.isEmpty(), "$asString\n${again.issues}")
        assertEquals(asString, again.processor!!.crossReferenceDomain.asString())
    }

    @Test
    fun chain__first_element_in_members_of_base() {
        val targets = test(
            """
            def D { feat h }
            def C { feat f : D  feat g = f.h }
            """.trimIndent()
        )
        assertTrue(targets.any { it.contains("D.h") }, targets.toString())
    }

    @Test
    fun chain__three_parts() {
        test(
            """
            def E { feat k }
            def D { feat h : E }
            def C { feat f : D  feat g = f.h.k }
            """.trimIndent()
        )
    }

    @Test
    fun chain__first_element_not_a_member_of_base() = test(
        """
        def D { feat h }
        def C { feat f : D  feat x  feat g = f.x }
        """.trimIndent(),
        listOf("Reference 'x' not resolved, to type(s) [Feat] in scope '/C/f'")
    ).let { }

    @Test
    fun chain__base_not_resolved__no_further_error() = test(
        """
        def C { feat g = y.h }
        """.trimIndent(),
        listOf("Reference 'y' not resolved, to type(s) [Def, Feat] in scope '/C/g'")
    ).let { }

    @Test
    fun chain__base_not_a_reference__normal_lookup() {
        test(
            """
            def C { feat h  feat g = new X().h }
            """.trimIndent()
        )
    }
}
