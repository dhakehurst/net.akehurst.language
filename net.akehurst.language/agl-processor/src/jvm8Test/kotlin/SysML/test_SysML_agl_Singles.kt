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
import net.akehurst.language.agl.language.grammar.AglGrammarSemanticAnalyser
import net.akehurst.language.agl.language.grammar.ContextFromGrammarRegistry
import net.akehurst.language.agl.default.ContextAsmDefault
import net.akehurst.language.api.asm.Asm
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.LanguageProcessorPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_SysML_agl_Singles {

    private companion object {
        private val languagePathStr = "/SysML/v2_2023-11/grammars/standard" //TODO: agl"
        private val grammarStr = this::class.java.getResource("$languagePathStr/grammar.agl").readText()
        private val crossReferenceModelStr = this::class.java.getResource("$languagePathStr/references.agl").readText()

        val processor: LanguageProcessor<Asm, ContextAsmDefault> by lazy {
            val res = Agl.processorFromStringDefault(
                grammarDefinitionStr = GrammarString(grammarStr),
                crossReferenceModelStr = CrossReferenceString(crossReferenceModelStr)
            )
            res.processor!!
        }

        fun test_parse(sentence: String, expIssues: Set<LanguageIssue> = emptySet()) {
            val result = processor.parse(sentence)
            assertEquals(expIssues, result.issues.all, result.issues.toString())
        }

        fun test_process(sentence: String, context: ContextAsmDefault, expIssues: Set<LanguageIssue>) {
            val result = processor.process(sentence, Agl.options {
                semanticAnalysis {
                    context(context)
                }
            })
            assertEquals(expIssues, result.issues.all, result.issues.toString())
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
    fun parse_error_at_0() {
        val sentence = """
          )
        """.trimIndent()

        val expIssues = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.PARSE,
                InputLocation(0, 1, 1, 1),
                "^)", null
            )
        )

        test_parse(sentence, expIssues)
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
            part def String;
            
            part def AClass {			
                f: String;
            }
        """.trimIndent()

        test_process(sentence, ContextAsmDefault(), emptySet())
    }

    @Test
    fun semanticAnalysis_RootNamespace_name_clash() {
        val sentence = """
            part def String;
            part def String;
            
            part def  AClass {			
                f: String;
            }
        """.trimIndent()

        val expIssues = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(17, 1, 2, 16),
                "(String,net.akehurst.language.sysML.SysMLv2_0.PartDefinition) already exists in scope", null
            )
        )

        test_process(sentence, ContextAsmDefault(), expIssues)
    }

}
