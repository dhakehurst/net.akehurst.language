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

package net.akehurst.language.agl.analyser

import net.akehurst.language.api.analyser.AsmElementSimple
import net.akehurst.language.processor.Agl
import kotlin.test.Test
import kotlin.test.assertEquals

class test_SyntaxAnalyserSimple {


    @Test
    fun oneLiteral() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """.trimIndent()

        val sentence = "a"

        val proc = Agl.processor(grammarStr, SyntaxAnalyserSimple())
        val actual = proc.process<AsmElementSimple>(sentence)

        assertEquals("S", actual.typeName)
        assertEquals(0, actual.properties.size)
    }

    @Test
    fun onePattern() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = ID ;
                leaf ID = "[a-z]" ;
            }
        """.trimIndent()

        val sentence = "a"

        val proc = Agl.processor(grammarStr, SyntaxAnalyserSimple())
        val actual = proc.process<AsmElementSimple>(sentence)

        assertEquals("S", actual.typeName)
        assertEquals(1, actual.properties.size)
        assertEquals(true, actual.hasProperty("ID"))
        assertEquals("a", actual.getPropertyValue("ID"))
    }


    @Test
    fun concatenation() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = ID NUMBER NAME ;
                leaf ID = "[a-z]" ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()

        val sentence = "a 8 fred"

        val proc = Agl.processor(grammarStr, SyntaxAnalyserSimple())
        val actual = proc.process<AsmElementSimple>(sentence)

        assertEquals("S", actual.typeName)
        assertEquals(3, actual.properties.size)
        assertEquals(true, actual.hasProperty("ID"))
        assertEquals("a", actual.getPropertyValue("ID"))
        assertEquals("8", actual.getPropertyValue("NUMBER"))
        assertEquals("fred", actual.getPropertyValue("NAME"))
    }

    @Test
    fun choice() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = ID item ;
                item = A | B | C ;
                A = NUMBER ;
                B = NAME ;
                C = NAME NUMBER ;
                leaf ID = "[a-z]" ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()

        val sentence1 = "a 8"
        val sentence2 = "a fred"
        val sentence3 = "a fred 8"

        val proc = Agl.processor(grammarStr, SyntaxAnalyserSimple())
        val actual1 = proc.process<AsmElementSimple>(sentence1)
        val actual2 = proc.process<AsmElementSimple>(sentence2)
        val actual3 = proc.process<AsmElementSimple>(sentence3)

        assertEquals("S", actual1.typeName)
        assertEquals(2, actual1.properties.size)
        assertEquals("a", actual1.getPropertyValue("ID"))
        assertEquals("A", actual1.getPropertyAstItem("item").typeName)
        assertEquals("8", actual1.getPropertyAstItem("item").getPropertyValue("NUMBER"))

        assertEquals("S", actual2.typeName)
        assertEquals(2, actual2.properties.size)
        assertEquals("a", actual2.getPropertyValue("ID"))
        assertEquals("B", actual2.getPropertyAstItem("item").typeName)
        assertEquals("fred", actual2.getPropertyAstItem("item").getPropertyValue("NAME"))

        assertEquals("S", actual3.typeName)
        assertEquals(2, actual3.properties.size)
        assertEquals("a", actual3.getPropertyValue("ID"))
        assertEquals("C", actual3.getPropertyAstItem("item").typeName)
        assertEquals("fred", actual3.getPropertyAstItem("item").getPropertyValue("NAME"))
        assertEquals("8", actual3.getPropertyAstItem("item").getPropertyValue("NUMBER"))
    }

    @Test
    fun multi() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = ID NAME* ;
                leaf ID = "[a-z]" ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()

        val sentence = "a adam betty charles"

        val proc = Agl.processor(grammarStr, SyntaxAnalyserSimple())
        val actual = proc.process<AsmElementSimple>(sentence)

        assertEquals("S", actual.typeName)
        assertEquals(2, actual.properties.size)
        assertEquals(true, actual.hasProperty("ID"))
        assertEquals("a", actual.getPropertyValue("ID"))
        assertEquals(true, actual.hasProperty("NAME"))
        assertEquals(3, (actual.getPropertyValue("NAME") as List<Any>).size)
    }
}