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
import kotlin.test.*

class test_Multi0N_noConcatAtTopLevel {

    companion object {
        val grammarStr = """
            namespace test
            grammar Test {
                S = ab* ;
                ab = 'a' 'b' ;
            }
        """.trimIndent()
        val processor = Agl.processor(grammarStr)
    }

    @Test
    fun empty_0() {
        val sentence = ""
        val position = 0

        val actual = processor.expectedAt(sentence, position, 1).map { it.text }.toSet()
        val expected = setOf<String>(
                "a"
        )
        assertEquals(expected,actual)
    }

    @Test
    fun a_before_a() {
        val sentence = "a"
        val position = 0

        val actual = processor.expectedAt(sentence, position, 1).map { it.text }.toSet()
        val expected = setOf<String>(
                "a"
        )
        assertEquals(expected,actual)
    }

    @Test
    fun a_after_a() {
        val sentence = "a"
        val position = 1

        val actual = processor.expectedAt(sentence, position, 1).map { it.text }.toSet()
        val expected = setOf<String>(
                "b"
        )
        assertEquals(expected,actual)
    }

    @Test
    fun ab_before_b() {
        val sentence = "ab"
        val position = 1

        val actual = processor.expectedAt(sentence, position, 1).map { it.text }.toSet()
        val expected = setOf<String>(
                "b"
        )
        assertEquals(expected,actual)
    }

    @Test
    fun ab_after_ab() {
        val sentence = "ab"
        val position = 2

        val actual = processor.expectedAt(sentence, position, 1).map { it.text }.toSet()
        val expected = setOf<String>(
                "a"
        )
        assertEquals(expected,actual)
    }
}