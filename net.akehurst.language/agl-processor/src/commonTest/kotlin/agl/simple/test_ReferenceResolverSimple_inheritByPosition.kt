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
 * 'inherit by-position when c': an element inherits the members of the element at the same position (among the items for which 'c' holds)
 * in each scope that its owner inherits (e.g. SysML/KerML: a parameter redefines the parameter at the same position of each general).
 */
class test_ReferenceResolverSimple_inheritByPosition {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = element* ;
                element = def | use | param | feat ;
                def = 'def' ID supertype? body? ;
                supertype = ':>' ID ;
                use = 'use' ID typing? ;
                param = 'param' ID typing? body? ;
                feat = 'feat' ID typing? body? ;
                typing = ':' ID ;
                body = '{' element* '}' ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
            """.trimIndent()
        )
        val referenceStr = CrossReferenceString(
            """
            namespace test.Test
                identify Def by id
                identify Use by id
                identify Param by id
                identify Feat by id
                scope Def {
                    inherit forall supertype members-of id
                }
                scope Param {
                    inherit forall typing members-of id
                    inherit by-position when ${'$'}self is Param
                }
                scope Feat {
                    inherit forall typing members-of id
                }
                references {
                    in Supertype { property id refers-to Def }
                    in Typing { property id refers-to Def }
                }
            """.trimIndent()
        )
        val processor = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = referenceStr).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        const val GENERAL = "def Image { def Q } def Other { def R } def Focus { param scene : Other param image : Image } "

        fun test(sentence: String, expectedErrors: List<String> = emptyList()) {
            val result = processor.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
            assertTrue(result.allIssues.errors.none { it.phase.name != "SEMANTIC_ANALYSIS" }, result.allIssues.toString())
            assertEquals(expectedErrors, result.allIssues.errors.map { it.message })
        }
    }

    @Test
    fun reference_dsl_asString_parses_again() {
        val asString = processor.crossReferenceDomain.asString()
        assertTrue(asString.contains("inherit by-position when \$self is Param"), asString)
        val again = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = CrossReferenceString(asString))
        assertTrue(again.issues.errors.isEmpty(), "$asString\n${again.issues}")
        assertEquals(asString, again.processor!!.crossReferenceDomain.asString())
    }

    @Test
    fun same_position__members_of_the_general_parameter() = test(
        GENERAL + "def F2 :> Focus { param scene param image { use u : Q } }"
    )

    @Test
    fun matched_by_position_not_by_name() = test(
        GENERAL + "def F2 :> Focus { param a param b { use u : Q } }"
    )

    @Test
    fun items_not_counted_do_not_shift_the_position() = test(
        GENERAL + "def F2 :> Focus { feat z param a feat y param b { use u : Q } }"
    )

    @Test
    fun other_position__not_found() = test(
        GENERAL + "def F2 :> Focus { param a { use u : Q } param b }",
        listOf("Reference 'Q' not resolved, to type(s) [Def] in scope '/F2/a'")
    )

    @Test
    fun not_counted_itself__nothing_inherited() = test(
        GENERAL + "def F2 :> Focus { param a feat b { use u : Q } }",
        listOf("Reference 'Q' not resolved, to type(s) [Def] in scope '/F2/b'")
    )

    @Test
    fun no_general__nothing_inherited() = test(
        GENERAL + "def F2 { param a param b { use u : Q } }",
        listOf("Reference 'Q' not resolved, to type(s) [Def] in scope '/F2/b'")
    )

    @Test
    fun position_beyond_the_general_parameters__nothing_inherited() = test(
        GENERAL + "def F2 :> Focus { param a param b param c { use u : Q } }",
        listOf("Reference 'Q' not resolved, to type(s) [Def] in scope '/F2/c'")
    )
}
