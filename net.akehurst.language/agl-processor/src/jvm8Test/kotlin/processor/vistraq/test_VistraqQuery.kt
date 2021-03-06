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

import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.ArrayList

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.agl.processor.Agl


@RunWith(Parameterized::class)
class test_QueryParserValid(val data:Data) {

    companion object {

        private val grammarStr = test_QueryParserValid::class.java.getResource("/vistraq/Query.agl")?.readText() ?: error("File not found")
        var processor: LanguageProcessor = tgqlprocessor()

        var sourceFiles = arrayOf("/vistraq/sampleValidQueries.txt")

        fun tgqlprocessor() : LanguageProcessor {
            return Agl.processor(grammarStr)
         }

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

    @Test(timeout=1000)
    fun test() {
        val queryStr = this.data.queryStr
        val result = processor.parse("query", queryStr)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(queryStr, resultStr)
    }



}
