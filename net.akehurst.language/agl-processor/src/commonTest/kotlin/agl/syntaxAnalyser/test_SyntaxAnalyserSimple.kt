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
import net.akehurst.language.api.processor.LanguageProcessor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TestData(
    val sentence: String,
    val expected: AsmSimple
)

class test_SyntaxAnalyserSimple {

    private companion object {
        val grammarProc = Agl.registry.agl.grammar.processor ?: error("Internal error: AGL language processor not found")

        fun processor(grammarStr: String) = Agl.processorFromStringDefault(grammarStr)

        fun testProc(grammarStr: String): LanguageProcessor<AsmSimple, ContextSimple> {
            val result = processor(grammarStr)
            assertNotNull(result.processor, result.issues.toString())
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
            return result.processor!!
        }

        fun MutableList<TestData>.define(sentence: String, expected: () -> AsmSimple) = this.add(TestData(sentence, expected()))

        fun test(proc: LanguageProcessor<AsmSimple, ContextSimple>, data: TestData) {
            println(data.sentence)
            val result = proc.process(data.sentence)
            assertNotNull(result.asm)
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
            val actual = result.asm!!

            assertEquals(data.expected.asString("  "), actual.asString("  "))
        }
    }

    @Test
    fun oneLiteral_ignored_because_not_a_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val sentence = "a"
        val expected = asmSimple {
            element("S") {
            }
        }
        test(proc, TestData(sentence, expected))
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
        val proc = testProc(grammarStr)

        val sentence = "a"
        val expected = asmSimple {
            element("S") {
                propertyString("a", "a")
            }
        }
        test(proc, TestData(sentence, expected))
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
        val proc = testProc(grammarStr)

        val sentence = "a"
        val expected = asmSimple {
            element("S") {
                propertyString("id", "a")
            }
        }
        test(proc, TestData(sentence, expected))
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
        val proc = testProc(grammarStr)

        val sentence = "a : A"
        val expected = asmSimple {
            element("S") {
                propertyString("id", "a")
                propertyString("type", "A")
            }
        }
        test(proc, TestData(sentence, expected))
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
        val proc = testProc(grammarStr)

