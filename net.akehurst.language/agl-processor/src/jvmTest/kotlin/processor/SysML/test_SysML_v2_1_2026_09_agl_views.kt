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
import net.akehurst.language.agl.processor.KerML.KerML_v1_1_2026_09
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
 * Tests the sub-language (view) grammars of the 'agl' grammar: SysMLStateMachine, SysMLStructure, SysMLActivity, SysMLSequence, SysMLRequirements.
 * A view is selected with the configuration 'targetGrammarName'. It accepts only its part of SysML,
 * so errors and completion are for that part only.
 */
class test_SysML_v2_1_2026_09_agl_views {

    private companion object {
        val languageDir = File("../language-repository/languages/SysML/v2.1_2026-09")
        val grammarDir = File(languageDir, "languages/agl")
        val validDir = File(languageDir, "valid")
        val grammarStr by lazy { File(grammarDir, "grammar.agl-grm").readText() }
        val styleStr by lazy { File(grammarDir, "style.agl-sty").readText() }
        val referenceStr by lazy { File(grammarDir, "references.agl-ref").readText() }

        val exampleFiles by lazy { listOf("examples", "training", "validation").flatMap { KerML_v1_1_2026_09.sentenceFiles(File(validDir, it), "sysml") } }

        /**
         * a view, the example files (path matches 'files') for it,
         * and those of them that also use parts of SysML that are not in the view ('notOnly', path -> reason)
         */
        class View(
            val grammar: String,
            val files: Regex,
            val notOnly: Map<String, String>,
            /** a sentence that ends where a body item of the view is expected */
            val completionAt: String,
            /** keywords offered there */
            val keywords: Set<String>,
            /** keywords of the full language not offered there */
            val otherKeywords: Set<String>,
        ) {
            val exampleFiles by lazy { Companion.exampleFiles.filter { files.containsMatchIn(it.relativeTo(validDir).invariantSeparatorsPath) } }
            override fun toString(): String = grammar
        }

        val stateMachine = View(
            "SysMLStateMachine", Regex("(?i)state"),
            mapOf(
                "examples/State Space Representation Examples/CartSample.sysml" to "flow",
                "examples/State Space Representation Examples/EVSample.sysml" to "flow, requirement",
            ),
            "package P { state def S { ",
            setOf("'state'", "'transition'", "'entry'", "'do'", "'exit'", "'first'", "'then'", "'action'", "'attribute'", "'part'", "'port'"),
            setOf("'connection'", "'connect'", "'flow'", "'requirement'", "'interface'", "'allocation'", "'view'", "'metadata'", "'@'"),
        )
        val structure = View(
            "SysMLStructure", Regex("^training/(02|07|08|09|10|11|12|13|38)\\."),
            mapOf(
                "training/38. Allocation/Allocation Definition Example.sysml" to "action",
                "training/38. Allocation/Allocation Usage Example.sysml" to "action",
            ),
            "package P { part def S { ",
            setOf("'part'", "'port'", "'item'", "'attribute'", "'connection'", "'connect'", "'interface'", "'flow'", "'bind'", "'allocation'"),
            setOf("'action'", "'state'", "'first'", "'succession'", "'message'", "'requirement'", "'calc'", "'view'", "'metadata'", "'@'"),
        )
        val activity = View(
            "SysMLActivity", Regex("^training/(14|15|16|17|18|19|20|22|30|31)\\."),
            mapOf(
                "training/31. Constraints/Time Constraints.sysml" to "state",
            ),
            "package P { action def S { ",
            setOf("'action'", "'first'", "'then'", "'succession'", "'flow'", "'bind'", "'decide'", "'merge'", "'fork'", "'join'", "'calc'", "'constraint'"),
            setOf("'state'", "'connection'", "'interface'", "'allocation'", "'message'", "'requirement'", "'view'", "'metadata'", "'@'"),
        )
        val sequence = View(
            "SysMLSequence", Regex("^(training/(21|27)\\.|examples/Interaction Sequencing Examples/|validation/17-)"),
            mapOf(
                "examples/Interaction Sequencing Examples/ServerSequenceOutsideRealization-2.sysml" to "exhibit state",
                "examples/Interaction Sequencing Examples/ServerSequenceRealization-2.sysml" to "exhibit state",
            ),
            "package P { occurrence def S { ",
            setOf("'event'", "'occurrence'", "'message'", "'flow'", "'first'", "'then'", "'succession'", "'part'", "'port'", "'snapshot'", "'timeslice'", "'action'"),
            setOf("'state'", "'allocation'", "'calc'", "'constraint'", "'requirement'", "'view'", "'metadata'", "'@'"),
        )
        val requirements = View(
            "SysMLRequirements", Regex("^(training/32\\.|validation/08-|examples/Requirements Examples/)"),
            mapOf(
                "examples/Requirements Examples/RequirementDerivationExample.sysml" to "#derivation connection",
                "examples/Requirements Examples/VehicleRequirementDerivation.sysml" to "#derivation connection",
                "validation/08-Requirements/8-Requirements.sysml" to "interface",
            ),
            "package P { requirement def S { ",
            setOf("'requirement'", "'concern'", "'subject'", "'require'", "'assume'", "'frame'", "'actor'", "'stakeholder'", "'constraint'", "'attribute'", "'satisfy'"),
            setOf("'state'", "'connection'", "'interface'", "'flow'", "'message'", "'view'", "'metadata'", "'@'"),
        )
        val views = listOf(stateMachine, structure, activity, sequence, requirements)
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

    private val full by lazy { processor(null) }
    private val viewProcessors = mutableMapOf<String, LanguageProcessor<Asm, SentenceContext>>()
    private fun processor(view: View) = viewProcessors.getOrPut(view.grammar) { processor(view.grammar) }

    private fun parses(p: LanguageProcessor<Asm, SentenceContext>, text: String) = p.parse(text).let { it.issues.errors.isEmpty() && null != it.sppt }

    @Test
    fun default_target_is_full_language() {
        assertEquals("SysMLv2_1", full.targetGrammar?.name?.value)
        for (v in views) assertEquals(v.grammar, processor(v).targetGrammar?.name?.value)
    }

    @Test
    fun views_parse_their_examples() {
        val problems = views.flatMap { v ->
            assertTrue(v.exampleFiles.size >= 5, "$v: example files not found in '${validDir.absolutePath}'")
            val p = processor(v)
            val failed = v.exampleFiles.associate { f -> f.relativeTo(validDir).invariantSeparatorsPath to p.parse(f.readText()).issues.errors.map { "${it.location?.line}:${it.location?.column} ${it.message}" }.take(1) }
                .filterValues { it.isNotEmpty() }
            (failed.keys - v.notOnly.keys).map { "$v: does not parse '$it': ${failed[it]}" } +
                    (v.notOnly.keys - failed.keys).map { "$v: parses '$it', remove it from 'notOnly'" }
        }
        assertEquals(emptyList(), problems, problems.joinToString("\n"))
        // the full language parses them all
        assertEquals(emptyList(), views.flatMap { it.exampleFiles }.distinct().filterNot { parses(full, it.readText()) }.map { it.name })
    }

    @Test
    fun views_reject_other_parts() {
        val sentence = "package P { state def S { connection c connect a to b; } }"
        assertTrue(parses(full, sentence))
        val res = processor(stateMachine).parse(sentence)
        assertTrue(res.issues.errors.isNotEmpty())
        assertEquals("package P { state def S { ".length, res.issues.errors.first().location?.position)
        val s2 = "package P { part def S { state s; } }"
        assertTrue(parses(full, s2))
        val pos2 = processor(structure).parse(s2).issues.errors.first().location?.position
        assertTrue(pos2 in "package P { part def S { ".length.."package P { part def S { state ".length, "error at $pos2")
    }

    @Test
    fun views_completion() {
        val problems = views.flatMap { v ->
            val sentence = v.completionAt
            val viewItems = processor(v).expectedTerminalsAt(sentence, sentence.length).items.map { it.label }.toSet()
            val fullItems = full.expectedTerminalsAt(sentence, sentence.length).items.map { it.label }.toSet()
            listOfNotNull(
                (v.keywords - viewItems).takeIf { it.isNotEmpty() }?.let { "$v: not offered $it" },
                viewItems.intersect(v.otherKeywords).takeIf { it.isNotEmpty() }?.let { "$v: offered $it" },
                (v.otherKeywords - fullItems).takeIf { it.isNotEmpty() }?.let { "$v: the full language does not offer $it" },
                (viewItems - fullItems).takeIf { it.isNotEmpty() }?.let { "$v: the full language does not offer $it" },
            )
        }
        assertEquals(emptyList(), problems, problems.joinToString("\n"))
    }

    /**
     * a view uses the references of the language; the libraries and the other files are processed with the full language
     */
    @Test
    fun views_references() {
        val kermlProcessor = KerML_v1_1_2026_09.processorWithReferences()
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
        assertEquals(emptyList(), KerML_v1_1_2026_09.processLibrary(kermlProcessor, ctx))
        val sysmlLib = KerML_v1_1_2026_09.sentenceFiles(File(validDir, "library"), "sysml")
        assertEquals(emptyList(), KerML_v1_1_2026_09.processFiles(fullRefs, sysmlLib, ctx, false))

        val failed = views.flatMap { v ->
            val viewRefs = withReferences(v.grammar)
            assertEquals(emptyList(), viewRefs.allIssues.errors.map { it.message })
            val files = v.exampleFiles.filterNot { it.relativeTo(validDir).invariantSeparatorsPath in v.notOnly }
            // the files that a file uses (other example files) are in the full language, so they are processed with the full language
            files.mapNotNull { f ->
                val fctx = ctx.union(contextAsmSimple())
                KerML_v1_1_2026_09.processFiles(fullRefs, KerML_v1_1_2026_09.usedFiles(f, exampleFiles), fctx, false)
                val errors = KerML_v1_1_2026_09.processFiles(viewRefs, listOf(f), fctx, true).map { it.message }
                val path = f.relativeTo(validDir).invariantSeparatorsPath
                // files that have reference errors with the full language too are not checked
                val fullErrors = errors.isNotEmpty() && KerML_v1_1_2026_09.processFiles(fullRefs, listOf(f), fctx, true).isNotEmpty()
                if (errors.isEmpty() || fullErrors) null else "$v: '$path': ${errors.take(2)}"
            }
        }
        assertEquals(emptyList(), failed, failed.joinToString("\n"))
    }
}
