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
 * 'from inherited E': each scope that E inherits is tried in turn, in the order the inheriting elements are in the sentence
 * (not the order of the inherit rules); the lookup starts in that scope and goes outwards from it; the first that gives a result is used.
 */
class test_ReferenceResolverSimple_fromInherited {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = element* ;
                element = package | def | use ;
                package = 'package' ID '{' element* '}' ;
                def = 'def' ID general* body? ;
                general = specialization | typing ;
                specialization = ':>' qname ;
                typing = ':' qname ;
                body = '{' element* '}' ;
                use = 'use' ID redefines? ;
                redefines = ':>>' qname ;
                qname = [ID / '::']+ ;
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
                scope Package { }
                scope Def {
                    inherit forall general of-type Typing members-of qname
                    inherit forall general of-type Specialization members-of qname
                }
                scope Use { }
                references {
                    in Specialization { property qname refers-to Def from ${'$'}owner.${'$'}owner }
                    in Typing { property qname refers-to Def from ${'$'}owner.${'$'}owner }
                    in Redefines { property qname refers-to Use from inherited ${'$'}owner.${'$'}owner }
                }
            """.trimIndent()
        )
        val processor = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = referenceStr).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun test(sentence: String, expectedErrors: List<String> = emptyList()) = processor.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } }).also { result ->
            assertTrue(result.allIssues.errors.none { it.phase.name != "SEMANTIC_ANALYSIS" }, result.allIssues.toString())
            assertEquals(expectedErrors, result.allIssues.errors.map { it.message })
        }
    }

    @Test
    fun first_general_in_sentence_is_used() = test(
        // 'B' (a specialization) is before 'C' (a typing) in the sentence, the typing rule is first in the references: B::e (a Use) is found
        "def B { use e } def C { def e } def S :> B : C { use x :>> e }"
    ).let { }

    @Test
    fun first_general_in_sentence_is_used__wrong_type_is_an_error() = test(
        // 'C' is first in the sentence, its 'e' is a Def, not a Use
        "def B { use e } def C { def e } def S : C :> B { use x :>> e }",
        listOf("Reference 'e' not resolved, to type(s) [Use] in scope '/S/x'")
    ).let { }

    @Test
    fun next_general_when_first_has_no_result() = test(
        "def B { use f } def C { use e } def S :> B :> C { use x :>> e }"
    ).let { }

    @Test
    fun lookup_goes_outwards_from_the_general() = test(
        // from the general P::a outwards, 'a' is P::a (with a Use 'e'); outwards from S it would be Q::a (with a Def 'e')
        "package P { def a { use e } } package Q { def a { def e } def S :> P::a { use x :>> a::e } }"
    ).let { }

    @Test
    fun nothing_inherited__lookup_outwards_from_owner() = test(
        "use e def S { use e :>> e }"
    ).let { }
}
