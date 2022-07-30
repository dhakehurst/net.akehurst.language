/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.agl.processor.statecharttools

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.processor.LanguageProcessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_StatechartTools_Singles {

    companion object {
        private val grammarStr1 = this::class.java.getResource("/statechart-tools/Expressions.agl")?.readText() ?: error("File not found")
        private val grammarStr2 = this::class.java.getResource("/statechart-tools/SText.agl")?.readText() ?: error("File not found")

        // must create processor for 'Expressions' so that SText can extend it
        val exprProcessor = Agl.processorFromString<Any, Any>(grammarStr1)
        var processor: LanguageProcessor<Any, Any> = Agl.processorFromString(grammarStr2)
    }

    @Test
    fun ConditionalExpression_integer() {
        val goal = "Expression"
        val sentence = "integer"
        val result = processor.parse(sentence, processor.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)

        val resultStr = result.sppt!!.asString
        assertEquals(sentence, resultStr)
    }

    @Test
    fun ConditionalExpression_97() {
        val goal = "Expression"
        val sentence = "97"
        val result = processor.parse(sentence, processor.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)

        val resultStr = result.sppt!!.asString
        assertEquals(sentence, resultStr)
    }

    @Test
    fun AssignmentExpression_integer_AS_97() {
        val goal = "Expression"
        val sentence = "integer = 97"
        val result = processor.parse(sentence, processor.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)

        val resultStr = result.sppt!!.asString
        assertEquals(sentence, resultStr)
    }

    @Test
    fun ScopeDeclaration_integer_AS_97() {
        val goal = "ScopeDeclaration"
        val sentence = "var MyVar : integer = 97"
        val result = processor.parse(sentence, processor.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)

        val resultStr = result.sppt!!.asString
        assertEquals(sentence, resultStr)
    }

    @Test
    fun expectedAt_TransitionSpecification_0() {
        val goal = "TransitionSpecification"
        val sentence = ""
        val actual = processor.expectedAt(sentence, 0, 1, processor.options { parse { goalRuleName(goal) } }).items.map { it.text }

        val expected = listOf<String>("ID", "after", "every", "entry", "exit", "always", "oncycle", "[", "default", "else", "/", "#")
        assertEquals(expected, actual)
    }
}
