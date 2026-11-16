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
import net.akehurst.language.agl.semanticAnalyser.contextFromTypesDomain
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Reference DSL: top-level 'import-into-scope', an import declared on the side of the importing element.
 * An import element imports into the scope that it is in, wherever it is (a package body, a def body, ...).
 */
class test_ReferenceResolverSimple_importIntoScope {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = element* ;
                element = package | def | use | allImport | oneImport | deepImport ;
                package = 'package' ID '{' element* '}' ;
                def = visibility? 'def' ID body? ;
                body = '{' element* '}' ;
                use = 'use' ID typing? ;
                typing = ':' qname ;
                allImport = visibility? 'import' 'all' qname ;
                oneImport = visibility? 'import' qname ;
                deepImport = visibility? 'import' 'deep' qname ;
                qname = [ID / '::']+ ;
                leaf visibility = 'private' | 'protected' | 'public' ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
            """.trimIndent()
        )
        val referenceStr = CrossReferenceString(
            """
            namespace test.Test
                identify Package by id
                identify Def by id visibility visibility
                identify Use by id
                import-into-scope AllImport members-of qname visibility visibility
                import-into-scope OneImport element qname visibility visibility
                import-into-scope DeepImport members-of qname recursive true visibility visibility

                scope Package { }
                scope Def { }
                scope Use { }

                references {
                    in Typing { property qname refers-to Def }
                    in AllImport { property qname refers-to Package|Def }
                    in OneImport { property qname refers-to Package|Def }
                    in DeepImport { property qname refers-to Package }
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
        assertTrue(asString.contains("import-into-scope AllImport members-of qname visibility visibility"), asString)
        val again = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = CrossReferenceString(asString))
        assertTrue(again.issues.errors.isEmpty(), "$asString\n${again.issues}")
        assertEquals(asString, again.processor!!.crossReferenceDomain.asString())
    }

    @Test
    fun type_not_found__error() {
        val refs = """
            namespace test.Test
                identify Def by id
                import-into-scope NoSuchType members-of qname
        """.trimIndent()
        val res = Agl.registry.agl.crossReference.processor!!.process(refs, Agl.options { semanticAnalysis { sentenceContext(contextFromTypesDomain(processor.typesDomain)) } })
        assertTrue(res.allIssues.errors.any { it.message == "For 'import-into-scope', the type 'NoSuchType' is not found" }, res.allIssues.toString())
    }

    @Test
    fun import_in_package__found() = test(
        "package A { def X } package B { import all A use u : X }"
    )

    @Test
    fun no_import__not_found() = test(
        "package A { def X } package B { use u : X }",
        listOf("Reference 'X' not resolved, to type(s) [Def] in scope '/B/u'")
    )

    @Test
    fun import_in_def_body__found_inside() = test(
        "package A { def X } package B { def D { import all A use u : X } }"
    )

    @Test
    fun import_in_def_body__not_in_sibling() = test(
        "package A { def X } package B { def D { import all A } def E { use u : X } }",
        listOf("Reference 'X' not resolved, to type(s) [Def] in scope '/B/E/u'")
    )

    @Test
    fun import_in_nested_def__not_in_containing_def() = test(
        "package A { def X } package B { def D { def N { import all A } use u : X } }",
        listOf("Reference 'X' not resolved, to type(s) [Def] in scope '/B/D/u'")
    )

    @Test
    fun import_one_in_def_body__only_that_element() = test(
        "package A { def X def Y } package B { def D { import A::X use u : X use v : Y } }",
        listOf("Reference 'Y' not resolved, to type(s) [Def] in scope '/B/D/v'")
    )

    @Test
    fun import_after_use__found() = test(
        "package A { def X } package B { def D { use u : X import all A } }"
    )

    @Test
    fun public_import_in_def__visible_through_import_of_def() = test(
        "package A { def X } package B { def D { public import all A } } package C { import all B::D use u : X }"
    )

    @Test
    fun private_import_in_def__not_visible_through_import_of_def() = test(
        "package A { def X } package B { def D { private import all A } } package C { import all B::D use u : X }",
        listOf("Reference 'X' not resolved, to type(s) [Def] in scope '/C/u'")
    )

    @Test
    fun deep_import_in_def_body__nested_members() = test(
        "package A { package A2 { def X } } package B { def D { import deep A use u : X } }"
    )
}
