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
package net.akehurst.language.agl.processor.sql

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.*
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_SQLValid {

    companion object {

        private val grammarStr = test_SQLValid::class.java.getResource("/sql/version_/grammar.agl").readText()
        val processor by lazy {
            Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
        }

        var sourceFiles = arrayOf("/sql/version_/valid/valid.txt")

        @JvmStatic
        fun data(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()
            for (sourceFile in sourceFiles) {
                val inps = test_SQLValid::class.java.getResourceAsStream(sourceFile)

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

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("data")
    @Timeout(5)
    fun test(data: Data) {
        val queryStr = data.queryStr
        val goal = "terminatedStatement"
        val result = processor.parse(queryStr, ParseOptionsDefault(goalRuleName = goal))
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        val resultStr = result.sppt!!.asSentence
        assertEquals(queryStr, resultStr)
    }


}
