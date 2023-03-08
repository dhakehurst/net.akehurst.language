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

import net.akehurst.language.agl.processor.Agl
import kotlin.test.Test
import kotlin.test.assertEquals

class test_Multi1N {

    companion object {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a+ b ;
                a = 'a' ;
                b = 'b' ;
            }
        """.trimIndent()
        val processor = Agl.processorFromString<Any,Any>(grammarStr).processor!!
    }

    @Test
    fun empty() {
        val sentence = ""
        val position = 0

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
                "a"
        )
        assertEquals(expected,actual)
    }

    @Test
    fun before_a() {
        val sentence = "a"
        val position = 0

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
                "a"
        )
        assertEquals(expected,actual)
    }

    @Test
    fun after_a() {
        val sentence = "a"
        val position = 1

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
                "a",
                "b"
        )
        assertEquals(expected,actual)
    }

    @Test
    fun before_b() {
        val sentence = "ab"
        val position = 1

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
                "a",
                "b"
        )
        assertEquals(expected,actual)
    }
}