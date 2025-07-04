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

package net.akehurst.language.scanner.common

import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.sentence.common.SentenceDefault
import kotlin.test.Test
import kotlin.test.assertEquals

class test_Sentence_locationFor {

    @Test
    fun singleLine() {
        val inputText = "abc"
        val sut = SentenceDefault(inputText, null)

        for (p in inputText.indices) {
            val actual = sut.locationFor(p, 1)
            val col = p + 1
            val line = 1
            val expected = InputLocation(p, col, line, 1, null)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun muiltLine() {
        val inputText = """
            abc
            def
            ghi
        """.trimIndent()
        val sut = SentenceDefault(inputText, null)

        for (p in inputText.indices) {
            val actual = sut.locationFor(p, 1)
            val col = (p % 4) + 1
            val line = (p / 4) + 1
            val expected = InputLocation(p, col, line, 1, null)
            assertEquals(expected, actual)
        }

    }
}