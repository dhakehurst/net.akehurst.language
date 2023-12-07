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

import net.akehurst.language.agl.language.grammar.AglGrammarSemanticAnalyser
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.semanticAnalyser.ContextSimple
import net.akehurst.language.api.asm.Asm
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.comparisons.common.FileData
import net.akehurst.language.comparisons.common.Java8TestFiles
import net.akehurst.language.comparisons.common.Results
import net.akehurst.language.comparisons.common.TimeLogger
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException
import java.nio.file.Files
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class Java8_compare_Test_antlrSpec(val file: FileData) {

    companion object {
        const val col = "agl_antlr_spec"
        var totalFiles = 0

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun files(): Collection<FileData> {
            val f = Java8TestFiles.files.subList(0, 3500) // after this we get java.lang.OutOfMemoryError: Java heap space
            totalFiles = f.size
            println("Number of files to test against: ${f.size}")
            return f
        }

        fun createAndBuildProcessor(aglFile: String): LanguageProcessor<Asm, ContextSimple> {
            val bytes = Java8_compare_Test_antlrSpec::class.java.getResourceAsStream(aglFile).readBytes()
            val javaGrammarStr = String(bytes)
            val res = Agl.processorFromString<Asm, ContextSimple>(
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

        val specJava8Processor = createAndBuildProcessor("/agl/Java8AntlrSpec.agl")

        var input: String? = null

        fun parseWithJava8Spec(file: FileData): SharedPackedParseTree? {
            return try {
                TimeLogger("${col}-fst", file).use { timer ->
                    specJava8Processor.parse(input!!, Agl.parseOptions { goalRuleName("compilationUnit") })
                    timer.success()
                }
                TimeLogger("${col}-snd", file).use { timer ->
                    val res = specJava8Processor.parse(input!!, Agl.parseOptions { goalRuleName("compilationUnit") })
                    timer.success()
                    res.sppt
                }
            } catch (e: ParseFailedException) {
                println("Error: ${e.message}")
                Results.logError("${col}-fst", file)
                assertTrue(file.isError)
                null
            }
        }

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
    }

    @Before
    fun setUp() {
        try {
            input = String(Files.readAllBytes(file.path))
        } catch (e: IOException) {
            e.printStackTrace()
            Assert.fail(e.message)
        }
    }

    @Test
    fun agl_antlr_spec_compilationUnit() {
        print("File: ${file.index} of $totalFiles")
        parseWithJava8Spec(file)
    }

}