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
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A language whose own types have the same simple names as types of the std library (Collection, List, Pair, Set).
 * Unqualified type names in the cross-reference definition ('refers-to', 'of-type' of an import, 'of-type' of a 'forall')
 * are the language's own types: they are resolved in the namespace of the declaration block, then in its imports.
 */
class test_ReferenceResolverSimple_stdTypeNames {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = element* ;
                element = collection | set | pair | list ;
                collection = 'package' ID '{' element* '}' ;
                set = 'def' ID ;
                pair = 'use' ID ':' qname ;
                list = 'import' qname ;
                qname = [ID / '::']+ ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
            """.trimIndent()
        )

        fun referenceStr(qualifier: String, imports: String = "") = CrossReferenceString(
            """
            namespace test.Test
                $imports
                identify Collection by id
                identify Set by id

                scope Collection {
                    import forall element of-type ${qualifier}List members-of qname
                }

                references {
                    in List { property qname refers-to ${qualifier}Collection }
                    in Collection {
                        forall element of-type ${qualifier}Pair { property qname refers-to ${qualifier}Set }
                    }
                }
            """.trimIndent()
        )

        fun processorFor(qualifier: String, imports: String = "") = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = referenceStr(qualifier, imports)).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun errors(qualifier: String, sentence: String, imports: String = ""): List<String> {
            val processor = processorFor(qualifier, imports)
            val result = processor.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
            assertTrue(result.allIssues.errors.none { it.phase.name != "SEMANTIC_ANALYSIS" }, result.allIssues.toString())
            return result.allIssues.errors.map { it.message }
        }

        // 'A' is found only through the import of 'Lib' into 'P'
        const val sentenceOk = "package Lib { def A } package P { import Lib use u : A }"

        // 'B' does not exist, the reference of the 'forall' gives an error
        const val sentenceNotFound = "package Lib { def A } package P { import Lib use u : B }"
    }

    @Test
    fun found_unqualified() {
        assertEquals(emptyList(), errors("", sentenceOk))
    }

    @Test
    fun found_qualified() {
        assertEquals(emptyList(), errors("test.Test.", sentenceOk))
    }

    @Test
    fun not_found_unqualified() {
        val errs = errors("", sentenceNotFound)
        assertEquals(1, errs.size, errs.toString())
        assertTrue(errs[0].startsWith("Reference 'B' not resolved"), errs.toString())
    }

    @Test
    fun not_found_qualified() {
        val errs = errors("test.Test.", sentenceNotFound)
        assertEquals(1, errs.size, errs.toString())
        assertTrue(errs[0].startsWith("Reference 'B' not resolved"), errs.toString())
    }

    private fun checkErrors(referenceStr: CrossReferenceString): List<String> {
        val processor = processorFor("")
        val res = CrossReferenceDomainDefault.fromString(contextFromTypesDomain(processor.typesDomain), referenceStr)
        return res.allIssues.errors.map { it.message }
    }

    @Test
    fun check_unqualified() {
        assertEquals(emptyList(), checkErrors(referenceStr("")))
    }

    @Test
    fun check_qualified() {
        assertEquals(emptyList(), checkErrors(referenceStr("test.Test.")))
    }

    @Test
    fun check_refers_to_std_type__not_found() {
        val errs = checkErrors(
            CrossReferenceString(
                """
                namespace test.Test
                    references {
                        in List { property qname refers-to String }
                    }
                """.trimIndent()
            )
        )
        assertEquals(listOf("For references in 'List', referred to type 'String' not found"), errs)
    }

    @Test
    fun check_refers_to_qualified_std_type__not_a_data_type() {
        val errs = checkErrors(
            CrossReferenceString(
                """
                namespace test.Test
                    references {
                        in List { property qname refers-to std.Collection }
                    }
                """.trimIndent()
            )
        )
        assertEquals(listOf("For references in 'List', referred to type 'std.Collection' is not a data type"), errs)
    }

    @Test
    fun check_refers_to_explicitly_imported_std_type__not_a_data_type() {
        val errs = checkErrors(
            CrossReferenceString(
                """
                namespace test.Test
                    import std
                    references {
                        in List { property qname refers-to String }
                    }
                """.trimIndent()
            )
        )
        assertEquals(listOf("For references in 'List', referred to type 'String' is not a data type"), errs)
    }

    @Test
    fun explicitly_imported_std_does_not_hide_own_types() {
        assertEquals(emptyList(), errors("", sentenceOk, "import std"))
    }
}
