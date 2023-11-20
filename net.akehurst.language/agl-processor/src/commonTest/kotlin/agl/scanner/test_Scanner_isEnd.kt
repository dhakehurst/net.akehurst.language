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

package net.akehurst.language.agl.scanner

import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_Scanner_isEnd {

    companion object {

        fun test(sentence: String, rrs: RuntimeRuleSet, position: Int, expected: Boolean) {
            val terms = rrs.terminalRules.filterNot { it.isEmptyTerminal }
            val scanners = listOf(
                InputFromString(terms.size, sentence),
                ScannerClassic(sentence, terms)
            )

            for (sc in scanners) {
                val actual = sc.isEnd(position)
                assertEquals(expected, actual)
            }
        }

    }

    @Test
    fun isEnd_empty_at_start() {
        val sentence = ""
        val rrs = runtimeRuleSet { }
        val position = 0

        val expected = true

        test(sentence, rrs, position, expected)
    }

    @Test
    fun isEnd_empty_after_start() {
        val inputText = ""
        val sut = InputFromString(10, inputText)

        val actual = sut.isEnd(1)

        assertEquals(true, actual)
    }

    @Test
    fun isEnd_full_at_start() {
        val inputText = "abcdefg"
        val sut = InputFromString(10, inputText)

        val actual = sut.isEnd(0)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_full_after_start() {
        val inputText = "abcdefg"
        val sut = InputFromString(10, inputText)

        val actual = sut.isEnd(1)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_full_before_end() {
        val inputText = "abcdefg"
        val sut = InputFromString(10, inputText)

        val actual = sut.isEnd(5)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_full_at_end() {
        val inputText = "abcdefg"
        val sut = InputFromString(10, inputText)

        val actual = sut.isEnd(6)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_full_after_end() {
        val inputText = "abcdefg"
        val sut = InputFromString(10, inputText)

        val actual = sut.isEnd(7)

        assertEquals(true, actual)
    }

}