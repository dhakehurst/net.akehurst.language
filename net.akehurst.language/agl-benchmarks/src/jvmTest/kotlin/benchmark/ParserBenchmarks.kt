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

import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.parser.api.RuleSet
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Level
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Param
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.util.concurrent.TimeUnit

/**
 * JMH benchmarks for [net.akehurst.language.parser.leftcorner.LeftCornerParser].
 *
 * This is the official, JMH-driven replacement for `test_Performance.kt`
 * (which is kept as a back-up / quick-look harness). Run it via:
 *
 *     ./gradlew :agl-parser:jmh
 *     ./gradlew :agl-parser:jmh -Pjmh.include='.*MultiList.*'
 *     ./gradlew :agl-parser:jmh -Pjmh.args='-f 5 -wi 10 -i 20'
 *
 * Methodology
 * -----------
 *  - `Mode.AverageTime` + `OutputTimeUnit.MICROSECONDS`: reports average ns
 *    per `parseForGoal` call, which is the natural unit for "did this code
 *    change make parsing faster". JMH also prints a 99.9 % confidence
 *    interval (`Score Error`) so we can tell whether a delta is real.
 *  - `@Fork(3)`: each benchmark is run in 3 fresh JVMs and the results are
 *    aggregated. This is the single most important defence against JIT and
 *    inline-cache state from previous benchmarks polluting the next.
 *  - `@Warmup(5 × 1s) / @Measurement(10 × 1s)`: ≥ 5 s of warmup gives HotSpot
 *    time to compile and stabilise; 10 measurement iterations × 3 forks =
 *    30 samples, enough to keep the confidence interval narrow without
 *    running for hours.
 *  - Each grammar / size combination is a separate `@State` class with a
 *    `@Param` for the input size; this lets JMH iterate over sizes
 *    automatically while keeping the parser + input strings allocated once
 *    per trial (`@Setup(Level.Trial)`).
 *  - The `@Benchmark` methods **return** the parse result so JMH consumes
 *    it via its hidden `Blackhole` — this prevents dead-code elimination
 *    from optimising the parse away.
 *  - Heap is fixed (`-Xms2g -Xmx2g -XX:+AlwaysPreTouch`) and GC is set to
 *    G1; configured in `agl-parser/build.gradle.kts`.
 *
 * Cross-reference: see `5_Documentation/PERFORMANCE_OPPORTUNITIES.md` for
 * the list of constant-factor improvements being tracked against this suite.
 */
private object Grammars {
    /** S = 'a'+   (multi list, runtime-guarded GRAFT) */
    fun multi()= ruleSet("Multi") {
        multi("S", 1, -1, "'a'")
        literal("'a'", "a")
    }

    /** S = 'a' | 'a' S   (right-recursive choice, exercises HEIGHT) */
    fun rightRecursive() = ruleSet("RightRecursive") {
        choiceLongest("S") {
            concatenation { literal("a") }
            concatenation { literal("a"); ref("S") }
        }
    }

    /** S = ['a' / ',']+   with whitespace skip */
    fun sList()= ruleSet("SList") {
        pattern("WS", "\\s+", isSkip = true)
        sList("S", 1, -1, "'a'", "','")
        literal("'a'", "a")
        literal("','", ",")
    }

    /** E = E '+' E | E '*' E | 'n'   (precedence + merge) */
    fun expression() = ruleSet("Expression") {
        pattern("WS", "\\s+", isSkip = true)
        choiceLongest("E") {
            concatenation { ref("E"); literal("+"); ref("E") }
            concatenation { ref("E"); literal("*"); ref("E") }
            concatenation { literal("n") }
        }
    }

}

private fun newParser(rrs: RuleSet): LeftCornerParser =
    LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.terminals), rrs)

private fun repeated(s: String, n: Int): String =
    buildString(s.length * n) { repeat(n) { append(s) } }

private fun separatedList(item: String, sep: String, n: Int): String =
    buildString {
        for (i in 0 until n) {
            if (i != 0) append(sep)
            append(item)
        }
    }

private fun expressionInput(n: Int): String = buildString {
    for (i in 0 until n) {
        if (i != 0) append('+')
        append('n')
    }
}

