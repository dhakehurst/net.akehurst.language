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

package net.akehurst.language.agl.benchmark

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.contextFromGrammarRegistry
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser
import net.akehurst.language.parser.api.RuleSet
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import kotlin.io.path.pathString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
 *  - Each (workload, size) is **calibrated** to choose `innerReps` so each
 *    timed sample takes ≈ 5 ms; this averages out per-call clock-granularity
 *    and short GC noise. We then take ITERATIONS such samples.
 *  - Reported statistics are `min`, p10, p50, p90 — `min` is the headline
 *    number (most JIT-noise-resistant), and p90/min is a stability indicator
 *    (≈1.0 means stable, >1.5 means the run had significant GC/scheduling
 *    noise and comparisons should be treated with caution).
 *  - We do NOT report `mean` or `max`: both are dominated by occasional GC
 *    pauses and JIT (de)optimisation events and obscure steady-state cost.
 *  - System.gc() is called between sample groups to reduce cross-sample
 *    GC pollution.
 *  - The grammars and inputs are deliberately small (so total runtime stays
 *    in the order of a few minutes), but exercise the four primary parser
 *    work-loads:
 *      1. multi list           — pure WIDTH/GRAFT (with runtime guard)
 *      2. right-recursive list — pure HEIGHT
 *      3. separated list       — GRAFT + runtime guard + skip
 *      4. expression grammar   — HEIGHT + precedence resolution
 *  - **Methodology note**: ignore n=50/n=100 deltas when comparing runs;
 *    sub-millisecond timings are noise-dominated. Use n≥500 for trend
 *    detection. A delta is only "real" if it exceeds ~5 % AND appears in
 *    `min` and `p50` together.
 *
 * Cross-reference: see `5_Documentation/PERFORMANCE_OPPORTUNITIES.md` for the
 * list of constant-factor improvements being tracked against this benchmark.
 */
class test_Performance {

    private companion object {
        // -----------------------------------------------------------------
        // Tuning knobs — see "Methodology" section in
        // 5_Documentation/PERFORMANCE_OPPORTUNITIES.md.
        //
        // Each timed *sample* runs the parser INNER_REPS times and divides
        // by INNER_REPS. INNER_REPS is calibrated per (workload, size) to
        // make each sample at least [TARGET_SAMPLE_MICROS] long; this
        // averages out per-call noise (clock granularity, safepoints, short
        // GC pauses) so that each sample is itself a mean-of-INNER_REPS
        // rather than a single noisy point.
        //
        // We then take ITERATIONS such samples and report `min`, p10, p50,
        // p90. We deliberately do NOT report `mean` or `max` — they are
        // dominated by occasional GC pauses and JIT (de)optimisation events
        // and obscure the underlying steady-state cost.
        //
        // Total runtime for the full suite at the values below is ~3-5 min.
        // -----------------------------------------------------------------
        const val WARMUP_MILLIS = 1_000L          // warm up for at least 1 s
        const val ITERATIONS = 100
        const val TARGET_SAMPLE_MICROS = 5_000L   // 5 ms per sample
        const val MIN_INNER_REPS = 1
        const val MAX_INNER_REPS = 100
        const val CALIBRATION_PROBES = 10         // median of N single-call times

        // Force a GC + short settle so a long-tail GC inside one sample
        // doesn't pollute the next. Settling before *every* sample is more
        // expensive but tightens the p10..p90 spread substantially.
        const val SETTLE_BETWEEN_SAMPLES = true

        // Input sizes. n=50/100 are kept for correctness-checking only; do
        // not use their timings to compare runs (sub-millisecond timings
        // are dominated by JIT warmup and clock granularity, ±30 % noise
        // is normal even with inner-rep averaging).
        val SIZES_SMALL = listOf(100, 1_000, 10_000)
        val SIZES_MEDIUM = listOf(50, 500, 5_000)

        // A delta between two runs is only "real" if it exceeds
        // RELIABLE_DELTA_PERCENT *and* appears in `min` and `p50` together.
        // Below this, treat as noise.
        const val RELIABLE_DELTA_PERCENT = 5
    }

