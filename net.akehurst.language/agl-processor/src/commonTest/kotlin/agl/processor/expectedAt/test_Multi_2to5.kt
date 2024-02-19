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
import kotlin.test.Test
import kotlin.test.assertEquals

class test_Multi_2to5 {

    companion object {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a{3..5} b ;
                a = 'a' ;
                b = 'b' ;
            }
        """.trimIndent()
        val processor = Agl.processorFromString<Any, Any>(grammarStr).processor!!
    }

    @Test
    fun empty_at_0() {
        val sentence = ""
        val position = 0

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun a_at_0() {
        val sentence = "a"
        val position = 0

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun a_at_1() {
        val sentence = "a"
        val position = 1

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun ab_at_1() {
        val sentence = "ab"
        val position = 1

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun ab_at_2() {
        val sentence = "ab"
        val position = 2

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun aa_at_1() {
        val sentence = "aa"
        val position = 1

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun aa_at_2() {
        val sentence = "aa"
        val position = 2

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun aaa_at_2() {
        val sentence = "aaa"
        val position = 2

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun aaa_at_3() {
        val sentence = "aaa"
        val position = 3

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a",
            "b"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun aaab_at_3() {
        val sentence = "aaab"
        val position = 3

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a",
            "b"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun aaab_at_4() {
        val sentence = "aaab"
        val position = 4

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(

        )
        assertEquals(expected, actual)
    }

    @Test
    fun aaaa_at_3() {
        val sentence = "aaaa"
        val position = 3

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a",
            "b"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun aaaa_at_4() {
        val sentence = "aaaa"
        val position = 4

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a",
            "b"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun aaaaa_at_4() {
        val sentence = "aaaaa"
        val position = 4

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a",
            "b"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun aaaaa_at_5() {
        val sentence = "aaaaa"
        val position = 5

        val actual = processor.expectedTerminalsAt(sentence, position, 1).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "b"
        )
        assertEquals(expected, actual)
    }
}