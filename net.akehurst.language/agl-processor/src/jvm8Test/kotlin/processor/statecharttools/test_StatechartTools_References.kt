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
import net.akehurst.language.agl.semanticAnalyser.TestContextSimple
import net.akehurst.language.agl.semanticAnalyser.contextFromTypeModel
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.agl.simple.SemanticAnalyserSimple
import net.akehurst.language.agl.simple.SyntaxAnalyserSimple
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.FormatString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.base.api.Import
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.collections.lazyMutableMapNonNull
import net.akehurst.language.format.asm.AglFormatModelDefault
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.transform.asm.TransformDomainDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_StatechartTools_References {

    companion object {
        private val grammarStr = GrammarString(this::class.java.getResource("/Statecharts/version_/grammar.agl")?.readText()?: error("File not found"))
        private val scopeModelStr = CrossReferenceString(this::class.java.getResource("/Statecharts/version_/references.agl")?.readText() ?: error("File not found"))

        private val grammarModel =
            Agl.registry.agl.grammar.processor!!.process(grammarStr.value, Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } }).asm!!
        private val processors = lazyMutableMapNonNull<String, LanguageProcessor<Asm, ContextWithScope<Any, Any>>> { grmName ->
            val grm = grammarModel
            val cfg = Agl.configuration {
                targetGrammarName(grmName) //use default
                defaultGoalRuleName(null) //use default
                // typeModelResolver { p -> ProcessResultDefault<TypeModel>(TypeModelFromGrammar.create(p.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                crossReferenceResolver { p -> CrossReferenceModelDefault.fromString(ContextFromTypeModel(p.typesModel), scopeModelStr) }
                syntaxAnalyserResolver { p ->
                    ProcessResultDefault(SyntaxAnalyserSimple(p.typesModel, p.transformModel, p.targetGrammar!!.qualifiedName))
                }
                semanticAnalyserResolver { p -> ProcessResultDefault(SemanticAnalyserSimple(p.typesModel, p.crossReferenceModel)) }
                //  styleResolver { p -> AglStyleModelDefault.fromString(ContextFromGrammar.createContextFrom(listOf(p.grammar!!)), "") }
                formatResolver { p -> AglFormatModelDefault.fromString(contextFromTypeModel(p.typesModel), FormatString("")) }
                // completionProvider { p ->
                //     ProcessResultDefault(
                //         CompletionProviderDefault(p.grammar!!, TypeModelFromGrammar.defaultConfiguration, p.typeModel, p.crossReferenceModel),
                //          IssueHolder(LanguageProcessorPhase.ALL)
                //     )
                // }
            }
            Agl.processorFromGrammar(grm, cfg)
        }

        fun test(grammar: String, goal: String, sentence: String, context: ContextWithScope<Any,Any>, resolveReferences: Boolean, expectedContext: ContextWithScope<Any,Any>, expectedAsm: Asm? = null) {
            val result = processors[grammar].process(sentence, Agl.options {
                parse { goalRuleName(goal) }
                semanticAnalysis {
                    context(context)
                    resolveReferences(resolveReferences)
                }
            })
            println(context.asString())
            println(result.asm?.asString())
            assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.joinToString("\n") { it.toString() })
            assertEquals(expectedContext.asString(), context.asString())
            expectedAsm?.let { assertEquals(expectedAsm.asString(), result.asm!!.asString()) }
            TestContextSimple.assertMatches(expectedContext, context)
        }
    }

    @Test
    fun typeModel() {
        val typeModel = TransformDomainDefault.fromGrammarModel(grammarModel).asm?.typeModel!!
        println(typeModel.asString())
    }

    @Test
    fun crossReferenceModel() {
        val typeModel = TransformDomainDefault.fromGrammarModel(grammarModel).asm?.typeModel!!
        val extNs = typeModel.findOrCreateNamespace(QualifiedName("external"), listOf(Import("std")))
        extNs.findOwnedOrCreateDataTypeNamed(SimpleName("AnnotationType"))
        extNs.findOwnedOrCreateDataTypeNamed(SimpleName("BuiltInType"))
        extNs.findOwnedOrCreateDataTypeNamed(SimpleName("RegularState"))

        typeModel.findNamespaceOrNull(QualifiedName("com.itemis.create.Global"))?.addImport(Import("external"))
        typeModel.resolveImports()

        val result = Agl.registry.agl.crossReference.processor!!.process(
            scopeModelStr.value,
            Agl.options {
                semanticAnalysis { context(ContextFromTypeModel(typeModel)) }
            }
        )
        assertTrue(result.allIssues.isEmpty(), result.allIssues.joinToString("\n") { it.toString() })
    }

    @Test
    fun Statechart_identify_root_region() {
        val grammar = "Statechart"
        val goal = "statechart"
        val sentence = """
            statechart 'Test' {
                region 'Root' {
                }
            }
        """.trimIndent()

//        val expected = contextSimple {
//            item("'Root'", "Region", "/0/regions/0")
//            scope("'Root'", "Region", "/0/regions/0") {
//            }
//        }
        val expected = contextAsmSimple {
            // only states are recorded, there are none
        }

        test(grammar, goal, sentence, contextAsmSimple(), true, expected)
    }

    @Test
    fun Statechart_identify_states_in_root_region_scope() {
        val grammar = "Statechart"
        val goal = "statechart"
        val sentence = """
            statechart 'Test' {
                region 'Root' {
                    state 'A' { }
                    state 'B' { }
                }
            }
        """.trimIndent()

        val expected = contextAsmSimple {
            forSentence(null) {
                item("'A'", "com.itemis.create.Statechart.State", null, "/0/regions/0/states/0")
                item("'B'", "com.itemis.create.Statechart.State", null, "/0/regions/0/states/1")
            }
        }

        test(grammar, goal, sentence, contextAsmSimple(), true, expected)
    }

    @Test
    fun Statechart_identify_regions_in_state_scope() {
        val grammar = "Statechart"
        val goal = "statechart"
        val sentence = """
            statechart 'Test' {
                region 'Root' {
                    state 'A' {
                        region 'R1' { }
                        region 'R2' { }
                    }
                }
            }
        """.trimIndent()

        val expected = contextAsmSimple {
            forSentence(null) {
                item("'A'", "com.itemis.create.Statechart.State", null, "/0/regions/0/states/0")
            }
        }

        test(grammar, goal, sentence, contextAsmSimple(), true, expected)
    }

    @Test
    fun Statechart_identify_states_in_nested_region_scope() {
        val grammar = "Statechart"
        val goal = "statechart"
        val sentence = """
            statechart 'Test' {
                region 'Root' {
                    state 'A' {
                        region 'R1' { 
                            state 'C' {}
                        }
                        region 'R2' {
                            state 'D' {}
                            state 'E' {}
                        }
                    }
                }
            }
        """.trimIndent()

        // TODO: missing state because of repeated state id - need to identify by qualified name !
        val expected = contextAsmSimple {
            forSentence(null) {
                item("'A'", "com.itemis.create.Statechart.State", null, "/0/regions/0/states/0")
                item("'C'", "com.itemis.create.Statechart.State", null, "/0/regions/0/states/0/regions/0/states/0")
                item("'D'", "com.itemis.create.Statechart.State", null, "/0/regions/0/states/0/regions/1/states/0")
                item("'E'", "com.itemis.create.Statechart.State", null, "/0/regions/0/states/0/regions/1/states/1")
            }
        }

        test(grammar, goal, sentence, contextAsmSimple(), true, expected)
    }

    @Test
    fun Statechart_identify_states_in_nested_region_scope_fail() {
        val grammar = "Statechart"
        val goal = "statechart"
        val sentence = """
            statechart 'Test' {
                region 'Root' {
                    state 'A' {
                        region 'R1' { 
                            state 'C' {}
                        }
                        region 'R2' {
                            state 'C' {}
                            state 'D' {}
                        }
                    }
                }
            }
        """.trimIndent()

        // TODO: missing state because of repeated state id - need to identify by qualified name !
        val expected = contextAsmSimple {
            forSentence(null) {
                item("'A'", "com.itemis.create.Statechart.State", null, "/0/regions/0/states/0")
                item("'C'", "com.itemis.create.Statechart.State", null, "/0/regions/0/states/0/regions/1/states/0")
                item("'D'", "com.itemis.create.Statechart.State", null, "/0/regions/0/states/0/regions/1/states/1")
            }
        }

        test(grammar, goal, sentence, contextAsmSimple(), true, expected)
    }

    @Test
    fun Statechart_transition_reference_states() {
        val grammar = "Statechart"
        val goal = "statechart"
        val sentence = """
            statechart 'Test' {
                region 'main region' {
                    state 'S1' {}
                    state 'S2' {}
                }
                transitions {
                    'S1' -- { } --> 'S2'
                }
            }
        """.trimIndent()

        val expectedContext = contextAsmSimple {
            forSentence(null) {
                item("'S1'", "com.itemis.create.Statechart.State", null, "/0/regions/0/states/0")
                item("'S2'", "com.itemis.create.Statechart.State", null, "/0/regions/0/states/1")
            }
        }

        val expectedAsm = asmSimple(
            typeModel = processors[grammar]!!.typesModel,
            crossReferenceModel = processors[grammar]!!.crossReferenceModel as CrossReferenceModelDefault,
            context = contextAsmSimple()
        ) {
            element("Statechart") {
                propertyString("name", "'Test'")
                propertyNothing("specification")
                propertyListOfElement("regions") {
                    element("Region") {
                        propertyString("name", "'main region'")
                        propertyListOfElement("states") {
                            element("State") {
                                propertyString("name", "'S1'")
                                propertyNothing("stateSpec")
                                propertyListOfElement("regions") {}
                            }
                            element("State") {
                                propertyString("name", "'S2'")
                                propertyNothing("stateSpec")
                                propertyListOfElement("regions") {}
                            }
                        }
                    }
                }
                propertyElementExplicitType("transitions", "Transitions") {
                    propertyListOfElement("transition") {
                        element("Transition") {
                            reference("name", "'S1'")
                            propertyNothing("transitionSpecification")
                            reference("name2", "'S2'")
                        }
                    }
                }
            }
        }

        test(grammar, goal, sentence, contextAsmSimple(), true, expectedContext, expectedAsm)
    }

    @Test
    fun Global_identify_interface() {
        val grammar = "Global"
        val goal = "StatechartSpecification"
        val sentence = """
            interface I :
        """.trimIndent()

        val expected = contextAsmSimple {
            forSentence(null) {
                scopedItem("I", "com.itemis.create.Global.Interface", null, "/0/statechartLevelDeclaration/0") {
                }
            }
        }

        test(grammar, goal, sentence, contextAsmSimple(), true, expected)
    }

    @Test
    fun identify_Global_variable_in_interface() {
        val grammar = "Global"
        val goal = "StatechartSpecification"
        val sentence = """
            interface I :
                var v
        """.trimIndent()

        val expected = contextAsmSimple {
            forSentence(null) {
                scopedItem("I", "com.itemis.create.Global.Interface", null, "/0/statechartLevelDeclaration/0") {
                    item("v", "com.itemis.create.Global.VariableDeclaration", null, "/0/statechartLevelDeclaration/0/annotatedDeclaration/0/memberDeclaration")
                }
            }
        }

        test(grammar, goal, sentence, contextAsmSimple(), true, expected)
    }

    @Test
    fun identify_Global_imports() {
        val grammar = "Global"
        val goal = "StatechartSpecification"
        val sentence = """
            import :
                "x"
                "y"
                "z"
        """.trimIndent()

        val expected = contextAsmSimple {
            forSentence(null) {
                item("\"x\"", "com.itemis.create.Global.ImportedName", null, "/0/statechartLevelDeclaration/0/importedName/0")
                item("\"y\"", "com.itemis.create.Global.ImportedName", null, "/0/statechartLevelDeclaration/0/importedName/1")
                item("\"z\"", "com.itemis.create.Global.ImportedName", null, "/0/statechartLevelDeclaration/0/importedName/2")
            }
        }

        test(grammar, goal, sentence, contextAsmSimple(), true, expected)
    }

    @Test
    fun identify_Global_operation_in_internal() {
        val grammar = "Global"
        val goal = "StatechartSpecification"
        val sentence = """
            internal :
                operation O()
        """.trimIndent()

        val expected = contextAsmSimple {
            forSentence(null) {
                scopedItem("O", "com.itemis.create.Global.OperationDeclaration", null, "/0/statechartLevelDeclaration/0/internalDeclaration/0/memberDeclaration")
            }
        }

        test(grammar, goal, sentence, contextAsmSimple(), true, expected)
    }

    @Test
    fun reference_Global_LocalReaction_call_internal_operation() {
        val grammar = "Global"
        val goal = "StatechartSpecification"
        val sentence = """
            internal :
                operation func()
                every 1s / func()
        """.trimIndent()

        val expSPPT = """
StatechartSpecification {
  §StatechartSpecification§opt1 { <EMPTY> }
  §StatechartSpecification§multi2 { <EMPTY> }
  §StatechartSpecification§multi3 { StatechartDeclaration { InternalDeclarations {
    'internal'  WS : ' '
    ':' WS : '⏎    '
    §InternalDeclarations§multi1 {
      InternalDeclaration { AnnotatedDeclaration {
        §AnnotatedDeclaration§multi1 { <EMPTY> }
        MemberDeclaration { OperationDeclaration {
          'operation' WS : ' '
          ID : 'func'
          '('
          ParameterList { <EMPTY> }
          ')' WS : '⏎    '
          §OperationDeclaration§opt1 { <EMPTY> }
        } }
      } }
      InternalDeclaration { LocalReaction {
        ReactionTrigger {
          EventSpecList { EventSpec { TimeEventSpec {
            TimeEventType {
              'every' WS : ' '
            }
            Expression { PrimaryExpression { PrimitiveValueExpression { Literal { IntLiteral : '1' } } } }
            TimeUnit : 's' WS : ' '
          } } }
          §ReactionTrigger§opt1 { <EMPTY> }
        }
        '/' WS : ' '
        ReactionEffect {
          §ReactionEffect§choice1 { Expression { PrimaryExpression { FeatureCall { FunctionCall {
            ID : 'func'
            ArgumentList {
              '('
              §ArgumentList§opt1 { <EMPTY> }
              ')'
            }
          } } } } }
          §ReactionEffect§multi1 { <EMPTY> }
        }
      } }
    }
  } } }
}
        """.trimIndent()

        val expectedContext = contextAsmSimple {
            forSentence(null) {
                scopedItem("func", "com.itemis.create.Global.OperationDeclaration", null, "/0/statechartLevelDeclaration/0/internalDeclaration/0/memberDeclaration")
            }
        }

        val expectedAsm = asmSimple(
            typeModel = processors[grammar]!!.typesModel,
            crossReferenceModel = processors[grammar]!!.crossReferenceModel as CrossReferenceModelDefault,
            context = contextAsmSimple()
        ) {
            element("StatechartSpecification") {
                propertyNothing("namespace")
                propertyListOfElement("annotation") {}
                propertyListOfElement("statechartLevelDeclaration") {
                    element("InternalDeclarations") {
                        propertyListOfElement("internalDeclaration") {
                            element("AnnotatedDeclaration") {
                                propertyListOfElement("annotation") {}
                                propertyElementExplicitType("memberDeclaration", "OperationDeclaration") {
                                    propertyString("id", "func")
                                    propertyListOfElement("parameterList") {}
                                    propertyNothing("\$group")
                                }
                            }
                            element("LocalReaction") {
                                propertyTuple("reactionTrigger") {
                                    propertyElementExplicitType("eventSpecList", "EventSpecList") {
                                        propertyListOfElement("eventSpec") {
                                            element("TimeEventSpec") {
                                                propertyString("timeEventType", "every")
                                                propertyElementExplicitType("expression", "PrimitiveValueExpression") {
                                                    propertyString("literal", "1")
                                                }
                                                propertyString("timeUnit", "s")
                                            }
                                        }
                                    }
                                    propertyNothing("guard")
                                }
                                propertyElementExplicitType("reactionEffect", "ReactionEffect") {
                                    propertyElementExplicitType("\$choice", "FunctionCall") {
                                        reference("id", "func")
                                        propertyElementExplicitType("argumentList", "ArgumentList") {
                                            propertyListOfElement("arguments") {}
                                        }
                                    }
                                    propertyListOfElement("\$list") {}
                                }
                            }
                        }
                    }
                }
            }
        }

        test(grammar, goal, sentence, contextAsmSimple(), true, expectedContext, expectedAsm)
    }

    @Test
    fun Global_variable_builtIn_type() {
        val grammar = "Global"
        val goal = "StatechartSpecification"
        val sentence = """
            internal :
                var v:integer
        """.trimIndent()

        // add to type-model for things externally added to context
        val ns = processors[grammar]!!.typesModel.findOrCreateNamespace(QualifiedName("external"), emptyList())
        val bit = ns.findOwnedOrCreatePrimitiveTypeNamed(SimpleName("BuiltInType"))

        val expectedContext = contextAsmSimple {
            forSentence(null) {
                item("integer", "external.BuiltInType", null, "")
            }
        }

        val expectedAsm = asmSimple(
            typeModel = processors[grammar]!!.typesModel,
            crossReferenceModel = processors[grammar]!!.crossReferenceModel as CrossReferenceModelDefault,
            context = expectedContext
        ) {
            element("StatechartSpecification") {
                propertyNothing("namespace")
                propertyListOfElement("annotation") {}
                propertyListOfElement("statechartLevelDeclaration") {
                    element("InternalDeclarations") {
                        propertyListOfElement("internalDeclaration") {
                            element("AnnotatedDeclaration") {
                                propertyListOfElement("annotation") {}
                                propertyElementExplicitType("memberDeclaration", "VariableDeclaration") {
                                    propertyString("variableDeclarationKind", "var")
                                    propertyString("id", "v")
                                    propertyTuple("\$group") {
                                        propertyElementExplicitType("typeSpecifier", "TypeSpecifier") {
                                            reference("fqn", "integer")
                                            propertyNothing("genericTypeArguments")
                                        }
                                    }
                                    propertyNothing("\$group2")
                                }
                            }
                        }
                    }
                }
            }
        }

        val context = contextAsmSimple {
            forSentence(null) {
                item("integer", bit.qualifiedName.value, null, "")
            }
        }
        test(grammar, goal, sentence, context, true, expectedContext, expectedAsm)
    }

}