    // ----- helpers ------------------------------------------------------

    private fun newParser(rrs: RuleSet): LeftCornerParser =
        LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)

    /**
     * Calibrate `innerReps` so that one timed sample takes ≈
     * [TARGET_SAMPLE_MICROS]. Larger samples reduce per-call clock-
     * granularity noise but cost wall-clock time.
     *
     * Stability matters: an unstable calibration would pick different
     * `innerReps` values across runs of identical code, which changes
     * what `min` measures. We therefore probe [CALIBRATION_PROBES] times
     * and take the **median** single-call time, which is robust to
     * outliers (a single GC pause during one probe).
     *
     * Calibration also serves as a tier-1 warmup pass — by the time we
     * return, the method has been called many times.
     */
    private fun calibrateInnerReps(block: () -> Unit): Int {
        // First, do a few un-timed runs so the JIT can compile.
        repeat(5) { block() }
        // Probe several times and take the median single-call time.
        val probes = LongArray(CALIBRATION_PROBES)
        for (i in 0 until CALIBRATION_PROBES) {
            val mark = TimeSource.Monotonic.markNow()
            block()
            probes[i] = mark.elapsedNow().inWholeMicroseconds.coerceAtLeast(1L)
        }
        probes.sort()
        val singleCallMicros = probes[probes.size / 2]
        val n = (TARGET_SAMPLE_MICROS / singleCallMicros).toInt()
        return n.coerceIn(MIN_INNER_REPS, MAX_INNER_REPS)
    }

    /**
     * Time-based warmup: run [block] continuously for at least
     * [WARMUP_MILLIS] so HotSpot has time to compile and any class-init,
     * lazy-init, and inline-cache effects settle. More reliable than a
     * fixed sample count because per-call time varies by workload.
     */
    private fun warmup(innerReps: Int, block: () -> Unit) {
        val mark = TimeSource.Monotonic.markNow()
        val deadlineMicros = WARMUP_MILLIS * 1_000L
        while (mark.elapsedNow().inWholeMicroseconds < deadlineMicros) {
            for (j in 0 until innerReps) block()
        }
    }

    /**
     * Runs [block] [innerReps] times in a tight loop and returns the
     * average elapsed time in microseconds. Inner-looping reduces per-call
     * clock-granularity / safepoint noise and lets each "sample" reflect a
     * stable measurement rather than a single noisy point.
     */
    private fun timeSample(innerReps: Int, block: () -> Unit): Long {
        val mark = TimeSource.Monotonic.markNow()
        for (i in 0 until innerReps) {
            block()
        }
        return mark.elapsedNow().inWholeMicroseconds / innerReps
    }

    private fun settle() {
        if (SETTLE_BETWEEN_SAMPLES) {
            // Two GC cycles to give finalisers a chance to run, then a
            // brief pause so the next sample doesn't start in a GC tail.
            System.gc()
            System.gc()
            try {
                Thread.sleep(2)
            } catch (_: InterruptedException) { /* ignore */
            }
        }
    }

    /**
     * Runs [block] [WARMUP_MILLIS] ms of warmup followed by [ITERATIONS]
     * timed samples and prints summary statistics. Reports `min`, p10,
     * p50, p90 and chars-per-second computed from `min` (the most
     * JIT-noise-resistant statistic).
     *
     * `min` is the headline number for cross-run comparison: it is the
     * fastest the JIT-compiled code achieved when nothing went wrong, and
     * is more stable than `med` across runs.
     *
     * The p90/min ratio is printed as a stability indicator. Values close
     * to 1.0 mean the measurement is tight; >1.5 means the run had
     * significant GC/scheduling noise and the comparison should be
     * treated with caution.
     */
    private fun benchmark(label: String, inputLength: Int, block: () -> Unit) {
        val innerReps = calibrateInnerReps(block)
        warmup(innerReps, block)
        settle()

        val samples = LongArray(ITERATIONS)
        for (i in 0 until ITERATIONS) {
            settle()                     // settle BEFORE every sample
            samples[i] = timeSample(innerReps, block)
        }
        samples.sort()
        val min = samples.first()
        val p10 = samples[(samples.size * 10) / 100]
        val p50 = samples[samples.size / 2]
        val p90 = samples[(samples.size * 90) / 100]
        // Throughput from `min`: the fastest the parser actually ran.
        val charsPerSec = if (min > 0) (inputLength.toLong() * 1_000_000L) / min else -1L
        // p90/min ratio: stability indicator. ~1.0 = very stable, >1.5 = noisy.
        val stabilityX10 = if (min > 0) (p90 * 10) / min else -1L

        println(
            "PERF | ${label.padEnd(40)} | n=${inputLength.toString().padStart(7)}" +
                    " | reps=${innerReps.toString().padStart(3)}" +
                    " | min=${min.toString().padStart(8)}µs" +
                    " | p10=${p10.toString().padStart(8)}µs" +
                    " | p50=${p50.toString().padStart(8)}µs" +
                    " | p90=${p90.toString().padStart(8)}µs" +
                    " | ${charsPerSec.toString().padStart(12)} ch/s" +
                    " | p90/min=${stabilityX10 / 10}.${stabilityX10 % 10}"
        )
    }

    // ----- grammars -----------------------------------------------------

    /** S = 'a'+   (multi list, runtime-guarded GRAFT) */
    private fun multiGrammar() = ruleSet("Multi") {
        multi("S", 1, -1, "'a'")
        literal("'a'", "a")
    }

    /** S = 'a' | 'a' S   (right-recursive choice, exercises HEIGHT) */
    private fun rightRecursiveGrammar() = ruleSet("RightRecursive") {
        choiceLongest("S") {
            concatenation { literal("a") }
            concatenation { literal("a"); ref("S") }
        }
    }

    /** S = ['a' / ',']+   with whitespace skip */
    private fun sListGrammar() = ruleSet("SList") {
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
    private fun expressionGrammar() = ruleSet("Expression") {
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

    /**
     * Variant of [bench_04_expression_grammar_with_precedence] that disables
     * error reporting (`reportErrors = false`). This isolates the cost of the
     * failed-transition recording paths in `RuntimeParserAgl` (see
     * `PERFORMANCE_OPPORTUNITIES.md` items #5, #11). Compare medians vs
     * `bench_04` to see the overhead of error recording on a successful parse.
     */
    @Test
    fun bench_05_expression_no_error_reporting() {
        val rrs = expressionGrammar()
        val parser = newParser(rrs)
        for (size in SIZES_MEDIUM) {
            val input = expressionInput(size)
            val opts = ParseOptionsDefault(goalRuleName = "E", reportErrors = false)
            val r0 = parser.parse(input, opts)
            assertNotNull(r0.sppt, r0.issues.joinToString("\n"))

            benchmark("expr   E=...    (reportErrors=off)", size) {
                parser.parse(input, opts)
            }
        }
    }

   // @Test
    fun bench_06_java8() {
        val grammarFile = "/Java/version_8/grammars/grammar_aglOptm.agl"
        val grammarStr = this::class.java.getResource(grammarFile)?.readText() ?: error("file not found '$grammarFile'")
        val proc = Agl.processorFromString<Asm, SentenceContext>(
            grammarDefinitionStr = grammarStr,
            aglOptions = Agl.options {
                semanticAnalysis {
                    sentenceContext(contextFromGrammarRegistry(Agl.registry))
                    // switch off ambiguity analysis for performance
                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                }
            }
        ).let {
            check(it.issues.errors.isEmpty()) { it.issues.toString() }
            check(null != it.processor) { "Processor is null" }
            it.processor!!
        }

        val parser = proc.parser!!
        val files = Java8TestFiles.files
        for (fileData in files) {
            val sentence =fileData.path.toFile().readText()
            val r0 = parser.parseForGoal("CompilationUnit", sentence)
            assertNotNull(r0.sppt, r0.issues.joinToString("\n"))
            // Note: ambiguity warnings may appear depending on grammar tweaks;
            // we tolerate non-error issues but require a parse tree.

            benchmark(fileData.path.pathString, sentence.length) {
                parser.parseForGoal("CompilationUnit", sentence)
            }
        }
    }
}