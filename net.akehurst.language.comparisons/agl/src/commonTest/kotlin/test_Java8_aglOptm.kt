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

import kotlinx.coroutines.test.runTest
import korlibs.io.file.std.StandardPaths
import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.contextFromGrammarRegistry
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.comparisons.common.*
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser
import kotlin.test.Test
import kotlin.time.measureTime

class test_Java8_aglOptm {

    companion object {
        const val col = "agl_optm"
        var totalFiles = 0

        suspend fun createAndBuildProcessor(aglFile: String): LanguageProcessor<Asm, ContextWithScope<Any,Any>> {
            println(StandardPaths.cwd)
            val javaGrammarStr = myResourcesVfs[aglFile].readString()
            val res = Agl.processorFromString<Asm, ContextWithScope<Any,Any>>(
                grammarDefinitionStr = javaGrammarStr,
                aglOptions = Agl.options {
                    semanticAnalysis {
                        // switch off ambiguity analysis for performance
                        option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                        context(contextFromGrammarRegistry(Agl.registry))
                    }
                }
            )
            // no need to build because, sentence is parsed twice in the test
            return res.processor!!
        }

        lateinit var aglProcessor: LanguageProcessor<Asm, ContextWithScope<Any,Any>>

        suspend fun files(): Collection<FileDataCommon> {
            val skipPatterns = aglProcessor.targetGrammar!!.allResolvedSkipTerminal.map { Regex(it.unescapedValue.escapedForRegex) }.toSet()
            val f =  FilesCommon.javaFiles("nogit/javaTestFiles.txt", skipPatterns) { it.endsWith(".java") }
            //val f = FilesCommon.files().subList(0, 5)//300) // after this we get java.lang.OutOfMemoryError: Java heap space
            totalFiles = f.size
            println("Number of files to test against: ${f.size}")
            return f
        }

        fun parseWithJava8Agl(file: FileDataCommon, input: String) {
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
    }

    @Test
    fun agl_optm_compilationUnit() = runTest {
        aglProcessor = createAndBuildProcessor("agl/Java8AglOptm.agl")

        ResultsCommon.reset()

        for (file in files()) {
            println("File: ${file.index} of $totalFiles ")
            val input = file.path.readString()
            parseWithJava8Agl(file, input)
        }

        ResultsCommon.write(col)
    }

}