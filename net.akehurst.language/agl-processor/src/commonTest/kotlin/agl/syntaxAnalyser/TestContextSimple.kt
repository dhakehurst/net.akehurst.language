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

import net.akehurst.language.api.asm.AsmElementPath
import kotlin.test.assertEquals

object TestContextSimple {

    fun assertMatches(expected: ContextSimple, actual: ContextSimple) {
        assertMatches(expected.rootScope, actual.rootScope)
    }

    fun assertMatches(expected: ScopeSimple<AsmElementPath>, actual: ScopeSimple<AsmElementPath>) {
        assertEquals(expected.path, actual.path)
        assertEquals(expected.items, actual.items)
        assertMatches(expected.childScopes, actual.childScopes)
    }


    fun assertMatches(expected: Map<String, ScopeSimple<AsmElementPath>>, actual: Map<String, ScopeSimple<AsmElementPath>>) {
        assertEquals(expected.keys, actual.keys)
        for (k in expected.keys) {
            assertMatches(expected[k]!!, actual[k]!!)
        }
    }


}