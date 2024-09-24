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
package net.akehurst.language.agl.processor.SysML

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.agl.default_.ContextAsmDefault
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.parser.leftcorner.ParseOptionsDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_SysML_UseCase_Singles {

    private companion object {

        private val grammarStr = this::class.java.getResource("/SysML/v2_2023-08/grammar.agl").readText()
        var processor: LanguageProcessor<Asm, ContextAsmDefault> = Agl.processorFromStringDefault(GrammarString(grammarStr)).processor!!

    }

    @Test
    fun parse_grammar() {
        val grammarStr = this::class.java.getResource("/SysML/v2_2023-08/grammar.agl").readText()
        val res = Agl.registry.agl.grammar.processor!!.parse(grammarStr)
        assertTrue(res.issues.isEmpty(), res.issues.toString())
    }

    @Test
    fun process_grammar() {
        val grammarStr = this::class.java.getResource("/SysML/v2_2023-08/grammar.agl").readText()
        val res = Agl.registry.agl.grammar.processor!!.process(grammarStr, Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } })
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
    }

    @Test
    fun SINGLE_LINE_COMMENT() {
        val goal = "graph"
        val sentence = """
          // a comment
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goal))
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        assertEquals(sentence, result.sppt!!.asSentence)
    }

    @Test
    fun MULTI_LINE_COMMENT() {
        val goal = "graph"
        val sentence = """
          /* a comment */
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goal))
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun package_empty() {
        val goal = "Package"
        val sentence = """
          package ;
        """.trimIndent()
        val result = processor.parse(sentence, ParseOptionsDefault(goal))
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

}
