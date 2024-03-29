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
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.api.processor.LanguageProcessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_StatechartTools_Singles {

    companion object {
        private val grammarStr1 = this::class.java.getResource("/statechart-tools/Expressions.agl")?.readText() ?: error("File not found")
        private val grammarStr2 = this::class.java.getResource("/statechart-tools/SText.agl")?.readText() ?: error("File not found")

        private val formatterStr = """
           AssignmentExpression -> "§expression §assignmentOperator §expression2"
           FeatureCall -> "§elementReferenceExpression§\§list"
           ElementReferenceExpression -> "§id"
           PrimitiveValueExpression -> "§literal"
           

        """.replace("§", "\$")

        // must create processor for 'Expressions' so that SText can extend it
        val exprProcessor = Agl.processorFromStringDefault(
            grammarDefinitionStr = grammarStr1,
            grammarAglOptions = Agl.options { semanticAnalysis { option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false) } }
        ).processor!!
        var processor: LanguageProcessor<AsmSimple, ContextSimple> = Agl.processorFromStringDefault(
            grammarDefinitionStr = grammarStr2,
            formatterModelStr = formatterStr,
            grammarAglOptions = Agl.options { semanticAnalysis { option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false) } }
        ).processor!!
    }

    @org.junit.Test
    fun parse() {
        val goal = "Expression"
        val sentence = "true || false"
        val result = processor.parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.toString())
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun Expression_integer() {
        val goal = "Expression"
        val sentence = "integer"
        val result = processor.process(sentence, Agl.options { parse { goalRuleName(goal) } })
        assertTrue(result.issues.isEmpty(), result.issues.joinToString("\n") { it.toString() })


        val resultStr = processor.formatAsm(result.asm!!).sentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun ConditionalExpression_97() {
        val goal = "Expression"
        val sentence = "97"
        val result = processor.process(sentence, Agl.options { parse { goalRuleName(goal) } })
        assertTrue(result.issues.isEmpty(), result.issues.joinToString("\n") { it.toString() })


        val resultStr = processor.formatAsm(result.asm!!).sentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun AssignmentExpression_integer_AS_97() {
        val goal = "Expression"
        val sentence = "integer = 97"
        val result = processor.process(sentence, Agl.options { parse { goalRuleName(goal) } })
        assertTrue(result.issues.isEmpty(), result.issues.joinToString("\n") { it.toString() })


        val resultStr = processor.formatAsm(result.asm!!).sentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun ReactionEffect_integer_AS_97() {
        val goal = "ReactionEffect"
        val sentence = "integer = 97"
        val result = processor.process(sentence, Agl.options { parse { goalRuleName(goal) } })
        assertTrue(result.issues.isEmpty(), result.issues.joinToString("\n") { it.toString() })


        val resultStr = processor.formatAsm(result.asm!!).sentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun ScopeDeclaration_var_MyVar_integer() {
        val goal = "ScopeDeclaration"
        val sentence = "var MyVar : integer"
        val result = processor.process(sentence, Agl.options { parse { goalRuleName(goal) } })
        assertTrue(result.issues.isEmpty(), result.issues.joinToString("\n") { it.toString() })


        val resultStr = processor.formatAsm(result.asm!!).sentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun ScopeDeclaration_var_MyVar_integer_AS_97() {
        val goal = "ScopeDeclaration"
        val sentence = "var MyVar : integer = 97"
        val result = processor.process(sentence, Agl.options { parse { goalRuleName(goal) } })
        assertTrue(result.issues.isEmpty(), result.issues.joinToString("\n") { it.toString() })

        val resultStr = processor.formatAsm(result.asm!!).sentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun Expression_a_bF() {
        val goal = "Expression"
        val sentence = "a.b()"
        val result = processor.process(sentence, Agl.options { parse { goalRuleName(goal) } })
        assertTrue(result.issues.isEmpty(), result.issues.joinToString("\n") { it.toString() })


        val resultStr = processor.formatAsm(result.asm!!).sentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun Expression_a_bA() {
        val goal = "Expression"
        val sentence = "a.b[1]"
        val result = processor.process(sentence, Agl.options { parse { goalRuleName(goal) } })
        assertTrue(result.issues.isEmpty(), result.issues.joinToString("\n") { it.toString() })


        val resultStr = processor.formatAsm(result.asm!!).sentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun ReactionTrigger_exit() {
        val goal = "ReactionTrigger"
        val sentence = "exit"
        val result = processor.process(sentence, Agl.options { parse { goalRuleName(goal) } })
        assertTrue(result.issues.isEmpty(), result.issues.joinToString("\n") { it.toString() })


        val resultStr = processor.formatAsm(result.asm!!).sentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun TransitionReaction_xxx() {
        val goal = "TransitionReaction"
        val sentence = "after 10 s / raise ABC.intEvent"
        val result = processor.process(sentence, Agl.options { parse { goalRuleName(goal) } })
        assertTrue(result.issues.isEmpty(), result.issues.joinToString("\n") { it.toString() })


        val resultStr = processor.formatAsm(result.asm!!).sentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun StextTrigger_else() {
        val goal = "StextTrigger"
        val sentence = "else"
        val result = processor.process(sentence, Agl.options { parse { goalRuleName(goal) } })
        assertTrue(result.issues.isEmpty(), result.issues.joinToString("\n") { it.toString() })


        val resultStr = processor.formatAsm(result.asm!!).sentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun expectedAt_TransitionSpecification_0() {
        val goal = "TransitionSpecification"
        val sentence = ""
        val actual = processor.expectedTerminalsAt(sentence, 0, 1, Agl.options {
            parse {
                goalRuleName(goal)
                //reportErrors(false)
            }
        })
            .items.map {
                when (it.kind) {
                    CompletionItemKind.LITERAL -> it.text
                    CompletionItemKind.PATTERN -> it.name
                    CompletionItemKind.SEGMENT -> error("Not expected")
                }
            }.toSet().sorted()

        val expected = setOf("ID", "after", "every", "entry", "exit", "always", "oncycle", "[", "default", "else", "/", "#").sorted()
        assertEquals(expected, actual)
    }
}
