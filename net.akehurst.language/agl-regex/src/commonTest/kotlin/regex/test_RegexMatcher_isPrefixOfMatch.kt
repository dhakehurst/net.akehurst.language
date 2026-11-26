/**
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.regex.agl

import kotlin.test.Test
import kotlin.test.assertEquals

class test_RegexMatcher_isPrefixOfMatch {

    private fun test(pattern: String, text: String, expected: Boolean, startPosition: Int = 0) {
        val m = regexMatcher(pattern)
        assertEquals(expected, m.isPrefixOfMatch(text, startPosition), "'$text' from $startPosition is prefix of a match of /$pattern/")
    }

    @Test fun empty_text() = test("abc", "", true)
    @Test fun literal_part() = test("abc", "ab", true)
    @Test fun literal_all() = test("abc", "abc", true)
    @Test fun literal_longer() = test("abc", "abcd", false)
    @Test fun literal_other() = test("abc", "ax", false)
    @Test fun plus_one() = test("[0-9]+", "1", true)
    @Test fun plus_many() = test("[0-9]+", "123", true)
    @Test fun plus_other() = test("[0-9]+", "1a", false)
    @Test fun decimal_int() = test("[0-9]+\\.[0-9]+", "1", true)
    @Test fun decimal_dot() = test("[0-9]+\\.[0-9]+", "1.", true)
    @Test fun decimal_all() = test("[0-9]+\\.[0-9]+", "1.5", true)
    @Test fun decimal_other() = test("[0-9]+\\.[0-9]+", "1.a", false)
    @Test fun choice_first() = test("ab|cd", "a", true)
    @Test fun choice_second() = test("ab|cd", "c", true)
    @Test fun choice_none() = test("ab|cd", "b", false)
    @Test fun optional_part() = test("ab?c", "a", true)
    @Test fun optional_skipped() = test("ab?c", "ac", true)
    @Test fun bounded_too_long() = test("a{2,3}", "aaaa", false)
    @Test fun bounded_ok() = test("a{2,3}", "aaa", true)
    @Test fun id() = test("[A-Za-z_][A-Za-z0-9_]*", "abc_1", true)
    @Test fun id_digit_first() = test("[A-Za-z_][A-Za-z0-9_]*", "1abc", false)
    @Test fun from_position() = test("[0-9]+", "ab12", true, 2)
    @Test fun from_position_other() = test("[0-9]+", "ab12", false, 1)
    @Test fun end_of_text() = test("[0-9]+", "ab", true, 2)
}
