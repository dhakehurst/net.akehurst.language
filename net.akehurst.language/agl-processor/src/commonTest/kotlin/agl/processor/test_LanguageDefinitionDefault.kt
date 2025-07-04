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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.api.processor.*
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.formatter.api.AglFormatModel
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammar.asm.GrammarModelDefault
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.style.api.AglStyleModel
import kotlin.test.*

class test_LanguageDefinitionDefault {

    lateinit var sut: LanguageDefinition<Asm, ContextWithScope<Any, Any>>

    val grammarStrObserverCalled = mutableListOf<Pair<GrammarString?, GrammarString?>>()
    val grammarStrObserver: (GrammarString?, GrammarString?) -> Unit = { old, new -> grammarStrObserverCalled.add(Pair(old, new)) }
    val grammarObserverCalled = mutableListOf<Pair<GrammarModel?, GrammarModel?>>()
    val grammarObserver: (GrammarModel?, GrammarModel?) -> Unit = { old, new -> grammarObserverCalled.add(Pair(old, new)) }

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
    val formatterObserverCalled = mutableListOf<Pair<AglFormatModel?, AglFormatModel?>>()
    val formatterObserver: (AglFormatModel?, AglFormatModel?) -> Unit = { old, new -> formatterObserverCalled.add(Pair(old, new)) }

