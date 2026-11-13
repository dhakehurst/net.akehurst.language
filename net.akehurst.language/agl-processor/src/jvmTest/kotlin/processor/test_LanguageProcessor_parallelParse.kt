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
package net.akehurst.language.agl.processor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.parser.leftcorner.ParseInterruptDefault
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.regex.api.RegexEngineKind
import net.akehurst.language.scanner.api.ScannerKind
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Several parses of one processor at the same time (JVM threads) must each give the result they give when run alone.
 */
class test_LanguageProcessor_parallelParse {

    private companion object {
        val SYSML_GRAMMAR by lazy { File("../language-repository/languages/SysML/v2_2026-09/grammars/standard/grammar.agl").readText() }

        val SENTENCES = listOf(
            "package Testsat {\n    part def Battery {\n        attribute capacity : Real = 42.5;\n    }\n}",
            "package A;",
            "package P {\n    part def Vehicle {\n        part engine : Engine;\n        part wheels : Wheel[4];\n    }\n    part def Engine;\n    part def Wheel;\n}",
            "package Q {\n    action def Drive {\n        in speed : Real;\n        out distance : Real;\n    }\n}",
            "package R {\n    state def S {\n        entry; then off;\n        state off;\n        state on;\n        transition first off then on;\n    }\n}",
        )

        fun processor(scannerKind: ScannerKind?): LanguageProcessor<Asm, SentenceContext> {
            val res = Agl.processorFromStringSimple(
                GrammarString(SYSML_GRAMMAR),
                configurationBase = when (scannerKind) {
                    null -> Agl.configurationSimple()
                    else -> Agl.configuration(Agl.configurationSimple()) { scanner(scannerKind, RegexEngineKind.PLATFORM) }
                }
            )
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            return res.processor!!
        }
    }

    private fun parseInParallel(scannerKind: ScannerKind?, buildFirst: Boolean) {
        val processor = processor(scannerKind)
        if (buildFirst) processor.parse("package A;")
        val text = SENTENCES[0]
        val results = runBlocking(Dispatchers.Default) { (1..40).map { async { processor.parse(text) } }.awaitAll() }
        assertEquals(emptyList(), results.flatMap { it.issues.errors }.map { it.toString() }.distinct())
        assertTrue(results.all { null != it.sppt })
        val expected = processor(scannerKind).parse(text).sppt!!.toStringAll
        assertTrue(results.all { it.sppt!!.toStringAll == expected })
    }

    @Test
    fun parse_same_sentence_after_first_parse() = parseInParallel(null, true)

    @Test
    fun parse_same_sentence_on_new_processor() = parseInParallel(null, false)

    @Test
    fun parse_same_sentence_after_first_parse_OnDemand_scanner() = parseInParallel(ScannerKind.OnDemand, true)

    @Test
    fun parse_same_sentence_on_new_processor_OnDemand_scanner() = parseInParallel(ScannerKind.OnDemand, false)

    @Test
    fun parse_and_process_different_sentences() {
        val processor = processor(null)
        // result of each sentence when parsed alone, each on its own processor
        val alone = SENTENCES.associateWith { s -> processor(null).parse(s).let { Pair(it.issues.errors.map { it.toString() }, it.sppt?.toStringAll) } }
        val results = runBlocking(Dispatchers.Default) {
            (1..10).flatMap { i ->
                SENTENCES.map { s ->
                    async {
                        val r = if (0 == i % 2) processor.parse(s) else processor.process(s).parse!!
                        Pair(s, Pair(r.issues.errors.map { it.toString() }, r.sppt?.toStringAll))
                    }
                }
            }.awaitAll()
        }
        results.forEach { (s, r) -> assertEquals(alone[s], r, "for sentence:\n$s") }
    }

    @Test
    fun process_different_sentences() {
        val processor = processor(null)
        val alone = SENTENCES.associateWith { s -> processor(null).process(s).let { r -> r.allIssues.errors.map { it.toString() } } }
        val results = runBlocking(Dispatchers.Default) {
            (1..10).flatMap { SENTENCES.map { s -> async { Pair(s, processor.process(s).allIssues.errors.map { it.toString() }) } } }.awaitAll()
        }
        results.forEach { (s, r) -> assertEquals(alone[s], r, "for sentence:\n$s") }
    }

    @Test
    fun interrupt_stops_only_its_own_parse() {
        val processor = processor(null)
        val text = SENTENCES[2]
        val expected = processor(null).parse(text).sppt!!.toStringAll
        val results = runBlocking(Dispatchers.Default) {
            (1..20).map { i ->
                async {
                    val opts = processor.parseOptionsDefault()
                    if (0 == i % 2) {
                        // interrupted before it starts, so it is sure to be stopped
                        opts.interrupt = ParseInterruptDefault().also { it.interrupt("stop $i") }
                    }
                    Pair(i, processor.parse(text, opts))
                }
            }.awaitAll()
        }
        results.forEach { (i, r) ->
            if (0 == i % 2) {
                assertEquals(null, r.sppt)
                assertEquals(listOf("The Parser was terminated: stop $i"), r.issues.errors.map { it.message })
            } else {
                assertEquals(emptyList(), r.issues.errors.map { it.toString() })
                assertEquals(expected, r.sppt?.toStringAll)
            }
        }
    }

    @Test
    fun parse_scan_and_expectedAt_at_the_same_time() {
        val processor = processor(null)
        val text = SENTENCES[2]
        val aloneParse = processor(null).parse(text).sppt!!.toStringAll
        val aloneScan = processor(null).scan(text)!!.allTokens.map { it.toString() }
        val aloneExpected = processor(null).expectedTerminalsAt(text, 25).items.map { it.toString() }.toSet()
        runBlocking(Dispatchers.Default) {
            (1..10).flatMap {
                listOf(
                    async { assertEquals(aloneParse, processor.parse(text).sppt?.toStringAll) },
                    async { assertEquals(aloneScan, processor.scan(text)!!.allTokens.map { it.toString() }) },
                    async { assertEquals(aloneExpected, processor.expectedTerminalsAt(text, 25).items.map { it.toString() }.toSet()) },
                )
            }.awaitAll()
        }
    }

}
