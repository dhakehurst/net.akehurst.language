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

class test_Concatination_with_skip {

    private companion object {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = 'a' 'b' 'c' 'd' ;
            }
        """.trimIndent()
        val processor = Agl.processorFromString<Any, Any>(grammarStr).processor!!
    }

    @Test
    fun empty_p0() {
        val sentence = ""
        val position = 0

        val actual = processor.expectedTerminalsAt(sentence, position).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun WS_p0() {
        val sentence = " "
        val position = 0

        val actual = processor.expectedTerminalsAt(sentence, position).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun WS_p1() {
        val sentence = " "
        val position = 1

        val actual = processor.expectedTerminalsAt(sentence, position).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun a_p0() {
        val sentence = "a"
        val position = 0

        val actual = processor.expectedTerminalsAt(sentence, position).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun WSa_p0() {
        val sentence = " a"
        val position = 0

        val actual = processor.expectedTerminalsAt(sentence, position, ).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun WSa_p1() {
        val sentence = " a"
        val position = 1

        val actual = processor.expectedTerminalsAt(sentence, position, ).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun WSa_p2() {
        val sentence = " a"
        val position = 2

        val actual = processor.expectedTerminalsAt(sentence, position, ).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun a_p1() {
        val sentence = "a"
        val position = 1

        val actual = processor.expectedTerminalsAt(sentence, position, ).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun ab_p1() {
        val sentence = "ab"
        val position = 1

        val actual = processor.expectedTerminalsAt(sentence, position, ).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "a"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun ab_p2() {
        val sentence = "ab"
        val position = 2

        val actual = processor.expectedTerminalsAt(sentence, position, ).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "b"
        )
        assertEquals(expected, actual)
    }

    @Test
    fun abWS_p2() {
        val sentence = "ab "
        val position = 2

        val actual = processor.expectedTerminalsAt(sentence, position, ).items.map { it.text }.toSet()
        val expected = setOf<String>(
            "c"
        )
        assertEquals(expected, actual)
    }
}