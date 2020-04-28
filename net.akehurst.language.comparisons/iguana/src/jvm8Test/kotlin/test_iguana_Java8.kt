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
package net.akehurst.language.comparisons.iguana

import iguana.utils.input.Input
import net.akehurst.language.comparisons.common.Java8TestFiles
import net.akehurst.language.comparisons.common.TimeLogger
import org.iguana.parser.IguanaParser
import org.iguana.sppf.NonterminalNode
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

@RunWith(Parameterized::class)
class test_iguana_Java8(val file:Path) {

    companion object {
        val javaTestFiles = "../javaTestFiles/javac"

        @JvmStatic
        @Parameterized.Parameters()//name = "{index}: {0}")
        fun files(): Collection<Array<Any>> {
            return Java8TestFiles.files
        }

        fun createAndBuildParser(grammarFileStr: String): IguanaParser {
            val inStrm = test_iguana_Java8::class.java.getResourceAsStream(grammarFileStr)
            val grammar = Iggy.getGrammar( inStrm )
            val parser = IguanaParser(grammar)
            return parser
        }


        val specJava8 = createAndBuildParser("/Java8.iggy")
        var input: String? = null

        fun parseWithJava8Spec(file: Path): NonterminalNode? {
            try {
                TimeLogger("iguana_spec", file.toString()).use { timer ->
                    val tree = specJava8.getSPPF(Input.fromString(input))
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
    fun spec_compilationUnit() {
        val tree = parseWithJava8Spec(file)
        Assert.assertNotNull("Failed to Parse", tree)
    }

}