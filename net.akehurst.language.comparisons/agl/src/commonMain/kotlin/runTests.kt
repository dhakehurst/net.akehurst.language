/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.comparisons.agl

import korlibs.io.file.std.StandardPaths
import net.akehurst.language.agl.language.grammar.AglGrammarSemanticAnalyser
import net.akehurst.language.agl.language.grammar.ContextFromGrammarRegistry
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.semanticAnalyser.ContextSimple
import net.akehurst.language.api.asm.Asm
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.ParseOptions
import net.akehurst.language.comparisons.common.*
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.measureTime

val maxFiles = 6000 //for testing tests with less files
val maxParseTimeBeforeBreak = 30 //seconds

fun maxD(d1:Duration, d2:Duration) = if (d1 > d2) d1 else d2

suspend fun output(msg: String) {
    runTest {
        println(msg)
    }
}

suspend fun javaFiles(numFiles: Int, skipPatterns: Set<Regex>): Collection<FileDataCommon> {
    val f = FilesCommon.javaFiles("nogit/javaTestFiles.txt", skipPatterns) { it.endsWith(".java") }
    val nf = min(min(numFiles, maxFiles), f.size)
    output("Number of files to test against: $nf of ${f.size}")
    return f.subList(0, nf)
}

suspend fun files(rootDirInfo: String, numFiles: Int, skipPatterns: Set<Regex>, filter: (path: String) -> Boolean): Collection<FileDataCommon> {
    val f = FilesCommon.filesRecursiveFromDir(rootDirInfo, skipPatterns, filter)
    val nf = min(min(numFiles, maxFiles), f.size)
    val sub = f.subList(0, nf)
    output("Number of files to test against: $nf of ${f.size}")
    return sub
}

suspend fun createAndBuildProcessor(aglFile: String): LanguageProcessor<Asm, ContextSimple> {
    output(StandardPaths.cwd)
    val javaGrammarStr = myResourcesVfs[aglFile].readString()
    val res = Agl.processorFromString<Asm, ContextSimple>(
        grammarDefinitionStr = javaGrammarStr,
        aglOptions = Agl.options {
            semanticAnalysis {
                // switch off ambiguity analysis for performance
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                context(ContextFromGrammarRegistry(Agl.registry))
            }
        }
    )
    // no need to build because, sentence is parsed twice in the test
    return res.processor!!
}

// split to enable separate profile view on each parse
fun parse1(aglProcessor: LanguageProcessor<Asm, ContextSimple>, opts: ParseOptions, input: String) = measureTime {
    aglProcessor.parse(input, opts)
}

fun parse2(aglProcessor: LanguageProcessor<Asm, ContextSimple>, opts: ParseOptions, input: String) = measureTime {
    aglProcessor.parse(input, opts)
}

fun parseTwiceAndMeasure(parserCode: String, aglProcessor: LanguageProcessor<Asm, ContextSimple>, goalRule: String, file: FileDataCommon, input: String): Duration {
    try {
        val opts = Agl.parseOptions {
            reportErrors(false) // don't need error reporting for speed tests
            goalRuleName(goalRule)
        }
        val tm1 = parse1(aglProcessor, opts, input)
        ResultsCommon.log(true, "${parserCode}-T1-$kotlinTarget", file, tm1)
        val tm2 = parse2(aglProcessor, opts, input)
        ResultsCommon.log(true, "${parserCode}-T2-$kotlinTarget", file, tm2)
        return maxD(tm1,tm2)
    } catch (e: Throwable) {
        println("Error: ${e.message}")
        ResultsCommon.logError("${parserCode}-T1-$kotlinTarget", file)
    }
    return Duration.ZERO
}

suspend fun parseJavaFiles(parserCode: String, numFiles: Int, grammarFile: String, goalRule: String) {
    output("----- $parserCode -----")
    ResultsCommon.reset()
    val aglProcessor = createAndBuildProcessor(grammarFile)
    // geting skip patterns like this only works because for the given test grammars, skip is all defined by terminals
    val skipPatterns = aglProcessor.grammar!!.allResolvedSkipTerminal.map { Regex(it.value) }.toSet()
    val files = javaFiles(numFiles, skipPatterns)
    val totalFiles = files.size
    for (file in files) {
        output("File: ${file.index} of $totalFiles ")
        val input = file.path.readString()
        val md = parseTwiceAndMeasure(parserCode, aglProcessor, goalRule, file, input)
        if (md.inWholeSeconds >= maxParseTimeBeforeBreak) break
    }

    ResultsCommon.write("$parserCode-$kotlinTarget")
}

suspend fun parseFiles(parserCode: String, numFiles: Int, grammarFile: String, goalRule: String, rootDirInfo: String, ext: String) {
    output("----- $parserCode -----")
    ResultsCommon.reset()
    val aglProcessor = createAndBuildProcessor(grammarFile)
    // geting skip patterns like this only works because for the given test grammars, skip is all defined by terminals
    val skipPatterns = aglProcessor.grammar!!.allResolvedSkipTerminal.map { Regex(it.value) }.toSet()
    val files = files(rootDirInfo, numFiles, skipPatterns) { it.endsWith(".$ext") }
    val totalFiles = files.size
    for (file in files) {
        output("File: ${file.index} of $totalFiles ")
        val input = file.path.readString()
        val md = parseTwiceAndMeasure(parserCode, aglProcessor, goalRule, file, input)
        if (md.inWholeSeconds >= maxParseTimeBeforeBreak) break
    }

    ResultsCommon.write(parserCode)
}

suspend fun runTests() {
    //parseFiles("stchrt", 500, "agl/Statechart.agl", "statechart", "nogit/statechartTestFiles.txt", "sctxt")
    //parseFiles("dot", 500, "agl/Dot.agl", "graph", "nogit/dotTestFiles.txt", "dot")

    parseJavaFiles("ant_optm",4800, "agl/Java8AntlrOptm.agl", "compilationUnit")
    parseJavaFiles("agl_optm", 5300, "agl/Java8AglOptm.agl", "CompilationUnit")
    parseJavaFiles("agl_spec",5240, "agl/Java8AglSpec.agl", "CompilationUnit")
    parseJavaFiles("ant_spec",3500, "agl/Java8AntlrSpec.agl", "compilationUnit")
}


expect suspend fun main()