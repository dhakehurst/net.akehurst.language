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

import net.akehurst.language.agl.processor.Agl
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
import kotlin.test.assertTrue

@RunWith(Parameterized::class)
class Java8_compare_Test_aglOptm(val file: FileData) {

    companion object {
        const val col = "agl_optm"

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun files(): Collection<FileData> {
            val f = Java8TestFiles.files
            println("Number of files to test against: ${f.size}")
            return f
        }

        fun createAndBuildProcessor(aglFile: String): LanguageProcessor {
            val bytes = Java8_compare_Test_aglOptm::class.java.getResourceAsStream(aglFile).readBytes()
            val javaGrammarStr = String(bytes)
            val proc = Agl.processor(javaGrammarStr)
            proc.buildFor("CompilationUnit")
            return proc
        }

        val aglProcessor = createAndBuildProcessor("/agl/Java8AglOptm.agl")

        var input: String? = null

        fun parseWithJava8Agl(file: FileData): SharedPackedParseTree? {
            return try {
                aglProcessor.parse("CompilationUnit", input!!)
                TimeLogger(col, file).use { timer ->
                    val tree = aglProcessor.parse("CompilationUnit", input!!)
                    timer.success()
                    tree
                }
            } catch (e: ParseFailedException) {
                Results.logError(col, file)
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
            input = file.path.toFile().readText()
        } catch (e: IOException) {
            e.printStackTrace()
            Assert.fail(e.message)
        }
    }

    @Test
    fun agl_optm_compilationUnit() {
        parseWithJava8Agl(file)
    }

}