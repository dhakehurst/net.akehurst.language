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

import net.akehurst.language.agl.grammar.grammar.AglGrammarSemanticAnalyser
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.api.processor.LanguageProcessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_StatechartTools_Singles {

    companion object {
        private val grammarStr1 = this::class.java.getResource("/statechart-tools/Expressions.agl")?.readText() ?: error("File not found")
        private val grammarStr2 = this::class.java.getResource("/statechart-tools/SText.agl")?.readText() ?: error("File not found")

        // must create processor for 'Expressions' so that SText can extend it
        val exprProcessor = Agl.processorFromString<Any, Any>(
            grammarDefinitionStr = grammarStr1,
            aglOptions = Agl.options { semanticAnalysis { option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS,false) } }
        )
        var processor: LanguageProcessor<Any, Any> = Agl.processorFromString(
            grammarDefinitionStr = grammarStr2,
            aglOptions = Agl.options { semanticAnalysis { option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS,false) } }
        )
    }

    @Test
    fun ConditionalExpression_integer() {
        val goal = "Expression"
        val sentence = "integer"
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)

        val resultStr = result.sppt!!.asString
        assertEquals(sentence, resultStr)
    }

    @Test
    fun ConditionalExpression_97() {
        val goal = "Expression"
        val sentence = "97"
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)

        val resultStr = result.sppt!!.asString
        assertEquals(sentence, resultStr)
    }

    @Test
    fun AssignmentExpression_integer_AS_97() {
        val goal = "Expression"
        val sentence = "integer = 97"
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)

        val resultStr = result.sppt!!.asString
        assertEquals(sentence, resultStr)
    }

    @Test
    fun ReactionEffect_integer_AS_97() {
        val goal = "ReactionEffect"
        val sentence = "integer = 97"
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)

        val resultStr = result.sppt!!.asString
        assertEquals(sentence, resultStr)
    }

    @Test
    fun ScopeDeclaration_integer_AS_97() {
        val goal = "ScopeDeclaration"
        val sentence = "var MyVar : integer = 97"
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)

        val resultStr = result.sppt!!.asString
        assertEquals(sentence, resultStr)
    }

    @Test
    fun expectedAt_TransitionSpecification_0() {
        val goal = "TransitionSpecification"
        val sentence = ""
        val actual = processor.expectedTerminalsAt(sentence, 0, 1, Agl.options { parse { goalRuleName(goal) } })
            .items.map {
                when (it.kind) {
                    CompletionItemKind.LITERAL -> it.text
                    CompletionItemKind.PATTERN -> it.ruleName
                    CompletionItemKind.SEGMENT -> error("Not expected")
                }
            }.toSet().sorted()

        val expected = setOf("ID", "after", "every", "entry", "exit", "always", "oncycle", "[", "default", "else", "/", "#").sorted()
        assertEquals(expected, actual)
    }
}
