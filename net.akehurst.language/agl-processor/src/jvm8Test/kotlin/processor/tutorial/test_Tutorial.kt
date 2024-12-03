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
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.agl.TransformString
import net.akehurst.language.agl.TypeModelString
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.api.processor.LanguageProcessor
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class test_Tutorial {

    data class TestData(
        val sentence: String,
        val asm: Asm
    )

    companion object {
        fun test(testData: List<TestData>, proc: LanguageProcessor<Asm, ContextAsmSimple>) {
            for (td in testData) {
                println(td.sentence)
                val res = proc.process(td.sentence)
                assertTrue(res.issues.isEmpty(), res.issues.toString())
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
            typeStr = TypeModelString(typeModelStr),
            transformStr = TransformString(transformStr)
        )
    }

}