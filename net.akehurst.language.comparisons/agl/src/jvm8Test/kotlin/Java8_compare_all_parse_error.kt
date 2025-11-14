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
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.comparisons.common.FileData
import net.akehurst.language.comparisons.common.Java8TestFiles
import net.akehurst.language.sppt.api.SharedPackedParseTree
import org.junit.*
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException
import java.nio.file.Files
import kotlin.test.assertTrue
import kotlin.time.*

@RunWith(Parameterized::class)
class Java8_compare_all_parse_error(val file: FileData) {

    companion object {
        val javaTestFiles = "../javaTestFiles/javac"

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun files(): Collection<FileData> {
            val f = Java8TestFiles.files
            println("Number of files to test against: ${f.size}")
            return f
        }

        fun createAndBuildProcessor(aglFile: String, goalRuleName: String): LanguageProcessor<Asm,ContextWithScope<Any,Any>> {
            val bytes = Java8_compare_all_parse_error::class.java.getResourceAsStream(aglFile).readBytes()
            val javaGrammarStr = String(bytes)
            val res = Agl.processorFromStringSimple(GrammarString(javaGrammarStr))
            res.processor!!.buildFor(Agl.parseOptions { goalRuleName(goalRuleName) })
            return res.processor!!
        }

        val aglSpec = createAndBuildProcessor("/agl/Java8AglSpec.agl", "CompilationUnit")
        val aglOptm = createAndBuildProcessor("/agl/Java8AglOptm.agl", "CompilationUnit")
        val antlrSpec = createAndBuildProcessor("/agl/Java8AntlrSpec.agl", "compilationUnit")
        val antlrOptm = createAndBuildProcessor("/agl/Java8AntlrOptm.agl", "compilationUnit")

        var input: String? = null

        @ExperimentalTime
        fun parse(file: FileData, proc: LanguageProcessor<Asm,ContextWithScope<Any,Any>>, goalRuleName: String): TimedValue<SharedPackedParseTree?> {
            return TimeSource.Monotonic.measureTimedValue {
                try {
                    val res = proc.parse(input!!,Agl.parseOptions { goalRuleName(goalRuleName) })
                    res.sppt
                } catch (t: Throwable) {
                    null
                }
            }
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

    @Ignore
    @ExperimentalTime
    //@Test
    fun compare() {
        val t1 = parse(file, aglSpec, "CompilationUnit")
        val t2 = parse(file, aglOptm, "CompilationUnit")
        val t3 = parse(file, antlrSpec, "compilationUnit")
        val t4 = parse(file, antlrOptm, "compilationUnit")

        val all = listOf(t1,t2,t3,t4)
        val same = all.all { null==it.value } || all.all{ null!=it.value }
        if (!same) {
            println("aglSpec = ${ if (null==t1.value) "fail" else "pass" }")
            println("aglOptm = ${ if (null==t2.value) "fail" else "pass" }")
            println("antlrSpec = ${ if (null==t3.value) "fail" else "pass" }")
            println("antlrOptm = ${ if (null==t4.value) "fail" else "pass" }")
        }
        assertTrue(same)

    }


}