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

/** an unnamed element whose type has a scope gets an anonymous scope: its members are separate from the parent's, found from inside only */
class test_SemanticAnalyserSimple_anonymousScope {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = member* ;
                member = block | use ;
                block = 'block' ID? '{' member* '}' ;
                use = 'use' ID ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
        """.trimIndent()
        )
        val crossReferenceStr = CrossReferenceString(
            """
            namespace test.Test
                identify Block by id
                scope Block { }
                references {
                    in Use {
                      property id refers-to Block
                    }
                }
        """.trimIndent()
        )
        val processor = Agl.processorFromStringSimple(grammarStr, referenceStr = crossReferenceStr).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun errorsFor(sentence: String) = processor.process(sentence, Agl.options {
            semanticAnalysis { sentenceContext(contextAsmSimple()) }
        }).allIssues.errors.map { it.message }
    }

    @Test
    fun same_name_in_two_unnamed_blocks() {
        assertEquals(emptyList(), errorsFor("block { block x { } } block { block x { } }"))
    }

    @Test
    fun duplicate_in_one_unnamed_block__fail() {
        val errors = errorsFor("block { block x { } block x { } }")
        assertEquals(1, errors.size, errors.toString())
        assertTrue(errors[0].startsWith("'x' with type 'test.Test.Block' already exists in scope"), errors.toString())
    }

    @Test
    fun reference_inside_unnamed_block() {
        assertEquals(emptyList(), errorsFor("block { block x { } use x }"))
    }

    @Test
    fun reference_from_unnamed_block_to_outside() {
        assertEquals(emptyList(), errorsFor("block y { } block { use y }"))
    }

    @Test
    fun reference_into_unnamed_block_from_outside__fail() {
        val errors = errorsFor("block { block x { } } use x")
        assertEquals(1, errors.size, errors.toString())
    }
}
