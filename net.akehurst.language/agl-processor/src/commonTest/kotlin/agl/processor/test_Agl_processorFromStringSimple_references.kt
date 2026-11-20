/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.issues.api.LanguageProcessorPhase
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Parse issues of the references string are reported in the result of processorFromStringSimple
 */
class test_Agl_processorFromStringSimple_references {

    private val grammarStr = """
        namespace test
        grammar Test {
            S = 'a' ;
        }
    """.trimIndent()

    @Test
    fun references_parse_error_is_reported() {
        val res = Agl.processorFromStringSimple(
            grammarDefinitionStr = GrammarString(grammarStr),
            referenceStr = CrossReferenceString(
                """
                namespace test.Test
                    scope S $ { }
                """.trimIndent()
            )
        )
        assertTrue(res.issues.errors.any { it.phase == LanguageProcessorPhase.PARSE }, res.issues.toString())
    }

    @Test
    fun references_ok_no_issues() {
        val res = Agl.processorFromStringSimple(
            grammarDefinitionStr = GrammarString(grammarStr),
            referenceStr = CrossReferenceString(
                """
                namespace test.Test
                    scope S { }
                """.trimIndent()
            )
        )
        assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
    }
}
