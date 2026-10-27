/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.sentence.common.SentenceDefault
import kotlin.test.Test
import kotlin.test.assertEquals

class test_Sentence_contextInText {

    private val sut = SentenceDefault("0123456789\nabcdefghijklmnop", null)

    @Test
    fun middle_of_first_line() {
        assertEquals("...2345678901^2\nabcdefgh...", SentenceDefault("0123456789012\nabcdefghijklmnop", null).contextInText(12))
    }

    @Test
    fun window_starts_at_start_of_line() {
        assertEquals("abcdefghij^klmnop", sut.contextInText(21))
    }

    @Test
    fun window_starts_with_eol() {
        assertEquals("abcdefghi^jklmnop", sut.contextInText(20))
    }

    @Test
    fun window_starts_mid_line() {
        assertEquals("...cdefghijkl^mnop", sut.contextInText(23))
    }
}
