/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.test.processor.tutorial

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.api.processor.AsmTransformString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.api.processor.TypesString
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.builder.asmSimple
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_Tutorial {

    data class TestData(
        val sentence: String,
        val asm: Asm
    )

    companion object {
        fun test(testData: List<TestData>, proc: LanguageProcessor<Asm, ContextWithScope<Any, Any>>) {
            for (td in testData) {
                println(td.sentence)
                val res = proc.process(td.sentence)
                assertTrue(res.allIssues.isEmpty(), res.allIssues.toString())
                assertEquals(td.asm.asString(), res.asm!!.asString())
            }
        }
    }

    @Test
    fun _00_defaults_from_grammar() {
        val grammarDefinitionStr = """
        """

        val testData = listOf(
            TestData(
                "",
                asmSimple { }
            )
        )

        val proc = Agl.processorFromStringSimple(
            grammarDefinitionStr = GrammarString(grammarDefinitionStr)
        )
        assertTrue(proc.issues.isEmpty(), proc.issues.toString())
        test(testData, proc.processor!!)
    }

    @Test
    fun _10_defineTypeModel_and_transform() {
        val grammarDefinitionStr = """
        """.trimIndent()

        val typeModelStr = """
        """.trimIndent()

        val transformStr = """
        """.trimIndent()

        val proc = Agl.processorFromStringSimple(
            grammarDefinitionStr = GrammarString(grammarDefinitionStr),
            typeStr = TypesString(typeModelStr),
            transformStr = AsmTransformString(transformStr)
        )
    }

}