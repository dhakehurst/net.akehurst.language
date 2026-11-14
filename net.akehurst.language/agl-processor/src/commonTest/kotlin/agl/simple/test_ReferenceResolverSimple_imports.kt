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
 * Reference DSL: 'import', 'inherit', 'visibility', 'alias-for', 'from $owner...' / 'from inherited ...' / 'from $root', and 'when' conditions.
 */
class test_ReferenceResolverSimple_imports {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = element* ;
                element = package | def | use | alias | allImport | oneImport | deepImport ;
                package = visibility? 'package' ID '{' element* '}' ;
                def = visibility? 'def' ID specialization? body? ;
                specialization = ':>' qname ;
                body = '{' element* '}' ;
                use = visibility? 'use' ID typing? redefines? ;
                typing = ':' global? qname ;
                redefines = ':>>' qname ;
                alias = 'alias' ID 'for' qname ;
                allImport = visibility? 'import' 'all' qname ;
                oneImport = visibility? 'import' qname ;
                deepImport = visibility? 'import' 'deep' qname ;
                qname = [ID / '::']+ ;
                leaf global = '$::' ;
                leaf visibility = 'private' | 'protected' | 'public' ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
            """.trimIndent()
        )
        val referenceStr = CrossReferenceString(
            """
            namespace test.Test
                identify Package by id visibility visibility
                identify Def by id visibility visibility
                identify Use by id visibility visibility
                identify Alias by id alias-for qname

                scope Package {
                    import forall element of-type AllImport members-of qname visibility visibility
                    import forall element of-type OneImport element qname visibility visibility
                    import forall element of-type DeepImport members-of qname recursive true visibility visibility
                }
                scope Def {
                    inherit forall specialization members-of qname
                }
                scope Use { }

                references {
                    in Specialization { property qname refers-to Def from ${'$'}owner.${'$'}owner }
                    in Typing {
                        property qname refers-to Def from ${'$'}root when global == '${'$'}::'
                        property qname refers-to Def
                    }
                    in Redefines { property qname refers-to Use from inherited ${'$'}owner.${'$'}owner }
                    in Alias { property qname refers-to Def|Package }
                    in AllImport { property qname refers-to Package|Def }
                    in OneImport { property qname refers-to Package|Def|Use }
                    in DeepImport { property qname refers-to Package }
                }
            """.trimIndent()
        )
        val processorResult = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = referenceStr)
        val processor = processorResult.let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun test(sentence: String, expectedErrors: List<String> = emptyList()): List<Any?> {
            val result = processor.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
            assertTrue(result.allIssues.errors.none { it.phase.name != "SEMANTIC_ANALYSIS" }, result.allIssues.toString())
            assertEquals(expectedErrors, result.allIssues.errors.map { it.message })
            return result.semanticAnalysis?.resolvedReferences?.map { it.target } ?: emptyList()
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
    fun reference_dsl_parses_and_prints() {
        val asString = processor.crossReferenceDomain.asString()
        assertTrue(asString.contains("import forall element of-type AllImport members-of qname"), asString)
    }

    @Test
    fun import_all__found() {
        test("package A { def X } package B { import all A use u : X }")
    }

    @Test
    fun no_import__not_found() = test(
        "package A { def X } package B { use u : X }",
        listOf("Reference 'X' not resolved, to type(s) [Def] in scope '/B/u'")
    ).let { }

    @Test
    fun import_one__only_that_element() = test(
        "package A { def X def Y } package B { import A::X use u : X use v : Y }",
        listOf("Reference 'Y' not resolved, to type(s) [Def] in scope '/B/v'")
    ).let { }

    @Test
    fun import_all__private_member_not_imported() = test(
        "package A { private def X } package B { import all A use u : X }",
        listOf("Reference 'X' not resolved, to type(s) [Def] in scope '/B/u'")
    ).let { }

    @Test
    fun private_member__visible_inside() {
        test("package A { private def X use u : X }")
    }

    @Test
    fun public_import__visible_through_import() {
        test("package A { def X } package B { import all A } package C { import all B use u : X }")
    }

    @Test
    fun private_import__not_visible_through_import() = test(
        "package A { def X } package B { private import all A } package C { import all B use u : X }",
        listOf("Reference 'X' not resolved, to type(s) [Def] in scope '/C/u'")
    ).let { }

    @Test
    fun deep_import__nested_members() {
        test("package A { package N { def X } } package B { import deep A use u : X }")
    }

    @Test
    fun import_all__not_nested_members() = test(
        "package A { package N { def X } } package B { import all A use u : X }",
        listOf("Reference 'X' not resolved, to type(s) [Def] in scope '/B/u'")
    ).let { }

    @Test
    fun import_cycle__terminates() {
        test("package A { import all B def X } package B { import all A use u : X }")
    }

    @Test
    fun import_cycle__not_found_terminates() = test(
        "package A { import all B } package B { import all A use u : Z }",
        listOf("Reference 'Z' not resolved, to type(s) [Def] in scope '/B/u'")
    ).let { }

    @Test
    fun inherit__member_of_general_visible_inside() {
        test("def Base { def Part } def Car :> Base { use p : Part }")
    }

    @Test
    fun no_inherit__member_not_visible() = test(
        "def Base { def Part } def Car { use p : Part }",
        listOf("Reference 'Part' not resolved, to type(s) [Def] in scope '/Car/p'")
    ).let { }

    @Test
    fun inherit__member_of_general_visible_from_outside() {
        test("def Base { def Part } def Car :> Base use p : Car::Part")
    }

    @Test
    fun inherit__protected_visible_inside() {
        test("def Base { protected def Part } def Car :> Base { use p : Part }")
    }

    @Test
    fun inherit__protected_not_visible_from_outside() = test(
        "def Base { protected def Part } def Car :> Base use p : Car::Part",
        listOf("Reference 'Car.Part' not resolved, to type(s) [Def] in scope '/p'")
    ).let { }

    @Test
    fun specialization__looked_up_from_owner() = test(
        "package P { def Car :> Inner { def Inner } }",
        listOf("Reference 'Inner' not resolved, to type(s) [Def] in scope '/P/Car'")
    ).let { }

    @Test
    fun redefinition__looked_up_in_general() {
        test("def Car { use engine } def Sports :> Car { use engine :>> engine }")
    }

    @Test
    fun redefinition__not_in_general() = test(
        "def Car { use engine } def Sports :> Car { use wheel :>> wheel }",
        listOf("Reference 'wheel' not resolved, to type(s) [Use] in scope '/Sports/wheel'")
    ).let { }

    @Test
    fun alias__resolves_to_target() {
        val targets = test("package A { def X } package B { alias Y for A::X use u : Y }")
        assertTrue(targets.any { it.toString().contains("Def") }, targets.toString())
    }

    @Test
    fun global_name__from_root() {
        test("package A { def X } package B { package A { } use u : $:: A::X }")
    }

    @Test
    fun not_global_name__nearest_first_part() = test(
        "package A { def X } package B { package A { } use u : A::X }",
        listOf("Reference 'A.X' not resolved, to type(s) [Def] in scope '/B/u'")
    ).let { }
}
