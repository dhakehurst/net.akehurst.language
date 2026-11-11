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
import net.akehurst.language.agl.processor.contextFromGrammarRegistry
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.StyleString
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests the SysML v2 2026-09 grammars with the example models of the SysML v2 Release (in 'valid')
 * and with some invalid sentences (in 'invalid').
 */
abstract class test_SysML_v2_2026_09(
    grammarName: String,
    /** relative paths (from 'valid') of example files that are known not to parse */
    private val knownFailing: Set<String>
) {

    private val languageDir = File("../language-repository/languages/SysML/v2_2026-09")
    private val grammarDir = File(languageDir, "grammars/$grammarName")
    private val grammarStr by lazy { File(grammarDir, "grammar.agl").readText() }
    private val styleStr by lazy { File(grammarDir, "style.agl").readText() }

    private val processor: LanguageProcessor<Asm, SentenceContext> by lazy {
        val res = Agl.processorFromStringSimple(GrammarString(grammarStr), styleStr = StyleString(styleStr))
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
        res.processor!!
    }

    private fun sentenceFiles(kind: String) = File(languageDir, kind).walkTopDown()
        .filter { it.isFile && it.name.endsWith(".sysml") }
        .sortedBy { it.path }
        .toList()

    private fun parses(file: File): Boolean = processor.parse(file.readText()).let { it.issues.errors.isEmpty() && null != it.sppt }

    @Test
    fun parse_grammar() {
        val res = Agl.registry.agl.grammar.processor!!.parse(grammarStr)
        assertTrue(res.issues.isEmpty(), res.issues.toString())
    }

    @Test
    fun process_grammar() {
        val res = Agl.registry.agl.grammar.processor!!.process(grammarStr, Agl.options { semanticAnalysis { sentenceContext(contextFromGrammarRegistry(Agl.registry)) } })
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
    }

    @Test
    fun processor_with_style() {
        assertTrue(processor.typesDomain.allDefinitions.isNotEmpty())
    }

    @Test
    fun parse_valid() {
        val files = sentenceFiles("valid")
        assertTrue(files.size > 200, "example files not found in '${languageDir.absolutePath}'")
        val failed = files.filterNot { parses(it) }.map { it.relativeTo(File(languageDir, "valid")).invariantSeparatorsPath }.toSet()
        val unexpectedFailures = failed - knownFailing
        val unexpectedPasses = knownFailing - failed
        assertTrue(unexpectedFailures.isEmpty(), "${unexpectedFailures.size} examples do not parse:\n${unexpectedFailures.joinToString("\n")}")
        assertTrue(unexpectedPasses.isEmpty(), "examples now parse, remove them from 'knownFailing':\n${unexpectedPasses.joinToString("\n")}")
    }

    @Test
    fun parse_invalid() {
        val files = sentenceFiles("invalid")
        assertTrue(files.isNotEmpty())
        val parsed = files.filter { parses(it) }.map { it.name }
        assertEquals(emptyList(), parsed, "invalid sentences that parse")
    }

    @Test
    fun process_valid_single() {
        val sentence = """
            package 'Part Definition Example' {
                private import ScalarValues::*;
                part def Vehicle {
                    attribute mass : Real;
                    part eng : Engine;
                    ref part driver : Person;
                }
                part def Engine;
                part def Person;
            }
        """.trimIndent()
        val res = processor.process(sentence)
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        assertTrue(null != res.asm)
    }
}

class test_SysML_v2_2026_09_standard : test_SysML_v2_2026_09("standard", emptySet())

class test_SysML_v2_2026_09_agl : test_SysML_v2_2026_09("agl", emptySet())
