/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.agl.processor.vistraq

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


@RunWith(Parameterized::class)
class test_QueryParserValid(val data: Data) {

    private companion object {

        val grammarStr = test_QueryParserValid::class.java.getResource("/vistraq/version_/grammar.agl")?.readText() ?: error("File not found")

        //val grammarStr = test_QueryParserValid::class.java.getResource("/vistraq/Query.agl")?.readText() ?: error("File not found")
        var processor = tgqlprocessor()

        var sourceFiles = arrayOf("/vistraq/version_/valid/examples.txt")

        fun tgqlprocessor() = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!

        @JvmStatic
        @Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()
            for (sourceFile in sourceFiles) {
                // val inps = ClassLoader.getSystemClassLoader().getResourceAsStream(sourceFile)
                val inps = test_QueryParserValid::class.java.getResourceAsStream(sourceFile) ?: error("File not found")

                val br = BufferedReader(InputStreamReader(inps))
                var line: String? = br.readLine()
                while (null != line) {
                    line = line.trim { it <= ' ' }
                    if (line.isEmpty()) {
                        // blank line
                        line = br.readLine()
                    } else if (line.startsWith("//")) {
                        // comment
                        line = br.readLine()
                    } else {
                        col.add(arrayOf(Data(sourceFile, line)))
                        line = br.readLine()
                    }
                }
            }
            return col
        }
    }

    class Data(val sourceFile: String, val queryStr: String) {

        // --- Object ---
        override fun toString(): String {
            return this.sourceFile + ": " + this.queryStr
        }
    }

    @Test(timeout = 2000)
    fun parse() {
        val queryStr = this.data.queryStr
        val goal = "query"
        val result = processor.parse(queryStr, ParseOptionsDefault(goal))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        val resultStr = result.sppt!!.asSentence
        Assert.assertEquals(queryStr, resultStr)
    }

    @Test(timeout = 2000)
    fun process() {
        val queryStr = this.data.queryStr
        val goal = "query"
        val result = processor.process(queryStr, Agl.options { parse { goalRuleName(goal) } })
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.asm)
    }
}
