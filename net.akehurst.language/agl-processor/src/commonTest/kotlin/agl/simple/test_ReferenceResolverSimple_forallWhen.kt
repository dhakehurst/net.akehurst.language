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

/** 'forall <nav> [of-type T] when <condition> { ... }': the reference expressions are used only for the elements for which the condition is true */
class test_ReferenceResolverSimple_forallWhen {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = member* ;
                member = def | use ;
                def = 'def' ID ;
                use = 'use' ID ;
                leaf ID = "[a-z]+" ;
            }
            """.trimIndent()
        )

        fun processor(references: String) = Agl.processorFromStringSimple(grammarStr, referenceStr = CrossReferenceString(references)).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun errorsFor(references: String, sentence: String) = processor(references).process(sentence, Agl.options {
            semanticAnalysis { sentenceContext(contextAsmSimple()) }
        }).allIssues.errors.map { it.message }
    }

    @Test
    fun without_when__all_elements_checked() {
        val refs = """
            namespace test.Test
                identify Def by id
                references {
                    in Unit { forall member of-type Use { property id refers-to Def } }
                }
        """.trimIndent()
        val errors = errorsFor(refs, "def a use a use skip")
        assertEquals(1, errors.size, errors.toString())
        assertTrue(errors[0].contains("'skip'"), errors.toString())
    }

    @Test
    fun with_when__elements_not_matching_are_skipped() {
        val refs = """
            namespace test.Test
                identify Def by id
                references {
                    in Unit { forall member of-type Use when id != 'skip' { property id refers-to Def } }
                }
        """.trimIndent()
        assertEquals(emptyList(), errorsFor(refs, "def a use a use skip"))
    }

    @Test
    fun with_when__matching_elements_are_resolved() {
        val refs = """
            namespace test.Test
                identify Def by id
                references {
                    in Unit { forall member of-type Use when id != 'skip' { property id refers-to Def } }
                }
        """.trimIndent()
        val errors = errorsFor(refs, "def a use b use skip")
        assertEquals(1, errors.size, errors.toString())
        assertTrue(errors[0].contains("'b'"), errors.toString())
    }

    @Test
    fun when_unknown_property__error_in_definition() {
        val refs = """
            namespace test.Test
                identify Def by id
                references {
                    in Unit { forall member of-type Use when nope != 'skip' { property id refers-to Def } }
                }
        """.trimIndent()
        val proc = Agl.processorFromStringSimple(grammarStr, referenceStr = CrossReferenceString(refs)).processor!!
        // errors in the references definition are in every process result
        val errors = proc.process("def a", Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } }).allIssues.errors.map { it.message }
        assertTrue(errors.any { it.contains("forall 'member', when") && it.contains("type 'Use' has no property 'nope'") }, errors.toString())
    }

    @Test
    fun asString_round_trip() {
        val refs = """
            namespace test.Test
                identify Def by id
                references {
                    in Unit { forall member of-type Use when id != 'skip' { property id refers-to Def } }
                }
        """.trimIndent()
        val p = processor(refs)
        val str = p.crossReferenceDomain!!.asString()
        assertTrue(str.contains("forall member of-type Use when id != 'skip' {"), str)
    }
}
