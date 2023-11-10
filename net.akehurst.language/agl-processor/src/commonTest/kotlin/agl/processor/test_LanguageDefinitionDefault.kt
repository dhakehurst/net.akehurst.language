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

import net.akehurst.language.agl.default.TypeModelFromGrammar
import net.akehurst.language.agl.language.grammar.ContextFromGrammarRegistry
import net.akehurst.language.agl.semanticAnalyser.ContextSimple
import net.akehurst.language.api.asm.Asm
import net.akehurst.language.api.asm.asmSimple
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.language.grammar.Grammar
import net.akehurst.language.api.language.reference.CrossReferenceModel
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.style.AglStyleModel
import kotlin.test.*

class test_LanguageDefinitionDefault {

    lateinit var sut: LanguageDefinition<Asm, ContextSimple>

    val grammarStrObserverCalled = mutableListOf<Pair<String?, String?>>()
    val grammarStrObserver: (String?, String?) -> Unit = { old, new -> grammarStrObserverCalled.add(Pair(old, new)) }
    val grammarObserverCalled = mutableListOf<Pair<List<Grammar>, List<Grammar>>>()
    val grammarObserver: (List<Grammar>, List<Grammar>) -> Unit = { old, new -> grammarObserverCalled.add(Pair(old, new)) }
    val scopeStrObserverCalled = mutableListOf<Pair<String?, String?>>()
    val scopeStrObserver: (String?, String?) -> Unit = { old, new -> scopeStrObserverCalled.add(Pair(old, new)) }
    val scopeModelObserverCalled = mutableListOf<Pair<CrossReferenceModel?, CrossReferenceModel?>>()
    val scopeModelObserver: (CrossReferenceModel?, CrossReferenceModel?) -> Unit = { old, new -> scopeModelObserverCalled.add(Pair(old, new)) }
    val processorObserverCalled = mutableListOf<Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>>()
    val processorObserver: (LanguageProcessor<*, *>?, LanguageProcessor<*, *>?) -> Unit = { old, new -> processorObserverCalled.add(Pair(old, new)) }
    val styleStrObserverCalled = mutableListOf<Pair<String?, String?>>()
    val styleStrObserver: (String?, String?) -> Unit = { old, new -> styleStrObserverCalled.add(Pair(old, new)) }
    val styleObserverCalled = mutableListOf<Pair<AglStyleModel?, AglStyleModel?>>()
    val styleObserver: (AglStyleModel?, AglStyleModel?) -> Unit = { old, new -> styleObserverCalled.add(Pair(old, new)) }
    val formatterStrObserverCalled = mutableListOf<Pair<String?, String?>>()
    val formatterStrObserver: (String?, String?) -> Unit = { old, new -> formatterStrObserverCalled.add(Pair(old, new)) }
    val formatterObserverCalled = mutableListOf<Pair<AglFormatterModel?, AglFormatterModel?>>()
    val formatterObserver: (AglFormatterModel?, AglFormatterModel?) -> Unit = { old, new -> formatterObserverCalled.add(Pair(old, new)) }

