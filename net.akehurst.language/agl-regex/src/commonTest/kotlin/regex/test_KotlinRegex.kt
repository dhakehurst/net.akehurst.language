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

package net.akehurst.language.regex.agl

import kotlin.test.Test
import kotlin.test.assertTrue

class test_KotlinRegex {

    @Test
    fun doubleQuote() {
        val regex = Regex("\"")
        assertTrue( regex.matches("\"") )
    }

    @Test
    fun doubleQuote_characterclass() {
        val regex = Regex("[\"]")
        assertTrue( regex.matches("\"") )
    }

    @Test
    fun string_in_doubleQuote_characterclass() {
        val regex = Regex("\"[^\"]+\"")
        assertTrue( regex.matches("\"abcd\"") )
        assertTrue( regex.matches("\"[a-c]\"") )
    }

    @Test
    fun string_with_escapes_in_doubleQuote_characterclass() {
        val regex = Regex("\"([^\"\\\\]|\\\\.)+\"")
        assertTrue( regex.matches("\"abcd\"") )
        assertTrue( regex.matches("\"[a-c]\"") )
        assertTrue( regex.matches("\"\\b\\c\"") )
        assertTrue( regex.matches("\"(|\\\\.)\"") )
        assertTrue( regex.matches("\"\\\"\"") )
        assertTrue( regex.matches("\"[^\\\"\\\\]\"") )
        assertTrue( regex.matches("\"([^\\\"\\\\]|)\"") )
        assertTrue( regex.matches("\"([^\\\"\\\\]|\\\\.)\"") )
    }
}