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

package net.akehurst.language.processor.expectedAt

import net.akehurst.language.agl.automaton.AutomatonKind
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.parser.expectedTerminalsAt.test_aOpt
import kotlin.test.Test
import kotlin.test.assertEquals

class test_DataTypes {
    private data class Data(val sentence: String, val position: Int, val expected: List<String>)

    private companion object {
        val grammarStr = """
            namespace test
    
            grammar Test {
                skip WS = "\s+" ;
    
                unit = declaration* ;
                declaration = 'class' ID '{' property* '}' ;
                property = ID ':' typeReference ;
                typeReference = ID typeArguments? ;
                typeArguments = '<' [typeReference / ',']+ '>' ;
    
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
    
            }
        """.trimIndent()
        val goal = "unit"
        val processor = Agl.processor(grammarStr)

        val testData = listOf(
            Data("",0, listOf("class")),
            Data(" ",0, listOf("class")),
            Data(" ",1, listOf("class")),
            Data("class",0, listOf("class")),
            Data("class",5, listOf("ID")),
            Data("class ",5, listOf("ID")),
            Data("class ",6, listOf("ID")),
            Data("class A",5, listOf("ID")),
            Data("class A",6, listOf("ID")),
            Data("class A",7, listOf("{")),
            Data("class A ",7, listOf("{")),
            Data("class A ",8, listOf("{")),
            Data("class A {",9, listOf("ID","}")),
            Data("class A { ",10, listOf("ID","}")),
            Data("class A { p",11, listOf(":")),
            Data("class A { p ",12, listOf(":")),
            Data("class A { p: ",13, listOf("ID")),
            Data("class A { p: X",14, listOf("<", "ID", "}")),
            Data("class A { p: X<",15, listOf("ID")),
            Data("class A { p: X<Y",16, listOf("<", ",",">")),
            Data("class A { p: X<Y>",17, listOf("ID", "}")),
            Data("class A { p: X<Y> }",19, listOf("class")),
        )
    }


    @Test
    fun test() {
        for (data in testData) {
            val sentence = data.sentence
            val position = data.position

            val result = processor.expectedAt(goal, sentence, position, 1, AutomatonKind.LOOKAHEAD_1)
            val actual = result.map { it.text }
            val expected = data.expected
            assertEquals(expected, actual, data.toString())
        }
    }
}