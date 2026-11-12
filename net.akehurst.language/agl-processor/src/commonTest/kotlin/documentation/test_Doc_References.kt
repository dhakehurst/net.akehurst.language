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

package net.akehurst.language.documentation

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.documentation.test_Doc_GettingStarted.Companion.SHAPES_GRAMMAR
import net.akehurst.language.documentation.test_Doc_GettingStarted.Companion.SHAPES_SENTENCE
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The examples in 5_Documentation/dsl/references.md.
 */
class test_Doc_References {

    private companion object {
        // references.md: "Example"
        val REFERENCES = """
            namespace example.Shapes
                identify Circle by name
                identify Square by name
                references {
                    in Connection {
                        property source refers-to Circle|Square
                        property target refers-to Circle|Square
                    }
                }
        """.trimIndent()

        val processor by lazy {
            val result = Agl.processorFromStringSimple(
                grammarDefinitionStr = GrammarString(SHAPES_GRAMMAR),
                referenceStr = CrossReferenceString(REFERENCES)
            )
            check(result.issues.errors.isEmpty()) { result.issues.toString() }
            result.processor!!
        }

        val options = Agl.options<Asm, SentenceContext> { semanticAnalysis { sentenceContext(contextAsmSimple()) } }
    }

    @Test
    fun resolved() {
        val result = processor.process(SHAPES_SENTENCE, options)
        assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())
        val asm = result.asm!!.asString()
        assertTrue(asm.contains("source = &{'c' : Circle}"), asm)
        assertTrue(asm.contains("target = &{'s' : Square}"), asm)
    }

    @Test
    fun not_resolved() {
        val result = processor.process("circle c connect c to x", options)
        assertEquals(
            listOf("Reference 'x' not resolved, to type(s) [Circle, Square] in scope '/'"),
            result.allIssues.errors.map { it.message }
        )
    }
}
