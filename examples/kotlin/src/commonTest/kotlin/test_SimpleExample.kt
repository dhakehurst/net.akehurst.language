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

package net.akehurst.language.examples.simple

import kotlin.test.Test
import kotlin.test.assertEquals


class test_SimpleExample {

    val text1 = """
         class class {
           property : String
           method(p1: Integer, p2: String) {
           }
         }
        """.trimIndent()

    @Test
    fun scan() {
        val result = SimpleExample.processor.scan(text1)
        println(result)

        val expected = listOf(
                Triple(0, "'class'", "class"),
                Triple(5, "\"\\s+\"", " "),
                Triple(6, "'class'", "class"),
                Triple(11, "\"\\s+\"", " "),
                Triple(12, "'{'", "{"),
                Triple(13, "\"\\s+\"", "\n  "),
                Triple(16, "\"[a-zA-Z_][a-zA-Z0-9_]*\"", "property"),
                Triple(24, "\"\\s+\"", " "),
                Triple(25, "':'", ":"),
                Triple(26, "\"\\s+\"", " "),
                Triple(27, "\"[a-zA-Z_][a-zA-Z0-9_]*\"", "String"),
                Triple(33, "\"\\s+\"", "\n  "),
                Triple(36, "\"[a-zA-Z_][a-zA-Z0-9_]*\"", "method"),
                Triple(42, "'('", "(")
        )

        //assertEquals(result.size, expected.size)
        result.take(14).forEachIndexed { i, leaf ->
            assertEquals(expected[i].first, leaf.startPosition)
            assertEquals(expected[i].second, leaf.name)
            assertEquals(expected[i].third, leaf.matchedText)
        }
    }

    @Test
    fun parse() {
        val result = SimpleExample.processor.parse("unit", text1)

        println("--- original text, from the parse tree ---")
        println(result.asString)

        println("--- the parse tree ---")
        println(result.toStringIndented("  "))

        //val expected =
    }

    @Test
    fun process() {
        val result: SimpleExampleUnit = SimpleExample.processor.process("unit", text1)
        println(result)
    }

    @Test
    fun format_ASM() {
        val result: SimpleExampleUnit = SimpleExample.processor.process("unit", text1)
        val formatted = SimpleExample.processor.formatAsm(result)
        print(formatted)
        assertEquals(text1, formatted)
    }

    @Test
    fun format_Text() {
        val unformattedText = "class class { property:String method(p1: Integer, p2: String) { } }"
        val formatted = SimpleExample.processor.formatTextForGoal<SimpleExampleUnit>("unit", unformattedText)
        print(formatted)
        assertEquals(text1, formatted)
    }
}