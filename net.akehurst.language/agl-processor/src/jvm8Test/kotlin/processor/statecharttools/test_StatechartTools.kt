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
package net.akehurst.language.agl.processor.statecharttools

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.processor.LanguageProcessor
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*


@RunWith(Parameterized::class)
class test_StatechartTools(val data: Data) {

    companion object {

        private val grammarStr1 = this::class.java.getResource("/statechart-tools/Expressions.agl")?.readText() ?: error("File not found")
        private val grammarStr2 = this::class.java.getResource("/statechart-tools/SText.agl")?.readText() ?: error("File not found")
        //private val grammarStr = ""//runBlockingNoSuspensions { resourcesVfs["/xml/Xml.agl"].readString() }

        // must create processor for 'Expressions' so that SText can extend it
        val exprProcessor = Agl.processorFromString(grammarStr1)
        var processor: LanguageProcessor = Agl.processorFromString(grammarStr2)
        var sourceFiles = arrayOf("/statechart-tools/samplesValid.txt")

        @JvmStatic
        @Parameters(name = "{0}")
        fun collectData(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()
            var ruleName = ""
            for (file in sourceFiles) {
                val inps = this::class.java.getResourceAsStream(file) ?: error("File not found")
                val lines = BufferedReader(InputStreamReader(inps)).lines()
                lines.forEach { it ->
                    val line = it.trim { it <= ' ' }
                    if (line.isEmpty()) {
                        // blank line
                    } else if (line.startsWith("//#")) {
                        // change goal rule
                        ruleName = line.substringAfter("#").trim()
                    } else if (line.startsWith("//")) {
                        // comment
                    } else {
                        col.add(arrayOf(Data(file, ruleName, line)))
                    }
                }
                return col
            }
            return col
        }
    }

    data class Data(val file: String, val ruleName: String, val text: String)

    @Test
    fun test() {
        val result = processor.parseForGoal(this.data.ruleName, this.data.text)
        Assert.assertNotNull(result)
        val resultStr = result.asString
        Assert.assertEquals(this.data.text, resultStr)
    }

}
