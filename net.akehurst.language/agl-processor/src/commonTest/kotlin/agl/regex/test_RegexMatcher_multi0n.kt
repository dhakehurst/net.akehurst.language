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

class test_RegexMatcher_multi0n {

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

}