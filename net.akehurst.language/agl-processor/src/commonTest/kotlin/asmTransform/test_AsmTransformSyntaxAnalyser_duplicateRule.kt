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
import net.akehurst.language.api.processor.AsmTransformString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.expressions.api.CreateObjectExpression
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Only one transformation rule per grammar rule in an asm-transform (AT-Q4):
 * a further rule is an error on its name, the first rule is used.
 */
class test_AsmTransformSyntaxAnalyser_duplicateRule {

    @Test
    fun second_rule_for_same_grammar_rule__error() {
        val transform = """
            namespace example
            asm-transform Shapes {
                circle: A() { id := child[1] }
                circle: B() { id := child[1] }
            }
        """.trimIndent()
        val res = Agl.registry.agl.asmTransform.processor!!.process(transform)
        val errors = res.allIssues.errors.map { Pair(it.location, it.message) }
        assertEquals(listOf(Pair(InputLocation(80, 5, 4, 6, null), "Rule 'circle' is already transformed in 'example.Shapes'")), errors)
        val rs = res.asm!!.allDefinitions.first()
        assertEquals("A", (rs.rules[GrammarRuleName("circle")]!!.expression as CreateObjectExpression).possiblyQualifiedTypeName.value)
    }

    @Test
    fun processor__first_rule_used_and_error_reported() {
        val grammar = """
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
        val transform = """
            #override-default-transform
            #create-missing-types
            namespace example
            asm-transform Shapes {
                circle: A() { id := child[1] }
                circle: B() { id := child[1] }
            }
        """.trimIndent()
        val proc = Agl.processorFromStringSimple(GrammarString(grammar), transformStr = AsmTransformString(transform)).processor!!
        val res = proc.process("circle c")
        assertTrue(res.allIssues.errors.any { it.message.endsWith("Rule 'circle' is already transformed in 'example.Shapes'") }, res.allIssues.toString())
        assertTrue(res.asm!!.asString().contains(":A {"), res.asm!!.asString())
    }
}
