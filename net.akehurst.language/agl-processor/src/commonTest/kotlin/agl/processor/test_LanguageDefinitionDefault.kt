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

import net.akehurst.language.agl.grammar.grammar.GrammarContext
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.agl.syntaxAnalyser.TypeModelFromGrammar
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.formatter.AglFormatterModel
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.style.AglStyleModel
import kotlin.test.*

class test_LanguageDefinitionDefault {

    lateinit var sut:LanguageDefinition<AsmSimple, ContextSimple>

    val processorObserverCalled = mutableListOf<Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>>()
    val processorObserver: (LanguageProcessor<*, *>?, LanguageProcessor<*, *>?) -> Unit = { old, new -> processorObserverCalled.add(Pair(old, new)) }
    val grammarStrObserverCalled = mutableListOf<Pair<String?, String?>>()
    val grammarStrObserver: (String?, String?) -> Unit = { old, new -> grammarStrObserverCalled.add(Pair(old, new)) }
    val grammarObserverCalled = mutableListOf<Pair<Grammar?, Grammar?>>()
    val grammarObserver: (Grammar?, Grammar?) -> Unit = { old, new -> grammarObserverCalled.add(Pair(old, new)) }
    val styleObserverCalled = mutableListOf<Pair<AglStyleModel?, AglStyleModel?>>()
    val styleObserver: (AglStyleModel?, AglStyleModel?) -> Unit = { old, new -> styleObserverCalled.add(Pair(old, new)) }
    val formatObserverCalled = mutableListOf<Pair<AglFormatterModel?, AglFormatterModel?>>()
    val formatObserver: (AglFormatterModel?, AglFormatterModel?) -> Unit = { old, new -> formatObserverCalled.add(Pair(old, new)) }

    @BeforeTest
    fun before() {
        Agl.registry.unregister("ns.test")
        this.sut = Agl.registry.register<AsmSimple, ContextSimple>(
            identity = "ns.test",
            grammarStr = null,
            aglOptions = null,
            buildForDefaultGoal = false,
            configuration = Agl.configurationDefault()
        )
        sut.processorObservers.add(processorObserver)
        sut.grammarStrObservers.add(grammarStrObserver)
        sut.grammarObservers.add(grammarObserver)
        sut.styleObservers.add(styleObserver)
        sut.formatterObservers.add(formatObserver)
    }

    private fun reset() {
        this.processorObserverCalled.clear()
        this.grammarObserverCalled.clear()
        this.styleObserverCalled.clear()
        this.formatObserverCalled.clear()
    }

    @Test
    fun createFromStr() {
        val g = "namespace ns grammar Test1 { S = 'b'; }"
        val def = Agl.registry.register<AsmSimple, ContextSimple>(
            identity = "ns.Test1",
            grammarStr = g,
            aglOptions = null,
            buildForDefaultGoal = false,
            Agl.configuration {
                targetGrammarName(null)
                defaultGoalRuleName(null)
                typeModelResolver {  ProcessResultDefault(TypeModelFromGrammar(it.grammar!!), emptyList()) }
                scopeModelResolver { ProcessResultDefault(null, emptyList()) }
                syntaxAnalyserResolver { ProcessResultDefault(null, emptyList()) }
                semanticAnalyserResolver {  ProcessResultDefault(null, emptyList()) }
                formatterResolver {  ProcessResultDefault(null, emptyList()) }
                styleResolver {  ProcessResultDefault(null, emptyList()) }
            }
        )

        assertEquals(g, def.grammarStr)
        assertNotNull(def.grammar)
        assertNotNull(def.processor)
        assertTrue(sut.issues.isEmpty())
    }

    @Test
    fun grammarStr_change_null_to_null() {
        sut.grammarStr = null
        assertNull(sut.grammarStr)
        assertNull(sut.grammar)
        assertNull(sut.processor)
        assertTrue(sut.issues.isEmpty())
        assertTrue(grammarObserverCalled.isEmpty())
        assertTrue(processorObserverCalled.isEmpty())
        assertTrue(styleObserverCalled.isEmpty())
        assertTrue(formatObserverCalled.isEmpty())
    }

