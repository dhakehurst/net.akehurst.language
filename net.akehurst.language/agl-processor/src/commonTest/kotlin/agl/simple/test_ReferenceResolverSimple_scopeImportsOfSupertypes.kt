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
 * The 'import' and 'inherit' lines of the scope blocks of a type and of its supertypes are combined, nearest first
 * (a nearer block does not replace the lines of a farther one).
 */
class test_ReferenceResolverSimple_scopeImportsOfSupertypes {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = item* ;
                item = def | sub ;
                member = item | use ;
                def = 'def' ID body? ;
                sub = 'sub' ID 'of' ID body? ;
                body = '{' member* '}' ;
                use = 'use' ID ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
            """.trimIndent()
        )
        val referenceStr = CrossReferenceString(
            """
            namespace test.Test
                identify Def by id
                identify Sub by id
                scope Def { identify Def by id }
                scope Item { inherit named U }
                scope Sub { identify Def by id inherit members-of id2 }
                references {
                    in Sub { property id2 refers-to Def }
                    in Use { property id refers-to Def }
                }
            """.trimIndent()
        )
        val processor = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = referenceStr).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun errors(sentence: String) =
            processor.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } }).allIssues.errors.map { it.message }
    }

    @Test
    fun lines_of_supertype_block_are_kept() {
        // M is inherited by Sub's own block, N by the block of Item (a supertype of Sub)
        assertEquals(emptyList(), errors("def A { def M } def U { def N } sub B of A { use M use N }"))
    }

    @Test
    fun without_item_block__N_not_found() {
        val p = Agl.processorFromStringSimple(grammarDefinitionStr = grammarStr, referenceStr = CrossReferenceString(referenceStr.value.replace("scope Item { inherit named U }", ""))).processor!!
        val e = p.process("def A { def M } def U { def N } sub B of A { use M use N }", Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } }).allIssues.errors.map { it.message }
        assertEquals(1, e.size, e.toString())
    }

    @Test
    fun not_found_in_either() {
        val e = errors("def A { def M } def U { def N } def V { def O } sub B of A { use O }")
        assertEquals(1, e.size, e.toString())
    }
}
