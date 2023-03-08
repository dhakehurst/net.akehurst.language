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

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.processor.LanguageProcessor
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.test.assertEquals
import kotlin.test.assertNotNull


@RunWith(Parameterized::class)
class test_SQLValid(val data: Data) {

    companion object {

        private val grammarStr = test_SQLValid::class.java.getResource("/sql/simple-sql.agl").readText()
        val processor by lazy {
            Agl.processorFromStringDefault(grammarStr).processor!!
        }

        var sourceFiles = arrayOf("/sql/valid.txt")

        @JvmStatic
        @Parameters(name = "{0}")
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

    @Test(timeout = 1000)
    fun test() {
        val queryStr = this.data.queryStr
        val goal = "terminated-statement"
        val result = processor.parse(queryStr, Agl.parseOptions { goalRuleName(goal) })
        assertNotNull(result.sppt)
        assertEquals(emptyList(), result.issues)
        val resultStr = result.sppt!!.asString
        assertEquals(queryStr, resultStr)
    }


}