// Shared annotations for every benchmark state class. Kept here so a single
// edit re-tunes the entire suite.
private const val WARMUP_ITERS = 5
private const val MEASURE_ITERS = 10
private const val FORKS = 3

// ---------------------------------------------------------------------------
// 1. multi list   — pure WIDTH/GRAFT (with runtime guard)
// ---------------------------------------------------------------------------
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = WARMUP_ITERS, time = 1)
@Measurement(iterations = MEASURE_ITERS, time = 1)
@Fork(FORKS)
@State(Scope.Benchmark)
open class BenchMultiList {
    @Param("100", "1000", "10000")
    var size: Int = 0

    private lateinit var parser: LeftCornerParser
    private lateinit var input: String

    @Setup(Level.Trial)
    fun setup() {
        parser = newParser(Grammars.multi())
        input = repeated("a", size)
    }

    @Benchmark
    fun parse(): Any? = parser.parseForGoal("S", input)
}

// ---------------------------------------------------------------------------
// 2. right-recursive choice   — pure HEIGHT
// ---------------------------------------------------------------------------
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = WARMUP_ITERS, time = 1)
@Measurement(iterations = MEASURE_ITERS, time = 1)
@Fork(FORKS)
@State(Scope.Benchmark)
open class BenchRightRecursive {
    @Param("50", "500", "5000")
    var size: Int = 0

    private lateinit var parser: LeftCornerParser
    private lateinit var input: String

    @Setup(Level.Trial)
    fun setup() {
        parser = newParser(Grammars.rightRecursive())
        input = repeated("a", size)
    }

    @Benchmark
    fun parse(): Any? = parser.parseForGoal("S", input)
}

// ---------------------------------------------------------------------------
// 3. separated list with skip — GRAFT + runtime guard + skip cache
// ---------------------------------------------------------------------------
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = WARMUP_ITERS, time = 1)
@Measurement(iterations = MEASURE_ITERS, time = 1)
@Fork(FORKS)
@State(Scope.Benchmark)
open class BenchSListWithSkip {
    @Param("50", "500", "5000")
    var size: Int = 0

    private lateinit var parser: LeftCornerParser
    private lateinit var input: String

    @Setup(Level.Trial)
    fun setup() {
        parser = newParser(Grammars.sList())
        input = separatedList("a", " , ", size)
    }

    @Benchmark
    fun parse(): Any? = parser.parseForGoal("S", input)
}

// ---------------------------------------------------------------------------
// 4. expression grammar — HEIGHT + precedence resolution / merge-decision
// ---------------------------------------------------------------------------
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = WARMUP_ITERS, time = 1)
@Measurement(iterations = MEASURE_ITERS, time = 1)
@Fork(FORKS)
@State(Scope.Benchmark)
open class BenchExpression {
    @Param("50", "500", "5000")
    var size: Int = 0

    private lateinit var parser: LeftCornerParser
    private lateinit var input: String

    @Setup(Level.Trial)
    fun setup() {
        parser = newParser(Grammars.expression())
        input = expressionInput(size)
    }

    @Benchmark
    fun parse(): Any? = parser.parseForGoal("E", input)
}

// ---------------------------------------------------------------------------
// 5. expression grammar with reportErrors=false — isolates failed-transition
//    recording overhead. Compare against [BenchExpression] at matching size.
// ---------------------------------------------------------------------------
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = WARMUP_ITERS, time = 1)
@Measurement(iterations = MEASURE_ITERS, time = 1)
@Fork(FORKS)
@State(Scope.Benchmark)
open class BenchExpressionNoErrorReporting {
    @Param("50", "500", "5000")
    var size: Int = 0

    private lateinit var parser: LeftCornerParser
    private lateinit var input: String
    private lateinit var opts: ParseOptionsDefault

    @Setup(Level.Trial)
    fun setup() {
        parser = newParser(Grammars.expression())
        input = expressionInput(size)
        opts = ParseOptionsDefault(goalRuleName = "E", reportErrors = false)
    }

    @Benchmark
    fun parse(): Any? = parser.parse(input, opts)
}

