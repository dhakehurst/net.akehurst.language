/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.regex.agl

import kotlin.test.Test
import kotlin.test.assertEquals

class test_RegexMatcher_length {

    private fun test(pattern: String, min: Int, max: Int) {
        val m = regexMatcher(pattern)
        assertEquals(min, m.minLength, "minLength of $pattern")
        assertEquals(max, m.maxLength, "maxLength of $pattern")
    }

    @Test fun literal() = test("abc", 3, 3)
    @Test fun charClass() = test("[a-z]", 1, 1)
    @Test fun choice() = test("a|bcd", 1, 3)
    @Test fun optional() = test("ab?", 1, 2)
    @Test fun star() = test("a*", 0, -1)
    @Test fun plus() = test("[0-9]+", 1, -1)
    @Test fun bounded() = test("a{2,4}", 2, 4)
    @Test fun lookahead() = test("a(?=b)", 1, 1)
}
