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

package net.akehurst.language.agl.regex

import net.akehurst.language.api.regex.RegexMatcher
import kotlin.test.Test
import kotlin.test.assertEquals

class test_RegexMatcher_group {

    @Test
    fun group_1() {
        val m = regexMatcher("(a)")
        val actual = m.match("a", 0)
        val expected = RegexMatcher.MatchResultAgl("a")
        assertEquals(expected, actual)
    }

    @Test
    fun group_2() {
        val m = regexMatcher("(a)(b)")
        val actual = m.match("ab", 0)
        val expected = RegexMatcher.MatchResultAgl("ab")
        assertEquals(expected, actual)
    }

    @Test
    fun group_3() {
        val m = regexMatcher("(a)|(b)")

        val actual = m.match("a", 0)
        val expected = RegexMatcher.MatchResultAgl("a")
        assertEquals(expected, actual)

        val actual2 = m.match("b", 0)
        val expected2 = RegexMatcher.MatchResultAgl("b")
        assertEquals(expected2, actual2)

    }

    @Test
    fun group1_1() {
        val m = regexMatcher("ab|c")
        val actual = m.match("ab", 0)
        val expected = RegexMatcher.MatchResultAgl("ab")
        assertEquals(expected, actual)
    }

    @Test
    fun group1_2() {
        val m = regexMatcher("ab|c")
        val actual = m.match("c", 0)
        val expected = RegexMatcher.MatchResultAgl("c")
        assertEquals(expected, actual)
    }

    @Test
    fun group1_1_fails() {
        val m = regexMatcher("ab|c")
        val actual = m.match("ac", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun group1_2_fails() {
        val m = regexMatcher("ab|c")
        val actual = m.match("b", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun group2_1() {
        val m = regexMatcher("(ab)|c")
        val actual = m.match("ab", 0)
        val expected = RegexMatcher.MatchResultAgl("ab")
        assertEquals(expected, actual)
    }

    @Test
    fun group2_2() {
        val m = regexMatcher("(ab)|c")
        val actual = m.match("c", 0)
        val expected = RegexMatcher.MatchResultAgl("c")
        assertEquals(expected, actual)
    }

    @Test
    fun group2_1_fails() {
        val m = regexMatcher("(ab)|c")
        val actual = m.match("ac", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun group2_2_fails() {
        val m = regexMatcher("(ab)|c")
        val actual = m.match("b", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun group3_1() {
        val m = regexMatcher("a(b|c)")
        val actual = m.match("ab", 0)
        val expected = RegexMatcher.MatchResultAgl("ab")
        assertEquals(expected, actual)
    }

    @Test
    fun group3_2() {
        val m = regexMatcher("a(b|c)")
        val actual = m.match("ac", 0)
        val expected = RegexMatcher.MatchResultAgl("ac")
        assertEquals(expected, actual)
    }

    @Test
    fun group3_1_fails() {
        val m = regexMatcher("a(b|c)")
        val actual = m.match("a", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun group3_2_fails() {
        val m = regexMatcher("a(b|c)")
        val actual = m.match("b", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun group3_3_fails() {
        val m = regexMatcher("a(b|c)")
        val actual = m.match("c", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun group3_4_fails() {
        val m = regexMatcher("a(b|c)")
        val actual = m.match("ad", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun group4() {
        val m = regexMatcher("c|d(e|f){0,1}")
        val text = "d"
        val actual = m.match(text, 0)
        val expected = RegexMatcher.MatchResultAgl(text)
        assertEquals(expected, actual)
    }

}