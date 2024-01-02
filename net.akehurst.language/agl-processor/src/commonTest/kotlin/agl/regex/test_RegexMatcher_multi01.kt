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

class test_RegexMatcher_multi01 {

    @Test
    fun multi01_0() {
        val m = regexMatcher(",?")
        val actual = m.match("", 0)
        val expected = RegexMatcher.MatchResultAgl("")
        assertEquals(expected, actual)
    }

    @Test
    fun multi01_1() {
        val m = regexMatcher("a?b")
        val actual = m.match("b", 0)
        val expected = RegexMatcher.MatchResultAgl("b")
        assertEquals(expected, actual)
    }

    @Test
    fun multi01_2() {
        val m = regexMatcher("a?b")
        val actual = m.match("ab", 0)
        val expected = RegexMatcher.MatchResultAgl("ab")
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

}