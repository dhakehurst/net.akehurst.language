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
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.asm.asmSimple
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_SyntaxAnalyserSimple {

    private companion object {
        val grammarProc = Agl.registry.agl.grammar.processor ?: error("Internal error: AGL language processor not found")
    }

    @Test
    fun oneLiteral_ignored_because_not_a_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """.trimIndent()

        val sentence = "a"

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple, ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result2 = proc.process(sentence)
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual = result2.asm!!.rootElements[0]

        val expected = asmSimple() {
            root("S") {
            }
        }.rootElements[0]

        assertEquals(expected.asString("  "), actual.asString("  "))
    }

    @Test
    fun oneLiteral_as_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ;
                leaf a = 'a' ;
            }
        """.trimIndent()

        val sentence = "a"

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result2 = proc.process(sentence)
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual = result2.asm!!.rootElements[0]

        val expected = asmSimple() {
            root("S") {
                propertyString("a", "a")
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result2 = proc.process(sentence)
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual = result2.asm!!.rootElements[0]

        val expected = asmSimple() {
            root("S") {
                propertyString("ID", "a")
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result2 = proc.process(sentence)
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual = result2.asm!!.rootElements[0]

        val expected = asmSimple() {
            root("S") {
                propertyString("ID", "a")
                propertyElement("type") {
                    propertyUnnamedString( "A")
                }
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result2 = proc.process(sentence)
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual = result2.asm!!.rootElements[0]

        val expected = asmSimple() {
            root("S") {
                propertyString("ID", "a")
                propertyString("NUMBER", "8")
                propertyString("NAME", "fred")
            }
        }.rootElements[0]

        assertEquals(expected.asString("  "), actual.asString("  "))
    }

    @Test
    fun root_choice_twoLiteral() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' | 'b' ;
            }
        """.trimIndent()

        val sentence = "a"

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result2 = proc.process(sentence)
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual = result2.asm!!.rootElements[0]

        val expected = asmSimple() {
            root("S") {
                propertyUnnamedString("a")
            }
        }.rootElements[0]

        assertEquals(expected.asString("  "), actual.asString("  "))
    }

    @Test
    fun root_empty() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a'? ;
            }
        """.trimIndent()

        val sentence = ""

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result2 = proc.process(sentence)
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual = result2.asm!!.rootElements[0]

        val expected = asmSimple() {
            root("S") {
                propertyUnnamedString(null)
            }
        }.rootElements[0]

        assertEquals(expected.asString("  "), actual.asString("  "))
    }

    @Test
    fun root_multi_literals() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a'* ;
            }
        """.trimIndent()

        val sentence = "aaaa"

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
//        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result2 = proc.process(sentence)
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual = result2.asm!!.rootElements[0]

        val expected = asmSimple() {
            root("S") {
                propertyUnnamedListOfString(emptyList())
            }
        }.rootElements[0]

        assertEquals(expected.asString("  "), actual.asString("  "))
    }

    @Test
    fun root_multi_leafs() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a* ;
                leaf a = 'a' ;
            }
        """.trimIndent()

        val sentence = "aaaa"

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
//        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result2 = proc.process(sentence)
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual = result2.asm!!.rootElements[0]

        val expected = asmSimple() {
            root("S") {
                propertyListOfString("a",listOf("a","a","a","a"))
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result1 = proc.process("a 8")
        assertNotNull(result1.asm)
        assertEquals(emptyList(), result1.issues)
        val actual1 = result1.asm!!.rootElements[0]

        val expected1 = asmSimple() {
            root("S") {
                propertyString("ID", "a")
                propertyElement("item", "A") {
                    propertyString("NUMBER", "8")
                }
            }
        }.rootElements[0]
        assertEquals(expected1.asString("  "), actual1.asString("  "))

        val result2 = proc.process("a fred")
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual2 = result2.asm!!.rootElements[0]

        val expected2 = asmSimple() {
            root("S") {
                propertyString("ID", "a")
                propertyElement("item", "B") {
                    propertyString("NAME", "fred")
                }
            }
        }.rootElements[0]
        assertEquals(expected2.asString("  "), actual2.asString("  "))


        val result3 = proc.process("a fred 8")
        assertNotNull(result3.asm)
        assertEquals(emptyList(), result3.issues)
        val actual3 = result3.asm!!.rootElements[0]

        val expected3 = asmSimple() {
            root("S") {
                propertyString("ID", "a")
                propertyElement("item", "C") {
                    propertyString("NAME", "fred")
                    propertyString("NUMBER", "8")
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result2 = proc.process(sentence)
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual = result2.asm!!.rootElements[0]

        val expected = asmSimple() {
            root("S") {
                propertyString("ID","a")
                propertyString("NUMBER","8")
                propertyString("NAME","fred")
            }
        }.rootElements[0]

        assertEquals(expected.asString("  "), actual.asString("  "))
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result2 = proc.process(sentence)
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual = result2.asm!!.rootElements[0]

        val expected = asmSimple() {
            root("S") {
                propertyString("ID", "a")
                propertyString("NUMBER", null)
                propertyString("NAME", "fred")
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
//TODO        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result2 = proc.process(sentence)
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual = result2.asm!!.rootElements[0]

        val expected = asmSimple() {
            root("S") {
                propertyString("ID", "a")
                propertyListOfString("NAME", emptyList<String>())
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
//TODO        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result2 = proc.process(sentence)
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual = result2.asm!!.rootElements[0]

        val expected = asmSimple() {
            root("S") {
                propertyString("ID", "a")
                propertyListOfString("NAME", listOf("adam", "betty", "charles"))
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
//TODO        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result2 = proc.process(sentence)
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual = result2.asm!!.rootElements[0]

        val expected = asmSimple {
            root("addressBook") {
                propertyString("ID", "bk1")
                propertyListOfElement("contacts") {
                    element("person") {
                        propertyString("NAME", "adam")
                        propertyString("NAME2", "ant")
                        propertyString("NUMBER", "12345")
                    }
                    element("person") {
                        propertyString("NAME", "betty")
                        propertyString("NAME2", "boo")
                        propertyString("NUMBER", "34567")
                    }
                    element("person") {
                        propertyString("NAME", "charlie")
                        propertyString("NAME2", "chaplin")
                        propertyString("NUMBER", "98765")
                    }
                }
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result2 = proc.process(sentence)
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual = result2.asm!!.rootElements[0]

        val expected = asmSimple {
            root("attr_stmt") {
                propertyString("attr_type", "graph")
                propertyListOfElement("attr_lists") {
                    element("attr_list") {
                        propertyString("'['", "[")
                        propertyListOfElement("attr_list_content") {
                            element("attr") {
                                propertyString("ID", "fontsize")
                                propertyString("ID2", "ss")
                            }
                            string(",")
                            element("attr") {
                                propertyString("ID", "labelloc")
                                propertyString("ID2", "yy")
                            }
                            string("")
                            element("attr") {
                                propertyString("ID", "label")
                                propertyString("ID2", "bb")
                            }
                            string(";")
                            element("attr") {
                                propertyString("ID", "splines")
                                propertyString("ID2", "true")
                            }
                            string("")
                            element("attr") {
                                propertyString("ID", "overlap")
                                propertyString("ID2", "false")
                            }
                        }
                        propertyString("']'", "]")
                    }
                }
            }
        }.rootElements[0]
        assertEquals(expected.asString("  "), actual.asString("  "))
    }

    @Test
    fun group_concat_leaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b c d) e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result1 = proc.process("abcde")
        assertNotNull(result1.asm)
        assertEquals(emptyList(), result1.issues)
        val actual1 = result1.asm!!.rootElements[0]

        val expected1 = asmSimple() {
            root("S") {
                propertyString("a","a")
                propertyTuple("\$group"){
                    propertyString("b","b")
                    propertyString("c","c")
                    propertyString("d","d")
                }
                propertyString("e","e")
            }
        }.rootElements[0]
        assertEquals(expected1.asString("  "), actual1.asString("  "))
    }

    @Test
    fun group_choice_leaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b | c | d) e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result1 = proc.process("abe")
        assertNotNull(result1.asm)
        assertEquals(emptyList(), result1.issues)
        val actual1 = result1.asm!!.rootElements[0]

        val expected1 = asmSimple() {
            root("S") {
                propertyString("a","a")
                propertyTuple("\$group"){
                    propertyUnnamedString("b")
                }
                propertyString("e","e")
            }
        }.rootElements[0]
        assertEquals(expected1.asString("  "), actual1.asString("  "))
    }

    @Test
    fun group_choice_concat_leaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b c | d) e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result1 = proc.process("abce")
        assertNotNull(result1.asm)
        assertEquals(emptyList(), result1.issues)
        val actual1 = result1.asm!!.rootElements[0]

        val expected1 = asmSimple() {
            root("S") {
                propertyString("a","a")
                propertyTuple("\$group"){
                    propertyString("b","b")
                    propertyString("c","c")
                }
                propertyString("e","e")
            }
        }.rootElements[0]
        assertEquals(expected1.asString("  "), actual1.asString("  "))

        val result2 = proc.process("ade")
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual2 = result1.asm!!.rootElements[0]

        val expected2 = asmSimple() {
            root("S") {
                propertyString("a","a")
                propertyTuple("\$group"){
                    propertyUnnamedString("d")
                }
                propertyString("e","e")
            }
        }.rootElements[0]
        assertEquals(expected2.asString("  "), actual2.asString("  "))

    }

    @Test
    fun nesting() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = type ;
                type = NAME typeArgs? ;
                typeArgs = '<' typeArgList '>' ;
                typeArgList = [NAME , ',']+ ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm,result.issues.joinToString(separator = "\n") { "$it" })
        assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        val typeModel = TypeModelFromGrammar(result.asm!!.last())
        val proc = Agl.processorFromString<AsmSimple,ContextSimple>(grammarStr, Agl.configuration { syntaxAnalyserResolver{SyntaxAnalyserSimple(typeModel)} }, null )
        val result1 = proc.process("A")
        assertNotNull(result1.asm)
        assertEquals(emptyList(), result1.issues)
        val actual1 = result1.asm!!.rootElements[0]

        val expected1 = asmSimple() {
            root("S") {
                propertyString("ID", "a")
                propertyElement("item", "A") {
                    propertyString("NUMBER", "8")
                }
            }
        }.rootElements[0]
        assertEquals(expected1.asString("  "), actual1.asString("  "))

        val result2 = proc.process("a fred")
        assertNotNull(result2.asm)
        assertEquals(emptyList(), result2.issues)
        val actual2 = result2.asm!!.rootElements[0]

        val expected2 = asmSimple() {
            root("S") {
                propertyString("ID", "a")
                propertyElement("item", "B") {
                    propertyString("NAME", "fred")
                }
            }
        }.rootElements[0]
        assertEquals(expected2.asString("  "), actual2.asString("  "))


        val result3 = proc.process("a fred 8")
        assertNotNull(result3.asm)
        assertEquals(emptyList(), result3.issues)
        val actual3 = result3.asm!!.rootElements[0]

        val expected3 = asmSimple() {
            root("S") {
                propertyString("ID", "a")
                propertyElement("item", "C") {
                    propertyString("NAME", "fred")
                    propertyString("NUMBER", "8")
                }
            }
        }.rootElements[0]
        assertEquals(expected3.asString("  "), actual3.asString("  "))
    }
}