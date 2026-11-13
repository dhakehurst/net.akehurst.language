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
import net.akehurst.language.agl.processor.contextFromGrammarRegistry
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.*

class test_KerML_agl_Singles {

    private companion object {
        private val languagePathStr = "/KerML/v2_2026-09/grammars/agl"
        private val grammarStr = this::class.java.getResource("$languagePathStr/grammar.agl").readText()

        val processor: LanguageProcessor<Asm, SentenceContext> by lazy {
            Agl.processorFromStringSimple(
                grammarDefinitionStr = GrammarString(grammarStr)
            ).processor!!
        }

        fun test_process(sentence: String, context: SentenceContext, expIssues: Set<LanguageIssue>) {
            val result = processor.process(sentence, Agl.options {
                semanticAnalysis {
                    sentenceContext(context)
                }
            })
            // only errors: the processor has no references, so there is a warning 'Empty CrossReferenceDomain'
            assertEquals(expIssues, result.allIssues.errors.toSet(), result.allIssues.toString())
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
        val res = Agl.registry.agl.grammar.processor!!.process(grammarStr, Agl.options { semanticAnalysis { sentenceContext(contextFromGrammarRegistry(Agl.registry)) } })
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
    }


    @Ignore
    @Test
    fun check_grammar() {
        val grammarStr = this::class.java.getResource("$languagePathStr/grammar.agl").readText()
        val res = Agl.registry.agl.grammar.processor!!.process(
            grammarStr,
            Agl.options {
                semanticAnalysis {
                    sentenceContext(contextFromGrammarRegistry(Agl.registry))
                    option(AglGrammarSemanticAnalyser.OPTIONS_KEY_AMBIGUITY_ANALYSIS, true)
                }
            }
        )
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
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

        test_process(sentence, contextAsmSimple(), emptySet())
    }

    @Test
    fun semanticAnalysis_RootNamespace() {
        val sentence = """
            datatype String;
            
            class AClass {			
                feature f: String;
            }
        """.trimIndent()

        test_process(sentence, contextAsmSimple(), emptySet())
    }

    // TODO(David): with KerML_v2_2026_09.processorWithReferences() (references.agl) the duplicate 'datatype String' gives no error,
    //  only warnings from evaluating the references ("Cannot get property 'name' on ${'$'}nothing", "Executing property 'shortName' on 'FeatureName' results in null")
    @Ignore
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
                InputLocation(17, 1, 2, 16, null),
                "(String,net.akehurst.language.kerML.KerMLv1_1.DataType) already exists in scope //", null
            )
        )

        test_process(sentence, contextAsmSimple(), expIssues)
    }

}
