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
import net.akehurst.language.agl.CrossReferenceString
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.agl.default_.ContextAsmDefault
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.issues.api.LanguageProcessorPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_KerML_agl_Singles {

    private companion object {
        private val languagePathStr = "/KerML/v2_2023-08/grammars/agl"
        private val grammarStr = this::class.java.getResource("$languagePathStr/grammar.agl").readText()
        private val crossReferenceModelStr = this::class.java.getResource("$languagePathStr/references.agl").readText()

        val processor: LanguageProcessor<Asm, ContextAsmDefault> by lazy {
            Agl.processorFromStringDefault(
                grammarDefinitionStr = GrammarString(grammarStr),
                crossReferenceModelStr = CrossReferenceString(crossReferenceModelStr)
            ).processor!!
        }

        fun test_process(sentence: String, context: ContextAsmDefault, expIssues: Set<LanguageIssue>) {
            val result = processor.process(sentence, Agl.options {
                semanticAnalysis {
                    context(context)
                }
            })
            assertEquals(result.issues.all, expIssues, result.issues.toString())
        }
    }

    @Test
    fun parse_grammar() {
        val grammarStr = this::class.java.getResource("$languagePathStr/grammar.agl").readText()
        val res = Agl.registry.agl.grammar.processor!!.parse(grammarStr)
        assertTrue(res.issues.isEmpty(), res.issues.toString())
    }

    @Test
    fun process_grammar() {
        val grammarStr = this::class.java.getResource("$languagePathStr/grammar.agl").readText()
        val res = Agl.registry.agl.grammar.processor!!.process(grammarStr, Agl.options { semanticAnalysis { context(ContextFromGrammarRegistry(Agl.registry)) } })
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
    }

    @Test
    fun check_grammar() {
        val grammarStr = this::class.java.getResource("$languagePathStr/grammar.agl").readText()
        val res = Agl.registry.agl.grammar.processor!!.process(
            grammarStr,
            Agl.options {
                semanticAnalysis {
                    context(ContextFromGrammarRegistry(Agl.registry))
                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                }
            }
        )
        assertTrue(res.issues.isEmpty(), res.issues.toString())
    }

    @Test
    fun parse_SINGLE_LINE_NOTE() {
        val sentence = """
          // a note
        """.trimIndent()
        val result = processor.parse(sentence)
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        assertEquals(sentence, result.sppt!!.asSentence)
    }

    @Test
    fun parse_MULTI_LINE_NOTE() {
        val sentence = """
          //* a note
            * that covers
            * multiple
            * lines
            */
        """.trimIndent()
        val result = processor.parse(sentence)
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        assertEquals(sentence, result.sppt!!.asSentence)
    }

    @Test
    fun parse_MULTI_LINE_COMMENT() {
        val sentence = """
          /* a comment
           * over multiple
           * lines
           */
        """.trimIndent()
        val result = processor.parse(sentence)
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun parse_package_empty() {
        //val goal = "Package"
        val sentence = """
          package ;
        """.trimIndent()
        val result = processor.parse(sentence)
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun parse_package_body_empty() {
        //val goal = "Package"
        val sentence = """
          package Pkg {
          }
        """.trimIndent()
        val result = processor.parse(sentence)
        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
    }

    @Test
    fun semanticAnalysis_RootNamespace_empty() {
        val sentence = """
        """.trimIndent()

        test_process(sentence, ContextAsmDefault(), emptySet())
    }

    @Test
    fun semanticAnalysis_RootNamespace() {
        val sentence = """
            datatype String;
            
            class AClass {			
                feature f: String;
            }
        """.trimIndent()

        test_process(sentence, ContextAsmDefault(), emptySet())
    }

    @Test
    fun semanticAnalysis_RootNamespace_name_clash() {
        val sentence = """
            datatype String;
            datatype String;
            
            class AClass {			
                feature f: String;
            }
        """.trimIndent()

        val expIssues = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(17, 1, 2, 16),
                "(String,com.itemis.sysml.kerml.cst.KerML.DataType) already exists in scope //", null
            )
        )

        test_process(sentence, ContextAsmDefault(), expIssues)
    }

}
