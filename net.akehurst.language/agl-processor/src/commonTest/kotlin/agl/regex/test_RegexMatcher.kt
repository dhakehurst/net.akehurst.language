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

import kotlin.test.Test
import kotlin.test.assertEquals

class test_RegexMatcher {

    @Test
    fun character() {
        val m = regexMatcher("a")
        val actual = m.match("a", 0)
        val expected = RegexMatcher.MatchResult("a", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun character_fails() {
        val m = regexMatcher("a")
        val actual = m.match("b", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun concatenation() {
        val m = regexMatcher("ab")
        val actual = m.match("ab", 0)
        val expected = RegexMatcher.MatchResult("ab", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun concatenation_fails() {
        val m = regexMatcher("ab")
        val actual = m.match("ac", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun choice_1() {
        val m = regexMatcher("a|b")
        val actual = m.match("a", 0)
        val expected = RegexMatcher.MatchResult("a", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun choice_2() {
        val m = regexMatcher("a|b")
        val actual = m.match("b", 0)
        val expected = RegexMatcher.MatchResult("b", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun choice_fails() {
        val m = regexMatcher("c")
        val actual = m.match("ac", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun group_1() {
        val m = regexMatcher("(a)")
        val actual = m.match("a", 0)
        val expected = RegexMatcher.MatchResult("a", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun group_2() {
        val m = regexMatcher("(a)(b)")
        val actual = m.match("ab", 0)
        val expected = RegexMatcher.MatchResult("ab", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun group_3() {
        val m = regexMatcher("(a)|(b)")

        val actual = m.match("a", 0)
        val expected = RegexMatcher.MatchResult("a", emptyList())
        assertEquals(expected, actual)

        val actual2 = m.match("b", 0)
        val expected2 = RegexMatcher.MatchResult("b", emptyList())
        assertEquals(expected2, actual2)

    }

    @Test
    fun group1_1() {
        val m = regexMatcher("ab|c")
        val actual = m.match("ab", 0)
        val expected = RegexMatcher.MatchResult("ab", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun group1_2() {
        val m = regexMatcher("ab|c")
        val actual = m.match("c", 0)
        val expected = RegexMatcher.MatchResult("c", emptyList())
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
        val expected = RegexMatcher.MatchResult("ab", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun group2_2() {
        val m = regexMatcher("(ab)|c")
        val actual = m.match("c", 0)
        val expected = RegexMatcher.MatchResult("c", emptyList())
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
        val expected = RegexMatcher.MatchResult("ab", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun group3_2() {
        val m = regexMatcher("a(b|c)")
        val actual = m.match("ac", 0)
        val expected = RegexMatcher.MatchResult("ac", emptyList())
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
    fun multi01_1() {
        val m = regexMatcher("a?b")
        val actual = m.match("b", 0)
        val expected = RegexMatcher.MatchResult("b", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun multi01_2() {
        val m = regexMatcher("a?b")
        val actual = m.match("ab", 0)
        val expected = RegexMatcher.MatchResult("ab", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun multi01_fails_1() {
        val m = regexMatcher("a?b")
        val actual = m.match("c", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun multi01_fails_2() {
        val m = regexMatcher("a?b")
        val actual = m.match("ac", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun multi0n_1() {
        val m = regexMatcher("a*b")
        val actual = m.match("b", 0)
        val expected = RegexMatcher.MatchResult("b", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun multi0n_2() {
        val m = regexMatcher("a*b")
        val actual = m.match("ab", 0)
        val expected = RegexMatcher.MatchResult("ab", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun multi0n_3() {
        val m = regexMatcher("a*b")
        val actual = m.match("aaaaaab", 0)
        val expected = RegexMatcher.MatchResult("aaaaaab", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun multi0n_fails_1() {
        val m = regexMatcher("a*b")
        val actual = m.match("c", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun multi0n_fails_2() {
        val m = regexMatcher("a*b")
        val actual = m.match("ac", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun multi0n_fails_3() {
        val m = regexMatcher("a*b")
        val actual = m.match("aaaaaaac", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun characterClass_abcd() {
        val m = regexMatcher("[abcd]")
        for (c in 'a'..'d') {
            val actual = m.match("$c", 0)
            val expected = RegexMatcher.MatchResult("$c", emptyList())
            assertEquals(expected, actual)
        }
    }

    @Test
    fun characterClass_abcd_fails() {
        val m = regexMatcher("[abcd]")
        for (c in 'a'..'d') {
            val actual = m.match("$c", 0)
            val expected = RegexMatcher.MatchResult("$c", emptyList())
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
            val expected = RegexMatcher.MatchResult("$c", emptyList())
            assertEquals(expected, actual)
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
        val expected = RegexMatcher.MatchResult("Ba2-dF9", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun noncapturinggroup_double_quote_string() {
        val m = regexMatcher("(\"(?:\\?.)*?\")")
        val actual = m.match("\"Ba2-dF9\"", 0)
        val expected = RegexMatcher.MatchResult("\"Ba2-dF9\"", emptyList())
        assertEquals(expected, actual)
    }


}