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
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser
import org.junit.Test
import kotlin.test.assertNotNull

class test_Java8_Grammars {

    @Test(timeout = 5000)
    fun aglSpec() {
        val grammarStr = this::class.java.getResource("/Java/version_8/grammars/grammar_aglSpec.agl").readText()
        val result = Agl.registry.agl.grammar.processor!!.process(
            sentence = grammarStr,
            options = Agl.options {
                semanticAnalysis {
                    context(contextFromGrammarRegistry(Agl.registry))
                    // switch off ambiguity analysis for performance
                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                }
            }
        )
        assertNotNull(result.asm)
        result.allIssues.forEach { println(it) }
    }

    @Test(timeout = 5000)
    fun aglOptm() {
        val grammarStr = this::class.java.getResource("/Java/version_8/grammars/grammar_aglOptm.agl").readText()
        val result = Agl.registry.agl.grammar.processor!!.process(
            sentence = grammarStr,
            options = Agl.options {
                semanticAnalysis {
                    context(contextFromGrammarRegistry(Agl.registry))
                    // switch off ambiguity analysis for performance
                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                }
            }
        )
        assertNotNull(result.asm)
        result.allIssues.forEach { println(it) }
    }

    @Test(timeout = 5000)
    fun antrlSpec() {
        //val grammarStr = this::class.java.getResource("/java8/Java8_all.agl").readText()
        val grammarStr = this::class.java.getResource("/Java/version_8/grammars/grammar_antlrSpec.agl").readText()
        val result = Agl.registry.agl.grammar.processor!!.process(
            sentence = grammarStr,
            options = Agl.options {
                semanticAnalysis {
                    context(contextFromGrammarRegistry(Agl.registry))
                    // switch off ambiguity analysis for performance
                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                }
            }
        )
        assertNotNull(result.asm)
        result.allIssues.forEach { println(it) }
    }


    @Test(timeout = 5000)
    fun antlrOptm() {
        val grammarStr = this::class.java.getResource("/Java/version_8/grammars/grammar_antlrOptm.agl").readText()
        val result = Agl.registry.agl.grammar.processor!!.process(
            sentence = grammarStr,
            options = Agl.options {
                semanticAnalysis {
                    context(contextFromGrammarRegistry(Agl.registry))
                    // switch off ambiguity analysis for performance
                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, false)
                }
            }
        )
        assertNotNull(result.asm)
        result.allIssues.forEach { println(it) }
    }

}