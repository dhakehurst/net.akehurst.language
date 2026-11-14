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
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reference DSL: 'inherit named a.b.c' (e.g. the implicit specializations of KerML/SysML: a library element every element of a type inherits),
 * 'when' conditions on 'inherit' and 'import', and the special value '\$owner'.
 */
class test_ReferenceResolverSimple_inheritNamed {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = element* ;
                element = package | def | partDef | use | import ;
                package = 'package' ID '{' element* '}' ;
                def = 'def' ID specialization? body? ;
                partDef = 'part' 'def' ID body? ;
                specialization = mode? ':>' qname ;
                body = '{' element* '}' ;
                use = 'use' ID ':' qname ;
                import = mode? 'import' qname ;
                qname = [ID / '::']+ ;
                leaf mode = 'on' | 'off' ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
            """.trimIndent()
        )
        val referenceStr = CrossReferenceString(
            """
            namespace test.Test
                identify Package by id
                identify Def by id
                identify PartDef by id
                identify Use by id

                scope Package {
                    import forall element of-type Import members-of qname when mode == 'on'
                }
                scope Def {
                    inherit forall specialization members-of qname when mode == 'on'
                    inherit named Lib.Base
                }
                scope PartDef {
                    inherit named 'Lib'.Part
                    inherit named Lib.Extra when ${'$'}owner is Package
                }

                references {
                    in Specialization { property qname refers-to Def from ${'$'}owner.${'$'}owner }
                    in Use { property qname refers-to Def|PartDef }
                    in Import { property qname refers-to Package }
                }
            """.trimIndent()
        )
        val processor = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = referenceStr).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        const val library = "package Lib { def Base { def B1 def X } def Part { def P1 } def Extra { def E1 } }"

        fun process(sentence: String, context: SentenceContext, id: String) =
            processor.process(sentence, Agl.options { parse { sentenceIdentity { id } }; semanticAnalysis { sentenceContext(context) } })

        /** processes 'sentence' in a context that holds the library (unless 'withLibrary' is false), returns the error and warning messages */
        fun test(sentence: String, withLibrary: Boolean = true): Pair<List<String>, List<String>> {
            val context = contextAsmSimple()
            if (withLibrary) {
                val lib = process(library, context, "lib")
                assertTrue(lib.allIssues.errors.isEmpty(), lib.allIssues.toString())
            }
            val result = process(sentence, context, "s1")
            assertTrue(result.allIssues.errors.none { it.phase.name != "SEMANTIC_ANALYSIS" }, result.allIssues.toString())
            return Pair(result.allIssues.errors.map { it.message }, result.allIssues.warnings.map { it.message })
        }
    }

    @Test
    fun reference_dsl_asString_parses_again() {
        val asString = processor.crossReferenceDomain.asString()
        assertTrue(asString.contains("inherit named Lib.Base"), asString)
        assertTrue(asString.contains("inherit named Lib.Extra when \$owner is Package"), asString)
        assertTrue(asString.contains("members-of qname when mode == 'on'"), asString)
        val again = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = CrossReferenceString(asString))
        assertTrue(again.issues.errors.isEmpty(), "$asString\n${again.issues}")
        assertEquals(asString, again.processor!!.crossReferenceDomain.asString())
    }

    @Test
    fun inherit_named__member_of_library_element_found() {
        assertEquals(emptyList(), test("def D { use u : B1 }").first)
    }

    @Test
    fun inherit_named__quoted_name() {
        assertEquals(emptyList(), test("part def Q { use u : P1 }").first)
    }

    @Test
    fun inherit_named__from_outside() {
        // the inherited member is also found via a qualified name
        assertEquals(emptyList(), test("def D use u : D::B1").first)
    }

    @Test
    fun inherit_named__condition_holds() {
        // the owner of Q is the package P
        assertEquals(emptyList(), test("package P { part def Q { use u : E1 } }").first)
    }

    @Test
    fun inherit_named__condition_does_not_hold() {
        // the owner of Q is the root of the sentence, not a Package
        assertEquals(
            listOf("Reference 'E1' not resolved, to type(s) [Def, PartDef] in scope '/Q'"),
            test("part def Q { use u : E1 }").first
        )
    }

    @Test
    fun inherit_named__after_inherit_forall() {
        // X is in S and in Lib.Base; the explicit specialization is searched first, so only S::X is found
        assertEquals(emptyList(), test("def S { def X } def D on :> S { use u : X }").first)
    }

    @Test
    fun inherit_named__not_found__one_warning() {
        val (errors, warnings) = test("def D { use u : B1 } def E { use v : B1 }", withLibrary = false)
        assertEquals(
            listOf(
                "Reference 'B1' not resolved, to type(s) [Def, PartDef] in scope '/D'",
                "Reference 'B1' not resolved, to type(s) [Def, PartDef] in scope '/E'"
            ), errors
        )
        assertEquals(listOf("Element 'Lib.Base' of 'inherit named' for type 'test.Test.Def' not found in the context, nothing is inherited from it"), warnings)
    }

    @Test
    fun when_on_inherit_forall__holds() {
        assertEquals(emptyList(), test("def S { def Y } def D on :> S { use u : Y }").first)
    }

    @Test
    fun when_on_inherit_forall__no_mode__does_not_hold() {
        assertEquals(
            listOf("Reference 'Y' not resolved, to type(s) [Def, PartDef] in scope '/D'"),
            test("def S { def Y } def D :> S { use u : Y }").first
        )
    }

    @Test
    fun when_on_inherit_forall__does_not_hold() {
        assertEquals(
            listOf("Reference 'Y' not resolved, to type(s) [Def, PartDef] in scope '/D'"),
            test("def S { def Y } def D off :> S { use u : Y }").first
        )
    }

    @Test
    fun when_on_import__holds() {
        assertEquals(emptyList(), test("package A { def Y } package B { on import A use u : Y }").first)
    }

    @Test
    fun when_on_import__does_not_hold() {
        assertEquals(
            listOf("Reference 'Y' not resolved, to type(s) [Def, PartDef] in scope '/B'"),
            test("package A { def Y } package B { off import A use u : Y }").first
        )
    }

    @Test
    fun owner_of_owner__specialization_looked_up_outside_of_the_def() {
        // 'from $owner.$owner': the owner of a Specialization is its Def, the owner of that is the package
        // so the Def's own member S is not found, the package's S is
        assertEquals(emptyList(), test("package P { def S { def Z } def D on :> S { def S use u : Z } }").first)
    }
}
