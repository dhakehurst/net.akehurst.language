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
 * A transformation rule for a grammar rule that the grammar does not have is an error, located on the rule name (AT-Q1).
 */
class test_AsmTransformSemanticAnalyser_unknownGrammarRule {

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

        fun analyse(transform: String): List<Pair<InputLocation?, String>> {
            val proc = Agl.processorFromStringSimple(GrammarString(GRAMMAR)).processor!!
            val res = AsmTransformDomainDefault.fromString(
                contextFromGrammarAndTypesDomain(proc.grammarDomain!!, proc.baseTypesDomain),
                AsmTransformString(transform.trimIndent())
            )
            return res.allIssues.errors.map { Pair(it.location, it.message) }
        }

        const val MESSAGE = "Grammar 'example.Shapes' has no rule 'nosuchrule' (in the transformation rule for 'nosuchrule')"
    }

    @Test
    fun create_rule_without_override() {
        val errors = analyse(
            """
            namespace example
            asm-transform Shapes {
                unit: Unit() { item := children }
                item: child[0] as Item
                circle: Circle() { id := child[1] }
                square: Square() { id := child[1] }
                nosuchrule: Circle() { id := child[1] }
            }
            """
        )
        assertEquals(listOf(Pair(InputLocation(190, 5, 7, 10, null), MESSAGE)), errors)
    }

    @Test
    fun create_rule_with_override() {
        val errors = analyse(
            """
            #override-default-transform
            namespace example
            asm-transform Shapes {
                nosuchrule: Circle() { id := child[1] }
            }
            """
        )
        assertEquals(listOf(MESSAGE), errors.map { it.second })
        assertEquals(InputLocation(73, 5, 4, 10, null), errors[0].first)
    }

    @Test
    fun type_only_rule_with_override__error_not_exception() {
        // was IllegalStateException 'Should exist!' (AT1)
        val errors = analyse(
            """
            #override-default-transform
            #create-missing-types
            namespace example
            asm-transform Shapes {
                nosuchrule: Foo
            }
            """
        )
        assertEquals(listOf(MESSAGE), errors.map { it.second })
    }

    @Test
    fun processor_gives_definition_error_and_still_processes() {
        val transform = """
            #override-default-transform
            #create-missing-types
            namespace example
            asm-transform Shapes {
                nosuchrule: Foo
            }
        """.trimIndent()
        val proc = Agl.processorFromStringSimple(GrammarString(GRAMMAR), transformStr = AsmTransformString(transform)).processor!!
        val res = proc.process("circle c square s")
        assertTrue(res.allIssues.errors.any { it.message.endsWith(MESSAGE) }, res.allIssues.toString())
        assertTrue(null != res.asm)
    }
}
