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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.default.SemanticAnalyserDefault
import net.akehurst.language.agl.default.SyntaxAnalyserDefault
import net.akehurst.language.agl.language.format.AglFormatterModelFromAsm
import net.akehurst.language.agl.language.grammar.ContextFromGrammarRegistry
import net.akehurst.language.agl.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.agl.default.ContextAsmDefault
import net.akehurst.language.api.asm.Asm
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.collections.lazyMutableMapNonNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_StatechartTools_Singles {

    companion object {
        private val grammarStr = this::class.java.getResource("/Statecharts/version_/grammar.agl")?.readText() ?: error("File not found")

        private val scopeModelStr = this::class.java.getResource("/Statecharts/version_/references.agl")?.readText() ?: error("File not found")

        private val formatterStr = """
           namespace com.itemis.create.Expressions {
               AssignmentExpression -> "§expression §assignmentOperator §expression2"
               FeatureCall -> "§elementReferenceExpression\§list"
               RootElement -> id
               FunctionCall -> "§id(§{argumentList.arguments.join(', ')})"
           }
           namespace com.itemis.create.Global {
               VariableDeclaration -> "§variableDeclarationKind §id" 
               ElementReferenceExpression -> id
               PrimitiveValueExpression -> literal
           }
        """.replace("§", "\$")

        private val grammarList = Agl.registry.agl.grammar.processor!!.process(grammarStr, Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } })
        private val processors = lazyMutableMapNonNull<String, LanguageProcessor<Asm, ContextAsmDefault>> { grmName ->
            val grm = grammarList.asm?.allDefinitions?.firstOrNull { it.name.value == grmName } ?: error("Can't find grammar for '$grmName'")
            val cfg = Agl.configuration {
                targetGrammarName(null) //use default
                defaultGoalRuleName(null) //use default
                // typeModelResolver { p -> ProcessResultDefault<TypeModel>(TypeModelFromGrammar.create(p.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                crossReferenceModelResolver { p -> CrossReferenceModelDefault.fromString(ContextFromTypeModel(p.typeModel), scopeModelStr) }
                syntaxAnalyserResolver { p ->
                    ProcessResultDefault(
                        SyntaxAnalyserDefault(p.typeModel, p.asmTransformModel, p.grammar!!.qualifiedName),
                        IssueHolder(LanguageProcessorPhase.ALL)
                    )
                }
                semanticAnalyserResolver { p -> ProcessResultDefault(SemanticAnalyserDefault(p.typeModel, p.crossReferenceModel), IssueHolder(LanguageProcessorPhase.ALL)) }
                //styleResolver { p -> AglStyleModelDefault.fromString(ContextFromGrammar.createContextFrom(listOf(p.grammar!!)), "") }
                formatterResolver { p -> AglFormatterModelFromAsm.fromString(ContextFromTypeModel(p.typeModel), formatterStr) }
                // completionProvider { p ->
                //     ProcessResultDefault(
                //         CompletionProviderDefault(p.grammar!!, TypeModelFromGrammar.defaultConfiguration, p.typeModel, p.crossReferenceModel),
                //         IssueHolder(LanguageProcessorPhase.ALL)
                //     )
                // }
            }
            Agl.processorFromGrammar(grm, cfg)
        }

        fun test_process_format(grammar: String, goal: String, sentence: String) {
            val result = processors[grammar].process(sentence, Agl.options {
                parse { goalRuleName(goal) }
                semanticAnalysis { context(ContextAsmDefault()) }
            })
            assertTrue(result.issues.isEmpty(), result.issues.joinToString("\n") { it.toString() })
            val resultStr = processors[grammar].formatAsm(result.asm!!).sentence
            assertEquals(sentence, resultStr)
        }
    }

    @Test
    fun parse_Expressions_Expression_true_OR_false_parse() {
        val grammar = "Expressions"
        val goal = "Expression"
        val sentence = "true || false"
        val result = processors[grammar].parse(sentence, ParseOptionsDefault(goal))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(sentence, resultStr)
    }

    @Test
    fun process_Expressions_Expression_integer() {
        val grammar = "Expressions"
        val goal = "Expression"
        val sentence = "integer"
        test_process_format(grammar, goal, sentence)
    }

    @Test
    fun process_Expressions_Expressions_97() {
        val grammar = "Expressions"
        val goal = "Expression"
        val sentence = "97"
        test_process_format(grammar, goal, sentence)
    }

    @Test
    fun process_Expressions_Expression_integer_ASS_97() {
        val grammar = "Expressions"
        val goal = "Expression"
        val sentence = "integer = 97"
        test_process_format(grammar, goal, sentence)
    }

    @Test
    fun process_States_ReactionEffect_integer_ASS_97() {
        val grammar = "States"
        val goal = "ReactionEffect"
        val sentence = "integer = 97"
        test_process_format(grammar, goal, sentence)
    }

    @Test
    fun process_Global_StatechartLevelDeclaration_var_MyVar_integer() {
        val grammar = "Global"
        val goal = "StatechartLevelDeclaration"
        val sentence = "internal: var MyVar : integer"
        test_process_format(grammar, goal, sentence)
    }

    @Test
    fun process_Global_StatechartLevelDeclaration_var_MyVar_integer_ASS_97() {
        val grammar = "Global"
        val goal = "StatechartLevelDeclaration"
        val sentence = "internal: var MyVar : integer = 97"
        test_process_format(grammar, goal, sentence)
    }

    @Test
    fun process_Expressions_Expression_a_bF() {
        val grammar = "Expressions"
        val goal = "Expression"
        val sentence = "a.b()"
        test_process_format(grammar, goal, sentence)
    }

    @Test
    fun process_Expressions_Expression_a_bA() {
        val grammar = "Expressions"
        val goal = "Expression"
        val sentence = "a.b[1]"
        test_process_format(grammar, goal, sentence)
    }

    @Test
    fun process_Transitions_ReactionTrigger_exit() {
        val grammar = "Transitions"
        val goal = "ReactionTrigger"
        val sentence = "exit"
        test_process_format(grammar, goal, sentence)
    }

    @Test
    fun process_Transitions_TransitionReaction_after_10_s__raise_ABD_intEvent() {
        val grammar = "Transitions"
        val goal = "TransitionReaction"
        val sentence = "after 10 s / raise ABC.intEvent"
        test_process_format(grammar, goal, sentence)
    }

    @Test
    fun process_Transitions_StextTrigger_else() {
        val grammar = "Transitions"
        val goal = "StextTrigger"
        val sentence = "else"
        test_process_format(grammar, goal, sentence)
    }

}
