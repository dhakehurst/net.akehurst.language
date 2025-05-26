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

class test_RegexMatcher_multi1n {

    private companion object {
        fun test(pattern:String, str:String) {
            val m = regexMatcher(pattern)
            val actual = m.match(str, 0)
            val expected = RegexMatcher.MatchResultAgl(str)
            assertEquals(expected, actual)
        }
    }

    @Test
    fun a1n() {
        test("a+", "a")
    }

    @Test
    fun a0n_b1n() {
        test("a*b+", "b")
        test("a*b+", "ab")
        test("a*b+", "aab")
        test("a*b+", "aabb")
    }

    @Test
    fun a0n_b1n_c() {
        test("a*b+c", "bc")
        test("a*b+c", "bbc")
        test("a*b+c", "aabc")
        test("a*b+c", "aabbc")
    }

}