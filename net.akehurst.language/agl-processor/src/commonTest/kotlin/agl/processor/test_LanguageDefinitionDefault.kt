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

package net.akehurst.language.agl.processor

import net.akehurst.language.agl.*
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.api.processor.LanguageDefinition
import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.formatter.api.AglFormatterModel
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammar.api.GrammarRuleName
import net.akehurst.language.grammar.asm.GrammarModelDefault
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.style.api.AglStyleModel
import kotlin.test.*

class test_LanguageDefinitionDefault {

    lateinit var sut: LanguageDefinition<Asm, ContextAsmSimple>

    val grammarStrObserverCalled = mutableListOf<Pair<GrammarString?, GrammarString?>>()
    val grammarStrObserver: (GrammarString?, GrammarString?) -> Unit = { old, new -> grammarStrObserverCalled.add(Pair(old, new)) }
    val grammarObserverCalled = mutableListOf<Pair<GrammarModel, GrammarModel>>()
    val grammarObserver: (GrammarModel, GrammarModel) -> Unit = { old, new -> grammarObserverCalled.add(Pair(old, new)) }

    val crossReferenceModelStrObserverCalled = mutableListOf<Pair<CrossReferenceString?, CrossReferenceString?>>()
    val crossReferenceModelStrObserver: (CrossReferenceString?, CrossReferenceString?) -> Unit = { old, new -> crossReferenceModelStrObserverCalled.add(Pair(old, new)) }
    val crossReferenceModelCalled = mutableListOf<Pair<CrossReferenceModel?, CrossReferenceModel?>>()
    val crossReferenceModelObserver: (CrossReferenceModel?, CrossReferenceModel?) -> Unit = { old, new -> crossReferenceModelCalled.add(Pair(old, new)) }

    val processorObserverCalled = mutableListOf<Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>>()
    val processorObserver: (LanguageProcessor<*, *>?, LanguageProcessor<*, *>?) -> Unit = { old, new -> processorObserverCalled.add(Pair(old, new)) }

    val styleStrObserverCalled = mutableListOf<Pair<StyleString?, StyleString?>>()
    val styleStrObserver: (StyleString?, StyleString?) -> Unit = { old, new -> styleStrObserverCalled.add(Pair(old, new)) }
    val styleObserverCalled = mutableListOf<Pair<AglStyleModel?, AglStyleModel?>>()
    val styleObserver: (AglStyleModel?, AglStyleModel?) -> Unit = { old, new -> styleObserverCalled.add(Pair(old, new)) }

    val formatterStrObserverCalled = mutableListOf<Pair<FormatString?, FormatString?>>()
    val formatterStrObserver: (FormatString?, FormatString?) -> Unit = { old, new -> formatterStrObserverCalled.add(Pair(old, new)) }
    val formatterObserverCalled = mutableListOf<Pair<AglFormatterModel?, AglFormatterModel?>>()
    val formatterObserver: (AglFormatterModel?, AglFormatterModel?) -> Unit = { old, new -> formatterObserverCalled.add(Pair(old, new)) }

