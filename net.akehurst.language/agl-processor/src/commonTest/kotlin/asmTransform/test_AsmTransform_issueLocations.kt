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
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Issues about a transform definition are located on the rule name (or the assignment),
 * issues of syntax analysis on the sentence text of the node (AT-Q7).
 */
class test_AsmTransform_issueLocations {

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

        fun transformErrors(transform: String): List<Pair<InputLocation?, String>> {
            val proc = Agl.processorFromStringSimple(GrammarString(GRAMMAR)).processor!!
            val res = AsmTransformDomainDefault.fromString(
                contextFromGrammarAndTypesDomain(proc.grammarDomain!!, proc.baseTypesDomain),
                AsmTransformString(transform.trimIndent())
            )
            return res.allIssues.errors.map { Pair(it.location, it.message) }
        }
    }

    @Test
    fun type_not_found__on_rule_name() {
        val errors = transformErrors(
            """
            #override-default-transform
            namespace example
            asm-transform Shapes {
                circle: NoSuchType() { id := child[1] }
            }
            """
        )
        assertEquals(
            listOf(Pair(InputLocation(73, 5, 4, 6, null), "Type 'NoSuchType' for rule 'circle' not found, define it or use '#create-missing-types'")),
            errors
        )
    }

    @Test
    fun modify_rule_unknown_property__on_assignment() {
        val errors = transformErrors(
            """
            #override-default-transform
            namespace example
            asm-transform Shapes {
                circle: { Circle -> nosuchprop := child[1] }
            }
            """
        )
        assertEquals(
            listOf(Pair(InputLocation(93, 25, 4, 23, null), "Type 'example.Shapes.Circle' has no property 'nosuchprop' (set in the transformation rule for 'circle'), use '#create-missing-types' to create it")),
            errors
        )
    }

    @Test
    fun missing_rule__on_asm_transform_and_sentence_node() {
        val transform = """
            namespace example
            asm-transform Shapes {
                circle: Circle() { id := child[1] }
            }
        """.trimIndent()
        val errors = transformErrors(transform)
        assertTrue(errors.isNotEmpty() && errors.all { it.first == InputLocation(18, 1, 2, 64, null) }, errors.toString())

        val proc = Agl.processorFromStringSimple(GrammarString(GRAMMAR), transformStr = AsmTransformString(transform)).processor!!
        val res = proc.process("square s")
        val saErrors = res.allIssues.errors.filter { it.message.startsWith("No transformation rule found for grammar rule 'square'") }
        assertEquals(listOf(InputLocation(0, 1, 1, 8, null)), saErrors.map { it.location }, res.allIssues.toString())
    }

    @Test
    fun missing_rule__reported_once_per_sentence_on_first_node() {
        // AT-Q16
        val transform = """
            namespace example
            asm-transform Shapes {
                circle: Circle() { id := child[1] }
            }
        """.trimIndent()
        val proc = Agl.processorFromStringSimple(GrammarString(GRAMMAR), transformStr = AsmTransformString(transform)).processor!!
        repeat(2) {
            val res = proc.process("square s square t")
            val saErrors = res.allIssues.errors.filter { it.message.startsWith("No transformation rule found for grammar rule 'square'") }
            assertEquals(listOf(InputLocation(0, 1, 1, 8, null)), saErrors.map { it.location }, res.allIssues.toString())
        }
    }
}
