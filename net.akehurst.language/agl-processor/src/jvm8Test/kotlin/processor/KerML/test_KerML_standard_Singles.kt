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
package net.akehurst.language.agl.processor.KerML

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import kotlin.test.*

class test_KerML_standard_Singles {

    private companion object {
        private val grammarPathStr = "/KerML/v2_2023-08/grammars/standard/grammar.agl"
        private val grammarStr = this::class.java.getResource(grammarPathStr).readText()
        var processor: LanguageProcessor<Asm, ContextWithScope<Any, Any>> = Agl.processorFromStringSimple(
            grammarDefinitionStr = GrammarString(grammarStr)
        ).processor!!

    }

    @Test
    fun parse_grammar() {
        val grammarStr = this::class.java.getResource(grammarPathStr).readText()
        val res = Agl.registry.agl.grammar.processor!!.parse(grammarStr)
        assertTrue(res.issues.isEmpty(), res.issues.toString())
    }

    @Test
    fun process_grammar() {
        val grammarStr = this::class.java.getResource(grammarPathStr).readText()
        val res = Agl.registry.agl.grammar.processor!!.process(grammarStr, Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } })
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
    }

    @Ignore
    @Test
    fun check_grammar() {
        val grammarStr = this::class.java.getResource(grammarPathStr).readText()
        val res = Agl.registry.agl.grammar.processor!!.process(
            grammarStr,
            Agl.options {
                semanticAnalysis {
                    context(ContextFromGrammarRegistry(Agl.registry))
                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                }
            }
        )
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
    }

    @Test
    fun SINGLE_LINE_NOTE() {
        val sentence = """
          // a note
        """.trimIndent()
        val result = processor.parse(sentence)
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertEquals(sentence, result.sppt!!.asSentence)
    }

    @Test
    fun MULTI_LINE_NOTE() {
        val sentence = """
          //* a note
            * that covers
            * multiple
            * lines
            */
        """.trimIndent()
        val result = processor.parse(sentence)
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertEquals(sentence, result.sppt!!.asSentence)
    }

    @Test
    fun MULTI_LINE_COMMENT() {
        val sentence = """
          /* a comment
           * over multiple
           * lines
           */
        """.trimIndent()
        val result = processor.parse(sentence)
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun package_empty() {
        //val goal = "Package"
        val sentence = """
          package ;
        """.trimIndent()
        val result = processor.parse(sentence)
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

}
