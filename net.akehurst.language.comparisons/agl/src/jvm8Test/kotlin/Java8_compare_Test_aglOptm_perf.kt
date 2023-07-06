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

import net.akehurst.language.agl.grammar.grammar.AglGrammarSemanticAnalyser
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.comparisons.common.FileData
import net.akehurst.language.comparisons.common.Java8TestFiles
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException
import java.nio.file.Files
import kotlin.time.*

@RunWith(Parameterized::class)
class Java8_compare_Test_aglOptm_perf(val file: FileData) {

    companion object {
        val javaTestFiles = "../javaTestFiles/javac"

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun files(): Collection<FileData> {
            val f = Java8TestFiles.files
            println("Number of files to test against: ${f.size}")
            return f
        }

        fun createAndBuildProcessor(aglFile: String): LanguageProcessor<AsmSimple,ContextSimple> {
            val bytes = Java8_compare_Test_aglOptm_perf::class.java.getResourceAsStream(aglFile).readBytes()
            val javaGrammarStr = String(bytes)
            val res = Agl.processorFromString<AsmSimple, ContextSimple>(
                grammarDefinitionStr = javaGrammarStr,
                aglOptions = Agl.options {
                    semanticAnalysis {
                        // switch off ambiguity analysis for performance
                        option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                    }
                }
            )
            //proc.buildFor("CompilationUnit")
            return res.processor!!
        }

        val agl1 = createAndBuildProcessor("/agl/Java8AglSpec.agl")
        val agl2 = createAndBuildProcessor("/agl/Java8AglOptm1.agl")

        var input: String? = null

        @ExperimentalTime
        fun parse(file: FileData, proc:LanguageProcessor<AsmSimple,ContextSimple>): TimedValue<SharedPackedParseTree> {
            val tree = proc.parse( input!!,Agl.parseOptions { goalRuleName("CompilationUnit") })
            return TimeSource.Monotonic.measureTimedValue {
                val res = proc.parse( input!!,Agl.parseOptions { goalRuleName("CompilationUnit") })
                res.sppt!!
            }
        }
/*
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
 */
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

    @Ignore
    @ExperimentalTime
    //@Test
    fun agl_compilationUnit() {
        val t1 = parse(file, agl1)
        val t2 = parse(file, agl2)
        Assert.assertTrue("${t1.value.maxNumHeads} >= ${t2.value.maxNumHeads}", t1.value.maxNumHeads >= t2.value.maxNumHeads)
        //Assert.assertTrue("${t1.duration} >= ${t2.duration} ?", t1.duration >= t2.duration)
    }


}