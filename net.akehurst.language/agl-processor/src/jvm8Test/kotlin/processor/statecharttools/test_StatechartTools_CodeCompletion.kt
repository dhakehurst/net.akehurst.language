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
import net.akehurst.language.agl.CrossReferenceString
import net.akehurst.language.asm.simple.AsmPathSimple
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.collections.lazyMutableMapNonNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_StatechartTools_CodeCompletion {

    companion object {
        private val grammarStr = this::class.java.getResource("/Statecharts/version_/grammar.agl")?.readText() ?: error("File not found")

        private val crossReferenceModelStr = this::class.java.getResource("/Statecharts/version_/references.agl")?.readText() ?: error("File not found")

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
        private val processors = lazyMutableMapNonNull<SimpleName, LanguageProcessor<Asm, ContextAsmSimple>> { grmName ->
            val grm = grammarList.asm ?: error("Can't find grammar for '$grmName'")
            /*            val cfg = Agl.configuration {
                            targetGrammarName(null) //use default
                            defaultGoalRuleName(null) //use default
                            typeModelResolver { p -> ProcessResultDefault<TypeModel>(TypeModelFromGrammar.create(p.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                            crossReferenceModelResolver { p -> CrossReferenceModelDefault.fromString(ContextFromTypeModel(p.typeModel), scopeModelStr) }
                            syntaxAnalyserResolver { p ->
                                ProcessResultDefault(
                                    SyntaxAnalyserDefault(p.grammar!!.qualifiedName, p.typeModel, p.crossReferenceModel),
                                    IssueHolder(LanguageProcessorPhase.ALL)
                                )
                            }
                            semanticAnalyserResolver { p -> ProcessResultDefault(SemanticAnalyserDefault(p.typeModel, p.crossReferenceModel), IssueHolder(LanguageProcessorPhase.ALL)) }
                            styleResolver { p -> AglStyleModelDefault.fromString(ContextFromGrammar.createContextFrom(listOf(p.grammar!!)), "") }
                            formatterResolver { p -> AglFormatterModelFromAsm.fromString(ContextFromTypeModel(p.typeModel), formatterStr) }
                            completionProvider { p ->
                                ProcessResultDefault(
                                    CompletionProviderDefault(p.grammar!!, TypeModelFromGrammar.defaultConfiguration, p.typeModel, p.crossReferenceModel),
                                    IssueHolder(LanguageProcessorPhase.ALL)
                                )
                            }
                        }*/
            val cfg = Agl.configuration(Agl.configurationSimple()) {
                crossReferenceModelResolver { p -> CrossReferenceModelDefault.fromString(ContextFromTypeModel(p.typeModel), CrossReferenceString( crossReferenceModelStr)) }
            }
            Agl.processorFromGrammar(grm, cfg)
        }

        fun test_process_format(grammar: String, goal: String, sentence: String) {
            val result = processors[SimpleName(grammar)].process(sentence, Agl.options {
                parse { goalRuleName(goal) }
                semanticAnalysis { context(ContextAsmSimple()) }
            })
            assertTrue(result.issues.isEmpty(), result.issues.joinToString("\n") { it.toString() })
            val resultStr = processors[SimpleName(grammar)].formatAsm(result.asm!!).sentence
            assertEquals(sentence, resultStr)
        }
    }

    @Test
    fun expectedTerminalsAt_Transitions_TransitionSpecification_0() {
        val grammar = "Transitions"
        val goal = "TransitionSpecification"
        val sentence = ""
        val actual = processors[SimpleName(grammar)].expectedTerminalsAt(sentence, 0, 1, Agl.options {
            parse {
                goalRuleName(goal)
                //reportErrors(false)
            }
        }).items.map { it.text }.toSet().sorted()

        val expected = setOf("<ID>", "after", "every", "entry", "exit", "always", "oncycle", "[", "default", "else", "/", "#").sorted()
        assertEquals(expected, actual)
    }

    @Test
    fun expectedTerminalsAt_Transitions_TransitionSpecification_after() {
        val grammar = "Transitions"
        val goal = "TransitionSpecification"
        val sentence = "after "
        val actual = processors[SimpleName(grammar)].expectedTerminalsAt(sentence, 6, 1, Agl.options {
            parse {
                goalRuleName(goal)
                //reportErrors(false)
            }
        }).items.map { it.text }.toSet().sorted()

        val expected = setOf(
            "!", "#", "(", ",", ".", ".@", "/", "<BinaryLiteral>", "<BoolLiteral>", "<DoubleLiteral>",
            "<FloatLiteral>", "<HexLiteral>", "<ID>", "<IntLiteral>", "<PrefixUnaryOperator>", "<StringLiteral>", "[", "active", "null", "valueof"
        ).sorted()
        assertEquals(expected, actual)
    }

    @Test
    fun expectedItemsAt_Transitions_TransitionSpecification_after() {
        val grammar = "Transitions"
        val goal = "TransitionSpecification"
        val sentence = "after "
        val actual = processors[SimpleName(grammar)].expectedItemsAt(sentence, sentence.length, 1, Agl.options {
            parse {
                goalRuleName(goal)
                //reportErrors(false)
            }
            completionProvider {
                context(ContextAsmSimple())
            }
        }).items.map { it.text }.toSet().sorted()

        val expected = setOf("").sorted()
        assertEquals(expected, actual)
    }

    @Test
    fun expectedItemsAt_Global_TransitionSpecification_after() {
        val grammar = "Global"
        val goal = "StatechartSpecification"
        val sentence = """
            internal:
              var x:
        """.trimIndent()
        val context = contextAsmSimple {
            item("int", "external.BultInType", AsmPathSimple.EXTERNAL.value)
        }
        val actual = processors[SimpleName(grammar)].expectedItemsAt(sentence, sentence.length, 1, Agl.options {
            parse {
                goalRuleName(goal)
                //reportErrors(false)
            }
            completionProvider {
                context(context)
            }
        }).items.map { it.text }.toSet().sorted()

        val expected = setOf("int", "<ID>").sorted()
        assertEquals(expected, actual)
    }

}
