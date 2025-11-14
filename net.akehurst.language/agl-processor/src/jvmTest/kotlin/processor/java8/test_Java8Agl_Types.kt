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

package net.akehurst.language.agl.processor.java8

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.contextFromGrammarRegistry
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser
import net.akehurst.language.processor.test.utils.notWidth
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class test_Java8Agl_Types {

    private companion object {

        private val grammarStr = this::class.java.getResource("/Java/version_8/grammars/grammar_aglOptm.agl").readText()

        val processor: LanguageProcessor<Any, Any> by lazy {
            Agl.processorFromString(
                grammarStr,
                Agl.configuration { targetGrammarName(("Types")); defaultGoalRuleName("TypeReference") },
                aglOptions = Agl.options {
                    semanticAnalysis {
                        context(contextFromGrammarRegistry(Agl.registry))
                        // switch off ambiguity analysis for performance
                        option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                    }
                }
            ).processor!!
        }

        var sourceFiles = arrayOf(
            "/java8/sentences/types-valid.txt"
        )

        @JvmStatic
        fun data(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()
            for (sourceFile in sourceFiles) {
                val inps = test_Java8Agl_Types::class.java.getResourceAsStream(sourceFile)

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

    class Data(val file: String, val text: String) {

        // --- Object ---
        override fun toString(): String {
            return "${this.file} | ${this.text}"
        }
    }

    @ParameterizedTest(name = "{index} {0}")
    @MethodSource("data")
    fun test(data:Data) {
        val result = processor.parse(data.text)
        assertTrue(result.issues.notWidth.isEmpty(), result.issues.joinToString("\n") { it.toString() })
        assertNotNull(result.sppt)
        val resultStr = result.sppt!!.asSentence
        assertEquals(data.text, resultStr)
        assertTrue(2 >= result.sppt!!.maxNumHeads, "number of heads = ${result.sppt!!.maxNumHeads}")
    }

}
