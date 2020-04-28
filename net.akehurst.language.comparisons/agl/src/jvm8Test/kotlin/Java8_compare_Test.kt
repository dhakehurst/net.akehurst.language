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
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.sppt.SharedPackedParseTree
import net.akehurst.language.comparisons.common.Java8TestFiles
import net.akehurst.language.comparisons.common.TimeLogger
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.ArrayList

@RunWith(Parameterized::class)
class Java8_compare_Test(val file: Path) {

    companion object {
        val javaTestFiles = "../javaTestFiles/javac"

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun files(): Collection<Array<Any>> {
            return Java8TestFiles.files
        }

        fun createAndBuildProcessor(aglFile: String): LanguageProcessor {
            val bytes = Java8_compare_Test::class.java.getResourceAsStream(aglFile).readBytes()
            val javaGrammarStr = String(bytes)
            val proc = Agl.processor(javaGrammarStr)
            proc.build()
            return proc
        }

        val specJava8Processor = createAndBuildProcessor("/agl/Java8Spec.agl")
        val optmAntlrJava8Processor = createAndBuildProcessor("/agl/Java8OptmAntlr.agl")
        val optm1Java8Processor = createAndBuildProcessor("/agl/Java8Optm1.agl")

        var input: String? = null

        fun parseWithJava8Spec(file: Path): SharedPackedParseTree? {
            try {
                TimeLogger("agl_spec", file.toString()).use { timer ->
                    val tree = specJava8Processor.parse("compilationUnit", input!!)
                    timer.success()
                    return tree
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        fun parseWithJava8OptmAntlr(file: Path): SharedPackedParseTree? {
            try {
                TimeLogger("agl_optmAntlr", file.toString()).use { timer ->
                    val tree = optmAntlrJava8Processor.parse("compilationUnit", input!!)
                    timer.success()
                    return tree
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        fun parseWithJava8Optm1(file: Path): SharedPackedParseTree? {
            try {
                TimeLogger("agl_optm1", file.toString()).use { timer ->
                    val tree = optm1Java8Processor.parse("compilationUnit", input!!)
                    timer.success()
                    return tree
                }
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
    }


    @Before
    fun setUp() {
        try {
            input = String(Files.readAllBytes(file))
        } catch (e: IOException) {
            e.printStackTrace()
            Assert.fail(e.message)
        }
    }

    @Test
    fun ogl_spec_compilationUnit() {
        val tree = parseWithJava8Spec(file)
        Assert.assertNotNull("Failed to Parse", tree)
    }

    @Test
    fun ogl_optmAntlr_compilationUnit() {
        val tree = parseWithJava8OptmAntlr(file)
        Assert.assertNotNull("Failed to Parse", tree)
    }

    @Test
    fun ogl_optm1_compilationUnit() {
        val tree = parseWithJava8Optm1(file)
        Assert.assertNotNull("Failed to Parse", tree)
    }


}