/**
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.regex.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class test_CommonRegexPatterns {

    @Test
    fun PATTERN__simple() {
        val str = """
            abc
        """.trimIndent()
        val actual = Regex(CommonRegexPatterns.PATTERN).matchEntire(str)

        assertNotNull(actual)
        assertEquals("abc", actual.value)
    }

    @Test
    fun PATTERN__charclass() {
        val str = """
            [a-z]
        """.trimIndent()
        val actual = Regex(CommonRegexPatterns.PATTERN).matchEntire(str)

        assertNotNull(actual)
        assertEquals("[a-z]", actual.value)
    }

    @Test
    fun PATTERN__backslash() {
        val str = """
            \
        """.trimIndent()
        val actual = Regex(CommonRegexPatterns.PATTERN).matchEntire(str)

        assertNotNull(actual)
        assertEquals("\\", actual.value)
    }

    @Test
    fun PATTERN__backslash_in_charclass() {
        val str = """
            [\\]
        """.trimIndent()
        val actual = Regex(CommonRegexPatterns.PATTERN).matchEntire(str)

        assertNotNull(actual)
        assertEquals("[\\\\]", actual.value)
    }

    @Test
    fun PATTERN__doublequote() {
        val str = """
            "
        """.trimIndent()
        val actual = Regex(CommonRegexPatterns.PATTERN).matchEntire(str)

        assertNotNull(actual)
        assertEquals("\"", actual.value)
    }

    @Test
    fun LITERAL__simple() {
        val str = """
            abc
        """.trimIndent()
        val actual = Regex(CommonRegexPatterns.LITERAL).matchEntire(str)

        assertNotNull(actual)
        assertEquals("abc", actual.value)
    }

    @Test
    fun LITERAL__backslash() {
        val str = """
            \\
        """.trimIndent()
        val actual = Regex(CommonRegexPatterns.LITERAL).matchEntire(str)

        assertNotNull(actual)
        assertEquals("\\\\", actual.value)
    }

    @Test
    fun LITERAL__backslash_singlequote() {
        val str = """
            \'
        """.trimIndent()
        val actual = Regex(CommonRegexPatterns.LITERAL).matchEntire(str)

        assertNotNull(actual)
        assertEquals("\\'", actual.value)
    }

    @Test
    fun LITERAL__singlequote__fails() {
        val str = """
            '
        """.trimIndent()
        val actual = Regex(CommonRegexPatterns.LITERAL).matchEntire(str)

        assertNull(actual)
    }

    @Test
    fun LITERAL__chars_singlequote__fails() {
        val str = """
            abc'
        """.trimIndent()
        val actual = Regex(CommonRegexPatterns.LITERAL).matchEntire(str)

        assertNull(actual)
    }
}