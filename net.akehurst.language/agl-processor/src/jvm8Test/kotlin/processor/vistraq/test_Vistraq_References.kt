/**
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor.vistraq

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.default_.ContextAsmDefault
import net.akehurst.language.agl.default_.SemanticAnalyserDefault
import net.akehurst.language.agl.default_.SyntaxAnalyserDefault
import net.akehurst.language.agl.default_.contextAsmDefault
import net.akehurst.language.transform.asm.TransformModelDefault
import net.akehurst.language.agl.language.format.AglFormatterModelFromAsm
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.agl.semanticAnalyser.TestContextSimple
import net.akehurst.language.api.asm.Asm
import net.akehurst.language.parser.api.InputLocation
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.collections.lazyMutableMapNonNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_Vistraq_References {

    companion object {
        private val grammarStr = this::class.java.getResource("/vistraq/version_/grammar.agl")?.readText() ?: error("File not found")
        private val scopeModelStr = this::class.java.getResource("/vistraq/version_/references.agl")?.readText() ?: error("File not found")

        private val grammarList =
            Agl.registry.agl.grammar.processor!!.process(grammarStr, Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } })
                .let {
                    check(it.issues.errors.isEmpty()) { it.issues.toString() }
                    it.asm!!
                }
        private val processors = lazyMutableMapNonNull<String, LanguageProcessor<Asm, ContextAsmDefault>> { grmName ->
            val grm = grammarList.allDefinitions.firstOrNull { it.name.value == grmName } ?: error("Can't find grammar for '$grmName'")
            val cfg = Agl.configuration {
                targetGrammarName(null) //use default
                defaultGoalRuleName(null) //use default
                //typeModelResolver { p -> ProcessResultDefault<TypeModel>(TypeModelFromGrammar.create(p.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                crossReferenceModelResolver { p -> CrossReferenceModelDefault.fromString(ContextFromTypeModel(p.typeModel), scopeModelStr) }
                syntaxAnalyserResolver { p ->
                    ProcessResultDefault(
                        SyntaxAnalyserDefault(p.typeModel, p.asmTransformModel, p.grammar!!.qualifiedName),
                        IssueHolder(LanguageProcessorPhase.ALL)
                    )
                }
                semanticAnalyserResolver { p -> ProcessResultDefault(SemanticAnalyserDefault(p.typeModel, p.crossReferenceModel), IssueHolder(LanguageProcessorPhase.ALL)) }
                //styleResolver { p -> AglStyleModelDefault.fromString(ContextFromGrammar.createContextFrom(listOf(p.grammar!!)), "") }
                formatterResolver { p -> AglFormatterModelFromAsm.fromString(ContextFromTypeModel(p.typeModel), "") }
                //completionProvider { p ->
                //     ProcessResultDefault(
                //        CompletionProviderDefault(p.grammar!!, TypeModelFromGrammar.defaultConfiguration, p.typeModel, p.crossReferenceModel),
                //        IssueHolder(LanguageProcessorPhase.ALL)
                //    )
                //}
            }
            Agl.processorFromGrammar(grm, cfg)
        }

        fun test(
            grammar: String,
            goal: String,
            sentence: String,
            context: ContextAsmDefault,
            resolveReferences: Boolean,
            expectedContext: ContextAsmDefault,
            expectedAsm: Asm? = null,
            expectedIssues: List<LanguageIssue> = emptyList()
        ) {
            val result = processors[grammar].process(sentence, Agl.options {
                parse { goalRuleName(goal) }
                semanticAnalysis {
                    context(context)
                    resolveReferences(resolveReferences)
                }
            })
            println(context.asString())
            println(result.asm?.asString())
            assertEquals(expectedIssues, result.issues.errors, result.issues.toString())
            assertEquals(expectedContext.asString(), context.asString())
            expectedAsm?.let { assertEquals(expectedAsm.asString(), result.asm!!.asString()) }
            TestContextSimple.assertMatches(expectedContext, context)
        }
    }

    @Test
    fun typeModel() {
        val typeModel = TransformModelDefault.fromGrammarModel(grammarList).asm?.typeModel!!
        println(typeModel.asString())
    }

    @Test
    fun crossReferenceModel() {
        val typeModel = TransformModelDefault.fromGrammarModel(grammarList).asm?.typeModel!!
        val result = Agl.registry.agl.crossReference.processor!!.process(
            scopeModelStr,
            Agl.options {
                semanticAnalysis { context(ContextFromTypeModel(typeModel)) }
            }
        )
        assertTrue(result.issues.isEmpty(), result.issues.toString())
    }

    @Test
    fun TIM_links_valid() {
        val grammar = "TIM"
        val goal = "model"
        val sentence = """
            typed-graph-model {
                node-type A {}
                node-type B {}
                link-type AB: A --- B
            }
        """.trimIndent()

        val expectedContext = contextAsmDefault {
            item("A", "vistraq.query.TIM.NodeType", "/0/nodeList/0")
            item("B", "vistraq.query.TIM.NodeType", "/0/nodeList/1")
            item("AB", "vistraq.query.TIM.LinkType", "/0/linkList/0")
        }

        test(grammar, goal, sentence, ContextAsmDefault(), true, expectedContext)
    }

    @Test
    fun TIM_links_invalid() {
        val grammar = "TIM"
        val goal = "model"
        val sentence = """
            typed-graph-model {
                node-type A {}
                node-type B {}
                link-type CD: C --- D
            }
        """.trimIndent()

        val expectedContext = contextAsmDefault {
            item("A", "vistraq.query.TIM.NodeType", "/0/nodeList/0")
            item("B", "vistraq.query.TIM.NodeType", "/0/nodeList/1")
            item("CD", "vistraq.query.TIM.LinkType", "/0/linkList/0")
        }

        val expectedIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(62, 5, 4, 21),
                "No target of type(s) [NodeType] found for referring value 'C' in scope of element ':LinkType[/0/linkList/0]'", null
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(62, 5, 4, 21),
                "No target of type(s) [NodeType] found for referring value 'D' in scope of element ':LinkType[/0/linkList/0]'", null
            )
        )

        test(grammar, goal, sentence, ContextAsmDefault(), true, expectedContext, null, expectedIssues)
    }

    @Test
    fun Unit_reference_to_NodeType_valid() {
        val grammar = "Unit"
        val goal = "unit"
        val sentence = """
            MATCH A
            
            typed-graph-model {
                node-type A {}
            }
        """.trimIndent()

        val expected = contextAsmDefault {
            item("A", "vistraq.query.TIM.NodeType", "/0/model/model/nodeList/0")
        }

        test(grammar, goal, sentence, ContextAsmDefault(), true, expected)
    }

    @Test
    fun Unit_reference_to_NodeType_invalid() {
        val grammar = "Unit"
        val goal = "unit"
        val sentence = """
            MATCH B
            
            typed-graph-model {
                node-type A {}
            }
        """.trimIndent()

        val expectedContext = contextAsmDefault {
            item("A", "vistraq.query.TIM.NodeType", "/0/model/model/nodeList/0")
        }

        val expectedIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(6, 7, 1, 1),
                "No target of type(s) [NodeType] found for referring value 'B' in scope of element ':NodeTypeReference[/0/query/querySource/pathExpression/nodeSelector/nodeTypeReferenceExpression]'",
                null
            )
        )

        test(grammar, goal, sentence, ContextAsmDefault(), true, expectedContext, null, expectedIssues)
    }

    @Test
    fun Unit_reference_to_NodeType_2_invalid() {
        val grammar = "Unit"
        val goal = "unit"
        val sentence = """
            MATCH B JOIN MATCH A
            
            typed-graph-model {
                node-type A {}
            }
        """.trimIndent()

        val expectedContext = contextAsmDefault {
            item("A", "vistraq.query.TIM.NodeType", "/0/model/model/nodeList/0")
        }

        val expectedIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(6, 7, 1, 1),
                "No target of type(s) [NodeType] found for referring value 'B' in scope of element ':NodeTypeReference[/0/query/singleQuery/0/querySource/pathExpression/nodeSelector/nodeTypeReferenceExpression]'",
                null
            )
        )

        test(grammar, goal, sentence, ContextAsmDefault(), true, expectedContext, null, expectedIssues)
    }


}
