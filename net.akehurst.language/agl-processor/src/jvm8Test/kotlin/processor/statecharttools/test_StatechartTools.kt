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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.Agl.configurationSimple
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.collections.lazyMap
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


@RunWith(Parameterized::class)
class test_StatechartTools(val data: Data) {

    companion object {

        private val grammarStr= this::class.java.getResource("/Statecharts/version_/grammar.agl")?.readText() ?: error("File not found")
        //private val grammarStr2 = this::class.java.getResource("/statechart-tools/SText.agl")?.readText() ?: error("File not found")
        //private val grammarStr = ""//runBlockingNoSuspensions { resourcesVfs["/xml/Xml.agl"].readString() }

        // must create processor for 'Expressions' so that SText can extend it
        //val exprProcessor = Agl.processorFromStringSimple(GrammarString(grammarStr1)).processor!!
        //var processor = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!
        var sourceFiles = arrayOf("/statechart-tools/samplesValid.txt")

        fun processorFor(targetGrammar:String) =Agl.processorFromStringSimple(
                GrammarString(grammarStr),
                configurationBase = Agl.configuration(configurationSimple()) {
                    targetGrammarName(targetGrammar)
                }
            ).let {
                check(it.issues.errors.isEmpty()) {it.issues.toString()}
            it.processor
            }

        val processor = lazyMap { tgtGram:String ->
            processorFor(tgtGram)
        }

        @JvmStatic
        @Parameters(name = "{0}")
        fun collectData(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()
            var grammarName = "Statechart"
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
                        val str = line.substringAfter("#").trim()
                        grammarName = str.substringBefore("::", "Statechart")
                        ruleName = str.substringAfter("::", str)
                    } else if (line.startsWith("//")) {
                        // comment
                    } else {
                        col.add(arrayOf(Data(file, grammarName, ruleName, line)))
                    }
                }
                return col
            }
            return col
        }
    }

    data class Data(val file: String, val grammarName:String, val ruleName: String, val text: String)

    @Test
    fun parse() {
        val result = processor[data.grammarName]!!.parse(this.data.text, Agl.parseOptions { goalRuleName(data.ruleName) })
        assertNotNull(result.sppt, result.issues.toString())
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        val resultStr = result.sppt!!.asSentence
        assertEquals(this.data.text, resultStr)
    }

    @Test
    fun process() {
        val result = processor[data.grammarName]!!.process(this.data.text, Agl.options { parse { goalRuleName(data.ruleName) } })
        assertNotNull(result.asm, result.allIssues.toString())
        assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())
    }
}
