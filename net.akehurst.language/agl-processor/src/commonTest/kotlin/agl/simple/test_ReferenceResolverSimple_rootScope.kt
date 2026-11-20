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
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The root element of a sentence owns the root scope of that sentence,
 * so a 'scope' block for the type of the root element applies to it, e.g. imports at file level (KerML 8.2.3.5).
 */
class test_ReferenceResolverSimple_rootScope {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = element* ;
                element = package | def | use | allImport ;
                package = 'package' ID '{' element* '}' ;
                def = 'def' ID ;
                use = 'use' ID ':' qname ;
                allImport = 'import' qname '::' '*' ;
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

                scope Unit {
                    import forall element of-type AllImport members-of qname
                }
                scope Package {
                    import forall element of-type AllImport members-of qname
                }

                references {
                    in Use { property qname refers-to Def }
                    in AllImport { property qname refers-to Package }
                }
            """.trimIndent()
        )
        val processorResult = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = referenceStr)
        val processor = processorResult.let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun process(sentence: String, context: SentenceContext, sentenceId: String, expectedErrors: List<String> = emptyList()) {
            val result = processor.process(sentence, Agl.options {
                parse { sentenceIdentity { sentenceId } }
                semanticAnalysis { sentenceContext(context) }
            })
            assertTrue(result.allIssues.errors.none { it.phase.name != "SEMANTIC_ANALYSIS" }, result.allIssues.toString())
            assertEquals(expectedErrors, result.allIssues.errors.map { it.message })
        }

        fun test(sentence: String, expectedErrors: List<String> = emptyList()) = process(sentence, contextAsmSimple(), "s", expectedErrors)
    }

    @Test
    fun import_at_root__found() = test("import A::* package A { def X } use u : X")

    @Test
    fun import_at_root__found_inside_package() = test("import A::* package A { def X } package B { use u : X }")

    @Test
    fun no_import__not_found() = test(
        "package A { def X } use u : X",
        listOf("Reference 'X' not resolved, to type(s) [Def] in scope '/'")
    )

    @Test
    fun import_in_package__still_found() = test("package A { def X } package B { import A::* use u : X }")

    @Test
    fun import_at_root__only_for_its_own_sentence() {
        val context = contextAsmSimple()
        process("import A::* package A { def X }", context, "s1")
        process(
            "use u : X", context, "s2",
            listOf("Reference 'X' not resolved, to type(s) [Def] in scope '/'")
        )
    }

    @Test
    fun import_at_root__of_other_sentence_used_by_its_own_names() {
        val context = contextAsmSimple()
        process("package A { def X }", context, "s1")
        process("import A::* use u : X", context, "s2")
    }

    @Test
    fun root_scope_owner__kept_by_union() {
        val context = contextAsmSimple()
        process("import A::* package A { def X }", context, "s1")
        val union = context.union(contextAsmSimple())
        assertNotNull(union.getScopeForSentenceOrNull("s1")?.ownerItem)
    }
}
