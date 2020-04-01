/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.processor

import net.akehurst.language.api.analyser.AsmElementSimple
import net.akehurst.language.api.parser.ParseFailedException
import kotlin.test.*

internal class test_Agl {

    @Test
    fun parser_grammarDefinitionStr1() {
        val grammarStr = """
            namespace test
            grammar Test {
              a = 'a';
            }
        """.trimIndent()
        val p = Agl.processor(grammarStr)
        p.parse("a", "a")
    }

    @Test
    fun parser_grammarDefinitionStr2_1() {
        val grammarStr = """
            namespace test
            grammar Test {
              a = 'a';
              b = 'b';
            }
        """.trimIndent()
        val p = Agl.processor(grammarStr)
        p.parse("a", "a")
    }

    @Test
    fun parser_grammarDefinitionStr2_b() {
        val grammarStr = """
            namespace test
            grammar Test {
              a = 'a';
              b = 'b';
            }
        """.trimIndent()
        val p = Agl.processor(grammarStr)
        p.parse("b", "b")
    }

    @Test
    fun parser_rules_List() {
        val p = Agl.processor(listOf("a = 'a';"))
        val pt = p.parse("a", "a")

        assertNotNull(pt)
    }

    @Test
    fun parser_rules_List_failAt_0() {
        val e = assertFailsWith(ParseFailedException::class) {
            val p = Agl.processor(listOf("!"))
            //p.parse("a", "a")
        }
        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun parser_rules_List_failAt_1() {
        val e = assertFailsWith(ParseFailedException::class) {
            val p = Agl.processor(listOf("a!"))
            p.parse("a", "a")
        }
        assertEquals(1, e.location.line)
        assertEquals(1, e.location.column)
    }

    @Test
    fun parser_rules_List_failAt_7() {
        val e = assertFailsWith(ParseFailedException::class) {
            val p = Agl.processor(listOf("a = 'a'1..6;"))
            p.parse("a", "aaaaaaa")
        }
        assertEquals(1, e.location.line)
        assertEquals(7, e.location.column)
    }


    @Test
    fun process() {
        val grammarStr = """
            namespace test

            import test.A

            grammar test {
              a:A = v="[a-z]" {this.value=v} ;
            }
        """.trimIndent()
        val sentence = "a"
        val myProcessor = Agl.processor(grammarStr)
        val asm = myProcessor.process<AsmElementSimple>(sentence)
    }

}