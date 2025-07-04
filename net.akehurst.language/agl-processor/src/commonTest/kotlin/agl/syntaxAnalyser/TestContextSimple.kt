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

package net.akehurst.language.agl.semanticAnalyser

import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.scope.asm.ScopeSimple
import kotlin.test.assertEquals

object TestContextSimple {

    fun assertMatches(expected: ContextWithScope<Any,Any>, actual: ContextWithScope<Any,Any>) {
        assertEquals(expected.scopeForSentence.size, actual.scopeForSentence.size)
        assertEquals(expected.scopeForSentence.keys, actual.scopeForSentence.keys)
        for (k in expected.scopeForSentence.keys) {
            assertMatches(expected.scopeForSentence[k]!!, actual.scopeForSentence[k]!!)
        }
    }

    fun assertMatches(expected: ScopeSimple<Any>, actual: ScopeSimple<Any>) {
        assertEquals(expected.scopeIdentity, actual.scopeIdentity)
        assertEquals(expected.items, actual.items)
        assertMatches(expected.childScopes, actual.childScopes)
    }


    fun assertMatches(expected: Map<String, ScopeSimple<Any>>, actual: Map<String, ScopeSimple<Any>>) {
        assertEquals(expected.keys, actual.keys)
        for (k in expected.keys) {
            assertMatches(expected[k]!!, actual[k]!!)
        }
    }


}