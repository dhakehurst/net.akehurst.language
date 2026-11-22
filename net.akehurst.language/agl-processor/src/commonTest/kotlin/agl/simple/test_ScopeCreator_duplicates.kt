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
import kotlin.test.assertTrue

/** duplicate definitions when several sentences are processed into one context */
class test_ScopeCreator_duplicates {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = member* ;
                member = block | use ;
                block = 'block' ID '{' member* '}' ;
                use = 'use' ID ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
        """.trimIndent()
        )
        val crossReferenceStr = CrossReferenceString(
            """
            namespace test.Test
                identify Block by id
                scope Block { identify Block by id }
                references {
                    in Use { property id refers-to Block }
                }
        """.trimIndent()
        )
        val processor = Agl.processorFromStringSimple(grammarStr, referenceStr = crossReferenceStr).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun errorsFor(context: SentenceContext, id: String, sentence: String) = processor.process(sentence, Agl.options {
            parse { sentenceIdentity { id } }
            semanticAnalysis { sentenceContext(context) }
        }).allIssues.errors.map { it.message }
    }

    @Test
    fun nested_name_same_as_top_level_name_of_other_sentence() {
        val ctx = contextAsmSimple()
        assertEquals(emptyList(), errorsFor(ctx, "s1", "block x { }"))
        assertEquals(emptyList(), errorsFor(ctx, "s2", "block a { block x { } use x }"))
    }

    @Test
    fun top_level_name_same_as_top_level_name_of_other_sentence__fail() {
        val ctx = contextAsmSimple()
        assertEquals(emptyList(), errorsFor(ctx, "s1", "block x { }"))
        val errors = errorsFor(ctx, "s2", "block x { }")
        assertEquals(1, errors.size, errors.toString())
        assertTrue(errors[0].startsWith("'x' with type 'test.Test.Block' already exists in scope"), errors.toString())
    }

    @Test
    fun nested_duplicate_in_one_sentence__fail() {
        val ctx = contextAsmSimple()
        val errors = errorsFor(ctx, "s1", "block a { block x { } block x { } }")
        assertEquals(1, errors.size, errors.toString())
        assertTrue(errors[0].startsWith("'x' with type 'test.Test.Block' already exists in scope"), errors.toString())
    }

    @Test
    fun reprocess_same_sentence() {
        val ctx = contextAsmSimple()
        assertEquals(emptyList(), errorsFor(ctx, "s1", "block a { block x { } }"))
        assertEquals(emptyList(), errorsFor(ctx, "s1", "block a { block x { } }"))
    }
}
