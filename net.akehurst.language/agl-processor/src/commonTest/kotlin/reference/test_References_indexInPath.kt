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

package net.akehurst.language.agl.language.reference

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * A referring property path may index into a list, e.g. 'target[0].id'.
 */
class test_References_indexInPath {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = item* ;
                item = def | use ;
                def = 'def' ID ;
                use = 'use' target+ ;
                target = ID ;
                leaf ID = "[a-z]+" ;
            }
            """.trimIndent()
        )
    }

    @Test
    fun index_in_referring_property_path() {
        val references = CrossReferenceString(
            """
            namespace test.Test
                identify Def by id
                references {
                    in Use { property target[0].id refers-to Def }
                }
            """.trimIndent()
        )
        val res = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = references)
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        val proc = res.processor!!
        proc.crossReferenceDomain // build it
        assertTrue(proc.allIssues.errors.isEmpty(), proc.allIssues.toString())

        val ok = proc.process("def a use a b", Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
        assertTrue(ok.allIssues.errors.isEmpty(), ok.allIssues.toString())
        println(ok.asm?.asString())

        // only the first target is a reference
        val bad = proc.process("def a use x b", Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
        assertTrue(bad.allIssues.errors.any { it.message.contains("'x'") }, bad.allIssues.toString())
    }

    @Test
    fun index_on_a_property_that_is_not_a_list_is_an_error() {
        val references = CrossReferenceString(
            """
            namespace test.Test
                identify Def by id
                references {
                    in Def { property id[0].x refers-to Def }
                }
            """.trimIndent()
        )
        val res = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = references)
        val proc = res.processor!!
        proc.crossReferenceDomain // build it
        assertTrue(proc.allIssues.errors.isNotEmpty(), proc.allIssues.toString())
    }
}
