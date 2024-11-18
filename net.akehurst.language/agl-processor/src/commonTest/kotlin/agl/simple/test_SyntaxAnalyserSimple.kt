/*
 * Copyright (C) 2024 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.GrammarString
import net.akehurst.language.agl.grammarTypeModel.GrammarTypeModelTest
import net.akehurst.language.grammarTypemodel.builder.grammarTypeModel
import net.akehurst.language.agl.processor.LanguageProcessorAbstract
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetTest.matches
import net.akehurst.language.agl.runtime.structure.ruleSet
import net.akehurst.language.api.processor.LanguageProcessor
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.parser.api.RuleSet
import net.akehurst.language.test.FixMethodOrder
import net.akehurst.language.test.MethodSorters
import net.akehurst.language.typemodel.api.TypeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class test_SyntaxAnalyserSimple {

    private companion object {
        fun processor(grammarStr: String) = Agl.processorFromStringSimple(GrammarString(grammarStr))

        fun testProc(grammarStr: String): LanguageProcessor<Asm, ContextAsmSimple> {
            val result = processor(grammarStr)
            assertNotNull(result.processor, result.issues.toString())
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
            return result.processor!!
        }

        class TestData(
            val sentence: String,
            val expected: Asm
        )

        fun MutableList<TestData>.define(sentence: String, sppt: String? = null, expected: () -> Asm) = this.add(TestData(sentence, expected()))

        fun test(proc: LanguageProcessor<Asm, ContextAsmSimple>, data: TestData) {
            println("'${data.sentence}'")
            val result = proc.process(data.sentence)
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
            assertNotNull(result.asm)
            val actual = result.asm!!

            assertEquals(data.expected.asString(indentIncrement = "  "), actual.asString(indentIncrement = "  "))
        }

        fun testAll(proc: LanguageProcessor<Asm, ContextAsmSimple>, tests: List<TestData>) {
            for (data in tests) {
                test(proc, data)
            }
        }

        fun checkRuntimeGrammar(proc: LanguageProcessor<Asm, ContextAsmSimple>, expected: RuleSet) {
            val actual = (proc as LanguageProcessorAbstract).targetRuleSet as RuntimeRuleSet
            assertEquals(expected.toString(), actual.toString())
            assertTrue(expected.matches(actual))
        }

        fun checkTypeModel(proc: LanguageProcessor<Asm, ContextAsmSimple>, expected: TypeModel) {
            GrammarTypeModelTest.tmAssertEquals(expected, proc.typeModel)
        }
    }

    // --- Group ---
    @Test
    fun _7_where_root_is_UnnamedSuperType() {
        TODO()
    }

    @Test
    fun _7_group_where_tuple_property_is_UnnamedSuperType() {
        TODO()
    }

    // Embedded
    @Test
    fun _8_e_2() {
        val grammarStr = """
            namespace test
            grammar I {
                S = A | SA ;
                SA = S A ;
                A = a ;
                leaf a = 'a' ;
            }
            grammar O {
               S = B | SBC ;
               SBC = S BC ;
               BC = B | C ;
               B = 'b' I::S 'b' ;
               C = 'c' I::S 'c' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)
        val Inner = ruleSet("test.I") {
            choiceLongest("S") {
                concatenation { ref("A") }
                concatenation { ref("SA") }
            }
            concatenation("SA") { ref("S"); ref("A") }
            concatenation("A") { ref("a") }
            literal("a", "a")
        }
        checkRuntimeGrammar(proc, ruleSet("test.O") {
            choiceLongest("S") {
                concatenation { ref("B") }
                concatenation { ref("SBC") }
            }
            concatenation("SBC") { ref("S"); ref("BC") }
            choiceLongest("BC") {
                concatenation { ref("B") }
                concatenation { ref("C") }
            }
            concatenation("B") { literal("b"); ref("§I§S§embedded1"); literal("b"); }
            concatenation("C") { literal("c"); ref("§I§S§embedded1"); literal("c") }
            embedded("§I§S§embedded1", Inner, "S", isPseudo = true)
        })

        val tests = mutableListOf<TestData>()
        tests.define(
            "bab",
            """
            S { B {
              'b'
              §I§S§embedded1 { S { A { 'a' } } }
              'b'
            } } 
        """
        ) {
            asmSimple {
                element("B") {
                    propertyElementExplicitType("s", "A") {
                        propertyString("a", "a")
                    }
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test
    fun _8_e_3() {
        // S = 'a' | S 'a' ;
        // S = B | S B;
        // B = 'b' Inner::S 'b' | 'c' Inner::S 'c' ;
        val grammarStr = """
            namespace test
            grammar I {
                S = 'a' | S 'a' ;
            }
            grammar O {
               S = B | S B;
               B = 'b' I::S 'b' | 'c' I::S 'c' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)
        val Inner = ruleSet("test.I") {
            choiceLongest("S") {
                concatenation { literal("a") }
                concatenation { ref("S"); literal("a") }
            }
        }
        checkRuntimeGrammar(proc, ruleSet("test.O") {
            choiceLongest("S") {
                concatenation { ref("B") }
                concatenation { ref("S"); ref("B") }
            }
            choiceLongest("B") {
                concatenation { literal("b"); ref("§I§S§embedded1"); literal("b"); }
                concatenation { literal("c"); ref("§I§S§embedded1"); literal("c") }
            }
            embedded("§I§S§embedded1", Inner, "S", isPseudo = true)
        })

        val tests = mutableListOf<TestData>()
        tests.define(
            "bab", """
            S { B {
              'b'
              §I§S§embedded1 : S { 'a' }
              'b'
            } }
        """
        ) {
            asmSimple {
                tuple {
                    propertyTuple("b") {
                        propertyString("b", "b")
                        propertyString("s", "a")
                        propertyString("b2", "b")
                    }
                    propertyString("s", "d")
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test
    fun _8_e_4() {
        val grammarStr = """
            namespace test
            grammar I {
                S = A | SA ;
                SA = S A ;
                A = a ;
                leaf a = 'a' ;
            }
            grammar O {
               S = B | S BC ;
               BC = B | C ;
               B = 'b' I::S 'b' ;
               C = 'c' I::S 'c' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)
        val Inner = ruleSet("test.I") {
            choiceLongest("S") {
                concatenation { ref("A") }
                concatenation { ref("SA") }
            }
            concatenation("SA") { ref("S"); ref("A") }
            concatenation("A") { ref("a") }
            literal("a", "a")
        }
        checkRuntimeGrammar(proc, ruleSet("test.O") {
            choiceLongest("S") {
                concatenation { ref("B") }
                concatenation { ref("S"); ref("BC") }
            }
            choiceLongest("BC") {
                concatenation { ref("B") }
                concatenation { ref("C") }
            }
            concatenation("B") { literal("b"); ref("§I§S§embedded1"); literal("b"); }
            concatenation("C") { literal("c"); ref("§I§S§embedded1"); literal("c") }
            embedded("§I§S§embedded1", Inner, "S", isPseudo = true)
        })

        val tests = mutableListOf<TestData>()
        tests.define(
            "bab", """
            S { B {
              'b'
              §I§S§embedded1 { S { A { 'a' } } }
              'b'
            } } 
        """
        ) {
            asmSimple {
                tuple {
                    propertyTuple("b") {
                        propertyString("b", "b")
                        propertyString("s", "a")
                        propertyString("b2", "b")
                    }
                    propertyString("s", "d")
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    // --- Misc ---
    @Test
    fun _9_nesting() {
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

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test") {
            stringTypeFor("NAME")
            // S = type ;
            dataType("S", "S") {
                propertyDataTypeOf("type", "Type", false, 0)
            }
            // type = NAME typeArgs? ;
            dataType("type", "Type") {
                propertyPrimitiveType("name", "String", false, 0)
                propertyDataTypeOf("typeArgs", "TypeArgs", true, 1)
            }
            // typeArgs = '<' typeArgList '>' ;
            dataType("typeArgs", "TypeArgs") {
                propertyListTypeOf("typeArgList", "Type", false, 1)
            }
            // typeArgList = [type / ',']+ ;
            dataType("typeArgList", "TypeArgList") {
                propertyListTypeOf("type", "Type", false, 0)
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("A") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("type", "Type") {
                        propertyString("name", "A")
                        propertyNothing("typeArgs")
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
                                    propertyNothing("typeArgs")
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
                                    propertyNothing("typeArgs")
                                }
                                element("Type") {
                                    propertyString("name", "C")
                                    propertyNothing("typeArgs")
                                }
                                element("Type") {
                                    propertyString("name", "D")
                                    propertyNothing("typeArgs")
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
                                                propertyNothing("typeArgs")
                                            }
                                            element("Type") {
                                                propertyString("name", "D")
                                                propertyElementExplicitType("typeArgs", "TypeArgs") {
                                                    propertyListOfElement("typeArgList") {
                                                        element("Type") {
                                                            propertyString("name", "E")
                                                            propertyNothing("typeArgs")
                                                        }
                                                        element("Type") {
                                                            propertyString("name", "F")
                                                            propertyNothing("typeArgs")
                                                        }
                                                        element("Type") {
                                                            propertyString("name", "G")
                                                            propertyNothing("typeArgs")
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
                                                            propertyNothing("typeArgs")
                                                        }
                                                        element("Type") {
                                                            propertyString("name", "J")
                                                            propertyNothing("typeArgs")
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

    @Test
    fun _9_patternChoice() {
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
    fun _9_concatenation() {
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
    fun _9_choice() {
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
    fun _9_optional_full() {
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
    fun _9_optional_empty() {
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
    fun _9_list_empty() {
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
    fun _9_list() {
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
    fun _9_list_of_group() {
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
                propertyListOfString("\$choiceList", listOf("adam", "2", "charles"))
            }
        }
        test(proc, TestData(sentence, expected))
    }

    @Test
    fun _9_sepList() {
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
    fun _9_sepList2() {
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
    fun _9_expressions_infix() {
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
    fun _9_expressions_sepList() {
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
        for (data in tests) {
            test(proc, data)
        }
    }
}