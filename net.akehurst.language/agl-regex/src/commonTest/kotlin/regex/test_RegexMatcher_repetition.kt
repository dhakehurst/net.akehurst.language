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

class test_RegexMatcher_repetition {

    @Test
    fun rep_0() {
        val m = regexMatcher("a{0}b")
        val actual = m.match("b", 0)
        val expected = RegexMatcher.MatchResultAgl("b")
        assertEquals(expected, actual)
    }

    @Test
    fun rep_0_fails() {
        val m = regexMatcher("a{0}b")
        val actual = m.match("ab", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun rep_1() {
        val m = regexMatcher("a{1}b")
        val actual = m.match("ab", 0)
        val expected = RegexMatcher.MatchResultAgl("ab")
        assertEquals(expected, actual)
    }

    @Test
    fun rep_1_fail() {
        val m = regexMatcher("a{1}b")
        val actual = m.match("aab", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun rep_2() {
        val m = regexMatcher("a{2}b")
        val actual = m.match("aab", 0)
        val expected = RegexMatcher.MatchResultAgl("aab")
        assertEquals(expected, actual)
    }

    @Test
    fun rep_2_fail1() {
        val m = regexMatcher("a{2}b")
        val actual = m.match("ab", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun rep_2_fail2() {
        val m = regexMatcher("a{2}b")
        val actual = m.match("aaab", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun rep_5() {
        val m = regexMatcher("a{5}")
        val actual = m.match("aaaaa", 0)
        val expected = RegexMatcher.MatchResultAgl("aaaaa")
        assertEquals(expected, actual)
    }

    @Test
    fun rep_3p() {
        val m = regexMatcher("a{3,}b")
        for (i in 3..7) {
            val t = "a".repeat(i) + "b"
            val actual = m.match(t, 0)
            val expected = RegexMatcher.MatchResultAgl(t)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun rep_3p_fail() {
        val m = regexMatcher("a{3,}b")
        val actual = m.match("aab", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun rep_1t5() {
        val m = regexMatcher("a{1,5}b")
        for (i in 1..5) {
            val t = "a".repeat(i) + "b"
            val actual = m.match(t, 0)
            val expected = RegexMatcher.MatchResultAgl(t)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun rep_1t5_fail() {
        val m = regexMatcher("a{1,5}b")
        val actual = m.match("aaaaaab", 0)
        val expected = null
        assertEquals(expected, actual)
    }
}