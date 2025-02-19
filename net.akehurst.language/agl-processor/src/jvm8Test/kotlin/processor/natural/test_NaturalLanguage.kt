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
package net.akehurst.language.agl.processor.natural

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.*
import net.akehurst.language.sentence.common.SentenceDefault
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
class test_NaturalLanguage(val data: Data) {

    companion object {

        private val grammarStr = this::class.java.getResource("/natural/English.agl").readText()
        var processor = processor()

        var sourceFiles = arrayOf("/natural/english-sentences-valid.txt")

        fun processor() = Agl.processorFromStringSimple(GrammarString(grammarStr)).processor!!

        @JvmStatic
        @Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()
            for (sourceFile in sourceFiles) {
                // val inps = ClassLoader.getSystemClassLoader().getResourceAsStream(sourceFile)
                val inps = this::class.java.getResourceAsStream(sourceFile)

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
                        val goal = line.substringBefore("|").trim()
                        val sentence = line.substringAfter("|").trim()
                        col.add(arrayOf(Data(sourceFile, goal, sentence)))
                        line = br.readLine()
                    }
                }
            }
            return col
        }
    }

    class Data(val sourceFile: String, val goal: String, val sentence: String) {

        // --- Object ---
        override fun toString(): String {
            return "$sourceFile : $goal : $sentence"
        }
    }

    @Test
    fun test() {
        val sentence = SentenceDefault(this.data.sentence)
        val scan = processor.scan(sentence.text).tokens
        scan.forEach { l ->
            if (l.name == "undefined") {
                throw RuntimeException("Found unknown words '${sentence.textAt(l.position,l.length)}', at ${sentence.locationFor(l.position, l.length)}")
            }
        }
        val result = processor.parse(this.data.sentence, Agl.parseOptions { goalRuleName(data.goal) })
        assertNotNull(result.sppt, result.issues.toString())
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val resultStr = result.sppt!!.asSentence
        assertEquals(this.data.sentence, resultStr)

    }

}
