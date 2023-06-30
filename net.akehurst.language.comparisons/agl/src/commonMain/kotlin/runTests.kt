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
import net.akehurst.language.agl.grammar.grammar.AglGrammarSemanticAnalyser
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.comparisons.common.FileDataCommon
import net.akehurst.language.comparisons.common.Java8TestFilesCommon
import net.akehurst.language.comparisons.common.ResultsCommon
import net.akehurst.language.comparisons.common.myResourcesVfs
import kotlin.time.measureTime

const val col = "agl_optm"
var totalFiles = 0

suspend fun files(): Collection<FileDataCommon> {
    val f = Java8TestFilesCommon.files().subList(0, 5)//300) // after this we get java.lang.OutOfMemoryError: Java heap space
    totalFiles = f.size
    println("Number of files to test against: ${f.size}")
    return f
}

suspend fun createAndBuildProcessor(aglFile: String): LanguageProcessor<AsmSimple, ContextSimple> {
    println(StandardPaths.cwd)
    val javaGrammarStr = myResourcesVfs[aglFile].readString()
    val res = Agl.processorFromString<AsmSimple, ContextSimple>(
        grammarDefinitionStr = javaGrammarStr,
        aglOptions = Agl.options {
            semanticAnalysis {
                // switch off ambiguity analysis for performance
                option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
            }
        }
    )
    // no need to build because, sentence is parsed twice in the test
    return res.processor!!
}

fun parseWithJava8Agl(aglProcessor: LanguageProcessor<AsmSimple, ContextSimple>, file: FileDataCommon, input: String) {
    try {
        val tm1 = measureTime {
            aglProcessor.parse(input, Agl.parseOptions { goalRuleName("CompilationUnit") })
        }
        ResultsCommon.log(true, "${col}-T1", file, tm1)
        val tm2 = measureTime {
            aglProcessor.parse(input, Agl.parseOptions { goalRuleName("CompilationUnit") })
        }
        ResultsCommon.log(true, "${col}-T2", file, tm2)
    } catch (e: Throwable) {
        println("Error: ${e.message}")
        ResultsCommon.logError("${col}-T1", file)
    }
}

suspend fun runTests() {
    val aglProcessor = createAndBuildProcessor("agl/Java8AglOptm.agl")

    ResultsCommon.reset()

    for (file in files()) {
        println("File: ${file.index} of $totalFiles ")
        val input = file.path.readString()
        parseWithJava8Agl(aglProcessor, file, input)
    }

    ResultsCommon.write(col)
}

expect suspend fun main()