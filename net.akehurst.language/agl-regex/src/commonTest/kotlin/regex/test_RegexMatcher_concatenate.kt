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

class test_RegexMatcher_concatenate {

    @Test
    fun empty() {
        val m = regexMatcher("")
        val actual = m.match("a", 0)
        val expected = null
        assertEquals(expected, actual)
    }

    @Test
    fun character() {
        val m = regexMatcher("a")
        val actual = m.match("a", 0)
        val expected = RegexMatcher.MatchResultAgl("a")
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
        val expected = RegexMatcher.MatchResultAgl("ab")
        assertEquals(expected, actual)
    }

    @Test
    fun concatenation_fails() {
        val m = regexMatcher("ab")
        val actual = m.match("ac", 0)
        val expected = null
        assertEquals(expected, actual)
    }

}