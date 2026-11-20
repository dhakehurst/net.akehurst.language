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

package net.akehurst.language.m2mTransform.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.contextFromRegistryGrammars
import net.akehurst.language.base.processor.AglBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * the grammarDomain (builder) is what is used, grammarString must say the same (review MT3)
 */
class test_M2mTransformGrammarString {

    @Test
    fun process_grammarString_EQ_grammarModel() {
        val res = Agl.registry.agl.grammar.processor!!.process(
            AglBase.grammarString + "\n" + M2mTransform.grammarString,
            Agl.options {
                semanticAnalysis {
                    sentenceContext(contextFromRegistryGrammars(Agl.registry))
                }
            }
        )
        println("--- builder grammar ---")
        println(M2mTransform.defaultTargetGrammar.asString())
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val actual = res.asm!!.findDefinitionByQualifiedNameOrNull(M2mTransform.defaultTargetGrammar.qualifiedName)!!.asString()
        val expected = M2mTransform.defaultTargetGrammar.asString()
        assertEquals(expected, actual)
    }
}
