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
import net.akehurst.language.agl.semanticAnalyser.ContextSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.collections.lazyMutableMapNonNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_StatechartTools_Singles {

    companion object {
        private val grammarStr = this::class.java.getResource("/itemis-create/version_/grammar.agl")?.readText() ?: error("File not found")

        private val formatterStr = """
           AssignmentExpression -> "§expression §assignmentOperator §expression2"
           FeatureCall -> "§elementReferenceExpression§\§list"
           ElementReferenceExpression -> "§id"
           PrimitiveValueExpression -> "§literal"
        """.replace("§", "\$")

        private val grammarList = Agl.registry.agl.grammar.processor!!.process(grammarStr)
        private val processors = lazyMutableMapNonNull<String, LanguageProcessor<AsmSimple, ContextSimple>> { grmName ->
            val grm = grammarList.asm?.firstOrNull { it.name == grmName } ?: error("Can't find grammar for '$grmName'")
            val cfg = Agl.configurationDefault()
            Agl.processorFromGrammar(grm, cfg)
        }

        fun test(grammar: String, goal: String, sentence: String) {
            val result = processors[grammar].process(sentence, Agl.options {
                parse { goalRuleName(goal) }
                semanticAnalysis { context(ContextSimple()) }
            })
            assertTrue(result.issues.isEmpty(), result.issues.joinToString("\n") { it.toString() })
            val resultStr = processors[grammar].formatAsm(result.asm!!).sentence
            assertEquals(sentence, resultStr)
        }
    }

    @org.junit.Test
    fun parse() {
        val grammar = "Expressions"
        val goal = "Expression"
        val sentence = "true || false"
        val result = processors[grammar].parse(sentence, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt, result.issues.toString())
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun Expression_integer() {
        val grammar = "Expressions"
        val goal = "Expression"
        val sentence = "integer"
        test(grammar, goal, sentence)
    }

    @Test
    fun ConditionalExpression_97() {
        val grammar = "Expressions"
        val goal = "Expression"
        val sentence = "97"
        test(grammar, goal, sentence)
    }

    @Test
    fun AssignmentExpression_integer_AS_97() {
        val grammar = "Expressions"
        val goal = "Expression"
        val sentence = "integer = 97"
        test(grammar, goal, sentence)
    }

    @Test
    fun ReactionEffect_integer_AS_97() {
        val grammar = "States"
        val goal = "ReactionEffect"
        val sentence = "integer = 97"
        test(grammar, goal, sentence)
    }

    @Test
    fun ScopeDeclaration_var_MyVar_integer() {
        val grammar = "Global"
        val goal = "ScopeDeclaration"
        val sentence = "var MyVar : integer"
        test(grammar, goal, sentence)
    }

    @Test
    fun ScopeDeclaration_var_MyVar_integer_AS_97() {
        val grammar = "Global"
        val goal = "ScopeDeclaration"
        val sentence = "var MyVar : integer = 97"
        test(grammar, goal, sentence)
    }

    @Test
    fun Expression_a_bF() {
        val grammar = "Expressions"
        val goal = "Expression"
        val sentence = "a.b()"
        test(grammar, goal, sentence)
    }

    @Test
    fun Expression_a_bA() {
        val grammar = "Expressions"
        val goal = "Expression"
        val sentence = "a.b[1]"
        test(grammar, goal, sentence)
    }

    @Test
    fun ReactionTrigger_exit() {
        val grammar = "Transitions"
        val goal = "ReactionTrigger"
        val sentence = "exit"
        test(grammar, goal, sentence)
    }

    @Test
    fun TransitionReaction_xxx() {
        val grammar = "Transitions"
        val goal = "TransitionReaction"
        val sentence = "after 10 s / raise ABC.intEvent"
        test(grammar, goal, sentence)
    }

    @Test
    fun StextTrigger_else() {
        val grammar = "Transitions"
        val goal = "StextTrigger"
        val sentence = "else"
        test(grammar, goal, sentence)
    }

    @Test
    fun expectedAt_TransitionSpecification_0() {
        val grammar = "Transitions"
        val goal = "TransitionSpecification"
        val sentence = ""
        val actual = processors[grammar].expectedTerminalsAt(sentence, 0, 1, Agl.options {
            parse {
                goalRuleName(goal)
                //reportErrors(false)
            }
        }).items.map { it.text }.toSet().sorted()

        val expected = setOf("<ID>", "after", "every", "entry", "exit", "always", "oncycle", "[", "default", "else", "/", "#").sorted()
        assertEquals(expected, actual)
    }

    @Test
    fun expectedTerminalsAt_TransitionSpecification_after() {
        val grammar = "Transitions"
        val goal = "TransitionSpecification"
        val sentence = "after "
        val actual = processors[grammar].expectedTerminalsAt(sentence, 6, 1, Agl.options {
            parse {
                goalRuleName(goal)
                //reportErrors(false)
            }
        }).items.map { it.text }.toSet().sorted()

        val expected = setOf("").sorted()
        assertEquals(expected, actual)
    }

    @Test
    fun expectedItemsAt_TransitionSpecification_after() {
        val grammar = "Transitions"
        val goal = "TransitionSpecification"
        val sentence = "after "
        val actual = processors[grammar].expectedItemsAt(sentence, 6, 1, Agl.options {
            parse {
                goalRuleName(goal)
                //reportErrors(false)
            }
            completionProvider {
                context(ContextSimple())
            }
        }).items.map { it.text }.toSet().sorted()

        val expected = setOf("").sorted()
        assertEquals(expected, actual)
    }
}
