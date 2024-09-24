/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
import net.akehurst.language.agl.GrammarString
import kotlin.test.Test
import kotlin.test.assertEquals

class test_SingleTerminalLiteral {

    @Test
    fun empty() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """.trimIndent()
        val pr = Agl.processorFromString<Any, Any>(grammarStr)

        val sentence = ""
        val position = 0
        val actual = pr.processor!!.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun before() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """.trimIndent()
        val pr = Agl.processorFromStringSimple(GrammarString(grammarStr))

        val sentence = "a"
        val position = 0
        val actual = pr.processor!!.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun end() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """.trimIndent()
        val pr = Agl.processorFromString<Any, Any>(grammarStr)

        val sentence = "a"
        val position = 1
        val actual = pr.processor!!.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(

        )
        assertEquals(expected, actual)
    }
}