    @BeforeTest
    fun before() {
        this.sut = Agl.languageDefinitionFromStringSimple(
            identity = LanguageIdentity("ns.test"),
            grammarDefinitionStr = null,
            typeStr = null,
            transformStr = null,
            referenceStr =  null,
            styleStr = null,
            formatterModelStr = null,
            configurationBase = Agl.configurationSimple(),
            grammarAglOptions =  Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } },
        )
        sut.grammarStrObservers.add(grammarStrObserver)
        sut.grammarObservers.add(grammarObserver)

        sut.crossReferenceStrObservers.add(crossReferenceModelStrObserver)
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
    fun languageDefinitionFromStringSimple__empty() {
        val def =Agl.languageDefinitionFromStringSimple(
            identity = LanguageIdentity("ns.Test1"),
            grammarDefinitionStr = null,
            typeStr = null,
            transformStr = null,
            referenceStr =  null,
            styleStr = null,
            formatterModelStr = null,
            configurationBase = Agl.configurationSimple(),
            grammarAglOptions =  Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } },
        )
        assertNull(def.grammarString)
        assertNull(def.targetGrammar)
        assertNull(def.processor)
        assertNull(def.typesModel)
        assertNull(def.transformModel)
        assertNull(def.crossReferenceModel)
        assertNotNull(def.styleModel)
        assertTrue(def.styleModel!!.isEmpty)
        assertNull(def.formatter)
        assertTrue(sut.issues.isEmpty())
    }

    @Test
    fun languageDefinitionFromStringSimple__grammar_only() {
        val g = GrammarString("namespace ns grammar Test1 { S = 'b'; }")
        val def =Agl.languageDefinitionFromStringSimple(
            identity = LanguageIdentity("ns.Test1"),
            grammarDefinitionStr = g,
            typeStr = null,
            transformStr = null,
            referenceStr =  null,
            styleStr = null,
            formatterModelStr = null,
            configurationBase = Agl.configurationSimple(),
            grammarAglOptions =  Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } },
        )
        println("assert")
        assertEquals(g, def.grammarString)
        assertNotNull(def.targetGrammar)
        assertNotNull(def.processor)
        assertNotNull(def.typesModel)
        assertNotNull(def.transformModel)
        assertNotNull(def.crossReferenceModel)
        assertNotNull(def.styleModel)
        assertTrue(def.styleModel!!.isEmpty)
        assertNotNull(def.formatter)
        assertTrue(sut.issues.isEmpty())
    }

    @Test
    fun languageDefinitionFromStringSimple__all() {
        val grammarStr = GrammarString("namespace ns grammar Test1 { S = 'b'; }")
        val transformStr = TransformString("")
        val referenceStr = CrossReferenceString("")
        val styleStr = StyleString("namespace test styles Test {}")
        val def  = Agl.languageDefinitionFromStringSimple(
            identity = LanguageIdentity("test"),
            grammarDefinitionStr = grammarStr,
            //typeStr = SimpleArch.typeStr,
            transformStr = transformStr,
            referenceStr = referenceStr,
            styleStr = styleStr,
            configurationBase = Agl.configuration(Agl.configurationSimple()) {
                targetGrammarName("Patterns")
                defaultGoalRuleName("statement")
            },
            grammarAglOptions = Agl.options {
                semanticAnalysis {
                    context(ContextFromGrammarRegistry(Agl.registry))
                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                }
            },
        )
        println("assert")
        assertEquals(grammarStr, def.grammarString)
        assertNotNull(def.targetGrammar)
        assertNotNull(def.processor)
        assertNotNull(def.typesModel)
        assertNotNull(def.transformModel)
        assertNotNull(def.crossReferenceModel)
        assertNotNull(def.styleModel)
        assertTrue(def.styleModel!!.isEmpty.not())
        assertNotNull(def.formatter)
        assertTrue(sut.issues.isEmpty())
    }


    @Test
    fun modifyObservers() {
        sut.grammarStrObservers.add { s1: GrammarString?, s2: GrammarString? ->
            println("Grammar changed: $s1, $s2")
        }

        sut.update( GrammarString("something new"))
    }

    @Test
    fun grammarStr_change_null_to_null() {
        sut.update(grammarString =  null)
        assertNull(sut.grammarString)
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
        sut. update(grammarString = g)
        assertEquals(g, sut.grammarString)
        assertNull(sut.targetGrammar)
        assertNull(sut.processor)
        assertEquals(
            setOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(0, 1, 1, 1, null), "Failed to match {<GOAL>} at: ^xxxxx", setOf("'namespace'"))
            ), sut.issues.all
        )
        assertEquals(listOf(Pair<GrammarString?, GrammarString?>(null, g)), grammarStrObserverCalled)
        val exp1: List<Pair<GrammarModel?, GrammarModel?>> = listOf(Pair(GrammarModelDefault(SimpleName("test")), GrammarModelDefault(SimpleName("Error"))))
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
        sut.update(grammarString = g)
        assertEquals(g, sut.grammarString)
        assertNull(sut.targetGrammar) // should be a grammar...though it is invalid
        assertNull(sut.processor)
        assertEquals(
            setOf(
                LanguageIssue(
                    LanguageIssueKind.ERROR,
                    LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                    InputLocation(26, 27, 1, 4, null),
                    "Grammar 'XX' not found",
                    null
                )
            ), sut.issues.all
        )

        assertEquals(listOf(Pair<GrammarString?, GrammarString?>(null, g)), grammarStrObserverCalled)
        val exp1: List<Pair<GrammarModel?, GrammarModel?>> = listOf(Pair(GrammarModelDefault(SimpleName("test")), GrammarModelDefault(SimpleName("Error"))))
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
        sut.update(grammarString = g)
        assertEquals(g, sut.grammarString)
        assertNull(sut.targetGrammar)
        assertNull(sut.processor)
        assertEquals(
            setOf(
                LanguageIssue(
                    LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                    InputLocation(32, 33, 1, 1, null), "GrammarRule 'b' not found in grammar 'Test'",
                    null
                )
            ), sut.issues.all
        )
        assertEquals(listOf(Pair<GrammarString?, GrammarString?>(null, g)), grammarStrObserverCalled)
        val exp1: List<Pair<GrammarModel?, GrammarModel?>> = listOf(Pair(GrammarModelDefault(SimpleName("test")), GrammarModelDefault(SimpleName("Error"))))
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
        sut.update(grammarString = g)

        assertEquals(g, sut.grammarString)
        assertNotNull(sut.targetGrammar)
        assertNotNull(sut.processor)
        assertTrue(sut.issues.isEmpty(), sut.issues.toString())

        assertEquals(listOf(Pair<GrammarString?, GrammarString?>(null, g)), grammarStrObserverCalled)
        assertEquals(
            listOf(Pair<GrammarModel?, GrammarModel?>(GrammarModelDefault(SimpleName("test")), sut.grammarModel)),
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
        sut.update(grammarString = g)
        val oldGrammar = sut.grammarModel
        val oldProc = sut.processor
        this.reset()
        sut.update(grammarString = null)

        assertNull(sut.grammarString)
        assertNull(sut.targetGrammar)
        assertNull(sut.processor)
        assertTrue(sut.issues.isEmpty())

        assertEquals(listOf(Pair<GrammarString?, GrammarString?>(g, null)), grammarStrObserverCalled)
        assertEquals(listOf(Pair<GrammarModel?, GrammarModel?>(oldGrammar, GrammarModelDefault(SimpleName("test")))), grammarObserverCalled)
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
        sut.update(grammarString = g1)
        val oldGrammar = sut.targetGrammar
        val oldProc = sut.processor
        this.reset()
        val g2 = GrammarString("namespace ns grammar Test { S = 'b'; }")
        sut.update(grammarString = g2)

        assertEquals(g2, sut.grammarString)
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
        sut.update(grammarString = g1)
        val oldGrammar = sut.grammarModel
        val oldProc = sut.processor
        this.reset()
        val g2 = GrammarString("namespace ns grammar Test { S = 'c'; }")
        sut.update(grammarString = g2)

        assertEquals(g2, sut.grammarString)
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
        sut.update(grammarString = g1)
        val oldGrammar = sut.grammarModel
        val oldProc = sut.processor
        this.reset()
        val g2 = GrammarString("namespace ns grammar Test { S = 'c'; }")
        sut.update(g2, null, null,null,null)
//        sut.defaultGoalRule = GrammarRuleName("statement")

        assertEquals(g2, sut.grammarString)
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
        sut.update(grammarString = g1)
        val oldGrammar = sut.grammarModel
        val oldProc = sut.processor
        this.reset()
        val g2 = GrammarString("namespace ns grammar Test { S = 'c'; }")
        sut.update(g2, null, null,null,null)

        assertEquals(g2, sut.grammarString)
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
        assertNull(sut.grammarString)
        assertNull(sut.crossReferenceString)
        assertNull(sut.styleString)

        val g1 = GrammarString(
            """
            namespace ns
            grammar Test {
                S = I* ;
                I = D | R ;
                D = 'def' N ;
                R = 'ref' N ;
                N = "[a-z]+" ;
            }
        """
        )
        sut.update(grammarString = g1)
        assertEquals(g1, sut.grammarString)
        val cm1 = CrossReferenceString(
            """
            namespace ns.Test
                identify R by n
        """
        )
        sut.update(crossReferenceString = cm1)
        assertEquals(cm1, sut.crossReferenceString)
        assertEquals("R", sut.crossReferenceModel!!.allDefinitions.get(0).scopeDefinition.values.first().identifiables.get(0).typeName.value)
        assertTrue(sut.crossReferenceModel!!.allDefinitions.get(0).references.isEmpty())
        val oldCrossReferenceModel = sut.crossReferenceModel
        val oldProc = sut.processor
        this.reset()

        val cm2 = CrossReferenceString(
            """
            namespace ns.Test
                identify D by n
                references {
                  in R {
                    property n refers-to D
                  }
                }
        """
        )
        sut.update(crossReferenceString = cm2)
        assertEquals(cm2, sut.crossReferenceString)
        assertEquals("D", sut.crossReferenceModel!!.allDefinitions.get(0).scopeDefinition.values.first().identifiables.get(0).typeName.value)
        assertTrue(1 == sut.crossReferenceModel!!.allDefinitions.get(0).references.size)

        assertEquals(cm2, sut.crossReferenceString)
        assertNotNull(sut.crossReferenceModel)
        assertNotNull(sut.processor)
        assertTrue(sut.issues.isEmpty())

        assertEquals(listOf(Pair(cm1, cm2)), crossReferenceModelStrObserverCalled.toList())
//        assertEquals(listOf(Pair(oldCrossReferenceModel, sut.crossReferenceModel)), crossReferenceModelCalled.toList())
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
    fun update_crossReferenceModelStr_change_empty_to_diff_value() {
        assertNull(sut.grammarString)
        assertNull(sut.crossReferenceString)
        assertNull(sut.styleString)

        val g1 = GrammarString(
            """
            namespace ns
            grammar Test {
                S = I* ;
                I = D | R ;
                D = 'def' N ;
                R = 'ref' N ;
                N = "[a-z]+" ;
            }
        """
        )
        sut.update(grammarString = g1)
        assertEquals(g1, sut.grammarString)

        val cm1 = CrossReferenceString(
            """
            namespace ns.Test
                identify R by n
        """
        )
        sut.update(g1,null,null, cm1, null)
        assertEquals(cm1, sut.crossReferenceString)
        assertEquals("R", sut.crossReferenceModel!!.allDefinitions.get(0).scopeDefinition.values.first().identifiables.get(0).typeName.value)
        assertTrue(sut.crossReferenceModel!!.allDefinitions.get(0).references.isEmpty())
    }

    @Ignore
    @Test
    fun targetGrammar_change_null_to_value() {
        sut.update(grammarString = null)
        TODO()
    }

    @Ignore
    @Test
    fun defaultGoalRule_change_null_to_value() {
        sut.update(grammarString = null)
        TODO()
    }

    @Ignore
    @Test
    fun style_change_null_to_value() {
        sut.update(grammarString = null)
        TODO()
    }

    @Ignore
    @Test
    fun format_change_null_to_value() {
        sut.update(grammarString = null)
        TODO()
    }

    @Ignore
    @Test
    fun syntaxAnalyserResolver_change_null_to_value() {
        sut.update(grammarString = null)
        TODO()
    }

    @Ignore
    @Test
    fun semanticAnalyserResolver_change_null_to_value() {
        sut.update(grammarString = null)
        TODO()
    }

    @Ignore
    @Test
    fun aglOptions_change_null_to_value() {
        sut.update(grammarString = null)
        TODO()
    }

    @Test
    fun checkReferencesWork() {
        val grammarStr = GrammarString(
            """
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
        )
        val scopeStr = CrossReferenceString(
            """
            namespace test.Test
                identify Primitive by id
                identify Datatype by id
                identify Collection by id
                references {
                    in TypeReference {
                        property type refers-to Primitive|Datatype|Collection
                    }
                }
            """
        )
        val sentence = """
            primitive String
            datatype A {
                a : String
            }
        """.trimIndent()

        sut.update(grammarString = grammarStr, crossReferenceString = scopeStr)
        val typeModel = sut.typesModel
        val crossReferenceModel = sut.crossReferenceModel
        assertTrue(sut.issues.errors.isEmpty(), sut.issues.toString())

        val result = sut.processor!!.process(sentence, Agl.options { semanticAnalysis { context(contextAsmSimple()) } })

        val expected = asmSimple(typeModel = typeModel!!, crossReferenceModel = crossReferenceModel!!, context = contextAsmSimple()) {
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
        assertTrue(result.allIssues.isEmpty(), result.allIssues.toString())
        assertEquals(expected.asString(indentIncrement = "  "), result.asm!!.asString(indentIncrement = "  "))
    }
}