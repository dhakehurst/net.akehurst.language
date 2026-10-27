/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.processor.expectedAt

import net.akehurst.language.agl.Agl
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * a partially typed word can be completed (offered from the word start)
 * or be complete, with what follows offered at the position. Each item has its own offeredPosition.
 */
class test_expectedAt_offeredPosition {

    private companion object {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = A | B ;
                A = 'a' 'b' ;
                B = 'ab' 'c' ;
            }
        """.trimIndent()
        val processor = Agl.processorFromString<Any, Any>(grammarStr).processor!!
    }

    @Test
    fun terminals_complete_word_or_next() {
        val result = processor.expectedTerminalsAt("a", 1)
        val actual = result.items.map { Pair(it.text, it.offeredPosition) }.toSet()
        assertEquals(setOf(Pair("ab", 0), Pair("b", 1)), actual)
        assertEquals(1, result.requestedPosition)
        assertEquals(0, result.offeredPosition)
    }

    @Test
    fun terminals_next_only() {
        val result = processor.expectedTerminalsAt("ab", 2)
        val actual = result.items.map { Pair(it.text, it.offeredPosition) }.toSet()
        assertEquals(setOf(Pair("c", 2)), actual)
        assertEquals(2, result.offeredPosition)
    }

    @Test
    fun items_complete_word_or_next() {
        val result = processor.expectedItemsAt("a", 1)
        val actual = result.items.filter { it.kind == net.akehurst.language.api.processor.CompletionItemKind.LITERAL }.map { Pair(it.text, it.offeredPosition) }.toSet()
        assertEquals(setOf(Pair("ab", 0), Pair("b", 1)), actual)
        assertEquals(0, result.offeredPosition)
    }
}
