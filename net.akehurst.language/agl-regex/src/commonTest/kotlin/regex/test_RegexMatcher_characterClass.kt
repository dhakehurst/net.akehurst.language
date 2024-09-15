/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.regex.api.RegexMatcher
import kotlin.test.Test
import kotlin.test.assertEquals

class test_RegexMatcher_characterClass {

    @Test
    fun characterClass_abcd() {
        val m = regexMatcher("[abcd]")
        for (c in 'a'..'d') {
            val actual = m.match("$c", 0)
            val expected = RegexMatcher.MatchResultAgl("$c")
            assertEquals(expected, actual)
        }
    }

    @Test
    fun characterClass_abcd_fails() {
        val m = regexMatcher("[abcd]")
        for (c in 'a'..'d') {
            val actual = m.match("$c", 0)
            val expected = RegexMatcher.MatchResultAgl("$c")
            assertEquals(expected, actual)
        }
        for (c in 'e'..'h') {
            val actual = m.match("$c", 0)
            val expected = null
            assertEquals(expected, actual)
        }
    }

    @Test
    fun characterClass_Range() {
        val m = regexMatcher("[a-z]")
        for (c in 'a'..'z') {
            val actual = m.match("$c", 0)
            val expected = RegexMatcher.MatchResultAgl("$c")
            assertEquals(expected, actual)
        }
    }

    @Test
    fun characterClass_minus() {
        val m = regexMatcher("[-]")
        for (c in listOf('-')) {
            val actual = m.match("$c", 0)
            val expected = RegexMatcher.MatchResultAgl("$c")
            assertEquals(expected, actual, "testing '$c',")
        }
    }

    @Test
    fun characterClass_X_minus() {
        val m = regexMatcher("[+-]")
        for (c in listOf('-', '+')) {
            val actual = m.match("$c", 0)
            val expected = RegexMatcher.MatchResultAgl("$c")
            assertEquals(expected, actual, "testing '$c',")
        }
    }

    @Test
    fun characterClass_minus_X() {
        val m = regexMatcher("[-+]")
        for (c in listOf('-', '+')) {
            val actual = m.match("$c", 0)
            val expected = RegexMatcher.MatchResultAgl("$c")
            assertEquals(expected, actual, "testing '$c',")
        }
    }

    @Test
    fun characterClass_range_minus() {
        val m = regexMatcher("[a-b-]")
        for (c in listOf('a', 'b', '-')) {
            val actual = m.match("$c", 0)
            val expected = RegexMatcher.MatchResultAgl("$c")
            assertEquals(expected, actual, "testing '$c',")
        }
    }

    @Test
    fun characterClass_Range_fails() {
        val m = regexMatcher("[a-z]")
        for (c in 'A'..'Z') {
            val actual = m.match("$c", 0)
            val expected = null
            assertEquals(expected, actual)
        }
    }

    @Test
    fun characterClass_many() {
        val m = regexMatcher("([a-zA-Z_][a-zA-Z_0-9-]*)")
        val actual = m.match("Ba2-dF9", 0)
        val expected = RegexMatcher.MatchResultAgl("Ba2-dF9")
        assertEquals(expected, actual)
    }

    @Test
    fun abcd_negated() {
        val m = regexMatcher("[^abcd]")
        for (c in 'e'..'h') {
            val actual = m.match("$c", 0)
            val expected = RegexMatcher.MatchResultAgl("$c")
            assertEquals(expected, actual)
        }
    }

    @Test
    fun abcd_negated_failed() {
        val m = regexMatcher("[^abcd]")
        for (c in 'a'..'d') {
            val actual = m.match("$c", 0)
            val expected = null
            assertEquals(expected, actual)
        }
    }

    @Test
    fun a2d_negated_failed() {
        val m = regexMatcher("[^a-d]")
        for (c in 'a'..'d') {
            val actual = m.match("$c", 0)
            val expected = null
            assertEquals(expected, actual)
        }
    }

    @Test
    fun a2d_negated_concat() {
        val m = regexMatcher("[^a-d]x")
        for (c in 'e'..'h') {
            val actual = m.match("${c}x", 0)
            val expected = RegexMatcher.MatchResultAgl("${c}x")
            assertEquals(expected, actual)
        }
    }

    @Test
    fun a2d_negated_concat_failed() {
        val m = regexMatcher("[^a-d]x")
        for (c in 'a'..'d') {
            val actual = m.match("${c}a", 0)
            val expected = null
            assertEquals(expected, actual)
        }
    }

    @Test
    fun hex_abc() {
        val m = regexMatcher("[\\x61b\\x63]")
        for (c in 'a'..'c') {
            val actual = m.match("${c}", 0)
            val expected = RegexMatcher.MatchResultAgl("${c}")
            assertEquals(expected, actual)
        }
    }

    @Test
    fun hex__abc_failed() {
        val m = regexMatcher("[\\x61b\\x63]")
        for (c in 'd'..'i') {
            val actual = m.match("${c}", 0)
            val expected = null
            assertEquals(expected, actual)
        }
    }
}