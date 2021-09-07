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

package net.akehurst.language.agl.syntaxAnalyser

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.syntaxAnalyser.AsmElementSimple
import net.akehurst.language.api.syntaxAnalyser.AsmSimple
import net.akehurst.language.api.syntaxAnalyser.asmSimple
import kotlin.test.Test
import kotlin.test.assertEquals

class test_SyntaxAnalyserSimple_datatypes {

    companion object {
        val grammarStr = """
            namespace test
            
            grammar Test {
                skip WS = "\s+" ;
            
                unit = declaration* ;
                declaration = 'datatype' ID '{' property* '}' ;
                property = ID ':' typeReference ;
                typeReference = ID typeArguments? ;
                typeArguments = '<' [typeReference / ',']+ '>' ;
            
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
            
            }
        """.trimIndent()
        val processor = Agl.processorFromString(grammarStr)
    }


    @Test
    fun dt1() {
        val sentence = """
            datatype A { }
        """.trimIndent()

        val actual = processor.process(AsmSimple::class,sentence)

        val expected = asmSimple {
            element("unit") {
                property("declaration", listOf(
                    element("declaration") {
                        property("ID", "A")
                        property("property", emptyList<AsmElementSimple>())
                    }
                ))
            }
        }

        assertEquals(expected.asString("  ",""), actual.asString("  ",""))
    }

    @Test
    fun dt2() {
        val sentence = """
            datatype A { }
            datatype B { }
        """.trimIndent()

        val actual = processor.process(AsmSimple::class,sentence)

        val expected = asmSimple {
            element("unit") {
                property("declaration", listOf(
                    element("datatype") {
                        property("ID", "A")
                        property("property", emptyList<AsmElementSimple>())
                    },
                    element("datatype") {
                        property("ID", "B")
                        property("property", emptyList<AsmElementSimple>())
                    }
                ))
            }
        }

        assertEquals(expected.asString("  ",""), actual.asString("  ",""))
    }

    @Test
    fun prop1() {
        val sentence = """
            datatype A {
                a : String
            }
        """.trimIndent()

        val actual = processor.process(AsmSimple::class,sentence)

        val expected = asmSimple {
            element("unit") {
                property("declaration", listOf(
                    element("declaration") {
                        property("ID", "A")
                        property("property", emptyList<AsmElementSimple>())
                    }
                ))
            }
        }

        assertEquals(expected.asString("  ",""), actual.asString("  ",""))
    }
}