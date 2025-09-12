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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.contextFromGrammarRegistry
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.comparisons.common.FileData
import net.akehurst.language.comparisons.common.Java8TestFiles
import net.akehurst.language.comparisons.common.Results
import net.akehurst.language.comparisons.common.TimeLogger
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser
import net.akehurst.language.sppt.api.SharedPackedParseTree
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class Java8_compare_Test_aglOptm(val file: FileData) {

    companion object {
        const val col = "agl_optm"
        var totalFiles = 0

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun files(): Collection<FileData> {
            val f = Java8TestFiles.files.subList(0, 5300) // after this we get java.lang.OutOfMemoryError: Java heap space
            totalFiles = f.size
            println("Number of files to test against: ${f.size}")
            return f
        }

        fun createProcessor(aglFile: String): LanguageProcessor<Asm,ContextWithScope<Any,Any>> {
            val bytes = Resources::class.java.getResourceAsStream(aglFile)?.readBytes() ?: error("Cannot find resource: $aglFile")
            val javaGrammarStr = String(bytes)
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
            // no need to build because, sentence is parsed twice in the test - pre-full-build maybe gives faster parse!
            val proc = res.let {
                check(it.issues.errors.isEmpty()) {it.issues.toString()}
                it.processor!!
            }
            return proc//.buildFor(Agl.parseOptions { goalRuleName("CompilationUnit") })
        }

        var input: String? = null

        @BeforeClass
        @JvmStatic
        fun init() {
            Results.reset()
        }

        @AfterClass
        @JvmStatic
        fun end() {
            Results.write()
        }


        fun parseWithJava8Agl(file: FileData): SharedPackedParseTree? {
            return try {
//                var aglProcessor = createProcessor("/agl/Java8AglOptm.agl")
//                //warm up JVM
//                TimeLogger("${col}-1", file).use { timer ->
//                    aglProcessor.parse(input!!, Agl.parseOptions { goalRuleName("CompilationUnit") })
//                    timer.success()
//                }
//                TimeLogger("${col}-2", file).use { timer ->
//                    val res = aglProcessor.parse( input!!,Agl.parseOptions { goalRuleName("CompilationUnit") })
//                    timer.success()
//                    res.sppt
//                }

                // do test
                val aglProcessor = createProcessor("/agl/Java8AglOptm.agl")
                TimeLogger("${col}-3", file).use { timer ->
                    aglProcessor.parse(input!!, Agl.parseOptions { goalRuleName("CompilationUnit") })
                    timer.success()
                }
                TimeLogger("${col}-4", file).use { timer ->
                    val res = aglProcessor.parse( input!!,Agl.parseOptions { goalRuleName("CompilationUnit") })
                    timer.success()
                    res.sppt
                }
            } catch (e: Throwable) {
                println("Error: ${e.message}")
                Results.logError("${col}-fst", file)
                assertTrue(file.isError)
                null
            }
        }
    }

    @Before
    fun setUp() {
        try {
            input = file.path.toFile().readText()
        } catch (e: IOException) {
            e.printStackTrace()
            Assert.fail(e.message)
        }
    }

    @Test
    fun agl_optm_compilationUnit() {
        print("File: ${file.index} of $totalFiles ")
        parseWithJava8Agl(file)
    }

}