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
import net.akehurst.language.asm.api.AsmStructure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reference DSL: 'from $previous...' inside a 'forall', e.g. for feature chains 'a.b.c'
 * where each element is looked up in the members of what the element before it refers to.
 */
class test_ReferenceResolverSimple_previous {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = element* ;
                element = def | use ;
                def = 'def' ID body? ;
                body = '{' element* '}' ;
                use = 'use' ID typing? subsets? body? ;
                typing = ':' qualifiedName ;
                subsets = ':>' chain ;
                chain = [qualifiedName / '.']+ ;
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
                identify Use by id

                scope Def { }
                scope Use {
                    inherit forall typing.qualifiedName members-of name
                }

                references {
                    in QualifiedName { property name refers-to Def|Use }
                    in Subsets {
                        forall chain {
                            property name refers-to Def|Use from ${'$'}previous.name
                        }
                    }
                }
            """.trimIndent()
        )
        val processorResult = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = referenceStr)
        val processor = processorResult.let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        /** returns the semantic paths of the resolved reference targets */
        fun test(sentence: String, expectedErrors: List<String> = emptyList()): List<String> {
            val result = processor.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
            assertTrue(result.allIssues.errors.none { it.phase.name != "SEMANTIC_ANALYSIS" }, result.allIssues.toString())
            assertEquals(expectedErrors, result.allIssues.errors.map { it.message })
            return result.semanticAnalysis?.resolvedReferences?.map { (it.target as AsmStructure).semanticQualifiedPath?.joinToString("/") ?: "?" } ?: emptyList()
        }
    }

    @Test
    fun chain_of_one__normal_lookup() {
        val targets = test("def P use p1 : P use x :> p1")
        assertEquals(listOf("P", "p1"), targets)
    }

    @Test
    fun chain_of_two__second_in_members_of_type_of_first() {
        val targets = test("def P { use po1 } use p1 : P use x :> p1.po1")
        assertEquals(listOf("P", "p1", "P/po1"), targets)
    }

    @Test
    fun chain__not_from_enclosing_scope() {
        // the top level 'po1' must not be found for the second element
        val targets = test("def P { use po1 } use po1 use p1 : P use x :> p1.po1")
        assertEquals(listOf("P", "p1", "P/po1"), targets)
    }

    @Test
    fun chain__member_not_found() = test(
        "def P { use po1 } use p1 : P use x :> p1.zz",
        listOf("Reference 'zz' not resolved, to type(s) [Def, Use] in scope '/p1'")
    ).let { }

    @Test
    fun chain__name_in_enclosing_scope_but_not_member() = test(
        "def P use zz use p1 : P use x :> p1.zz",
        listOf("Reference 'zz' not resolved, to type(s) [Def, Use] in scope '/p1'")
    ).let { }

    @Test
    fun chain__first_not_found__one_error_only() = test(
        "def P { use po1 } use x :> q.po1.r",
        listOf("Reference 'q' not resolved, to type(s) [Def, Use] in scope '/x'")
    ).let { }

    @Test
    fun chain_of_three() {
        val targets = test("def E { use m } def V { use eng : E } use v : V use x :> v.eng.m")
        assertEquals(listOf("E", "V", "v", "V/eng", "E/m"), targets)
    }

    @Test
    fun chain__owned_member_of_usage() {
        val targets = test("use a { use b } use x :> a.b")
        assertEquals(listOf("a", "a/b"), targets)
    }

    @Test
    fun chain__used_before_declared() {
        val targets = test("use x :> p1.po1 def P { use po1 } use p1 : P")
        assertEquals(listOf("p1", "P/po1", "P").sorted(), targets.sorted())
    }
}
