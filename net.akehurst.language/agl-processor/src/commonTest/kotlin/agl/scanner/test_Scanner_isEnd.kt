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

import net.akehurst.language.agl.agl.parser.SentenceDefault
import net.akehurst.language.agl.regex.RegexEnginePlatform
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_Scanner_isEnd {

    companion object {

        fun test(text: String, rrs: RuntimeRuleSet, position: Int, expected: Boolean) {
            val sentence = SentenceDefault(text)
            val terms = rrs.terminals.filterNot { it.isEmptyTerminal }
            val scanners = listOf(
                ScannerOnDemand(RegexEnginePlatform, terms),
                ScannerClassic(RegexEnginePlatform, terms)
            )

            for (sc in scanners) {
                val actual = sc.isEnd(sentence, position)
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
        val sentence = SentenceDefault("")
        val sut = ScannerOnDemand(RegexEnginePlatform, emptyList())

        val actual = sut.isEnd(sentence, 1)

        assertEquals(true, actual)
    }

    @Test
    fun isEnd_full_at_start() {
        val sentence = SentenceDefault("abcdefg")
        val sut = ScannerOnDemand(RegexEnginePlatform, emptyList())

        val actual = sut.isEnd(sentence, 0)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_full_after_start() {
        val sentence = SentenceDefault("abcdefg")
        val sut = ScannerOnDemand(RegexEnginePlatform, emptyList())

        val actual = sut.isEnd(sentence, 1)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_full_before_end() {
        val sentence = SentenceDefault("abcdefg")
        val sut = ScannerOnDemand(RegexEnginePlatform, emptyList())

        val actual = sut.isEnd(sentence, 5)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_full_at_end() {
        val sentence = SentenceDefault("abcdefg")
        val sut = ScannerOnDemand(RegexEnginePlatform, emptyList())

        val actual = sut.isEnd(sentence, 6)

        assertEquals(false, actual)
    }

    @Test
    fun isEnd_full_after_end() {
        val sentence = SentenceDefault("abcdefg")
        val sut = ScannerOnDemand(RegexEnginePlatform, emptyList())

        val actual = sut.isEnd(sentence, 7)

        assertEquals(true, actual)
    }

}