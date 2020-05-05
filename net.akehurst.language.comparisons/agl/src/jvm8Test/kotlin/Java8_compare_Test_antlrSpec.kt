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
import net.akehurst.language.comparisons.common.TimeLogger
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

@RunWith(Parameterized::class)
class Java8_compare_Test_antlrSpec(val file: FileData) {

    companion object {
        val javaTestFiles = "../javaTestFiles/javac"

        @JvmStatic
        @Parameterized.Parameters(name = "{index}: {0}")
        fun files(): Collection<FileData> {
            val f = Java8TestFiles.files
            println("Number of files to test against: ${f.size}")
            return f
        }

        fun createAndBuildProcessor(aglFile: String): LanguageProcessor {
            val bytes = Java8_compare_Test_antlrSpec::class.java.getResourceAsStream(aglFile).readBytes()
            val javaGrammarStr = String(bytes)
            val proc = Agl.processor(javaGrammarStr)
            proc.build()
            return proc
        }

        val specJava8Processor = createAndBuildProcessor("/agl/Java8Spec.agl")

        var input: String? = null

        fun parseWithJava8Spec(file: FileData): SharedPackedParseTree? {
            val tree = specJava8Processor.parse("compilationUnit", input!!)
            TimeLogger("agl_spec", file).use { timer ->
                val tree = specJava8Processor.parse("compilationUnit", input!!)
                timer.success()
                return tree
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

    @Test
    fun spec_compilationUnit() {
        val tree = parseWithJava8Spec(file)
        Assert.assertNotNull("Failed to Parse", tree)
    }

}