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

class test_RegexMatcher {


    @Test
    fun double_quote_string() {
        val m = regexMatcher("\"[^\"]*\"")
        val actual = m.match("\"Ba2-dF9\"", 0)
        val expected = RegexMatcher.MatchResult("\"Ba2-dF9\"", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun double_quote_string_2() {
        val m = regexMatcher("\"([^\"\\\\]|\\\\.)*\"")
        val actual = m.match("\"Ba2\\\"F9\"", 0)
        val expected = RegexMatcher.MatchResult("\"Ba2\\\"F9\"", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun double_quote_string_3() {
        val m = regexMatcher("\"([^\"\\\\]|\\\\.)*\"")
        val actual = m.match("\"\\\"([^\\\"\\\\]|\\\\.)*\\\"\"", 0)
        val expected = RegexMatcher.MatchResult("\"\\\"([^\\\"\\\\]|\\\\.)*\\\"\"", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun t() {
        val m = regexMatcher("abc*d+(ef*g+)*h")
        val actual = m.match("abcccdddefffgggh", 0)
        val expected = RegexMatcher.MatchResult("abcccdddefffgggh", emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun multiline_comment() {
        val m = regexMatcher("/\\*[^*]*\\*+([^*/][^*]*\\*+)*/")
        val actual = m.match("/* fgh /*sdf dfgj */", 0)
        val expected = RegexMatcher.MatchResult("/* fgh /*sdf dfgj */", emptyList())
        assertEquals(expected, actual)
    }


    @Test
    fun integer() {
        val m = regexMatcher("((0[xX][0-9a-fA-F]([0-9a-fA-F_]*[0-9a-fA-F])?))(((l)|(L)){0,1})|((0_*[0-7]([0-7_]*[0-7])?))(((l)|(L)){0,1})|((0[bB][01]([01_]*[01])?))(((l)|(L)){0,1})|(((0|[1-9]([0-9_]*[0-9])?)))(((l)|(L)){0,1})")
        //val m = regexMatcher("c|d(e|f){0,1}")
        val text = "1"
        val actual = m.match(text, 0)
        val expected = RegexMatcher.MatchResult(text, emptyList())
        assertEquals(expected, actual)
    }

    @Test
    fun ex() {
        val ex = """
        (\Q\\E)
        """.trimIndent()
        val m = regexMatcher(ex)
        val text = "\\"
        val actual = m.match(text, 0)
        val expected = RegexMatcher.MatchResult(text, emptyList())
        assertEquals(expected, actual)
    }


}