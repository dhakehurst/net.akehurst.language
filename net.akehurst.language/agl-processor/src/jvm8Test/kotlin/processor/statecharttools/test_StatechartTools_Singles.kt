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
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.agl.simple.SemanticAnalyserSimple
import net.akehurst.language.agl.simple.SyntaxAnalyserSimple
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.format.asm.AglFormatModelDefault
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_StatechartTools_Singles {

    companion object {
        private val grammarStr = GrammarString(this::class.java.getResource("/Statecharts/version_/grammar.agl")?.readText() ?: error("File not found"))

        private val scopeModelStr = CrossReferenceString(this::class.java.getResource("/Statecharts/version_/references.agl")?.readText() ?: error("File not found"))

        private val formatterStr = FormatString($$"""
           namespace com.itemis.create
             format Expressions {
               PrimitiveValueExpression -> "$literal"
               AssignmentExpression -> "$expression $assignmentOperator $expression2"
               InfixExpression -> "$[expression / ' ']"
               RootElement -> id
               FunctionCall -> "$id(${argumentList.arguments.join(', ')})"
             }
           
             format Global : Expressions {
               VariableDeclaration -> "$variableDeclarationKind $id" 
               ElementReferenceExpression -> id
               PrimitiveValueExpression -> literal
             }
             
             format States : Expressions {
               ReactionEffect -> "$self"
             }
           
             format Transitions : Expressions {
             
             }
             
             format Statechart {
             
             }
             
        """)

        private val grammarList = Agl.registry.agl.grammar.processor!!.process(grammarStr.value, Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } })
            .also {
                assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            }
        private val processors = lazyMutableMapNonNull<String, LanguageProcessor<Asm, ContextAsmSimple>> { grmName ->
            val grm = grammarList.asm ?: error("Can't find grammar for '$grmName'")
            val cfg = Agl.configuration {
                targetGrammarName(grmName) //use default
                defaultGoalRuleName(null) //use default
                // typeModelResolver { p -> ProcessResultDefault<TypeModel>(TypeModelFromGrammar.create(p.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                crossReferenceResolver { p -> CrossReferenceModelDefault.fromString(ContextFromTypeModel(p.typeModel), scopeModelStr) }
                syntaxAnalyserResolver { p ->
                    ProcessResultDefault(
                        SyntaxAnalyserSimple(p.typeModel, p.asmTransformModel, p.targetGrammar!!.qualifiedName),
                        IssueHolder(LanguageProcessorPhase.ALL)
                    )
                }
                semanticAnalyserResolver { p -> ProcessResultDefault(SemanticAnalyserSimple(p.typeModel, p.crossReferenceModel), IssueHolder(LanguageProcessorPhase.ALL)) }
                //styleResolver { p -> AglStyleModelDefault.fromString(ContextFromGrammar.createContextFrom(listOf(p.grammar!!)), "") }
                formatResolver { p -> AglFormatModelDefault.fromString(ContextFromTypeModel(p.typeModel), formatterStr) }
//TODO                formatterResolver { p -> FormatterSimple(p.) }
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
                semanticAnalysis { context(ContextAsmSimple()) }
            })
            assertTrue(result.issues.errors.isEmpty(), result.issues.joinToString("\n") { it.toString() })
            val resultStr = processors[grammar].formatAsm(result.asm!!).sentence
            assertEquals(sentence, resultStr)
        }
    }

    @Test
    fun parse_Expressions_Expression_true_OR_false_parse() {
        val grammar = "Expressions"
        val goal = "Expression"
        val sentence = "true || false"
        val result = processors[grammar].parse(sentence, ParseOptionsDefault(goalRuleName = goal))
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
