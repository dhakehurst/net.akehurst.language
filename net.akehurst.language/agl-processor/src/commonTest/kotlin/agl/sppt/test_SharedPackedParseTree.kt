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

import net.akehurst.language.agl.processor.Agl
import kotlin.test.Test
import kotlin.test.assertEquals


class test_SharedPackedParseTree {

    @Test
    fun tokensByLine_a() {
        val proc = Agl.processorFromString("""
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                
                S = expr ;
                expr = VAR < infix ;
                infix = expr '+' expr ;
                VAR = "[a-z]+" ;
            }
        """.trimIndent())

        val sut = proc.parse("a".trimIndent())

        val actual = sut.tokensByLine(0)

        assertEquals("a", actual[0].matchedText)
    }

    @Test
    fun tokensByLine_eolx1() {
        val proc = Agl.processorFromString("""
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                
                S = expr ;
                expr = VAR < infix ;
                infix = expr '+' expr ;
                VAR = "[a-z]+" ;
            }
        """.trimIndent())

        val sut = proc.parse("""
            a + b
            + c
        """.trimIndent())

        val actual = listOf(
                sut.tokensByLine(0),
                sut.tokensByLine(1)
        )

        assertEquals("a + b\n", actual[0].map { it.matchedText }.joinToString(""))
        assertEquals(1, actual[0][0].location.line)
        assertEquals(2, actual[1][0].location.line)
    }

    @Test
    fun tokensByLine_eolx1_indent() {
        val proc = Agl.processorFromString("""
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                
                S = expr ;
                expr = VAR < infix ;
                infix = expr '+' expr ;
                VAR = "[a-z]+" ;
            }
        """.trimIndent())

        val sut = proc.parse("""
            a + b
              + c
        """.trimIndent())

        val actual = listOf(
                sut.tokensByLine(0),
                sut.tokensByLine(1)
        )

        assertEquals("a + b\n", actual[0].map { it.matchedText }.joinToString(""))
        assertEquals(1, actual[0][0].location.line)
        assertEquals(2, actual[1][0].location.line)
        assertEquals("  ", actual[1][0].matchedText)
    }

    @Test
    fun tokensByLine_eolx2() {
        val proc = Agl.processorFromString("""
            namespace test

            grammar Test {
                skip WS = "\s+" ;

                declaration = 'class' ID '{' property* '}' ;
                property = ID ':' typeReference ;
                typeReference = ID typeArguments? ;
                typeArguments = '<' [typeReference / ',']+ '>' ;

                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;

            }
        """.trimIndent())

        val text = """
class XXX {
    
    prop1 : String
    prop2 : Integer
    propList : List<String>
}
        """
        val text2 = text.trimStart()
        val sut = proc.parse(text2)

        val actual = listOf(
                sut.tokensByLine(0),
                sut.tokensByLine(1),
                sut.tokensByLine(2)
        )

        assertEquals("class XXX {\n", actual[0].map { it.matchedText }.joinToString(""))
        assertEquals(1, actual[0][0].location.line)
        assertEquals(2, actual[1][0].location.line)
        assertEquals("    \n", actual[1][0].matchedText)
        assertEquals(3, actual[2][0].location.line)
        assertEquals("    ", actual[2][0].matchedText)
        assertEquals("prop1", actual[2][1].matchedText)
    }



}