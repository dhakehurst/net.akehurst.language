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
import kotlin.test.assertEquals

class test_RegexValueProvider {

    private companion object {
        fun test(pattern:String, anyChar:Char = 'X', expected:String) {
            println("Testing: $pattern")
            val provider = RegexValueProvider(pattern, anyChar)
            val actual = provider.provide()
            assertEquals(expected, actual)
            assertEquals(actual, provider.matcher.match(actual)?.matchedText )
        }
    }

    @Test
    fun empty() {
        val pattern = ""
        test(pattern, 'X', "")
    }

    @Test
    fun literal_a() {
        val pattern = "a"
        test(pattern, 'X', "a")
    }

    @Test
    fun characterClass_abcd() {
        val pattern = "[abcd]"
        test(pattern, 'X', "a")
    }

    @Test
    fun characterClass_Range() {
        val pattern = "[a-z]"
        test(pattern, 'X', "a")
    }

    @Test
    fun characterClass_minus() {
        test("[-]", 'X', "-")
        test("[+-]", 'X', "+")
        test("[-+]", 'X', "-")
        test("[a-b-]", 'X', "a")
    }

    @Test
    fun characterClass_many() {
        test("([a-zA-Z_][a-zA-Z_0-9-]*)", 'X', "a")
    }

    @Test
    fun negated() {
        test("[^a]", 'X', "A")
        test("[^A]", 'X', "B")
        test("[^abcd]", 'X', "A")
        test("[^a-zA-Z]", 'X', "0")
        test("[^a-zA-Z0-9]", 'X', "!")
    }

    @Test
    fun a2d_negated_concat() {
        test("[^a-d]x", 'X', "Ax")
    }

    @Test
    fun hex_abc() {
        test("[\\x61b\\x63]", 'X', "a")
    }

    @Test
    fun concatenation() {
        test("abc",'X',"abc")
    }

    @Test
    fun choice() {
        test("a|b",'X',"a")
    }

    @Test
    fun group() {
        test("(a)",'X',"a")
        test("(a)(b)",'X',"ab")
        test("(a)|(b)",'X',"a")
        test("ab|c",'X',"ab")
        test("(ab)|c",'X',"ab")
        test("a(b|c)",'X',"ab")
        test("(a|b)",'X',"a")
    }

    @Test
    fun multi0n() {
        test("a*",'X',"")
        test("a*b",'X',"b")
        test("a*b*c",'X',"c")
        test("a*b*c*d",'X',"d")
    }

    @Test
    fun multi1n() {
       test("a+",'X',"a")
       test("a+b",'X',"ab")
       test("a+b*c",'X',"ac")
       test("a*b+c",'X',"abc") //TODO: currently gives 'abc' (not 'bc') because shortest path not found
    }

    @Test
    fun repetition() {
        test("a{0}b",'X',"b")
        test("a{1}b",'X',"ab")
        test("a{2}b",'X',"aab")
        test("a{5}",'X',"aaaaa")
        test("a{3,}b",'X',"aaab")
        test("a{2,5}b",'X',"aab")
    }
}