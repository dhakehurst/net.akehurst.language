/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.agl.processor.SysML

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.KerML.KerML_v1_0_2026_04
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.agl.semanticAnalyser.contextFromTypesDomain
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.StyleString
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the sub-language (view) grammars of the 'agl' grammar, e.g. SysMLStateMachine.
 * A view is selected with the configuration 'targetGrammarName'. It accepts only its part of SysML,
 * so errors and completion are for that part only.
 */
class test_SysML_v2_0_2026_04_agl_views {

    private companion object {
        val languageDir = File("../language-repository/languages/SysML/v2.0_2026-04")
        val grammarDir = File(languageDir, "languages/agl")
        val validDir = File(languageDir, "valid")
        val grammarStr by lazy { File(grammarDir, "grammar.agl-grm").readText() }
        val styleStr by lazy { File(grammarDir, "style.agl-sty").readText() }
        val referenceStr by lazy { File(grammarDir, "references.agl-ref").readText() }

        /** the example files about states (not the library) */
        val stateFiles by lazy {
            listOf("examples", "training", "validation")
                .flatMap { KerML_v1_0_2026_04.sentenceFiles(File(validDir, it), "sysml") }
                .filter { Regex("(?i)state").containsMatchIn(it.relativeTo(validDir).invariantSeparatorsPath) }
        }

        /** state example files that also use parts of SysML that are not in the state machine view */
        val notStateMachineOnly = setOf(
            "examples/State Space Representation Examples/CartSample.sysml", // flow
            "examples/State Space Representation Examples/EVSample.sysml", // flow, requirement
        )
    }

    private fun processor(targetGrammar: String?): LanguageProcessor<Asm, SentenceContext> {
        val res = Agl.processorFromStringSimple(
            GrammarString(grammarStr),
            styleStr = StyleString(styleStr),
            configurationBase = Agl.configuration(Agl.configurationSimple()) {
                targetGrammar?.let { targetGrammarName(it) }
            }
        )
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        return res.processor!!
    }

    private val stateMachine by lazy { processor("SysMLStateMachine") }
    private val full by lazy { processor(null) }

    private fun parses(p: LanguageProcessor<Asm, SentenceContext>, text: String) = p.parse(text).let { it.issues.errors.isEmpty() && null != it.sppt }

    @Test
    fun default_target_is_full_language() {
        assertEquals("SysMLv2_0", full.targetGrammar?.name?.value)
        assertEquals("SysMLStateMachine", stateMachine.targetGrammar?.name?.value)
    }

    @Test
    fun stateMachine_parses_state_examples() {
        assertTrue(stateFiles.size >= 12, "example files not found in '${validDir.absolutePath}'")
        val failed = stateFiles.filterNot { parses(stateMachine, it.readText()) }.map { it.relativeTo(validDir).invariantSeparatorsPath }.toSet()
        assertEquals(notStateMachineOnly, failed)
        // the full language parses them all
        assertEquals(emptyList(), stateFiles.filterNot { parses(full, it.readText()) }.map { it.name })
    }

    @Test
    fun stateMachine_rejects_other_parts() {
        val sentence = "package P { state def S { connection c connect a to b; } }"
        assertTrue(parses(full, sentence))
        val res = stateMachine.parse(sentence)
        assertTrue(res.issues.errors.isNotEmpty())
        assertEquals("package P { state def S { ".length, res.issues.errors.first().location?.position)
    }

    @Test
    fun stateMachine_completion() {
        val sentence = "package P { state def S { "
        val stateItems = stateMachine.expectedTerminalsAt(sentence, sentence.length).items.map { it.label }.toSet()
        val fullItems = full.expectedTerminalsAt(sentence, sentence.length).items.map { it.label }.toSet()
        val stateKeywords = setOf("'state'", "'transition'", "'entry'", "'do'", "'exit'", "'first'", "'then'", "'action'", "'attribute'", "'part'", "'port'")
        val otherKeywords = setOf("'connection'", "'connect'", "'flow'", "'requirement'", "'interface'", "'allocation'", "'view'", "'metadata'", "'@'")
        assertEquals(stateKeywords, stateItems.intersect(stateKeywords))
        assertEquals(emptySet(), stateItems.intersect(otherKeywords))
        assertEquals(otherKeywords, fullItems.intersect(otherKeywords))
        assertTrue(fullItems.containsAll(stateItems), "items of the view that the full language does not offer: ${stateItems - fullItems}")
    }

    /**
     * a view uses the references of the language; the libraries and the other files are processed with the full language
     */
    @Test
    fun stateMachine_references() {
        val kermlProcessor = KerML_v1_0_2026_04.processorWithReferences()
        fun withReferences(targetGrammar: String?): LanguageProcessor<Asm, SentenceContext> {
            val kermlCrossReferenceDomain = kermlProcessor.crossReferenceDomain
            val res = Agl.processorFromStringSimple(
                GrammarString(grammarStr),
                styleStr = StyleString(styleStr),
                configurationBase = Agl.configuration(Agl.configurationSimple()) {
                    targetGrammar?.let { targetGrammarName(it) }
                    typesResolver { ProcessResultDefault(kermlProcessor.typesDomain) }
                    crossReferenceResolver { p ->
                        val r = CrossReferenceDomainDefault.fromString(contextFromTypesDomain(p.typesDomain), CrossReferenceString(referenceStr))
                        r.asm?.merge(kermlCrossReferenceDomain)
                        r
                    }
                }
            )
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            return res.processor!!
        }

        val fullRefs = withReferences(null)
        val ctx = contextAsmSimple()
        assertEquals(emptyList(), KerML_v1_0_2026_04.processLibrary(kermlProcessor, ctx))
        val sysmlLib = KerML_v1_0_2026_04.sentenceFiles(File(validDir, "library"), "sysml")
        assertEquals(emptyList(), KerML_v1_0_2026_04.processFiles(fullRefs, sysmlLib, ctx, false))

        val stateMachineRefs = withReferences("SysMLStateMachine")
        assertEquals(emptyList(), stateMachineRefs.allIssues.errors.map { it.message })
        val files = stateFiles.filterNot { it.relativeTo(validDir).invariantSeparatorsPath in notStateMachineOnly }
        // the files that a file uses (other example files) are in the full language, so they are processed with the full language
        val allFiles = listOf("examples", "training", "validation").flatMap { KerML_v1_0_2026_04.sentenceFiles(File(validDir, it), "sysml") }
        val failed = files.associate { f ->
            val fctx = ctx.union(contextAsmSimple())
            KerML_v1_0_2026_04.processFiles(fullRefs, KerML_v1_0_2026_04.usedFiles(f, allFiles), fctx, false)
            f.relativeTo(validDir).invariantSeparatorsPath to KerML_v1_0_2026_04.processFiles(stateMachineRefs, listOf(f), fctx, true).map { it.message }
        }.filterValues { it.isNotEmpty() }
        assertEquals(emptyMap(), failed)
    }
}
