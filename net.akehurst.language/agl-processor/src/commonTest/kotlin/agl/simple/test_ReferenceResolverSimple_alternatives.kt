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
 * Reference DSL: 'identify T by a | b', an element is found by each of its names (e.g. a name and a short name, as in SysML).
 */
class test_ReferenceResolverSimple_alternatives {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = element* ;
                element = package | def | use ;
                package = 'package' identification '{' element* '}' ;
                def = 'def' identification ;
                use = 'use' ID ':' qname ;
                identification = shortName? ID? ;
                shortName = '<' ID '>' ;
                qname = [ID / '::']+ ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
            """.trimIndent()
        )
        val referenceStr = CrossReferenceString(
            """
            namespace test.Test
                identify Package by identification.id | identification.shortName.id
                identify Def by identification.id | identification.shortName.id
                identify Use by id

                scope Package { }

                references {
                    in Use { property qname refers-to Def }
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
    fun reference_dsl_asString_parses_again() {
        val asString = processor.crossReferenceDomain.asString()
        val again = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = CrossReferenceString(asString))
        assertTrue(again.issues.errors.isEmpty(), "$asString\n${again.issues}")
        assertEquals(asString, again.processor!!.crossReferenceDomain.asString())
    }

    @Test
    fun reference_dsl_prints_alternatives() {
        val asString = processor.crossReferenceDomain.asString()
        assertTrue(asString.contains("identify Def by identification.id | identification.shortName.id"), asString)
    }

    @Test
    fun by_name() = test("def <m> metre use u : metre")

    @Test
    fun by_short_name() = test("def <m> metre use u : m")

    @Test
    fun only_short_name() = test("def <m> use u : m")

    @Test
    fun only_name() = test("def metre use u : metre")

    @Test
    fun not_found() = test(
        "def <m> metre use u : x",
        listOf("Reference 'x' not resolved, to type(s) [Def] in scope '/'")
    )

    @Test
    fun qualified__package_by_short_name() = test("package <si> SI { def <m> metre } use u : si::m")

    @Test
    fun qualified__package_by_name_def_by_short_name() = test("package <si> SI { def <m> metre } use u : SI::m")
}
