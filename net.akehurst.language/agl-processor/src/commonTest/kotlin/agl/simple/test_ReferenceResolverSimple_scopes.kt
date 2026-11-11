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
 * Name lookup through enclosing scopes.
 * The first part of a (possibly qualified) name is looked up in the local scope, then outwards to the root.
 * The nearest scope with that name is used. The other parts are looked up only inside that scope.
 */
class test_ReferenceResolverSimple_scopes {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = element* ;
                element = package | def | use | useq ;
                package = 'package' ID '{' element* '}' ;
                def = 'def' ID ;
                use = 'use' ID ':' ref ;
                useq = 'useq' ID ':' qname ;
                qname = [ID / '::']+ ;
                leaf ref = "[A-Za-z_][A-Za-z0-9_.]*" ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
            """.trimIndent()
        )
        val referenceStr = CrossReferenceString(
            """
            namespace test.Test
                identify Package by id
                identify Def by id
                identify Use by id
                identify Useq by id
                scope Package {
                    identify Package by id
                    identify Def by id
                    identify Use by id
                    identify Useq by id
                }
                references {
                    in Use { property ref refers-to Def }
                    in Useq { property qname refers-to Def }
                }
            """.trimIndent()
        )
        val processor = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = referenceStr).processor!!

        fun test(sentence: String, expectedErrors: List<String> = emptyList()) {
            val result = processor.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
            assertEquals(expectedErrors, result.allIssues.errors.map { it.message })
            if (expectedErrors.isEmpty()) {
                assertTrue(result.semanticAnalysis?.resolvedReferences?.isNotEmpty() == true, "no resolved references")
            }
        }
    }

    @Test
    fun simple_name__in_same_scope() = test("package P { def A use x : A }")

    @Test
    fun simple_name__in_root_scope() = test("def A package P { use x : A }")

    @Test
    fun simple_name__in_enclosing_scope() = test("package P { def A package Q { use x : A } }")

    @Test
    fun simple_name__in_root_scope_two_levels_out() = test("def A package P { package Q { use x : A } }")

    @Test
    fun simple_name__not_found() = test(
        "package P { def A } package Q { use x : A }",
        listOf("Reference 'A' not resolved, to type(s) [Def] in scope '/Q'")
    )

    @Test
    fun simple_name__nearest_has_name_with_other_type__error() = test(
        "def A package P { package A { } use x : A }",
        listOf("Reference 'A' not resolved, to type(s) [Def] in scope '/P'")
    )

    @Test
    fun list_name_one_part__in_same_scope() = test("package P { def A useq x : A }")

    @Test
    fun list_name_one_part__in_enclosing_scope() = test("package P { def A package Q { useq x : A } }")

    @Test
    fun list_name__from_root() = test("package P { def A } package Q { useq x : P::A }")

    @Test
    fun list_name__relative() = test("package P { package R { def A } useq x : R::A }")

    @Test
    fun list_name__relative_from_nested() = test("package P { package R { def A } package Q { useq x : R::A } }")

    @Test
    fun list_name__last_part_not_looked_up_outside_qualifying_scope() = test(
        "package P { def A package R { } useq x : R::A }",
        listOf("Reference 'R.A' not resolved, to type(s) [Def] in scope '/P'")
    )

    @Test
    fun list_name__nearest_first_part_is_used() = test(
        "package R { def A } package P { package R { } useq x : R::A }",
        listOf("Reference 'R.A' not resolved, to type(s) [Def] in scope '/P'")
    )

    @Test
    fun string_qualified_name__relative() = test("package P { package R { def A } package Q { use x : R.A } }")
}
