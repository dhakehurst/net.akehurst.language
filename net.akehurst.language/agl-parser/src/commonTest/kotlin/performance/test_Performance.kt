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

package net.akehurst.language.agl.parser.performance

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Performance smoke-tests for [net.akehurst.language.parser.leftcorner.LeftCornerParser].
 *
 * Goal: provide a small, repeatable benchmark that prints throughput numbers
 * after each parser modification so improvements (or regressions) can be
 * checked manually.
 *
 * Design notes:
 *  - Tests print results and only assert parse correctness; they do not assert
 *    timing thresholds (which vary by hardware / target).
 *  - Cross-platform timing uses `kotlin.time.TimeSource.Monotonic` so the
 *    same numbers are produced on JVM / JS / Native / Wasm.
 *  - Each scenario runs a brief warm-up (`WARMUP`) followed by [ITERATIONS]
 *    timed iterations. The minimum, median, mean and max are printed.
 *  - The grammars and inputs are deliberately small (so total runtime stays
 *    in the order of a few seconds), but exercise the four primary parser
 *    work-loads:
 *      1. multi list           — pure WIDTH/GRAFT (with runtime guard)
 *      2. right-recursive list — pure HEIGHT
 *      3. separated list       — GRAFT + runtime guard + skip
 *      4. expression grammar   — HEIGHT + precedence resolution
 *
 * Cross-reference: see `5_Documentation/PERFORMANCE_OPPORTUNITIES.md` for the
 * list of constant-factor improvements being tracked against this benchmark.
 */
class test_Performance {

    private companion object {
        const val WARMUP = 10
        const val ITERATIONS = 50

        // Bigger N => more accurate timings but longer test run.
        // Adjust if the suite becomes too slow on CI.
        val SIZES_SMALL = listOf(100, 1_000, 10_000)
        val SIZES_MEDIUM = listOf(50, 500, 5_000)
    }

    // ----- helpers ------------------------------------------------------

