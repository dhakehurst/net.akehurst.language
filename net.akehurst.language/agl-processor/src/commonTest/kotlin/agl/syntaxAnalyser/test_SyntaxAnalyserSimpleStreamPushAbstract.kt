/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.syntaxAnalyser

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.api.syntaxAnalyser.SyntaxAnalyser
import net.akehurst.language.asm.api.AsmPath
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.typemodel.api.DataType
import net.akehurst.language.typemodel.api.PrimitiveType
import net.akehurst.language.typemodel.api.PropertyDeclaration
import net.akehurst.language.typemodel.api.TypeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_SyntaxAnalyserSimpleStreamPushAbstract {

    private companion object {
        class SyntaxAnalyserToString(
            typeModel: TypeModel,
            scopeModel: CrossReferenceModel
        ) : SyntaxAnalyserSimpleStreamPushAbstract<String>(QualifiedName("ns"), typeModel, scopeModel) {

            private val sb = StringBuilder()
            private var indent = ""
            private val eol get() = "\n$indent"
            fun indentInc() {
                indent += "  "
            }

            fun indentDec() {
                indent = indent.substring(1)
            }

            override val embeddedSyntaxAnalyser: Map<QualifiedName, SyntaxAnalyser<String>>
                get() = TODO("not implemented")

            override val asm: String get() = sb.toString()

            override fun startAsm() {
                sb.append(indent)
                sb.append("ASM {$eol")
                indentInc()
            }

            override fun finishAsm() {
                indentDec()
                sb.append("}")
            }

            override fun primitive(type: PrimitiveType, value: String?) {
                sb.append(value)
            }

            override fun startList() {
                sb.append("[")
                indentInc()
            }

            override fun finishList() {
                sb.append("]")
                indentDec()
            }

            override fun startListSeparated() {
                sb.append("[")
            }

            override fun finishListSeparated() {
                sb.append("]")
            }

            override fun listElement() {
                TODO("not implemented")
            }

            override fun startAsmElement(path: AsmPath, type: DataType) {
                sb.append("${type.name} {$eol")
                indentInc()
            }

            override fun finishAsmElement(path: AsmPath, type: DataType) {
                indentDec()
                sb.append(indent)
                sb.append("}$eol")
            }

            override fun startTuple() {
                sb.append("Tuple {$eol")
            }

            override fun finishTuple(path: AsmPath) {
                sb.append("}$eol")
            }

            override fun startProperty(declaration: PropertyDeclaration, isRef: Boolean) {
                sb.append("${declaration.name} = ")
            }

            override fun finishProperty(declaration: PropertyDeclaration, isRef: Boolean) {
                sb.append("")
            }

        }

        class SemanticAnalyserToString() : SemanticAnalyser<String, ContextWithScope<Any, Any>> {
            override fun clear() {}

            //override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> = emptyList()

            override fun analyse(
                sentenceIdentity:Any?,
                asm: String,
                locationMap: LocationMap?,
                options: SemanticAnalysisOptions<ContextWithScope<Any, Any>>
            ): SemanticAnalysisResult {
                return SemanticAnalysisResultDefault(IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS))
            }

        }

        fun processor(grammarStr: String) = Agl.processorFromString<String, ContextWithScope<Any, Any>>(
            grammarDefinitionStr = grammarStr,
            configuration = Agl.configuration {
                //typeModelResolver { p -> ProcessResultDefault(TypeModelFromGrammar.create(p.grammar!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                syntaxAnalyserResolver { p -> ProcessResultDefault(SyntaxAnalyserToString(p.typesModel!!, p.crossReferenceModel!!), IssueHolder(LanguageProcessorPhase.ALL)) }
                semanticAnalyserResolver { p -> ProcessResultDefault(SemanticAnalyserToString(), IssueHolder(LanguageProcessorPhase.ALL)) }
            }
        )

        fun testProc(grammarStr: String): LanguageProcessor<String, ContextWithScope<Any, Any>> {
            val result = processor(grammarStr)
            assertNotNull(result.processor, result.issues.toString())
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
            return result.processor!!
        }

        class TestData(
            val sentence: String,
            val expected: String
        )

        fun test(proc: LanguageProcessor<String, ContextWithScope<Any, Any>>, data: TestData) {
            println("'${data.sentence}'")
            val result = proc.process(data.sentence)
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
            assertNotNull(result.asm)
            val actual = result.asm!!

            assertEquals(data.expected, actual)
        }
    }

    // --- Empty ---
    @Test // S =  ;
    fun _0_empty() {
        val grammarStr = """
            namespace test
            grammar Test {
                S =  ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val sentence = ""
        val expected = """
            ASM {
              S {
              }
            }
        """.trimIndent()

        test(proc, TestData(sentence, expected))
    }

}