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
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind
import net.akehurst.language.api.parser.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_InputFromString {

    @Test
    fun construct() {
        val inputText = ""
        val sut = InputFromString(0,inputText)

        assertNotNull(sut)
    }

    @Test
    fun isStart_empty_at_start() {
        val inputText = ""
        val sut = InputFromString(10,inputText)

        val actual = sut.isStart(0)

        assertEquals(true, actual)
    }

    @Test
    fun isStart_empty_after_start() {
        val inputText = ""
        val sut = InputFromString(10,inputText)

        val actual = sut.isStart(1)

        assertEquals(false, actual)
    }

    @Test
    fun isStart_full_at_start() {
        val inputText = "abcdefg"
        val sut = InputFromString(10,inputText)

        val actual = sut.isStart(0)

        assertEquals(true, actual)
    }

    @Test
    fun isStart_full_after_start() {
        val inputText = "abcdefg"
        val sut = InputFromString(10,inputText)

        val actual = sut.isStart(1)

        assertEquals(false, actual)
    }

    @Test
    fun isStart_full_before_end() {
        val inputText = "abcdefg"
        val sut = InputFromString(10,inputText)

        val actual = sut.isStart(5)

        assertEquals(false, actual)
    }

    @Test
    fun isStart_full_at_end() {
        val inputText = "abcdefg"
        val sut = InputFromString(10,inputText)

        val actual = sut.isStart(6)

        assertEquals(false, actual)
    }

    @Test
    fun isStart_full_after_end() {
        val inputText = "abcdefg"
        val sut = InputFromString(10,inputText)

        val actual = sut.isStart(7)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_empty_at_start() {
        val inputText = ""
        val sut = InputFromString(10,inputText)

        val actual = sut.isEnd(0)

        assertEquals(true, actual)
    }

    @Test
    fun isEnd_empty_after_start() {
        val inputText = ""
        val sut = InputFromString(10,inputText)

        val actual = sut.isEnd(1)

        assertEquals(true, actual)
    }

    @Test
    fun isEnd_full_at_start() {
        val inputText = "abcdefg"
        val sut = InputFromString(10,inputText)

        val actual = sut.isEnd(0)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_full_after_start() {
        val inputText = "abcdefg"
        val sut = InputFromString(10,inputText)

        val actual = sut.isEnd(1)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_full_before_end() {
        val inputText = "abcdefg"
        val sut = InputFromString(10,inputText)

        val actual = sut.isEnd(5)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_full_at_end() {
        val inputText = "abcdefg"
        val sut = InputFromString(10,inputText)

        val actual = sut.isEnd(6)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_full_after_end() {
        val inputText = "abcdefg"
        val sut = InputFromString(10,inputText)

        val actual = sut.isEnd(7)

        assertEquals(true, actual)
    }

    @Test
    fun tryMatchText_empty_at_start_literal_empty() {
        val inputText = ""
        val sut = InputFromString(10,inputText)

        val rr = RuntimeRule(0,1,"","",RuntimeRuleKind.TERMINAL,false,false)
        val actual = sut.tryMatchText(0, rr)

        assertEquals(null, actual)
    }

    @Test
    fun tryMatchText_empty_at_start_literal_abc() {
        val inputText = ""
        val sut = InputFromString(10,inputText)

        val rr = RuntimeRule(0,1,"'abc'","abc",RuntimeRuleKind.TERMINAL,false,false)
        val actual = sut.tryMatchText(0, rr)

        assertEquals(null, actual)
    }

    @Test
    fun tryMatchText_empty_at_start_pattern_empty() {
        val inputText = ""
        val sut = InputFromString(10,inputText)

        val rr = RuntimeRule(0,1,"","",RuntimeRuleKind.TERMINAL,false,false)
        val actual = sut.tryMatchText(0, rr)

        assertEquals(null, actual)
    }

    @Test
    fun tryMatchText_empty_at_start_pattern_abc() {
        val inputText = ""
        val sut = InputFromString(10,inputText)

        val rr = RuntimeRule(0,1,"'abc'","abc",RuntimeRuleKind.TERMINAL,false,false)
        val actual = sut.tryMatchText(0, rr)

        assertEquals(null, actual)
    }

    @Test
    fun tryMatchText_empty_at_start_pattern_a_to_c() {
        val inputText = ""
        val sut = InputFromString(10,inputText)

        val rr = RuntimeRule(0,1,"'[a-c]'","[a-c]",RuntimeRuleKind.TERMINAL,true,false)
        val actual = sut.tryMatchText(0, rr)

        assertEquals(null, actual)
    }


    @Test
    fun tryMatchText_abc_at_start_pattern_abc() {
        val inputText = "abc"
        val sut = InputFromString(10,inputText)

        val rr = RuntimeRule(0,1,"'abc'","abc",RuntimeRuleKind.TERMINAL,false,false)
        val actual = sut.tryMatchText(0, rr)

        assertEquals("abc", actual?.matchedText)
    }

    @Test
    fun tryMatchText_abc_at_start_pattern_a_to_c() {
        val inputText = "abc"
        val sut = InputFromString(10,inputText)

        val rr = RuntimeRule(0,1,"'[a-c]'","[a-c]",RuntimeRuleKind.TERMINAL,true,false)
        val actual = sut.tryMatchText(0, rr)

        assertEquals("a", actual?.matchedText)
    }

    @Test
    fun tryMatchText_abc_at_1_pattern_a_to_c() {
        val inputText = "abc"
        val sut = InputFromString(10,inputText)

        val rr = RuntimeRule(0,1,"'[a-c]'","[a-c]",RuntimeRuleKind.TERMINAL,true,false)
        val actual = sut.tryMatchText(1, rr)

        assertEquals("b", actual?.matchedText)
    }
    //TODO:....tryMatchText


    @Test
    fun locationFor_singleLine() {
        val inputText = "abc"
        val sut = InputFromString(10,inputText)

        for(p in inputText.indices) {
            val actual = sut.locationFor(p, 1)
            val col = p+1
            val line = 1
            val expected = InputLocation(p, col, line, 1)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun locationFor_muiltLine() {
        val inputText = """
            abc
            def
            ghi
        """.trimIndent()
        val sut = InputFromString(10,inputText)

        for(p in inputText.indices) {
            val actual = sut.locationFor(p, 1)
            val col = (p % 4)+1
            val line = (p / 4)+1
            val expected = InputLocation(p, col, line, 1)
            assertEquals(expected, actual)
        }

    }
}