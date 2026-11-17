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
 * errors in a definition of the language (here the references) are in the result of every 'process',
 * and an identify path may go through the supertype of a grammar choice to a property of one of its alternatives
 */
class test_LanguageProcessor_definitionErrors {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = block* ;
                block = 'block' head ;
                head = named | unnamed ;
                named = ID ;
                unnamed = '_' ;
                leaf ID = "[a-z]+" ;
            }
        """.trimIndent()
        )

        fun processorFor(references: String) = Agl.processorFromStringSimple(grammarStr, referenceStr = CrossReferenceString(references)).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun errorsFor(references: String, sentence: String): List<String> {
            val processor = processorFor(references)
            return processor.process(sentence, Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } }).allIssues.errors.map { it.message }
        }
    }

    @Test
    fun identify_through_choice_supertype() {
        // 'head' has the type Head (the supertype of the choice alternatives Named and Unnamed), only Named has 'id'
        assertEquals(emptyList(), errorsFor("namespace test.Test\n  identify Block by head.id", "block a block _"))
    }

    @Test
    fun identify_property_of_no_alternative__error_in_every_process_result() {
        val processor = processorFor("namespace test.Test\n  identify Block by head.nope")
        val expected = "In the references definition of the language: In root scope, 'head.nope' not found for identifying property of 'Block'"
        val errors1 = processor.process("block a", Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } }).allIssues.errors.map { it.message }
        assertTrue(errors1.contains(expected), errors1.toString())
        val errors2 = processor.process("block b", Agl.options { semanticAnalysis { sentenceContext(contextAsmSimple()) } }).allIssues.errors.map { it.message }
        assertTrue(errors2.contains(expected), errors2.toString())
    }
}
