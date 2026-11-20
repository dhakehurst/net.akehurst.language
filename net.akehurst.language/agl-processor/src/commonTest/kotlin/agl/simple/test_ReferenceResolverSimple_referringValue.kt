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
 * 'property p.m(...) refers-to T': the referring navigation may end with method calls;
 * the value of the whole expression is looked up, the property 'p' becomes the reference
 * (e.g. quoted names, where 'a' and a are the same name).
 */
class test_ReferenceResolverSimple_referringValue {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = element* ;
                element = package | def | use | ref | oneImport ;
                package = 'package' ID '{' element* '}' ;
                def = 'def' ID ;
                use = 'use' ID ':' qname ;
                ref = 'ref' ID '=' ID ;
                oneImport = 'import' qname ;
                qname = [ID / '::']+ ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*|'[^']*'" ;
            }
            """.trimIndent()
        )
        val referenceStr = CrossReferenceString(
            """
            namespace test.Test
                identify Package by id.removeSurrounding('\'')
                identify Def by id.removeSurrounding('\'')
                identify Use by id
                identify Ref by id
                import-into-scope OneImport element qname
                scope Package { }
                references {
                    in Use { property qname.map({ n -> n.removeSurrounding('\'') }) refers-to Package|Def }
                    in Ref { property id2.removeSurrounding('\'') refers-to Def }
                    in OneImport { property qname.map({ n -> n.removeSurrounding('\'') }) refers-to Package|Def }
                }
            """.trimIndent()
        )
        val processor = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = referenceStr).let {
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
    fun quoted_declaration__unquoted_reference() {
        assertEquals(listOf(":Def[a]"), test("def 'a' ref r = a"))
    }

    @Test
    fun unquoted_declaration__quoted_reference() {
        assertEquals(listOf(":Def[a]"), test("def a ref r = 'a'"))
    }

    @Test
    fun quoted_parts_of_a_qualified_name() {
        assertEquals(listOf(":Def[p q.b]"), test("package 'p q' { def b } use u : 'p q'::'b'"))
    }

    @Test
    fun not_found__error_names_the_looked_up_value() = test(
        "def a ref r = 'c'",
        listOf("Reference 'c' not resolved, to type(s) [Def] in scope '/'")
    ).let { }

    @Test
    fun element_import_of_a_quoted_name__imported_by_the_looked_up_name() {
        // the import brings 'x' into B under the name it was looked up by (x), not the text of the property ('x')
        assertEquals(
            listOf(":Def[A.x]", ":Def[A.x]", ":Def[A.x]"),
            test("package A { def 'x' } package B { import A::'x' use u1 : x use u2 : 'x' }")
        )
    }
}