    private fun newParser(rrs: RuntimeRuleSet): LeftCornerParser =
        LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)

    private fun timeOnce(block: () -> Unit): Duration {
        val mark = TimeSource.Monotonic.markNow()
        block()
        return mark.elapsedNow()
    }

    /**
     * Runs [block] [WARMUP] + [ITERATIONS] times and prints summary statistics.
     * Reports `inputLength / median time` as throughput.
     */
    private fun benchmark(label: String, inputLength: Int, block: () -> Unit) {
        repeat(WARMUP) { block() }
        val samples = LongArray(ITERATIONS)
        for (i in 0 until ITERATIONS) {
            samples[i] = timeOnce(block).inWholeMicroseconds
        }
        samples.sort()
        val min = samples.first()
        val max = samples.last()
        val median = samples[samples.size / 2]
        val mean = samples.sum() / samples.size
        val charsPerSec = if (median > 0) (inputLength.toLong() * 1_000_000L) / median else -1L
        // Padded for easy diffing across runs.
        println(
            "PERF | ${label.padEnd(40)} | n=${inputLength.toString().padStart(7)}" +
                " | min=${min.toString().padStart(8)}µs" +
                " | med=${median.toString().padStart(8)}µs" +
                " | mean=${mean.toString().padStart(8)}µs" +
                " | max=${max.toString().padStart(8)}µs" +
                " | ${charsPerSec.toString().padStart(12)} ch/s"
        )
    }

    // ----- grammars -----------------------------------------------------

    /** S = 'a'+   (multi list, runtime-guarded GRAFT) */
    private fun multiGrammar(): RuntimeRuleSet = runtimeRuleSet {
        multi("S", 1, -1, "'a'")
        literal("'a'", "a")
    }

    /** S = 'a' | 'a' S   (right-recursive choice, exercises HEIGHT) */
    private fun rightRecursiveGrammar(): RuntimeRuleSet = runtimeRuleSet {
        choiceLongest("S") {
            concatenation { literal("a") }
            concatenation { literal("a"); ref("S") }
        }
    }

    /** S = ['a' / ',']+   with whitespace skip */
    private fun sListGrammar(): RuntimeRuleSet = runtimeRuleSet {
        pattern("WS", "\\s+", isSkip = true)
        sList("S", 1, -1, "'a'", "','")
        literal("'a'", "a")
        literal("','", ",")
    }

    /**
     * Simple expression grammar with `+` and `*`.
     *
     *   E = E '+' E | E '*' E | 'n'
     *
     * Implemented with `choiceLongest`; this exercises the precedence
     * resolution path in `RuntimeParserAgl` (and the merge-decision logic).
     */
    private fun expressionGrammar(): RuntimeRuleSet = runtimeRuleSet {
        pattern("WS", "\\s+", isSkip = true)
        choiceLongest("E") {
            concatenation { ref("E"); literal("+"); ref("E") }
            concatenation { ref("E"); literal("*"); ref("E") }
            concatenation { literal("n") }
        }
    }

    // ----- inputs -------------------------------------------------------

    private fun repeated(s: String, n: Int): String = buildString(s.length * n) {
        repeat(n) { append(s) }
    }

    /** "a<sep>a<sep>...<sep>a" with `n` items. */
    private fun separatedList(item: String, sep: String, n: Int): String = buildString {
        for (i in 0 until n) {
            if (i != 0) append(sep)
            append(item)
        }
    }

    /** "n+n+n+...+n" with `n` operands. */
    private fun expressionInput(n: Int): String = buildString {
        for (i in 0 until n) {
            if (i != 0) append('+')
            append('n')
        }
    }

    // ----- tests --------------------------------------------------------

    @Test
    fun bench_01_multi_list() {
        // Parses "aaa...a" as `S = 'a'+`, exercising WIDTH + GRAFT in a tight
        // loop. The runtime guard for `multi` fires on every GRAFT.
        val rrs = multiGrammar()
        val parser = newParser(rrs)
        for (size in SIZES_SMALL) {
            val input = repeated("a", size)
            // Sanity-check on the first iteration only (otherwise the assertion
            // overhead would skew the per-iteration timings).
            val r0 = parser.parseForGoal("S", input)
            assertNotNull(r0.sppt, r0.issues.joinToString("\n"))
            assertEquals(0, r0.issues.size)

            benchmark("multi  S='a'+", size) {
                parser.parseForGoal("S", input)
            }
        }
    }

    @Test
    fun bench_02_right_recursive_choice() {
        // Parses "aaa...a" via S = 'a' | 'a' S (right recursion through HEIGHT).
        val rrs = rightRecursiveGrammar()
        val parser = newParser(rrs)
        for (size in SIZES_MEDIUM) {
            val input = repeated("a", size)
            val r0 = parser.parseForGoal("S", input)
            assertNotNull(r0.sppt, r0.issues.joinToString("\n"))
            assertEquals(0, r0.issues.size)

            benchmark("right-recursive S='a' | 'a' S", size) {
                parser.parseForGoal("S", input)
            }
        }
    }

    @Test
    fun bench_03_separated_list_with_skip() {
        // Parses "a , a , a , ... , a" with whitespace skip, exercising the
        // separator GRAFT path and the skip cache.
        val rrs = sListGrammar()
        val parser = newParser(rrs)
        for (size in SIZES_MEDIUM) {
            val input = separatedList("a", " , ", size)
            val r0 = parser.parseForGoal("S", input)
            assertNotNull(r0.sppt, r0.issues.joinToString("\n"))
            assertEquals(0, r0.issues.size)

            benchmark("sList  S=['a'/',']+   (with ws skip)", size) {
                parser.parseForGoal("S", input)
            }
        }
    }

    @Test
    fun bench_04_expression_grammar_with_precedence() {
        // Parses "n+n+n+...+n" through a self-recursive choice; exercises
        // the precedence / merge-decision machinery at every operator.
        val rrs = expressionGrammar()
        val parser = newParser(rrs)
        for (size in SIZES_MEDIUM) {
            val input = expressionInput(size)
            val r0 = parser.parseForGoal("E", input)
            assertNotNull(r0.sppt, r0.issues.joinToString("\n"))
            // Note: ambiguity warnings may appear depending on grammar tweaks;
            // we tolerate non-error issues but require a parse tree.

            benchmark("expr   E='n' (+|*) 'n' ...", size) {
                parser.parseForGoal("E", input)
            }
        }
    }
}