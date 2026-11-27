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
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 'inherit named': of the items found in the inherited elements, one that another of them inherits (e.g. a redefined feature,
 * inherited by the feature that redefines it) is hidden by it, as for 'inherit forall'.
 * (Like the KerML library: 'self' of Object redefines 'self' of Anything, so a type that specializes both finds only Object's.)
 */
class test_ReferenceResolverSimple_inheritNamedHidesRedefined {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = element* ;
                element = package | def | partDef | use ;
                package = 'package' ID '{' element* '}' ;
                def = 'def' ID specialization? body? ;
                partDef = 'part' 'def' ID body? ;
                specialization = ':>' qname ;
                body = '{' element* '}' ;
                use = 'use' ID ':' qname ;
                qname = [ID / '::']+ ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
            """.trimIndent()
        )

        fun referenceStr(inherits: String) = CrossReferenceString(
            """
            namespace test.Test
                identify Package by id
                identify Def by id
                identify PartDef by id
                identify Use by id
                scope Package { }
                scope Def {
                    inherit forall specialization members-of qname
                }
                scope PartDef {
                    $inherits
                }
                references {
                    in Specialization { property qname refers-to Def from ${'$'}owner.${'$'}owner }
                    in Use { property qname refers-to Def|PartDef }
                }
            """.trimIndent()
        )

        // B::self redefines (specializes) A::self, C::other does not redefine anything
        const val library = "package Lib { def A { def self def other } def B { def self :> A::self } def C { def other } }"

        fun errors(inherits: String, sentence: String): List<String> {
            val processor = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = referenceStr(inherits)).let {
                assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
                it.processor!!
            }
            val context = contextAsmSimple()
            val lib = processor.process(library, Agl.options { parse { sentenceIdentity { "lib" } }; semanticAnalysis { sentenceContext(context) } })
            assertTrue(lib.allIssues.errors.isEmpty(), lib.allIssues.toString())
            val result = processor.process(sentence, Agl.options { parse { sentenceIdentity { "s1" } }; semanticAnalysis { sentenceContext(context) } })
            assertTrue(result.allIssues.errors.none { it.phase.name != "SEMANTIC_ANALYSIS" }, result.allIssues.toString())
            return result.allIssues.errors.map { it.message }
        }
    }

    @Test
    fun redefined_item_is_hidden() {
        // A::self and B::self are both inherited, B::self redefines A::self: only B::self is found
        assertEquals(emptyList(), errors("inherit named Lib.A  inherit named Lib.B", "part def Q { use u : self }"))
    }

    @Test
    fun redefined_item_is_hidden__other_order() {
        assertEquals(emptyList(), errors("inherit named Lib.B  inherit named Lib.A", "part def Q { use u : self }"))
    }

    @Test
    fun items_that_do_not_redefine__still_ambiguous() {
        // A::other and C::other are unrelated: both are found
        val e = errors("inherit named Lib.A  inherit named Lib.C", "part def Q { use u : other }")
        assertEquals(1, e.size, e.toString())
        assertTrue(e[0].startsWith("Multiple target"), e.toString())
    }
}