    @BeforeTest
    fun before() {
        Agl.registry.unregister(LanguageIdentity("ns.test"))
        this.sut = Agl.registry.register<Asm, ContextAsmSimple>(
            identity = LanguageIdentity("ns.test"),
            grammarStr = null,
            aglOptions = Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } },
            buildForDefaultGoal = false,
            configuration = Agl.configurationSimple()
        )
        sut.grammarStrObservers.add(grammarStrObserver)
        sut.grammarObservers.add(grammarObserver)

        sut.crossReferenceModelStrObservers.add(crossReferenceModelStrObserver)
        //sut.crossReferenceModelObservers.add(crossReferenceModelObserver)

        sut.processorObservers.add(processorObserver)

        sut.styleStrObservers.add(styleStrObserver)
        //sut.styleObservers.add(styleObserver)

        sut.formatterStrObservers.add(formatterStrObserver)
        //sut.formatterObservers.add(formatterObserver)

        assertTrue(sut.issues.errors.isEmpty(), sut.issues.toString())
    }

    private fun reset() {
        this.grammarStrObserverCalled.clear()
        this.grammarObserverCalled.clear()

        this.crossReferenceModelStrObserverCalled.clear()
        this.crossReferenceModelCalled.clear()

        this.processorObserverCalled.clear()

        this.styleStrObserverCalled.clear()
        this.styleObserverCalled.clear()

        this.formatterStrObserverCalled.clear()
        this.formatterObserverCalled.clear()
    }

    @Test
    fun createFromStr() {
        val g = GrammarString("namespace ns grammar Test1 { S = 'b'; }")
        val def = Agl.registry.register<Asm, ContextAsmSimple>(
            identity = LanguageIdentity("ns.Test1"),
            grammarStr = g,
            aglOptions = Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } },
            buildForDefaultGoal = false,
            Agl.configuration {
                targetGrammarName(null)
                defaultGoalRuleName(null)
//                typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(it.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                crossReferenceModelResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                syntaxAnalyserResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                semanticAnalyserResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                formatterResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
                styleResolver { ProcessResultDefault(null, IssueHolder(LanguageProcessorPhase.ALL)) }
            }
        )

        assertEquals(g, def.grammarStr)
        assertNotNull(def.targetGrammar)
        assertNotNull(def.processor)
        assertTrue(sut.issues.isEmpty())
    }

    @Test
    fun modifyObservers() {
        sut.grammarStrObservers.add { s1: GrammarString?, s2: GrammarString? ->
            println("Grammar changed: $s1, $s2")
        }

        sut.grammarStr = GrammarString("something new")
    }

    @Test
    fun grammarStr_change_null_to_null() {
        sut.grammarStr = null
        assertNull(sut.grammarStr)
        assertNull(sut.targetGrammar)
        assertNull(sut.processor)
        assertTrue(sut.issues.isEmpty())
        assertTrue(grammarStrObserverCalled.isEmpty())
        assertTrue(grammarObserverCalled.isEmpty())
        assertTrue(crossReferenceModelStrObserverCalled.isEmpty())
        assertTrue(crossReferenceModelCalled.isEmpty())
        assertTrue(processorObserverCalled.isEmpty())
        assertTrue(styleStrObserverCalled.isEmpty())
        assertTrue(styleObserverCalled.isEmpty())
        assertTrue(formatterStrObserverCalled.isEmpty())
        assertTrue(formatterObserverCalled.isEmpty())
    }

    @Test
    fun grammarStr_change_null_to_value_invalid_parse() {
        val g = GrammarString("xxxxx")
        sut.grammarStr = g
        assertEquals(g, sut.grammarStr)
        assertNull(sut.targetGrammar)
        assertNull(sut.processor)
        assertEquals(
            setOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(0, 1, 1, 1), "Failed to match {<GOAL>} at: ^xxxxx", setOf("'namespace'"))
            ), sut.issues.all
        )
        assertEquals(listOf(Pair<GrammarString?, GrammarString?>(null, g)), grammarStrObserverCalled)
        val exp1:List<Pair<GrammarModel,GrammarModel>> = listOf(Pair(GrammarModelDefault(SimpleName("test"), emptyList()),GrammarModelDefault(SimpleName("Error"), emptyList())))
        assertEquals(exp1, grammarObserverCalled)
        assertEquals(emptyList(), crossReferenceModelStrObserverCalled)
        assertEquals(emptyList(), crossReferenceModelCalled)
        assertEquals(emptyList(), processorObserverCalled)
        assertEquals(emptyList(), styleStrObserverCalled)
        assertEquals(emptyList(), styleObserverCalled)
        assertEquals(emptyList(), formatterStrObserverCalled)
        assertEquals(emptyList(), formatterObserverCalled)
    }

    @Test
    fun grammarStr_change_null_to_value_invalid_syn() {
        val g = GrammarString("namespace ns grammar Test : XX { S = 'b'; }")
        sut.grammarStr = g
        assertEquals(g, sut.grammarStr)
        assertNull(sut.targetGrammar) // should be a grammar...though it is invalid
        assertNull(sut.processor)
        assertEquals(
            setOf(
                LanguageIssue(
                    LanguageIssueKind.ERROR,
                    LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                    InputLocation(26, 27, 1, 4),
                    "Grammar 'XX' not found",
                    null
                )
            ), sut.issues.all
        )

        assertEquals(listOf(Pair<GrammarString?, GrammarString?>(null, g)), grammarStrObserverCalled)
        val exp1:List<Pair<GrammarModel,GrammarModel>> = listOf(Pair(GrammarModelDefault(SimpleName("test"), emptyList()),GrammarModelDefault(SimpleName("Error"), emptyList())))
        assertEquals(exp1, grammarObserverCalled)
        assertEquals(emptyList(), crossReferenceModelStrObserverCalled)
        assertEquals(emptyList(), crossReferenceModelCalled)
        assertEquals(emptyList(), processorObserverCalled)
        assertEquals(emptyList(), styleStrObserverCalled)
        assertEquals(emptyList(), styleObserverCalled)
        assertEquals(emptyList(), formatterStrObserverCalled)
        assertEquals(emptyList(), formatterObserverCalled)
    }

    @Test
    fun grammarStr_change_null_to_value_invalid_sem() {
        val g = GrammarString("namespace ns grammar Test { S = b; }")
        sut.grammarStr = g
        assertEquals(g, sut.grammarStr)
        assertNull(sut.targetGrammar)
        assertNull(sut.processor)
        assertEquals(
            setOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                    InputLocation(32, 33, 1, 1), "GrammarRule 'b' not found in grammar 'Test'",
                    null
                )
            ), sut.issues.all
        )
        assertEquals(listOf(Pair<GrammarString?, GrammarString?>(null, g)), grammarStrObserverCalled)
        val exp1:List<Pair<GrammarModel,GrammarModel>> = listOf(Pair(GrammarModelDefault(SimpleName("test"), emptyList()),GrammarModelDefault(SimpleName("Error"), emptyList())))
        assertEquals(exp1, grammarObserverCalled)
        assertEquals(emptyList(), crossReferenceModelStrObserverCalled)
        assertEquals(emptyList(), crossReferenceModelCalled)
        assertEquals(emptyList(), processorObserverCalled)
        assertEquals(emptyList(), styleStrObserverCalled)
        assertEquals(emptyList(), styleObserverCalled)
        assertEquals(emptyList(), formatterStrObserverCalled)
        assertEquals(emptyList(), formatterObserverCalled)
    }

    @Test
    fun grammarStr_change_null_to_value() {
        val g = GrammarString("namespace ns grammar Test { S = 'b'; }")
        sut.grammarStr = g

        assertEquals(g, sut.grammarStr)
        assertNotNull(sut.targetGrammar)
        assertNotNull(sut.processor)
        assertTrue(sut.issues.isEmpty(), sut.issues.toString())

        assertEquals(listOf(Pair<GrammarString?, GrammarString?>(null, g)), grammarStrObserverCalled)
        assertEquals(
            listOf(Pair<GrammarModel, GrammarModel>(GrammarModelDefault(SimpleName("test"), emptyList()), sut.grammarModel)),
            grammarObserverCalled
        )
        assertEquals(emptyList(), crossReferenceModelStrObserverCalled)
        assertEquals(emptyList(), crossReferenceModelCalled)
        assertEquals(listOf(Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(null, sut.processor)), processorObserverCalled)
        assertEquals(emptyList(), styleStrObserverCalled)
        assertEquals(emptyList(), styleObserverCalled)
        assertEquals(emptyList(), formatterStrObserverCalled)
        assertEquals(emptyList(), formatterObserverCalled)
    }

    @Test
    fun grammarStr_change_value_to_null() {
        val g = GrammarString("namespace ns grammar Test { S = 'b'; }")
        sut.grammarStr = g
        val oldGrammar = sut.grammarModel
        val oldProc = sut.processor
        this.reset()
        sut.grammarStr = null

        assertNull(sut.grammarStr)
        assertNull(sut.targetGrammar)
        assertNull(sut.processor)
        assertTrue(sut.issues.isEmpty())

        assertEquals(listOf(Pair<GrammarString?, GrammarString?>(g, null)), grammarStrObserverCalled)
        assertEquals(listOf(Pair<GrammarModel, GrammarModel>(oldGrammar, GrammarModelDefault(SimpleName("test"), emptyList()))), grammarObserverCalled)
        assertEquals(emptyList(), crossReferenceModelStrObserverCalled)
        assertEquals(emptyList(), crossReferenceModelCalled)
        assertEquals(listOf(Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(oldProc, null)), processorObserverCalled)
        assertEquals(emptyList(), styleStrObserverCalled)
        assertEquals(emptyList(), styleObserverCalled)
        assertEquals(emptyList(), formatterStrObserverCalled)
        assertEquals(emptyList(), formatterObserverCalled)
    }

    @Test
    fun grammarStr_change_value_to_same_value() {
        val g1 = GrammarString("namespace ns grammar Test { S = 'b'; }")
        sut.grammarStr = g1
        val oldGrammar = sut.targetGrammar
        val oldProc = sut.processor
        this.reset()
        val g2 =GrammarString("namespace ns grammar Test { S = 'b'; }")
        sut.grammarStr = g2

        assertEquals(g2, sut.grammarStr)
        assertNotNull(sut.targetGrammar)
        assertNotNull(sut.processor)
        assertTrue(sut.issues.isEmpty())

        assertEquals(emptyList(), grammarStrObserverCalled)
        assertEquals(emptyList(), grammarObserverCalled)
        assertEquals(emptyList(), crossReferenceModelStrObserverCalled)
        assertEquals(emptyList(), crossReferenceModelCalled)
        assertEquals(emptyList(), processorObserverCalled)
        assertEquals(emptyList(), styleStrObserverCalled)
        assertEquals(emptyList(), styleObserverCalled)
        assertEquals(emptyList(), formatterStrObserverCalled)
        assertEquals(emptyList(), formatterObserverCalled)
    }

    @Test
    fun grammarStr_change_value_to_diff_value() {
        val g1 = GrammarString("namespace ns grammar Test { S = 'b'; }")
        sut.grammarStr = g1
        val oldGrammar = sut.grammarModel
        val oldProc = sut.processor
        this.reset()
        val g2 = GrammarString("namespace ns grammar Test { S = 'c'; }")
        sut.grammarStr = g2

        assertEquals(g2, sut.grammarStr)
        assertNotNull(sut.targetGrammar)
        assertNotNull(sut.processor)
        assertTrue(sut.issues.isEmpty())

        assertEquals(listOf(Pair<GrammarString?, GrammarString?>(g1, g2)), grammarStrObserverCalled)
        assertEquals(listOf(Pair(oldGrammar, sut.grammarModel)), grammarObserverCalled)
        assertEquals(emptyList(), crossReferenceModelStrObserverCalled)
        assertEquals(emptyList(), crossReferenceModelCalled)
        assertEquals(
            listOf(
                Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(oldProc, null),
                Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(null, sut.processor)
            ), processorObserverCalled
        )
        assertEquals(emptyList(), styleStrObserverCalled)
        assertEquals(emptyList(), styleObserverCalled)
        assertEquals(emptyList(), formatterStrObserverCalled)
        assertEquals(emptyList(), formatterObserverCalled)
    }

    @Test
    fun update_grammarStr_change_empty_to_diff_value_and_set_defaultGoalRule() {
        val g1 = GrammarString("")
        sut.grammarStr = g1
        val oldGrammar = sut.grammarModel
        val oldProc = sut.processor
        this.reset()
        val g2 = GrammarString("namespace ns grammar Test { S = 'c'; }")
        sut.update(g2,null,null)
        sut.defaultGoalRule = GrammarRuleName("statement")

        assertEquals(g2, sut.grammarStr)
        assertNotNull(sut.targetGrammar)
        assertNotNull(sut.processor)
        assertTrue(sut.issues.isEmpty())

        assertEquals(listOf(Pair<GrammarString?, GrammarString?>(g1, g2)), grammarStrObserverCalled)
        assertEquals(listOf(Pair(oldGrammar, sut.grammarModel)), grammarObserverCalled)
        assertEquals(emptyList(), crossReferenceModelStrObserverCalled)
        assertEquals(emptyList(), crossReferenceModelCalled)
        assertEquals(
            listOf(
                Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(oldProc, null),
                Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(null, sut.processor)
            ), processorObserverCalled
        )
        assertEquals(emptyList(), styleStrObserverCalled)
        assertEquals(emptyList(), styleObserverCalled)
        assertEquals(emptyList(), formatterStrObserverCalled)
        assertEquals(emptyList(), formatterObserverCalled)
    }

    @Test
    fun update_grammarStr_change_value_to_diff_value() {
        val g1 = GrammarString("namespace ns grammar Test { S = 'b'; }")
        sut.grammarStr = g1
        val oldGrammar = sut.grammarModel
        val oldProc = sut.processor
        this.reset()
        val g2 = GrammarString("namespace ns grammar Test { S = 'c'; }")
        sut.update(g2,null,null)

        assertEquals(g2, sut.grammarStr)
        assertNotNull(sut.targetGrammar)
        assertNotNull(sut.processor)
        assertTrue(sut.issues.isEmpty())

        assertEquals(listOf(Pair<GrammarString?, GrammarString?>(g1, g2)), grammarStrObserverCalled)
        assertEquals(listOf(Pair(oldGrammar, sut.grammarModel)), grammarObserverCalled)
        assertEquals(emptyList(), crossReferenceModelStrObserverCalled)
        assertEquals(emptyList(), crossReferenceModelCalled)
        assertEquals(
            listOf(
                Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(oldProc, null),
                Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(null, sut.processor)
            ), processorObserverCalled
        )
        assertEquals(emptyList(), styleStrObserverCalled)
        assertEquals(emptyList(), styleObserverCalled)
        assertEquals(emptyList(), formatterStrObserverCalled)
        assertEquals(emptyList(), formatterObserverCalled)
    }

    @Test
    fun crossReferenceModelStr_change_value_to_diff_value() {
        val g1 = GrammarString("""
            namespace ns
            grammar Test {
                S = I* ;
                I = D | R ;
                D = 'def' N ;
                R = 'ref' N ;
                N = "[a-z]+" ;
            }
        """)
        sut.grammarStr = g1
        val cm1 = CrossReferenceString("""
            namespace ns.Test
                identify D by n
        """)
        sut.crossReferenceModelStr = cm1
        val oldCrossReferenceModel = sut.crossReferenceModel
        val oldProc = sut.processor
        this.reset()

        val cm2 = CrossReferenceString("""
            namespace ns.Test
                identify D by n
                references {
                  in R {
                    property n refers-to D
                  }
                }
        """)
        sut.crossReferenceModelStr = cm2

        assertEquals(cm2, sut.crossReferenceModelStr)
        assertNotNull(sut.crossReferenceModel)
        assertNotNull(sut.processor)
        assertTrue(sut.issues.isEmpty())

        assertEquals(listOf(Pair(cm1, cm2)), crossReferenceModelStrObserverCalled.toList())
        assertEquals(listOf(Pair(oldCrossReferenceModel, sut.crossReferenceModel)), crossReferenceModelCalled.toList())
        assertEquals(emptyList(), crossReferenceModelStrObserverCalled)
        assertEquals(emptyList(), crossReferenceModelCalled)
        assertEquals(
            listOf(
                Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(oldProc, null),
                Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(null, sut.processor)
            ), processorObserverCalled
        )
        assertEquals(emptyList(), styleStrObserverCalled)
        assertEquals(emptyList(), styleObserverCalled)
        assertEquals(emptyList(), formatterStrObserverCalled)
        assertEquals(emptyList(), formatterObserverCalled)
    }

    @Test
    fun targetGrammar_change_null_to_value() {
        sut.grammarStr = null
        TODO()
    }

    @Test
    fun defaultGoalRule_change_null_to_value() {
        sut.grammarStr = null
        TODO()
    }

    @Test
    fun style_change_null_to_value() {
        sut.grammarStr = null
        TODO()
    }

    @Test
    fun format_change_null_to_value() {
        sut.grammarStr = null
        TODO()
    }

    @Test
    fun syntaxAnalyserResolver_change_null_to_value() {
        sut.grammarStr = null
        TODO()
    }

    @Test
    fun semanticAnalyserResolver_change_null_to_value() {
        sut.grammarStr = null
        TODO()
    }

    @Test
    fun aglOptions_change_null_to_value() {
        sut.grammarStr = null
        TODO()
    }

    @Test
    fun checkReferencesWork() {
        val grammarStr = GrammarString("""
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                skip leaf COMMENT = "//[^\n]*(\n)" ;
            
                unit = declaration* ;
                declaration = datatype | primitive | collection ;
                primitive = 'primitive' ID ;
                collection = 'collection' ID typeParameters? ;
                typeParameters = '<' typeParameterList '>' ;
                typeParameterList = [ID / ',']+ ;
                datatype = 'datatype' ID '{' property* '}' ;
                property = ID ':' typeReference ;
                typeReference = type typeArguments? ;
                typeArguments = '<' typeArgumentList '>' ;
                typeArgumentList = [typeReference / ',']+ ;
            
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
                leaf type = ID;
            }
        """)
        val scopeStr = CrossReferenceString("""
            namespace test.Test
                identify Primitive by id
                identify Datatype by id
                identify Collection by id
                references {
                    in TypeReference {
                        property type refers-to Primitive|Datatype|Collection
                    }
                }
            """)
        val sentence = """
            primitive String
            datatype A {
                a : String
            }
        """.trimIndent()

        sut.grammarStr = grammarStr
        sut.crossReferenceModelStr = scopeStr
        val typeModel = sut.typeModel
        val crossReferenceModel = sut.crossReferenceModel
        assertTrue(sut.issues.errors.isEmpty(), sut.issues.toString())

        val result = sut.processor!!.process(sentence, Agl.options { semanticAnalysis { context(ContextAsmSimple()) } })

        val expected = asmSimple(typeModel = typeModel!!, crossReferenceModel = crossReferenceModel!!, context = ContextAsmSimple()) {
            element("Unit") {
                propertyListOfElement("declaration") {
                    element("Primitive") {
                        propertyString("id", "String")
                    }
                    element("Datatype") {
                        propertyString("id", "A")
                        propertyListOfElement("property") {
                            element("Property") {
                                propertyString("id", "a")
                                propertyElementExplicitType("typeReference", "TypeReference") {
                                    reference("type", "String")
                                    propertyNothing("typeArguments")
                                }
                            }
                        }
                    }
                }
            }
        }

        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertEquals(expected.asString(indentIncrement = "  "), result.asm!!.asString(indentIncrement = "  "))
    }
}