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
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


@RunWith(Parameterized::class)
class test_Java8Agl_BlocksAndStatements(val data: Data) {

    private companion object {

        private val grammarStr = this::class.java.getResource("/Java/version_8/grammars/grammar_aglOptm.agl").readText()

        val processor by lazy {
            Agl.processorFromString(
                grammarStr,
                Agl.configuration(Agl.configurationSimple()) { targetGrammarName(("BlocksAndStatements")); defaultGoalRuleName("Block") },
                aglOptions = Agl.options {
                    semanticAnalysis {
                        context(ContextFromGrammarRegistry(Agl.registry))
                        // switch off ambiguity analysis for performance
                        option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                    }
                }
            ).processor!!
        }

        var sourceFiles = arrayOf(
            "/java8/sentences/blocks-valid.txt"
        )

        @JvmStatic
        @Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> {
            val col = ArrayList<Array<Any>>()
            for (sourceFile in sourceFiles) {
                val inps = test_Java8Agl_BlocksAndStatements::class.java.getResourceAsStream(sourceFile)

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

    @Test(timeout = 5000)
    fun parse() {
        val result = processor.parse(this.data.text)
        assertNotNull(result.sppt)
        assertTrue(result.issues.errors.isEmpty())
        val resultStr = result.sppt!!.asSentence
        assertEquals(this.data.text, resultStr)
        assertEquals(1, result.sppt!!.maxNumHeads)
    }

    @Test(timeout = 5000)
    fun process() {
        val result = processor.process(this.data.text)
        assertNotNull(result.asm)
        assertTrue(result.allIssues.errors.isEmpty())
        val resultStr = result.asm!!.asString("", " ")
        //assertEquals(this.data.text, resultStr)
        assertEquals(1, result.asm?.root?.size)
    }
}
