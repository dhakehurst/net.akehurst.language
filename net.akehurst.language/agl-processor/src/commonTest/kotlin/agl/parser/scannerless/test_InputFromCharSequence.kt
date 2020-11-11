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

package net.akehurst.language.parser.scanondemand

import net.akehurst.language.agl.parser.InputFromString
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_InputFromCharSequence {

    @Test
    fun construct() {
        val inputText = ""
        val sut = InputFromString(inputText)

        assertNotNull(sut)
    }

    @Test
    fun isStart_empty_at_start() {
        val inputText = ""
        val sut = InputFromString(inputText)

        val actual = sut.isStart(0)

        assertEquals(true, actual)
    }

    @Test
    fun isStart_empty_after_start() {
        val inputText = ""
        val sut = InputFromString(inputText)

        val actual = sut.isStart(1)

        assertEquals(false, actual)
    }

    @Test
    fun isStart_full_at_start() {
        val inputText = "abcdefg"
        val sut = InputFromString(inputText)

        val actual = sut.isStart(0)

        assertEquals(true, actual)
    }

    @Test
    fun isStart_full_after_start() {
        val inputText = "abcdefg"
        val sut = InputFromString(inputText)

        val actual = sut.isStart(1)

        assertEquals(false, actual)
    }

    @Test
    fun isStart_full_before_end() {
        val inputText = "abcdefg"
        val sut = InputFromString(inputText)

        val actual = sut.isStart(5)

        assertEquals(false, actual)
    }

    @Test
    fun isStart_full_at_end() {
        val inputText = "abcdefg"
        val sut = InputFromString(inputText)

        val actual = sut.isStart(6)

        assertEquals(false, actual)
    }

    @Test
    fun isStart_full_after_end() {
        val inputText = "abcdefg"
        val sut = InputFromString(inputText)

        val actual = sut.isStart(7)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_empty_at_start() {
        val inputText = ""
        val sut = InputFromString(inputText)

        val actual = sut.isEnd(0)

        assertEquals(true, actual)
    }

    @Test
    fun isEnd_empty_after_start() {
        val inputText = ""
        val sut = InputFromString(inputText)

        val actual = sut.isEnd(1)

        assertEquals(true, actual)
    }

    @Test
    fun isEnd_full_at_start() {
        val inputText = "abcdefg"
        val sut = InputFromString(inputText)

        val actual = sut.isEnd(0)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_full_after_start() {
        val inputText = "abcdefg"
        val sut = InputFromString(inputText)

        val actual = sut.isEnd(1)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_full_before_end() {
        val inputText = "abcdefg"
        val sut = InputFromString(inputText)

        val actual = sut.isEnd(5)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_full_at_end() {
        val inputText = "abcdefg"
        val sut = InputFromString(inputText)

        val actual = sut.isEnd(6)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_full_after_end() {
        val inputText = "abcdefg"
        val sut = InputFromString(inputText)

        val actual = sut.isEnd(7)

        assertEquals(true, actual)
    }

    @Test
    fun tryMatchText_empty_at_start_literal_empty() {
        val inputText = ""
        val sut = InputFromString(inputText)

        val actual = sut.tryMatchText(0, "", null)

        assertEquals(null, actual)
    }

    @Test
    fun tryMatchText_empty_at_start_literal_abc() {
        val inputText = ""
        val sut = InputFromString(inputText)

        val actual = sut.tryMatchText(0, "abc", null)

        assertEquals(null, actual)
    }

    @Test
    fun tryMatchText_empty_at_start_pattern_empty() {
        val inputText = ""
        val sut = InputFromString(inputText)

        val actual = sut.tryMatchText(0, "", Regex(""))

        assertEquals(null, actual)
    }

    @Test
    fun tryMatchText_empty_at_start_pattern_abc() {
        val inputText = ""
        val sut = InputFromString(inputText)

        val actual = sut.tryMatchText(0, "abc", Regex("abc"))

        assertEquals(null, actual)
    }

    @Test
    fun tryMatchText_empty_at_start_pattern_a_to_c() {
        val inputText = ""
        val sut = InputFromString(inputText)

        val actual = sut.tryMatchText(0, "[a-c]", Regex("[a-c]"))

        assertEquals(null, actual)
    }

    //TODO:....tryMatchText
}