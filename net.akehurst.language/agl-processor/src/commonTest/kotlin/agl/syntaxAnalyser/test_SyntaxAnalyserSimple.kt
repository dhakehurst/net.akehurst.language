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
        val actual = proc.process(AsmElementSimple::class,sentence)

        assertEquals("S", actual.typeName)
        assertEquals(1, actual.properties.size)
        assertEquals("a", actual.getPropertyValue("'a'"))
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
        val actual = proc.process(AsmElementSimple::class,sentence)

        assertEquals("S", actual.typeName)
        assertEquals(1, actual.properties.size)
        assertEquals(true, actual.hasProperty("ID"))
        assertEquals("a", actual.getPropertyValue("ID"))
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
        val actual = proc.process<AsmElementSimple>(AsmElementSimple::class,sentence)

        assertEquals("S", actual.typeName)
        assertEquals(3, actual.properties.size)
        assertEquals(true, actual.hasProperty("ID"))
        assertEquals("a", actual.getPropertyValue("ID"))
        assertEquals(true, actual.hasProperty("type"))
        assertEquals("A", actual.getPropertyValue("type"))
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
        val actual = proc.process<AsmElementSimple>(AsmElementSimple::class,sentence)

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

        val proc = Agl.processorFromString(grammarStr, SyntaxAnalyserSimple())
        val actual1 = proc.process<AsmElementSimple>(AsmElementSimple::class,sentence1)
        val actual2 = proc.process<AsmElementSimple>(AsmElementSimple::class,sentence2)
        val actual3 = proc.process<AsmElementSimple>(AsmElementSimple::class,sentence3)

        assertEquals("S", actual1.typeName)
        assertEquals(2, actual1.properties.size)
        assertEquals("a", actual1.getPropertyValue("ID"))
        assertEquals("A", actual1.getPropertyAsAsmElement("item").typeName)
        assertEquals("8", actual1.getPropertyAsAsmElement("item").getPropertyValue("NUMBER"))

        assertEquals("S", actual2.typeName)
        assertEquals(2, actual2.properties.size)
        assertEquals("a", actual2.getPropertyValue("ID"))
        assertEquals("B", actual2.getPropertyAsAsmElement("item").typeName)
        assertEquals("fred", actual2.getPropertyAsAsmElement("item").getPropertyValue("NAME"))

        assertEquals("S", actual3.typeName)
        assertEquals(2, actual3.properties.size)
        assertEquals("a", actual3.getPropertyValue("ID"))
        assertEquals("C", actual3.getPropertyAsAsmElement("item").typeName)
        assertEquals("fred", actual3.getPropertyAsAsmElement("item").getPropertyValue("NAME"))
        assertEquals("8", actual3.getPropertyAsAsmElement("item").getPropertyValue("NUMBER"))
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
        val actual = proc.process<AsmElementSimple>(AsmElementSimple::class,sentence)

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
        val actual = proc.process<AsmElementSimple>(AsmElementSimple::class,sentence)

        assertEquals("S", actual.typeName)
        assertEquals(3, actual.properties.size)
        assertEquals(true, actual.hasProperty("ID"))
        assertEquals("a", actual.getPropertyValue("ID"))
        assertEquals(null, actual.getPropertyValue("NUMBER"))
        assertEquals("fred", actual.getPropertyValue("NAME"))
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
        val actual = proc.process<AsmElementSimple>(AsmElementSimple::class,sentence)

        assertEquals("S", actual.typeName)
        assertEquals(2, actual.properties.size)
        assertEquals(true, actual.hasProperty("ID"))
        assertEquals("a", actual.getPropertyValue("ID"))
        assertEquals(true, actual.hasProperty("NAME"))
        assertEquals(0, (actual.getPropertyValue("NAME") as List<Any>).size)
        assertEquals(emptyList<String>(), actual.getPropertyValue("NAME"))
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
        val actual = proc.process<AsmElementSimple>(AsmElementSimple::class,sentence)

        assertEquals("S", actual.typeName)
        assertEquals(2, actual.properties.size)
        assertEquals(true, actual.hasProperty("ID"))
        assertEquals("a", actual.getPropertyValue("ID"))
        assertEquals(true, actual.hasProperty("NAME"))
        assertEquals(3, (actual.getPropertyValue("NAME") as List<Any>).size)
        assertEquals(listOf("adam", "betty", "charles"), actual.getPropertyValue("NAME"))
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
        val actual = proc.process<AsmElementSimple>(AsmElementSimple::class,sentence)

        assertEquals("addressBook", actual.typeName)
        assertEquals(2, actual.properties.size)
        assertEquals(true, actual.hasProperty("ID"))
        assertEquals("bk1", actual.getPropertyValue("ID"))
        assertEquals(true, actual.hasProperty("contacts"))
        assertEquals(5, (actual.getPropertyValue("contacts") as List<Any>).size)
        val list = (actual.getPropertyValue("contacts") as List<Any>)
        val actual0 = list[0] as AsmElementSimple
        assertEquals(true, actual0.hasProperty("NAME"))
        assertEquals("adam", actual0.getPropertyValue("NAME"))
        assertEquals(true, actual0.hasProperty("NAME2"))
        assertEquals("ant", actual0.getPropertyValue("NAME2"))
        assertEquals(true, actual0.hasProperty("NUMBER"))
        assertEquals("12345", actual0.getPropertyValue("NUMBER"))
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
                a_list_sep = (';' | ',')? ;
                ID = "[a-zA-Z_][a-zA-Z_0-9]+" ;
            }
        """.trimIndent()

        val sentence = "graph [fontsize=ss labelloc=yy label=bb splines=true overlap=false]"

        val proc = Agl.processorFromString(grammarStr, SyntaxAnalyserSimple())
        val actual = proc.processForGoal(AsmElementSimple::class,"attr_stmt", sentence)

        assertEquals("addressBook", actual.typeName)
        assertEquals(2, actual.properties.size)
        assertEquals(true, actual.hasProperty("ID"))
        assertEquals("bk1", actual.getPropertyValue("ID"))
        assertEquals(true, actual.hasProperty("contacts"))
        assertEquals(5, (actual.getPropertyValue("contacts") as List<Any>).size)
        val list = (actual.getPropertyValue("contacts") as List<Any>)
        val actual0 = list[0] as AsmElementSimple
        assertEquals(true, actual0.hasProperty("NAME"))
        assertEquals("adam", actual0.getPropertyValue("NAME"))
        assertEquals(true, actual0.hasProperty("NAME2"))
        assertEquals("ant", actual0.getPropertyValue("NAME2"))
        assertEquals(true, actual0.hasProperty("NUMBER"))
        assertEquals("12345", actual0.getPropertyValue("NUMBER"))
    }
}