        val sentence = "a 8 fred"
        val expected = asmSimple {
            element("S") {
                propertyString("id", "a")
                propertyString("number", "8")
                propertyString("name", "fred")
            }
        }
        test(proc, TestData(sentence, expected))
    }

    @Test
    fun root_choice_twoLiteral() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' | 'b' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val sentence = "a"
        val expected = asmSimple {
            string("a")
        }
        test(proc, TestData(sentence, expected))
    }

    @Test
    fun root_empty() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a'? ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val sentence = ""
        val expected = asmSimple {
            string("<null>")
        }
        test(proc, TestData(sentence, expected))
    }

    @Test
    fun root_multi_literals() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a'* ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val sentence = "aaaa"
        val expected = asmSimple {
            element("S") {

            }
        }
        test(proc, TestData(sentence, expected))
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
        val proc = testProc(grammarStr)

        val sentence = "aaaa"
        val expected = asmSimple {
            element("S") {
                propertyListOfString("a", listOf("a", "a", "a", "a"))
            }
        }
        test(proc, TestData(sentence, expected))
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
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("a 8") {
            asmSimple {
                element("S") {
                    propertyString("id", "a")
                    propertyElementExplicitType("item", "A") {
                        propertyString("number", "8")
                    }
                }
            }
        }
        tests.define("a fred") {
            asmSimple {
                element("S") {
                    propertyString("id", "a")
                    propertyElementExplicitType("item", "B") {
                        propertyString("name", "fred")
                    }
                }
            }
        }
        tests.define("a fred 8") {
            asmSimple {
                element("S") {
                    propertyString("id", "a")
                    propertyElementExplicitType("item", "C") {
                        propertyString("name", "fred")
                        propertyString("number", "8")
                    }
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
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
        val proc = testProc(grammarStr)

        val sentence = "a 8 fred"
        val expected = asmSimple {
            element("S") {
                propertyString("id", "a")
                propertyString("number", "8")
                propertyString("name", "fred")
            }
        }
        test(proc, TestData(sentence, expected))
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
        val proc = testProc(grammarStr)

        val sentence = "a fred"
        val expected = asmSimple {
            element("S") {
                propertyString("id", "a")
                propertyString("number", null)
                propertyString("name", "fred")
            }
        }
        test(proc, TestData(sentence, expected))
    }

    @Test
    fun list_empty() {
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
        val proc = testProc(grammarStr)

        val sentence = "a"
        val expected = asmSimple {
            element("S") {
                propertyString("id", "a")
                propertyListOfString("name", emptyList<String>())
            }
        }
        test(proc, TestData(sentence, expected))
    }

    @Test
    fun list() {
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
        val proc = testProc(grammarStr)

        val sentence = "a adam betty charles"
        val expected = asmSimple {
            element("S") {
                propertyString("id", "a")
                propertyListOfString("name", listOf("adam", "betty", "charles"))
            }
        }
        test(proc, TestData(sentence, expected))
    }

    @Test
    fun expressions_infix() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = exprList ;
                exprList = expr (';' expr)* ;
                expr = root | mul | add ;
                root = var | literal ;
                mul = expr '*' expr ;
                add = expr '+' expr ;
                var = NAME ;
                literal = NUMBER ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("v") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("exprList", "ExprList") {
                        propertyElementExplicitType("expr", "Var") {
                            propertyString("name", "v")
                        }
                        propertyUnnamedListOfElement() {
                        }
                    }
                }
            }
        }
        tests.define("v+w") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("exprList", "ExprList") {
                        propertyElementExplicitType("expr", "Add") {
                            propertyElementExplicitType("expr", "Var") {
                                propertyString("name", "v")
                            }
                            propertyElementExplicitType("expr2", "Var") {
                                propertyString("name", "w")
                            }
                        }
                        propertyUnnamedListOfElement() {
                        }
                    }
                }
            }
        }
        tests.define("v*w") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("exprList", "ExprList") {
                        propertyElementExplicitType("expr", "Mul") {
                            propertyElementExplicitType("expr", "Var") {
                                propertyString("name", "v")
                            }
                            propertyElementExplicitType("expr2", "Var") {
                                propertyString("name", "w")
                            }
                        }
                        propertyUnnamedListOfElement { }
                    }
                }
            }
        }
        tests.define("v*w+x*y+z") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("exprList", "ExprList") {
                        propertyElementExplicitType("expr", "Add") {
                            propertyElementExplicitType("expr", "Add") {
                                propertyElementExplicitType("expr", "Mul") {
                                    propertyElementExplicitType("expr", "Var") {
                                        propertyString("name", "v")
                                    }
                                    propertyElementExplicitType("expr2", "Var") {
                                        propertyString("name", "w")
                                    }
                                }
                                propertyElementExplicitType("expr2", "Mul") {
                                    propertyElementExplicitType("expr", "Var") {
                                        propertyString("name", "x")
                                    }
                                    propertyElementExplicitType("expr2", "Var") {
                                        propertyString("name", "y")
                                    }
                                }
                            }
                            propertyElementExplicitType("expr2", "Var") {
                                propertyString("name", "z")
                            }
                        }
                        propertyUnnamedListOfElement { }
                    }
                }
            }
        }
        tests.define("v;w") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("exprList", "ExprList") {
                        propertyElementExplicitType("expr", "Var") {
                            propertyString("name", "v")
                        }
                        propertyUnnamedListOfElement {
                            tuple {
                                propertyElementExplicitType("expr", "Var") {
                                    propertyString("name", "w")
                                }
                            }
                        }
                    }
                }
            }
        }
        tests.define("v;w;x") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("exprList", "ExprList") {
                        propertyElementExplicitType("expr", "Var") {
                            propertyString("name", "v")
                        }
                        propertyUnnamedListOfElement {
                            tuple {
                                propertyElementExplicitType("expr", "Var") {
                                    propertyString("name", "w")
                                }
                            }
                            tuple {
                                propertyElementExplicitType("expr", "Var") {
                                    propertyString("name", "x")
                                }
                            }
                        }
                    }
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test
    fun expressions_sepList() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = exprList ;
                exprList = expr (';' expr)* ;
                expr = root | mul | add ;
                root = var | literal ;
                mul = [expr / '*']2+ ;
                add = [expr / '+']2+ ;
                var = NAME ;
                literal = NUMBER ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("v") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("exprList", "ExprList") {
                        propertyElementExplicitType("expr", "Var") {
                            propertyString("name", "v")
                        }
                        propertyUnnamedListOfElement() {
                        }
                    }
                }
            }
        }
        tests.define("v+w") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("exprList", "ExprList") {
                        propertyElementExplicitType("expr", "Add") {
                            propertyListOfElement("expr") {
                                element("Var") {
                                    propertyString("name", "v")
                                }
                                element("Var") {
                                    propertyString("name", "w")
                                }
                            }
                        }
                        propertyUnnamedListOfElement() {
                        }
                    }
                }
            }
        }
        tests.define("v*w") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("exprList", "ExprList") {
                        propertyElementExplicitType("expr", "Mul") {
                            propertyListOfElement("expr") {
                                element("Var") {
                                    propertyString("name", "v")
                                }
                                element("Var") {
                                    propertyString("name", "w")
                                }
                            }
                        }
                        propertyUnnamedListOfElement { }
                    }
                }
            }
        }
        tests.define("v*w+x*y+z") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("exprList", "ExprList") {
                        propertyElementExplicitType("expr", "Add") {
                            propertyListOfElement("expr") {
                                element("Mul") {
                                    propertyListOfElement("expr") {
                                        element("Var") {
                                            propertyString("name", "v")
                                        }
                                        element("Var") {
                                            propertyString("name", "w")
                                        }
                                    }
                                }
                                element("Mul") {
                                    propertyListOfElement("expr") {
                                        element("Var") {
                                            propertyString("name", "x")
                                        }
                                        element("Var") {
                                            propertyString("name", "y")
                                        }
                                    }
                                }
                                element("Var") {
                                    propertyString("name", "z")
                                }
                            }
                        }
                        propertyUnnamedListOfElement { }
                    }
                }
            }
        }
        tests.define("v;w") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("exprList", "ExprList") {
                        propertyElementExplicitType("expr", "Var") {
                            propertyString("name", "v")
                        }
                        propertyUnnamedListOfElement {
                            tuple {
                                propertyElementExplicitType("expr", "Var") {
                                    propertyString("name", "w")
                                }
                            }
                        }
                    }
                }
            }
        }
        tests.define("v;w;x") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("exprList", "ExprList") {
                        propertyElementExplicitType("expr", "Var") {
                            propertyString("name", "v")
                        }
                        propertyUnnamedListOfElement {
                            tuple {
                                propertyElementExplicitType("expr", "Var") {
                                    propertyString("name", "w")
                                }
                            }
                            tuple {
                                propertyElementExplicitType("expr", "Var") {
                                    propertyString("name", "x")
                                }
                            }
                        }
                    }
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test
    fun list_of_group() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = ID (NAME | NUMBER)* ;
                leaf ID = "[a-z]" ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val sentence = "a adam 2 charles"
        val expected = asmSimple {
            element("S") {
                propertyString("id", "a")
                propertyUnnamedListOfString(listOf("adam", "2", "charles"))
            }
        }
        test(proc, TestData(sentence, expected))
    }

    @Test
    fun sepList() {
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
        val proc = testProc(grammarStr)

        val sentence = "bk1 adam ant 12345, betty boo 34567, charlie chaplin 98765"
        val expected = asmSimple {
            element("AddressBook") {
                propertyString("id", "bk1")
                propertyListOfElement("contacts") {
                    element("Person") {
                        propertyString("name", "adam")
                        propertyString("name2", "ant")
                        propertyString("number", "12345")
                    }
                    element("Person") {
                        propertyString("name", "betty")
                        propertyString("name2", "boo")
                        propertyString("number", "34567")
                    }
                    element("Person") {
                        propertyString("name", "charlie")
                        propertyString("name2", "chaplin")
                        propertyString("number", "98765")
                    }
                }
            }
        }
        test(proc, TestData(sentence, expected))
    }

    @Test
    fun sepList2() {
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
        val proc = testProc(grammarStr)

        val sentence = "graph [fontsize=ss, labelloc=yy label=bb; splines=true overlap=false]"
        val expected = asmSimple {
            element("Attr_stmt") {
                propertyString("attr_type", "graph")
                propertyListOfElement("attr_lists") {
                    element("Attr_list") {
                        propertyListOfElement("attr_list_content") {
                            element("Attr") {
                                propertyString("id", "fontsize")
                                propertyString("id2", "ss")
                            }
                            string(",")
                            element("Attr") {
                                propertyString("id", "labelloc")
                                propertyString("id2", "yy")
                            }
                            string("")
                            element("Attr") {
                                propertyString("id", "label")
                                propertyString("id2", "bb")
                            }
                            string(";")
                            element("Attr") {
                                propertyString("id", "splines")
                                propertyString("id2", "true")
                            }
                            string("")
                            element("Attr") {
                                propertyString("id", "overlap")
                                propertyString("id2", "false")
                            }
                        }
                    }
                }
            }
        }
        test(proc, TestData(sentence, expected))
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
        val proc = testProc(grammarStr)

        val sentence = "abcde"
        val expected = asmSimple {
            element("S") {
                propertyString("a", "a")
                propertyTuple("\$group") {
                    propertyString("b", "b")
                    propertyString("c", "c")
                    propertyString("d", "d")
                }
                propertyString("e", "e")
            }
        }
        test(proc, TestData(sentence, expected))
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
        val proc = testProc(grammarStr)

        val sentence = "abe"
        val expected = asmSimple {
            element("S") {
                propertyString("a", "a")
                propertyString("\$group", "b")
                propertyString("e", "e")
            }
        }
        test(proc, TestData(sentence, expected))
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
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("abce") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyTuple("\$group") {
                        propertyString("b", "b")
                        propertyString("c", "c")
                    }
                    propertyString("e", "e")
                }
            }
        }
        tests.define("ade") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("\$group", "d")
                    propertyString("e", "e")
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test
    fun group_concat_optional() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b? c) e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("abce") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyTuple("\$group") {
                        propertyString("b", "b")
                        propertyString("c", "c")
                    }
                    propertyString("e", "e")
                }
            }
        }
        tests.define("ace") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyTuple("\$group") {
                        propertyString("b", null)
                        propertyString("c", "c")
                    }
                    propertyString("e", "e")
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test
    fun group_choice_group_concat_optional() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ( (b | c) (d?) e ) f ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
                leaf f = 'f' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("abef") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyTuple("\$group") {
                        propertyString("\$group", "b")
                        propertyTuple("\$group2") {
                            propertyString("d", null)
                        }
                        propertyString("e", "e")
                    }
                    propertyString("f", "f")
                }
            }
        }
        tests.define("acef") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyTuple("\$group") {
                        propertyString("\$group", "c")
                        propertyTuple("\$group2") {
                            propertyString("d", null)
                        }
                        propertyString("e", "e")
                    }
                    propertyString("f", "f")
                }
            }
        }
        tests.define("abdef") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyTuple("\$group") {
                        propertyString("\$group", "b")
                        propertyTuple("\$group2") {
                            propertyString("d", "d")
                        }
                        propertyString("e", "e")
                    }
                    propertyString("f", "f")
                }
            }
        }
        tests.define("acdef") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyTuple("\$group") {
                        propertyString("\$group", "c")
                        propertyTuple("\$group2") {
                            propertyString("d", "d")
                        }
                        propertyString("e", "e")
                    }
                    propertyString("f", "f")
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
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
                typeArgList = [type / ',']+ ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]*" ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("A") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("type", "Type") {
                        propertyString("name", "A")
                        propertyNull("typeArgs")
                    }
                }
            }
        }
        tests.define("A<B>") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("type", "Type") {
                        propertyString("name", "A")
                        propertyElementExplicitType("typeArgs", "TypeArgs") {
                            propertyListOfElement("typeArgList") {
                                element("Type") {
                                    propertyString("name", "B")
                                    propertyNull("typeArgs")
                                }
                            }
                        }
                    }
                }
            }
        }
        tests.define("A<B,C,D>") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("type", "Type") {
                        propertyString("name", "A")
                        propertyElementExplicitType("typeArgs", "TypeArgs") {
                            propertyListOfElement("typeArgList") {
                                element("Type") {
                                    propertyString("name", "B")
                                    propertyNull("typeArgs")
                                }
                                element("Type") {
                                    propertyString("name", "C")
                                    propertyNull("typeArgs")
                                }
                                element("Type") {
                                    propertyString("name", "D")
                                    propertyNull("typeArgs")
                                }
                            }
                        }
                    }
                }
            }
        }
        tests.define("A<B<C,D<E,F,G>,H<I,J>>>") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("type", "Type") {
                        propertyString("name", "A")
                        propertyElementExplicitType("typeArgs", "TypeArgs") {
                            propertyListOfElement("typeArgList") {
                                element("Type") {
                                    propertyString("name", "B")
                                    propertyElementExplicitType("typeArgs", "TypeArgs") {
                                        propertyListOfElement("typeArgList") {
                                            element("Type") {
                                                propertyString("name", "C")
                                                propertyNull("typeArgs")
                                            }
                                            element("Type") {
                                                propertyString("name", "D")
                                                propertyElementExplicitType("typeArgs", "TypeArgs") {
                                                    propertyListOfElement("typeArgList") {
                                                        element("Type") {
                                                            propertyString("name", "E")
                                                            propertyNull("typeArgs")
                                                        }
                                                        element("Type") {
                                                            propertyString("name", "F")
                                                            propertyNull("typeArgs")
                                                        }
                                                        element("Type") {
                                                            propertyString("name", "G")
                                                            propertyNull("typeArgs")
                                                        }
                                                    }
                                                }
                                            }
                                            element("Type") {
                                                propertyString("name", "H")
                                                propertyElementExplicitType("typeArgs", "TypeArgs") {
                                                    propertyListOfElement("typeArgList") {
                                                        element("Type") {
                                                            propertyString("name", "I")
                                                            propertyNull("typeArgs")
                                                        }
                                                        element("Type") {
                                                            propertyString("name", "J")
                                                            propertyNull("typeArgs")
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }
}