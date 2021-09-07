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
import net.akehurst.language.api.syntaxAnalyser.AsmSimple
import net.akehurst.language.api.syntaxAnalyser.asmSimple
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

        val proc = Agl.processorFromString(grammarStr, SyntaxAnalyserSimple())
        val actual = proc.process(AsmSimple::class, sentence).rootElements[0]

        val expected = asmSimple() {
            element("S") {
                property("'a'", "a")
            }
        }.rootElements[0]

        assertEquals(expected.asString("  "), actual.asString("  "))
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

        val proc = Agl.processorFromString(grammarStr, SyntaxAnalyserSimple())
        val actual = proc.process(AsmSimple::class, sentence).rootElements[0]

        val expected = asmSimple() {
            element("S") {
                property("ID", "a")
            }
        }.rootElements[0]

        assertEquals(expected.asString("  "), actual.asString("  "))
    }

    @Test
    fun patternChoice() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = ID ':' type ;
                type = 'int' | 'bool' | "[A-Z][a-z]*" ;
                leaf ID = "[a-z]" ;
            }
        """.trimIndent()

        val sentence = "a : A"

        val proc = Agl.processorFromString(grammarStr, SyntaxAnalyserSimple())
        val actual = proc.process(AsmSimple::class, sentence).rootElements[0]

        val expected = asmSimple() {
            element("S") {
                property("ID", "a")
                property("':'", ":")
                property("type", "A")
            }
        }.rootElements[0]

        assertEquals(expected.asString("  "), actual.asString("  "))
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

        val proc = Agl.processorFromString(grammarStr, SyntaxAnalyserSimple())
        val actual = proc.process(AsmSimple::class, sentence).rootElements[0]

        val expected = asmSimple() {
            element("S") {
                property("ID", "a")
                property("NUMBER", "8")
                property("NAME", "fred")
            }
        }.rootElements[0]

        assertEquals(expected.asString("  "), actual.asString("  "))
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

        val proc = Agl.processorFromString(grammarStr, SyntaxAnalyserSimple())
        val actual1 = proc.process(AsmSimple::class, "a 8").rootElements[0]
        val actual2 = proc.process(AsmSimple::class, "a fred").rootElements[0]
        val actual3 = proc.process(AsmSimple::class, "a fred 8").rootElements[0]

        val expected1 = asmSimple() {
            element("S") {
                property("ID", "a")
                property("item", "A") {
                    property("NUMBER", "8")
                }
            }
        }.rootElements[0]
        assertEquals(expected1.asString("  "), actual1.asString("  "))

        val expected2 = asmSimple() {
            element("S") {
                property("ID", "a")
                property("item", "B") {
                    property("NAME", "fred")
                }
            }
        }.rootElements[0]
        assertEquals(expected2.asString("  "), actual2.asString("  "))

        val expected3 = asmSimple() {
            element("S") {
                property("ID", "a")
                property("item", "C") {
                    property("NAME", "fred")
                    property("NUMBER", "8")
                }
            }
        }.rootElements[0]
        assertEquals(expected3.asString("  "), actual3.asString("  "))
    }

    @Test
    fun optional_full() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = ID NUMBER? NAME ;
                leaf ID = "[a-z]" ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()

        val sentence = "a 8 fred"

        val proc = Agl.processorFromString(grammarStr, SyntaxAnalyserSimple())
        val actual = proc.process(AsmSimple::class, sentence).rootElements[0]

        assertEquals("S", actual.typeName)
        assertEquals(3, actual.properties.size)
        assertEquals(true, actual.hasProperty("ID"))
        assertEquals("a", actual.getPropertyValue("ID"))
        assertEquals("8", actual.getPropertyValue("NUMBER"))
        assertEquals("fred", actual.getPropertyValue("NAME"))
    }

    @Test
    fun optional_empty() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = ID NUMBER? NAME ;
                leaf ID = "[a-z]" ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()

        val sentence = "a fred"

        val proc = Agl.processorFromString(grammarStr, SyntaxAnalyserSimple())
        val actual = proc.process(AsmSimple::class, sentence).rootElements[0]

        val expected = asmSimple() {
            element("S") {
                property("ID", "a")
                property("NUMBER", null)
                property("NAME", "fred")
            }
        }.rootElements[0]
        assertEquals(expected.asString("  "), actual.asString("  "))
    }

    @Test
    fun multi_empty() {
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

        val sentence = "a"

        val proc = Agl.processorFromString(grammarStr, SyntaxAnalyserSimple())
        val actual = proc.process(AsmSimple::class, sentence).rootElements[0]

        val expected = asmSimple() {
            element("S") {
                property("ID", "a")
                property("NAME", emptyList<String>())
            }
        }.rootElements[0]
        assertEquals(expected.asString("  "), actual.asString("  "))
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

        val proc = Agl.processorFromString(grammarStr, SyntaxAnalyserSimple())
        val actual = proc.process(AsmSimple::class, sentence).rootElements[0]

        val expected = asmSimple() {
            element("S") {
                property("ID", "a")
                property("NAME", listOf("adam", "betty", "charles"))
            }
        }.rootElements[0]
        assertEquals(expected.asString("  "), actual.asString("  "))
    }

    @Test
    fun slist() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                addressBook = ID contacts;
                contacts = [person / ',']* ;
                person = NAME NAME NUMBER ;
                leaf ID = "[a-zA-Z0-9]+" ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()

        val sentence = "bk1 adam ant 12345, betty boo 34567, charlie chaplin 98765"

        val proc = Agl.processorFromString(grammarStr, SyntaxAnalyserSimple())
        val actual = proc.process(AsmSimple::class, sentence).rootElements[0]
        val expected = asmSimple() {
            element("addressBook") {
                property("ID", "bk1")
                property("contacts", listOf(
                    element("person") {
                        property("NAME", "adam")
                        property("NAME2", "ant")
                        property("NUMBER", "12345")
                    },
                    ",",
                    element("person") {
                        property("NAME", "betty")
                        property("NAME2", "boo")
                        property("NUMBER", "34567")
                    },
                    ",",
                    element("person") {
                        property("NAME", "charlie")
                        property("NAME2", "chaplin")
                        property("NUMBER", "98765")
                    }
                ))
            }
        }.rootElements[0]
        assertEquals(expected.asString("  "), actual.asString("  "))
    }

    @Test
    fun slist2() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip leaf WHITESPACE = "\s+" ;
                attr_stmt = attr_type attr_lists ;
                attr_type = 'graph' | 'node' | 'edge' ;
                attr_lists = attr_list+ ;
                attr_list = '[' attr_list_content ']' ;
                attr_list_content = [ attr / a_list_sep ]* ;
                attr = ID '=' ID ;
                leaf a_list_sep = (';' | ',')? ;
                leaf ID = "[a-zA-Z_][a-zA-Z_0-9]+" ;
            }
        """.trimIndent()

        val sentence = "graph [fontsize=ss, labelloc=yy label=bb; splines=true overlap=false]"

        val proc = Agl.processorFromString(grammarStr, SyntaxAnalyserSimple())
        val actual = proc.processForGoal(AsmSimple::class, "attr_stmt", sentence).rootElements[0]

        val expected = asmSimple() {
            element("attr_stmt") {
                property("attr_type", "graph")
                property("attr_lists", listOf(
                        element("attr_list") {
                            property("'['", "[")
                            property("attr_list_content", listOf(
                                element("attr") {
                                    property("ID","fontsize")
                                    property("'='","=")
                                    property("ID2","ss")
                                },",",
                                element("attr") {
                                    property("ID","labelloc")
                                    property("'='","=")
                                    property("ID2","yy")
                                },"",
                                element("attr") {
                                    property("ID","label")
                                    property("'='","=")
                                    property("ID2","bb")
                                },";",
                                element("attr") {
                                    property("ID","splines")
                                    property("'='","=")
                                    property("ID2","true")
                                },"",
                                element("attr") {
                                    property("ID","overlap")
                                    property("'='","=")
                                    property("ID2","false")
                                }
                            ))
                            property("']'", "]")
                        },
                    )
                )
            }
        }.rootElements[0]
        assertEquals(expected.asString("  "), actual.asString("  "))
    }
}