    @Test
    fun grammarStr_change_null_to_value_invalid_parse() {
        val g = "xxxxx"
        sut.grammarStr = g
        assertEquals(g, sut.grammarStr)
        assertNull(sut.grammar)
        assertNull(sut.processor)
        assertEquals(
            listOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE, InputLocation(0, 1, 1, 1), "^xxxxx", setOf("'namespace'"))
            ), sut.issues
        )
        assertTrue(grammarObserverCalled.isEmpty())
        assertEquals(listOf(Pair<String?, String?>(null,g)),grammarStrObserverCalled)
        assertEquals(listOf(Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(null, sut.processor)), processorObserverCalled)
        assertTrue(styleObserverCalled.isEmpty())
        assertTrue(formatObserverCalled.isEmpty())
    }

    @Test
    fun grammarStr_change_null_to_value_invalid_syn() {
        val g = "namespace ns grammar Test extends XX { S = 'b'; }"
        sut.grammarStr = g
        assertEquals(g, sut.grammarStr)
        assertNull(sut.grammar)
        assertNull(sut.processor)
        assertEquals(
            listOf(
                LanguageIssue(
                    LanguageIssueKind.ERROR,
                    LanguageProcessorPhase.SYNTAX_ANALYSIS,
                    InputLocation(26, 27, 1, 11),
                    "Trying to extend but failed to find grammar 'XX' as a qualified name or in namespace 'ns'",
                    null
                )
            ), sut.issues
        )
        assertTrue(grammarObserverCalled.isEmpty())
        assertTrue(processorObserverCalled.isEmpty())
        assertTrue(styleObserverCalled.isEmpty())
        assertTrue(formatObserverCalled.isEmpty())
    }

    @Test
    fun grammarStr_change_null_to_value_invalid_sem() {
        val g = "namespace ns grammar Test { S = b; }"
        sut.grammarStr = g
        assertEquals(g, sut.grammarStr)
        assertNull(sut.grammar)
        assertNull(sut.processor)
        assertEquals(
            listOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(32, 33, 1, 1), "Grammar Rule with name 'b' not found", null)
            ), sut.issues
        )
        assertTrue(grammarObserverCalled.isEmpty())
        assertTrue(processorObserverCalled.isEmpty())
        assertTrue(styleObserverCalled.isEmpty())
        assertTrue(formatObserverCalled.isEmpty())
    }

    @Test
    fun grammarStr_change_null_to_value() {
        val g = "namespace ns grammar Test { S = 'b'; }"
        sut.grammarStr = g

        assertEquals(g, sut.grammarStr)
        assertNotNull(sut.grammar)
        assertNotNull(sut.processor)
        assertTrue(sut.issues.isEmpty())
        assertEquals(listOf(Pair<Grammar?, Grammar?>(null, sut.grammar)), grammarObserverCalled)
        assertEquals(listOf(Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(null, sut.processor)), processorObserverCalled)
        assertTrue(styleObserverCalled.isEmpty())
        assertTrue(formatObserverCalled.isEmpty())
    }

    @Test
    fun grammarStr_change_value_to_null() {
        val g = "namespace ns grammar Test { S = 'b'; }"
        sut.grammarStr = g
        val oldGrammar = sut.grammar
        val oldProc = sut.processor
        this.reset()
        sut.grammarStr = null

        assertNull(sut.grammarStr)
        assertNull(sut.grammar)
        assertNull(sut.processor)
        assertTrue(sut.issues.isEmpty())
        assertEquals(listOf(Pair<Grammar?, Grammar?>(oldGrammar, null)), grammarObserverCalled)
        assertEquals(listOf(Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(oldProc, null)), processorObserverCalled)
        assertTrue(styleObserverCalled.isEmpty())
        assertTrue(formatObserverCalled.isEmpty())
    }

    @Test
    fun grammarStr_change_value_to_same_value() {
        val g1 = "namespace ns grammar Test { S = 'b'; }"
        sut.grammarStr = g1
        val oldGrammar = sut.grammar
        val oldProc = sut.processor
        this.reset()
        val g2 = "namespace ns grammar Test { S = 'b'; }"
        sut.grammarStr = g2

        assertEquals(g2, sut.grammarStr)
        assertNotNull(sut.grammar)
        assertNotNull(sut.processor)
        assertTrue(sut.issues.isEmpty())
        assertTrue(grammarObserverCalled.isEmpty())
        assertTrue(processorObserverCalled.isEmpty())
        assertTrue(styleObserverCalled.isEmpty())
        assertTrue(formatObserverCalled.isEmpty())
    }

    @Test
    fun grammarStr_change_value_to_diff_value() {
        val g1 = "namespace ns grammar Test { S = 'b'; }"
        sut.grammarStr = g1
        val oldGrammar = sut.grammar
        val oldProc = sut.processor
        this.reset()
        val g2 = "namespace ns grammar Test { S = 'c'; }"
        sut.grammarStr = g2

        assertEquals(g2, sut.grammarStr)
        assertNotNull(sut.grammar)
        assertNotNull(sut.processor)
        assertTrue(sut.issues.isEmpty())
        assertEquals(listOf(Pair<Grammar?, Grammar?>(oldGrammar, sut.grammar)), grammarObserverCalled)
        assertEquals(
            listOf(
                Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(oldProc, null),
                Pair<LanguageProcessor<*, *>?, LanguageProcessor<*, *>?>(null, sut.processor)
            ), processorObserverCalled
        )
        assertTrue(styleObserverCalled.isEmpty())
        assertTrue(formatObserverCalled.isEmpty())
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
}