    @BeforeTest
    fun before() {
        Agl.registry.unregister("ns.test")
        this.sut = Agl.registry.register<Asm, ContextSimple>(
            identity = "ns.test",
            grammarStr = null,
            aglOptions = Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } },
            buildForDefaultGoal = false,
            configuration = Agl.configurationDefault()
        )
        sut.grammarStrObservers.add(grammarStrObserver)
        sut.grammarObservers.add(grammarObserver)
        sut.scopeStrObservers.add(scopeStrObserver)
        sut.crossReferenceModelObservers.add(scopeModelObserver)
        sut.processorObservers.add(processorObserver)
        sut.styleStrObservers.add(styleStrObserver)
        sut.styleObservers.add(styleObserver)
        sut.formatterStrObservers.add(formatterStrObserver)
        sut.formatterObservers.add(formatterObserver)

        assertTrue(sut.issues.errors.isEmpty(), sut.issues.toString())
    }

    private fun reset() {
        this.grammarStrObserverCalled.clear()
        this.grammarObserverCalled.clear()
        this.scopeStrObserverCalled.clear()
        this.scopeModelObserverCalled.clear()
        this.processorObserverCalled.clear()
        this.styleStrObserverCalled.clear()
        this.styleObserverCalled.clear()
        this.formatterStrObserverCalled.clear()
        this.formatterObserverCalled.clear()
    }

    @Test
    fun createFromStr() {
        val g = "namespace ns grammar Test1 { S = 'b'; }"
        val def = Agl.registry.register<Asm, ContextSimple>(
            identity = "ns.Test1",
            grammarStr = g,
            aglOptions = Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } },
            buildForDefaultGoal = false,
            Agl.configuration {
                targetGrammarName(null)
                defaultGoalRuleName(null)
                typeModelResolver { ProcessResultDefault(TypeModelFromGrammar.create(it.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
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
        sut.grammarStrObservers.add { s1: String?, s2: String? ->
            println("Grammar changed: $s1, $s2")
        }

        sut.grammarStr = "something new"
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
        assertTrue(scopeStrObserverCalled.isEmpty())
        assertTrue(scopeModelObserverCalled.isEmpty())
        assertTrue(processorObserverCalled.isEmpty())
        assertTrue(styleStrObserverCalled.isEmpty())
        assertTrue(styleObserverCalled.isEmpty())
        assertTrue(formatterStrObserverCalled.isEmpty())
        assertTrue(formatterObserverCalled.isEmpty())
    }

    @Test
    fun grammarStr_change_null_to_value_invalid_parse() {
        val g = "xxxxx"
        sut.grammarStr = g
        assertEquals(g, sut.grammarStr)
        assertNull(sut.targetGrammar)
        assertNull(sut.processor)
        assertEquals(
            setOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(0, 1, 1, 1), "^xxxxx", setOf("'namespace'"))
            ), sut.issues.all
        )
        assertEquals(listOf(Pair<String?, String?>(null, g)), grammarStrObserverCalled)
        assertEquals(emptyList(), grammarObserverCalled)
        assertEquals(emptyList(), scopeStrObserverCalled)
        assertEquals(emptyList(), scopeModelObserverCalled)
        assertEquals(emptyList(), processorObserverCalled)
        assertEquals(emptyList(), styleStrObserverCalled)
        assertEquals(emptyList(), styleObserverCalled)
        assertEquals(emptyList(), formatterStrObserverCalled)
        assertEquals(emptyList(), formatterObserverCalled)
    }

    @Test
    fun grammarStr_change_null_to_value_invalid_syn() {
        val g = "namespace ns grammar Test extends XX { S = 'b'; }"
        sut.grammarStr = g
        assertEquals(g, sut.grammarStr)
        assertNull(sut.targetGrammar) // should be a grammar...though it is invalid
        assertNull(sut.processor)
        assertEquals(
            setOf(
                LanguageIssue(
                    LanguageIssueKind.ERROR,
                    LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                    InputLocation(34, 35, 1, 2),
                    "Grammar 'XX' not found",
                    null
                )
            ), sut.issues.all
        )

        assertEquals(listOf(Pair<String?, String?>(null, g)), grammarStrObserverCalled)
        assertEquals(emptyList(), grammarObserverCalled)
        assertEquals(emptyList(), scopeStrObserverCalled)
        assertEquals(emptyList(), scopeModelObserverCalled)
        assertEquals(emptyList(), processorObserverCalled)
        assertEquals(emptyList(), styleStrObserverCalled)
        assertEquals(emptyList(), styleObserverCalled)
        assertEquals(emptyList(), formatterStrObserverCalled)
        assertEquals(emptyList(), formatterObserverCalled)
    }

    @Test
    fun grammarStr_change_null_to_value_invalid_sem() {
        val g = "namespace ns grammar Test { S = b; }"
        sut.grammarStr = g
        assertEquals(g, sut.grammarStr)
        assertNull(sut.targetGrammar)
        assertNull(sut.processor)
        assertEquals(
            setOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(32, 33, 1, 1), "GrammarRule 'b' not found in grammar 'Test'", null)
            ), sut.issues.all
        )
        assertEquals(listOf(Pair<String?, String?>(null, g)), grammarStrObserverCalled)
        assertEquals(emptyList(), grammarObserverCalled)
        assertEquals(emptyList(), scopeStrObserverCalled)
        assertEquals(emptyList(), scopeModelObserverCalled)
        assertEquals(emptyList(), processorObserverCalled)
        assertEquals(emptyList(), styleStrObserverCalled)
        assertEquals(emptyList(), styleObserverCalled)
        assertEquals(emptyList(), formatterStrObserverCalled)
        assertEquals(emptyList(), formatterObserverCalled)
    }

    @Test
    fun grammarStr_change_null_to_value() {
        val g = "namespace ns grammar Test { S = 'b'; }"
        sut.grammarStr = g

        assertEquals(g, sut.grammarStr)
        assertNotNull(sut.targetGrammar)
        assertNotNull(sut.processor)
        assertTrue(sut.issues.isEmpty(), sut.issues.toString())

        assertEquals(listOf(Pair<String?, String?>(null, g)), grammarStrObserverCalled)
        assertEquals(listOf(Pair<List<Grammar>, List<Grammar>>(emptyList(), sut.grammarList)), grammarObserverCalled)
        assertEquals(emptyList(), scopeStrObserverCalled)
        assertEquals(emptyList(), scopeModelObserverCalled)
        assertEquals(listOf(Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(null, sut.processor)), processorObserverCalled)
        assertEquals(emptyList(), styleStrObserverCalled)
        assertEquals(emptyList(), styleObserverCalled)
        assertEquals(emptyList(), formatterStrObserverCalled)
        assertEquals(emptyList(), formatterObserverCalled)
    }

    @Test
    fun grammarStr_change_value_to_null() {
        val g = "namespace ns grammar Test { S = 'b'; }"
        sut.grammarStr = g
        val oldGrammar = sut.grammarList
        val oldProc = sut.processor
        this.reset()
        sut.grammarStr = null

        assertNull(sut.grammarStr)
        assertNull(sut.targetGrammar)
        assertNull(sut.processor)
        assertTrue(sut.issues.isEmpty())

        assertEquals(listOf(Pair<String?, String?>(g, null)), grammarStrObserverCalled)
        assertEquals(listOf(Pair<List<Grammar>, List<Grammar>>(oldGrammar, emptyList())), grammarObserverCalled)
        assertEquals(emptyList(), scopeStrObserverCalled)
        assertEquals(emptyList(), scopeModelObserverCalled)
        assertEquals(listOf(Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(oldProc, null)), processorObserverCalled)
        assertEquals(emptyList(), styleStrObserverCalled)
        assertEquals(emptyList(), styleObserverCalled)
        assertEquals(emptyList(), formatterStrObserverCalled)
        assertEquals(emptyList(), formatterObserverCalled)
    }

    @Test
    fun grammarStr_change_value_to_same_value() {
        val g1 = "namespace ns grammar Test { S = 'b'; }"
        sut.grammarStr = g1
        val oldGrammar = sut.targetGrammar
        val oldProc = sut.processor
        this.reset()
        val g2 = "namespace ns grammar Test { S = 'b'; }"
        sut.grammarStr = g2

        assertEquals(g2, sut.grammarStr)
        assertNotNull(sut.targetGrammar)
        assertNotNull(sut.processor)
        assertTrue(sut.issues.isEmpty())

        assertEquals(emptyList(), grammarStrObserverCalled)
        assertEquals(emptyList(), grammarObserverCalled)
        assertEquals(emptyList(), scopeStrObserverCalled)
        assertEquals(emptyList(), scopeModelObserverCalled)
        assertEquals(emptyList(), processorObserverCalled)
        assertEquals(emptyList(), styleStrObserverCalled)
        assertEquals(emptyList(), styleObserverCalled)
        assertEquals(emptyList(), formatterStrObserverCalled)
        assertEquals(emptyList(), formatterObserverCalled)
    }

    @Test
    fun grammarStr_change_value_to_diff_value() {
        val g1 = "namespace ns grammar Test { S = 'b'; }"
        sut.grammarStr = g1
        val oldGrammar = sut.grammarList
        val oldProc = sut.processor
        this.reset()
        val g2 = "namespace ns grammar Test { S = 'c'; }"
        sut.grammarStr = g2

        assertEquals(g2, sut.grammarStr)
        assertNotNull(sut.targetGrammar)
        assertNotNull(sut.processor)
        assertTrue(sut.issues.isEmpty())

        assertEquals(listOf(Pair<String?, String?>(g1, g2)), grammarStrObserverCalled)
        assertEquals(listOf(Pair<List<Grammar>, List<Grammar>>(oldGrammar, sut.grammarList)), grammarObserverCalled)
        assertEquals(emptyList(), scopeStrObserverCalled)
        assertEquals(emptyList(), scopeModelObserverCalled)
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
        val grammarStr = """
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
        """
        val scopeStr = """
            namespace test.Test {
                identify Primitive by id
                identify Datatype by id
                identify Collection by id
                references {
                    in TypeReference {
                        property type refers-to Primitive|Datatype|Collection
                    }
                }
            }
            """
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

        val result = sut.processor!!.process(sentence, Agl.options { semanticAnalysis { context(ContextSimple()) } })

        val expected = asmSimple(typeModel = typeModel!!, crossReferenceModel = crossReferenceModel!!, context = ContextSimple()) {
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
                                    propertyNull("typeArguments")
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