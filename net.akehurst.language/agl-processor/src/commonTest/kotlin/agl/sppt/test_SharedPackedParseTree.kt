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

package net.akehurst.language.agl.sppt

import net.akehurst.language.agl.agl.parser.SentenceDefault
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.sppt.LeafData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class test_SharedPackedParseTree {

    @Test
    fun tokensByLine_a() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                
                S = expr ;
                expr = VAR < infix ;
                infix = expr '+' expr ;
                VAR = "[a-z]+" ;
            }
        """.trimIndent()
        val pr = Agl.processorFromString<Any, Any>(grammarStr)

        val text = "a"
        val result = pr.processor!!.parse(text)

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        val actual = result.sppt!!.tokensByLine(0)

        assertEquals("a", actual[0].matchedText(SentenceDefault(text)))
    }

    @Test
    fun tokensByLine_eolx1() {
        val pr = Agl.processorFromString<Any, Any>(
            """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                
                S = expr ;
                expr = VAR < infix ;
                infix = expr '+' expr ;
                leaf VAR = "[a-z]+" ;
            }
        """.trimIndent()
        )

        val text = """
            a + b
            + c
        """.trimIndent()
        val result = pr.processor!!.parse(text)

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        val actual = listOf(
            result.sppt!!.tokensByLine(0),
            result.sppt!!.tokensByLine(1)
        )

        val expected_1 = listOf(
            LeafData("VAR", true, 0, 1, listOf("S", "expr", "infix", "expr", "infix", "expr", "VAR")),
            LeafData("WS", true, 1, 1, listOf("S", "expr", "infix", "expr", "infix", "expr", "WS")),
            LeafData("'+'", false, 2, 1, listOf("S", "expr", "infix", "expr", "infix", "'+'")),
            LeafData("WS", true, 3, 1, listOf("S", "expr", "infix", "expr", "infix", "WS")),
            LeafData("VAR", true, 4, 1, listOf("S", "expr", "infix", "expr", "infix", "expr", "VAR")),
            LeafData("WS", true, 5, 1, listOf("S", "expr", "infix", "expr", "infix", "expr", "WS")),
        )
        val expected_2 = listOf(
            LeafData("'+'", false, 6, 1, listOf("S", "expr", "infix", "'+'")),
            LeafData("WS", true, 7, 1, listOf("S", "expr", "infix", "WS")),
            LeafData("VAR", true, 8, 1, listOf("S", "expr", "infix", "expr", "VAR")),
        )


        assertEquals(expected_1, actual[0])
        assertEquals(expected_2, actual[1])
    }

    @Test
    fun tokensByLine_eolx1_indent() {
        val pr = Agl.processorFromString<Any, Any>(
            """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                
                S = expr ;
                expr = VAR < infix ;
                infix = expr '+' expr ;
                VAR = "[a-z]+" ;
            }
        """.trimIndent()
        )

        val sentence = SentenceDefault(
            """
            a + b
              + c
        """.trimIndent()
        )
        val result = pr.processor!!.parse(sentence.text)

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        val actual = listOf(
            result.sppt!!.tokensByLine(0),
            result.sppt!!.tokensByLine(1)
        )

        assertEquals("a + b\n", actual[0].map { it.matchedText(sentence) }.joinToString(""))
//        assertEquals(1, actual[0][0].location.line)
//        assertEquals(2, actual[1][0].location.line)
        assertEquals("  ", actual[1][0].matchedText(sentence))
    }

    @Test
    fun tokensByLine_eolx2() {
        val pr = Agl.processorFromString<Any, Any>(
            """
            namespace test

            grammar Test {
                skip WS = "\s+" ;

                declaration = 'class' ID '{' property* '}' ;
                property = ID ':' typeReference ;
                typeReference = ID typeArguments? ;
                typeArguments = '<' [typeReference / ',']+ '>' ;

                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;

            }
        """.trimIndent()
        )

        val text = """
class XXX {
    
    prop1 : String
    prop2 : Integer
    propList : List<String>
}
        """
        val text2 = text.trimStart()
        val result = pr.processor!!.parse(text2)
        val sentence = SentenceDefault(text2)

        assertNotNull(result.sppt)
        assertTrue(result.issues.isEmpty())
        val actual = listOf(
            result.sppt!!.tokensByLine(0),
            result.sppt!!.tokensByLine(1),
            result.sppt!!.tokensByLine(2)
        )

        assertEquals("class XXX {\n", actual[0].map { it.matchedText(sentence) }.joinToString(""))
//        assertEquals(1, actual[0][0].location.line)
//        assertEquals(2, actual[1][0].location.line)
//        assertEquals("    \n", actual[1][0].matchedText(sentence))
//        assertEquals(3, actual[2][0].location.line)
        assertEquals("    ", actual[2][0].matchedText(sentence))
        assertEquals("prop1", actual[2][1].matchedText(sentence))
    }


}