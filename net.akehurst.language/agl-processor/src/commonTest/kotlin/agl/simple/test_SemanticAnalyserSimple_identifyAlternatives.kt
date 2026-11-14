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
package net.akehurst.language.agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** 'identify T by a | b': an alternative without a value (e.g. no short name) is normal, it gives no warning */
class test_SemanticAnalyserSimple_identifyAlternatives {

    private companion object {
        val grammarStr = GrammarString(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                unit = declaration* ;
                declaration = primitive | use ;
                primitive = 'primitive' shortName? ID ;
                shortName = '<' ID '>' ;
                use = 'use' ID ;
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            }
        """.trimIndent()
        )
        val crossReferenceStr = CrossReferenceString(
            """
            namespace test.Test
                identify Primitive by id | shortName.id
                references {
                    in Use {
                      property id refers-to Primitive
                    }
                }
        """.trimIndent()
        )
        val processor = Agl.processorFromStringSimple(grammarStr, referenceStr = crossReferenceStr).let {
            assertTrue(it.issues.errors.isEmpty(), it.issues.toString())
            it.processor!!
        }

        fun issuesFor(sentence: String) = processor.process(sentence, Agl.options {
            semanticAnalysis { sentenceContext(contextAsmSimple()) }
        }).allIssues
    }

    @Test
    fun alternative_absent__no_warning() {
        val issues = issuesFor("primitive String use String")
        assertEquals(emptySet(), issues.all)
    }

    @Test
    fun alternative_present__found_by_both_names() {
        val issues = issuesFor("primitive <S> String use String use S")
        assertEquals(emptySet(), issues.all)
    }

    @Test
    fun duplicate_via_alternative__fail() {
        val issues = issuesFor("primitive <S> String primitive S")
        assertEquals(1, issues.errors.size, issues.toString())
        assertTrue(issues.errors[0].message.startsWith("'S' with type 'test.Test.Primitive' already exists in scope"), issues.toString())
    }
}
