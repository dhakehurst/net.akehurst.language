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

package net.akehurst.language.asmTransform.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.contextFromGrammarAndTypesDomain
import net.akehurst.language.api.processor.AsmTransformString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.asmTransform.asm.AsmTransformDomainDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The namespace and name of an asm-transform must be those of its grammar (AT-Q3):
 * an error in the semantic analysis of the transform, and process gives issues rather than an exception.
 */
class test_AsmTransformSemanticAnalyser_ruleSetName {

    private companion object {
        val GRAMMAR = """
            namespace example
            grammar Shapes {
                skip leaf WS = "\s+" ;
                unit = item* ;
                item = circle | square ;
                circle = 'circle' ID ;
                square = 'square' ID ;
                leaf ID = "[a-z]+" ;
            }
        """.trimIndent()

        fun transformErrors(transform: String): List<String> {
            val proc = Agl.processorFromStringSimple(GrammarString(GRAMMAR)).processor!!
            val res = AsmTransformDomainDefault.fromString(
                contextFromGrammarAndTypesDomain(proc.grammarDomain!!, proc.baseTypesDomain),
                AsmTransformString(transform.trimIndent())
            )
            return res.allIssues.errors.map { it.message }
        }
    }

    @Test
    fun wrong_namespace__error() {
        val errors = transformErrors(
            """
            namespace other
            asm-transform Shapes {
                unit: Unit() { item := children }
            }
            """
        )
        assertTrue(
            errors.contains("No grammar 'other.Shapes' for asm-transform 'Shapes' in namespace 'other', the namespace and name of an asm-transform must be those of its grammar (grammars: example.Shapes)"),
            errors.toString()
        )
    }

    @Test
    fun wrong_name_with_override__one_error() {
        val errors = transformErrors(
            """
            namespace example
            asm-transform Shape {
                #override-default-transform
                circle: Circle() { id := child[1] }
            }
            """
        )
        assertEquals(
            listOf("No grammar 'example.Shape' for asm-transform 'Shape' in namespace 'example', the namespace and name of an asm-transform must be those of its grammar (grammars: example.Shapes)"),
            errors
        )
    }

    @Test
    fun wrong_namespace__process_gives_issues_not_exception() {
        val transform = """
            namespace other
            asm-transform Shapes {
                unit: Unit() { item := children }
            }
        """.trimIndent()
        val proc = Agl.processorFromStringSimple(GrammarString(GRAMMAR), transformStr = AsmTransformString(transform)).processor
        assertNotNull(proc)
        val res = proc.process("circle c")
        assertNull(res.asm)
        val messages = res.allIssues.errors.map { it.message }
        assertTrue(messages.any { it.contains("No grammar 'other.Shapes' for asm-transform 'Shapes'") }, messages.toString())
        assertTrue(messages.any { it.contains("No asm-transform rule set for the target grammar 'example.Shapes'") }, messages.toString())
    }
}
