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

import net.akehurst.language.agl.grammarTypeModel.GrammarTypeModelTest
import net.akehurst.language.agl.grammarTypeModel.grammarTypeModel
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.processor.LanguageProcessorAbstract
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSetTest.matches
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.agl.semanticAnalyser.ContextSimple
import net.akehurst.language.api.asm.Asm
import net.akehurst.language.api.asm.asmSimple
import net.akehurst.language.api.processor.LanguageProcessor
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
        fun processor(grammarStr: String) = Agl.processorFromStringDefault(grammarStr)

        fun testProc(grammarStr: String): LanguageProcessor<Asm, ContextSimple> {
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

        fun test(proc: LanguageProcessor<Asm, ContextSimple>, data: TestData) {
            println("'${data.sentence}'")
            val result = proc.process(data.sentence)
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
            assertNotNull(result.asm)
            val actual = result.asm!!

            assertEquals(data.expected.asString(indentIncrement = "  "), actual.asString(indentIncrement = "  "))
        }

        fun testAll(proc: LanguageProcessor<Asm, ContextSimple>, tests: List<TestData>) {
            for (data in tests) {
                test(proc, data)
            }
        }

        fun checkRuntimeGrammar(proc: LanguageProcessor<Asm, ContextSimple>, expected: RuntimeRuleSet) {
            val actual = (proc as LanguageProcessorAbstract).runtimeRuleSet
            assertEquals(expected.toString(), actual.toString())
            assertTrue(expected.matches(actual))
        }

        fun checkTypeModel(proc: LanguageProcessor<Asm, ContextSimple>, expected: TypeModel) {
            GrammarTypeModelTest.tmAssertEquals(expected, proc.typeModel)
        }
    }

    // --- Empty ---
    @Test // S =  ;
    fun _0_empty() {
        val grammarStr = """
            namespace test
            grammar Test {
                S =  ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val sentence = ""
        val expected = asmSimple {
            element("S") {
            }
        }
        test(proc, TestData(sentence, expected))
    }

    // --- Literal ---
    @Test // S = 'a' ;
    fun _1_terminal_nonleaf_literal() {
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

    @Test // S = a ; leaf a = 'a' ;
    fun _1_terminal_leaf_literal() {
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

    // --- Pattern ---
    @Test //  S = "[a-z]" ;
    fun _1_terminal_nonLeaf_pattern() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = "[a-z]" ;
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

    @Test // S = v ; leaf v = "[a-z]" ;
    fun _1_terminal_leaf_pattern() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = v ;
                leaf v = "[a-z]" ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val sentence = "a"
        val expected = asmSimple {
            element("S") {
                propertyString("v", "a")
            }
        }
        test(proc, TestData(sentence, expected))
    }

    // --- Concatenation ---
    @Test // S = A B C ;
    fun _2_concat_nonTerm_x3_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A B C ;
                A = 'a' ;
                B = 'b' ;
                C = 'c' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("abc") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("b", "b")
                    propertyString("c", "c")
                }
            }
        }
        testAll(proc, tests)
    }

    @Test
    fun _2_concat_nonTerm_x3_literal_with_separator() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ',' b ',' c ;
                a = 'a' ;
                b = 'b' ;
                c = 'c' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("a,b,c") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("b", "b")
                    propertyString("c", "c")
                }
            }
        }
        testAll(proc, tests)
    }

    // --- Choice ---
    @Test // S = 'a' | 'b' | 'c' ;
    fun _3_choice_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' | 'b' | 'c' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val sentence = "a"
        val expected = asmSimple {
            string("a")
        }
        test(proc, TestData(sentence, expected))
    }

    @Test // S = A | B | C ; A = a x; B = b x; C = c x;
    fun _3_choice_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A | B | C ;
                A = a x;
                B = b x;
                C = c x;
                leaf a = 'a';
                leaf b = 'b';
                leaf c = 'c';
                leaf x = 'x';
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkRuntimeGrammar(proc, runtimeRuleSet("test.Test") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { ref("A"); ref("B"); ref("C") }
            concatenation("A") { ref("a"); ref("x") }
            concatenation("B") { ref("b"); ref("x") }
            concatenation("C") { ref("c"); ref("x") }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("x", "x")
        })

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("x")
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
            dataType("B", "B") {
                propertyPrimitiveType("b", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
            dataType("C", "C") {
                propertyPrimitiveType("c", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
            dataType("S", "S") {
                subtypes("A", "B", "C")
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("ax") {
            asmSimple {
                element("A") {
                    propertyString("a", "a")
                    propertyString("x", "x")
                }
            }
        }
        tests.define("bx") {
            asmSimple {
                element("B") {
                    propertyString("b", "b")
                    propertyString("x", "x")
                }
            }
        }
        tests.define("cx") {
            asmSimple {
                element("C") {
                    propertyString("c", "c")
                    propertyString("x", "x")
                }
            }
        }
        testAll(proc, tests)
    }

    @Test // S = L | M ; L = 'a' | 'b' | 'c' ; M = 'x' | 'y' ;
    fun _3_choice_of_choice_all_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = L | M ;
                L = 'a' | 'b' | 'c' ;
                M = 'x' | 'y' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("a") {
            asmSimple {
                string("a")
            }
        }
        tests.define("b") {
            asmSimple {
                string("b")
            }
        }
        tests.define("c") {
            asmSimple {
                string("c")
            }
        }
        tests.define("x") {
            asmSimple {
                string("x")
            }
        }
        tests.define("y") {
            asmSimple {
                string("y")
            }
        }
        testAll(proc, tests)
    }

    @Test // S = L | M ; L = a | b | c ;  M = x | y ;
    fun _3_choice_of_choice_all_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = L | M ;
                L = a | b | c ;
                M = x | y ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf x = 'x' ;
                leaf y = 'y' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("a") {
            asmSimple {
                string("a")
            }
        }
        tests.define("b") {
            asmSimple {
                string("b")
            }
        }
        tests.define("c") {
            asmSimple {
                string("c")
            }
        }
        tests.define("x") {
            asmSimple {
                string("x")
            }
        }
        tests.define("y") {
            asmSimple {
                string("y")
            }
        }
        testAll(proc, tests)
    }

    @Test // S = A | B | C ; A = a x ; B = C | D ; C = c x; D = d x ;
    fun _3_choice_of_choice_all_concats() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A | B | C ;
                A = a x ;
                B = C | D ;
                C = c x ;
                D = d x ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf x = 'x' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("ax") {
            asmSimple {
                element("A") {
                    propertyString("a", "a")
                    propertyString("x", "x")
                }
            }
        }
        tests.define("cx") {
            asmSimple {
                element("C") {
                    propertyString("c", "c")
                    propertyString("x", "x")
                }
            }
        }
        tests.define("dx") {
            asmSimple {
                element("D") {
                    propertyString("d", "d")
                    propertyString("x", "x")
                }
            }
        }
        testAll(proc, tests)
    }

    @Test // S = A | B | C ; A = a x ; B = c | D ; C = c ; D = d ;
    fun _3_choice_of_choice_mixed_literal_and_concats() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A | B | C ;
                A = a x ;
                B = c | D ;
                C = c ;
                D = d ;
                leaf a = 'a';
                leaf b = 'b';
                leaf c = 'c';
                leaf d = 'd';
                leaf x = 'x';
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkRuntimeGrammar(proc, runtimeRuleSet("test.Test") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { ref("A"); ref("B"); ref("C") }
            concatenation("A") { ref("a"); ref("x") }
            choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) { ref("c"); ref("D") }
            concatenation("C") { ref("c") }
            concatenation("D") { ref("d") }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("x", "x")
        })

        val tests = mutableListOf<TestData>()
        tests.define(
            "ax", """
            S { A {
              'a'
              'x'
            } } 
        """.trimIndent()
        ) {
            asmSimple {
                element("A") {
                    propertyString("a", "a")
                    propertyString("x", "x")
                }
            }
        }
        tests.define("c") {
            asmSimple {
                element("C") {
                    propertyString("c", "c")
                }
            }
        }
        tests.define("d") {
            asmSimple {
                element("D") {
                    propertyString("d", "d")
                }
            }
        }
        testAll(proc, tests)
    }

    // --- Optional ---
    @Test // S = 'a'? ;
    fun _4_optional_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a'? ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val sentence = ""
        val expected = asmSimple {
            element("S") { }
        }
        test(proc, TestData(sentence, expected))
    }

    @Test // S = a 'b'? c ;
    fun _4_concat_optional_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a 'b'? c ;
                leaf a = 'a' ;
                leaf c = 'c' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("abc") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("c", "c")
                }
            }
        }
        tests.define("ac") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("c", "c")
                }
            }
        }
        testAll(proc, tests)
    }

    @Test //S = a? ;
    fun _4_optional_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a? ;
                leaf a = 'a';
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("") {
            asmSimple {
                element("S") {
                    propertyNull("a")
                }
            }
        }
        tests.define("a") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                }
            }
        }
        testAll(proc, tests)
    }

    @Test // S = a 'b'? c ;
    fun _4_concat_optional_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a b? c ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("abc") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("b", "b")
                    propertyString("c", "c")
                }
            }
        }
        tests.define("ac") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("b", null)
                    propertyString("c", "c")
                }
            }
        }
        testAll(proc, tests)
    }

    @Test // S = A?; A = a; leaf a = 'a';
    fun _4_optional_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A? ;
                A = a ;
                leaf a = 'a';
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            dataType("S", "S") {
                propertyDataTypeOf("a", "A", true, 0)
            }
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("") {
            asmSimple {
                element("S") {
                    propertyNull("a")
                }
            }
        }
        tests.define("a") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("a", "A") {
                        propertyString("a", "a")
                    }
                }
            }
        }
        testAll(proc, tests)
    }

    @Test // S = b A?; A = a; leaf a = 'a';
    fun _4_concat_optional_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = b A? ;
                A = a ;
                leaf a = 'a';
                leaf b = 'b';
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            dataType("S", "S") {
                propertyPrimitiveType("b", "String", false, 0)
                propertyDataTypeOf("a", "A", true, 1)
            }
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("b") {
            asmSimple {
                element("S") {
                    propertyString("b", "b")
                    propertyNull("a")
                }
            }
        }
        tests.define("ba") {
            asmSimple {
                element("S") {
                    propertyString("b", "b")
                    propertyElementExplicitType("a", "A") {
                        propertyString("a", "a")
                    }
                }
            }
        }
        testAll(proc, tests)
    }

    @Test // S = oA; oA=A?; A = a; leaf a = 'a';
    fun _4_nonTerm_optional_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = oA ;
                oA = A? ;
                A = a ;
                leaf a = 'a';
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            dataType("S", "S") {
                propertyDataTypeOf("oA", "OA", false, 0)
            }
            dataType("oA", "OA") {
                propertyDataTypeOf("a", "A", true, 0)
            }
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("oA", "OA") {
                        propertyNull("a")
                    }
                }
            }
        }
        tests.define("a") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("oA", "OA") {
                        propertyElementExplicitType("a", "A") {
                            propertyString("a", "a")
                        }
                    }
                }
            }
        }
        testAll(proc, tests)
    }

    // --- ListSimple ---
    @Test //  S = 'a'* ;
    fun _5_list_literal_nonLeaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a'* ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("") {
            asmSimple {
                element("S") {}
            }
        }
        tests.define("a") {
            asmSimple {
                element("S") {}
            }
        }
        tests.define("aa") {
            asmSimple {
                element("S") {}
            }
        }
        tests.define("aaa") {
            asmSimple {
                element("S") {}
            }
        }
        testAll(proc, tests)
    }

    @Test // S = a 'b'* c ;
    fun _5_concat_list_literal_nonLeaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a 'b'* c ;
                leaf a = 'a' ;
                leaf c = 'c' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("ac") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("c", "c")
                }
            }
        }
        tests.define("abc") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("c", "c")
                }
            }
        }
        tests.define("abbc") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("c", "c")
                }
            }
        }
        tests.define("abbbc") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("c", "c")
                }
            }
        }
        testAll(proc, tests)
    }

    @Test //  S = a* ;
    fun _5_list_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a* ;
                leaf a = 'a' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("") {
            asmSimple {
                element("S") {
                    propertyListOfString("a", listOf())
                }
            }
        }
        tests.define("a") {
            asmSimple {
                element("S") {
                    propertyListOfString("a", listOf("a"))
                }
            }
        }
        tests.define("aa") {
            asmSimple {
                element("S") {
                    propertyListOfString("a", listOf("a", "a"))
                }
            }
        }
        tests.define("aaa") {
            asmSimple {
                element("S") {
                    propertyListOfString("a", listOf("a", "a", "a"))
                }
            }
        }
        testAll(proc, tests)
    }

    @Test // S = a b* c ;
    fun _5_concat_list_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a b* c ;
                leaf a = 'a';
                leaf b = 'b';
                leaf c = 'c';
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("ac") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyListOfString("b", emptyList())
                    propertyString("c", "c")
                }
            }
        }
        tests.define("abc") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyListOfString("b", listOf("b"))
                    propertyString("c", "c")
                }
            }
        }
        tests.define("abbc") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyListOfString("b", listOf("b", "b"))
                    propertyString("c", "c")
                }
            }
        }
        tests.define("abbbc") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyListOfString("b", listOf("b", "b", "b"))
                    propertyString("c", "c")
                }
            }
        }
        testAll(proc, tests)
    }

    @Test // S = A*; A = a; leaf a = 'a';
    fun _5_list_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A* ;
                A = a ;
                leaf a = 'a';
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            dataType("S", "S") {
                propertyListTypeOf("a", "A", false, 0)
            }
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("") {
            asmSimple {
                element("S") {
                    propertyListOfElement("a") {}
                }
            }
        }
        tests.define("a") {
            asmSimple {
                element("S") {
                    propertyListOfElement("a") {
                        element("A") {
                            propertyString("a", "a")
                        }
                    }
                }
            }
        }
        tests.define("aa") {
            asmSimple {
                element("S") {
                    propertyListOfElement("a") {
                        element("A") {
                            propertyString("a", "a")
                        }
                        element("A") {
                            propertyString("a", "a")
                        }
                    }
                }
            }
        }
        testAll(proc, tests)
    }

    @Test // S = b A* c; A = a; leaf a = 'a';
    fun _5_concat_list_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = b A* c;
                A = a ;
                leaf a = 'a';
                leaf b = 'b';
                leaf c = 'c';
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            dataType("S", "S") {
                propertyPrimitiveType("b", "String", false, 0)
                propertyListTypeOf("a", "A", false, 1)
                propertyPrimitiveType("c", "String", false, 2)
            }
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("bc") {
            asmSimple {
                element("S") {
                    propertyString("b", "b")
                    propertyListOfElement("a") {}
                    propertyString("c", "c")
                }
            }
        }
        tests.define("bac") {
            asmSimple {
                element("S") {
                    propertyString("b", "b")
                    propertyListOfElement("a") {
                        element("A") {
                            propertyString("a", "a")
                        }
                    }
                    propertyString("c", "c")
                }
            }
        }
        tests.define("baac") {
            asmSimple {
                element("S") {
                    propertyString("b", "b")
                    propertyListOfElement("a") {
                        element("A") {
                            propertyString("a", "a")
                        }
                        element("A") {
                            propertyString("a", "a")
                        }
                    }
                    propertyString("c", "c")
                }
            }
        }
        testAll(proc, tests)
    }

    @Test //S = as ; as = a* ;
    fun _5_nonTerm_list_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = a* ;
                leaf a = 'a' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            dataType("S", "S") {
                propertyListTypeOf("as", "String", false, 0)
            }
            dataType("as", "As") {
                propertyListTypeOf("a", "String", false, 0)
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("") {
            asmSimple {
                element("S") {
                    propertyListOfString("as", listOf())
                }
            }
        }
        tests.define("a") {
            asmSimple {
                element("S") {
                    propertyListOfString("as", listOf("a"))
                }
            }
        }
        tests.define("aa") {
            asmSimple {
                element("S") {
                    propertyListOfString("as", listOf("a", "a"))
                }
            }
        }
        tests.define("aaa") {
            asmSimple {
                element("S") {
                    propertyListOfString("as", listOf("a", "a", "a"))
                }
            }
        }
        testAll(proc, tests)
    }

    @Test //S = abs; abs = ab*; ab = A | B;
    fun _5_list_of_supertype() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = abs ;
                abs = AB* ;
                AB = A | B ;
                A = a ;
                B = b ;
                leaf a = 'a' ;
                leaf b = 'b' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            dataType("S", "S") {
                propertyListTypeOf("abs", "AB", false, 0)
            }
            dataType("abs", "Abs") {
                propertyListTypeOf("ab", "AB", false, 0)
            }
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
            }
            dataType("B", "B") {
                propertyPrimitiveType("b", "String", false, 0)
            }
            dataType("AB", "AB") {
                subtypes("A", "B")
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("") {
            asmSimple {
                element("S") {
                    propertyListOfElement("abs") {

                    }
                }
            }
        }
        tests.define("a") {
            asmSimple {
                element("S") {
                    propertyListOfElement("abs") {
                        element("A") {
                            propertyString("a", "a")
                        }
                    }
                }
            }
        }
        tests.define("b") {
            asmSimple {
                element("S") {
                    propertyListOfElement("abs") {
                        element("B") {
                            propertyString("b", "b")
                        }
                    }
                }
            }
        }
        tests.define("aa") {
            asmSimple {
                element("S") {
                    propertyListOfElement("abs") {
                        element("A") {
                            propertyString("a", "a")
                        }
                        element("A") {
                            propertyString("a", "a")
                        }
                    }
                }
            }
        }
        tests.define("bb") {
            asmSimple {
                element("S") {
                    propertyListOfElement("abs") {
                        element("B") {
                            propertyString("b", "b")
                        }
                        element("B") {
                            propertyString("b", "b")
                        }
                    }
                }
            }
        }
        tests.define("ab") {
            asmSimple {
                element("S") {
                    propertyListOfElement("abs") {
                        element("A") {
                            propertyString("a", "a")
                        }
                        element("B") {
                            propertyString("b", "b")
                        }
                    }
                }
            }
        }
        tests.define("ababab") {
            asmSimple {
                element("S") {
                    propertyListOfElement("abs") {
                        element("A") {
                            propertyString("a", "a")
                        }
                        element("B") {
                            propertyString("b", "b")
                        }
                        element("A") {
                            propertyString("a", "a")
                        }
                        element("B") {
                            propertyString("b", "b")
                        }
                        element("A") {
                            propertyString("a", "a")
                        }
                        element("B") {
                            propertyString("b", "b")
                        }
                    }
                }
            }
        }
        testAll(proc, tests)
    }

    @Test
    fun _5_supertype_of_list() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = E ;
                E = V | A ;
                A = E{2+} ;
                V = NAME ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("v") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("e", "V") {
                        propertyString("name", "v")
                    }
                }
            }
        }
        tests.define("v w") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("e", "A") {
                        propertyListOfElement("e") {
                            element("V") {
                                propertyString("name", "v")
                            }
                            element("V") {
                                propertyString("name", "w")
                            }
                        }
                    }
                }
            }
        }
        tests.define("v w x y z") {
            asmSimple {
                element("S") {
                    propertyElementExplicitType("e", "A") {
                        propertyListOfElement("e") {
                            element("V") {
                                propertyString("name", "v")
                            }
                            element("V") {
                                propertyString("name", "w")
                            }
                            element("V") {
                                propertyString("name", "x")
                            }
                            element("V") {
                                propertyString("name", "y")
                            }
                            element("V") {
                                propertyString("name", "z")
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

    // --- ListSeparated ---
    @Test // S = as ; as = ['a' / ',']* ;
    fun _6_rhs_nonTerm_sepList_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = ['a' / ',']* ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("") {
            asmSimple {
                element("S") {
                }
            }
        }
        tests.define("a") {
            asmSimple {
                element("S") {
                }
            }
        }
        tests.define("a,a") {
            asmSimple {
                element("S") {
                }
            }
        }
        tests.define("a,a,a") {
            asmSimple {
                element("S") {
                }
            }
        }
        tests.define("a,a,a,a") {
            asmSimple {
                element("S") {
                }
            }
        }
        testAll(proc, tests)
    }

    @Test // S = a bs c ; bs = ['b' / ',']* ;
    fun _6_concat_nonTerm_sepList_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a bs c;
                bs = ['b' / ',']* ;
                leaf a = 'a' ;
                leaf c = 'c' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("ac") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("c", "c")
                }
            }
        }
        tests.define("abc") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("c", "c")
                }
            }
        }
        tests.define("ab,bc") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("c", "c")
                }
            }
        }
        tests.define("ab,b,bc") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("c", "c")
                }
            }
        }
        tests.define("ab,b,b,bc") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("c", "c")
                }
            }
        }
        testAll(proc, tests)
    }

    @Test // S = as ; as = [a / ',']* ;
    fun _6_rhs_nonTerm_sepList_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = [a / ',']* ;
                leaf a = 'a' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("") {
            asmSimple {
                element("S") {
                    propertyListOfString("as", listOf())
                }
            }
        }
        tests.define("a") {
            asmSimple {
                element("S") {
                    propertyListOfString("as", listOf("a"))
                }
            }
        }
        tests.define("a,a") {
            asmSimple {
                element("S") {
                    propertyListOfString("as", listOf("a", "a"))
                }
            }
        }
        tests.define("a,a,a") {
            asmSimple {
                element("S") {
                    propertyListOfString("as", listOf("a", "a", "a"))
                }
            }
        }
        tests.define("a,a,a,a") {
            asmSimple {
                element("S") {
                    propertyListOfString("as", listOf("a", "a", "a", "a"))
                }
            }
        }
        testAll(proc, tests)
    }

    @Test //S = abs; abs = [ab / ',']*; ab = A | B;
    fun _6_sepList_of_supertype() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = abs ;
                abs = [AB / ',']* ;
                AB = A | B ;
                A = a ;
                B = b ;
                leaf a = 'a' ;
                leaf b = 'b' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            dataType("S", "S") {
                propertyListTypeOf("abs", "AB", false, 0)
            }
            dataType("abs", "Abs") {
                propertyListTypeOf("ab", "AB", false, 0)
            }
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
            }
            dataType("B", "B") {
                propertyPrimitiveType("b", "String", false, 0)
            }
            dataType("AB", "AB") {
                subtypes("A", "B")
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("") {
            asmSimple {
                element("S") {
                    propertyListOfElement("abs") {

                    }
                }
            }
        }
        tests.define("a") {
            asmSimple {
                element("S") {
                    propertyListOfElement("abs") {
                        element("A") {
                            propertyString("a", "a")
                        }
                    }
                }
            }
        }
        tests.define("b") {
            asmSimple {
                element("S") {
                    propertyListOfElement("abs") {
                        element("B") {
                            propertyString("b", "b")
                        }
                    }
                }
            }
        }
        tests.define("a,a") {
            asmSimple {
                element("S") {
                    propertyListOfElement("abs") {
                        element("A") {
                            propertyString("a", "a")
                        }
                        element("A") {
                            propertyString("a", "a")
                        }
                    }
                }
            }
        }
        tests.define("b,b") {
            asmSimple {
                element("S") {
                    propertyListOfElement("abs") {
                        element("B") {
                            propertyString("b", "b")
                        }
                        element("B") {
                            propertyString("b", "b")
                        }
                    }
                }
            }
        }
        tests.define("a,b") {
            asmSimple {
                element("S") {
                    propertyListOfElement("abs") {
                        element("A") {
                            propertyString("a", "a")
                        }
                        element("B") {
                            propertyString("b", "b")
                        }
                    }
                }
            }
        }
        tests.define("a,b,a,b,a,b") {
            asmSimple {
                element("S") {
                    propertyListOfElement("abs") {
                        element("A") {
                            propertyString("a", "a")
                        }
                        element("B") {
                            propertyString("b", "b")
                        }
                        element("A") {
                            propertyString("a", "a")
                        }
                        element("B") {
                            propertyString("b", "b")
                        }
                        element("A") {
                            propertyString("a", "a")
                        }
                        element("B") {
                            propertyString("b", "b")
                        }
                    }
                }
            }
        }
        testAll(proc, tests)
    }

    // --- Group ---
    @Test // S = a ('b' 'c' 'd') e ;
    fun _7_concat_group_concat_literal_nonLeaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ('b' 'c' 'd') e ;
                leaf a = 'a' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("abcde") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("e", "e")
                }
            }
        }
        testAll(proc, tests)
    }

    @Test // S = a (b c d) e ;
    fun _7_concat_group_concat_literal_leaf() {
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

    @Test // S = a (b c d) (b a c) e ;
    fun _7_concat_group_concat_leaf_literal_2() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b c d) (b a c) e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val sentence = "abcdbace"
        val expected = asmSimple {
            element("S") {
                propertyString("a", "a")
                propertyTuple("\$group") {
                    propertyString("b", "b")
                    propertyString("c", "c")
                    propertyString("d", "d")
                }
                propertyTuple("\$group2") {
                    propertyString("b", "b")
                    propertyString("a", "a")
                    propertyString("c", "c")
                }
                propertyString("e", "e")
            }
        }
        test(proc, TestData(sentence, expected))
    }

    @Test // S = a (b) e ;
    fun _7_concat_group_1_leaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b) e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val sentence = "abe"
        val expected = asmSimple {
            element("S") {
                propertyString("a", "a")
                propertyTuple("\$group") {
                    propertyString("b", "b")
                }
                propertyString("e", "e")
            }
        }
        test(proc, TestData(sentence, expected))
    }

    @Test // S = a (b (c) d) e ;
    fun _7_group_concat_group_group_leaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b (c) d) e ;
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
                    propertyTuple("\$group") {
                        propertyString("c", "c")
                    }
                    propertyString("d", "d")
                }
                propertyString("e", "e")
            }
        }
        test(proc, TestData(sentence, expected))
    }

    @Test // S = a (b | c | d) e ;
    fun _7_concat_group_choice_leaf_literal() {
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

        val tests = mutableListOf<TestData>()
        tests.define("abe") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("\$choice", "b")
                    propertyString("e", "e")
                }
            }
        }
        tests.define("ace") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("\$choice", "c")
                    propertyString("e", "e")
                }
            }
        }
        tests.define("ade") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("\$choice", "d")
                    propertyString("e", "e")
                }
            }
        }
        testAll(proc, tests)
    }

    @Test // S = a (b c | d) e ;
    fun _7_concat_group_choice_concat_leaf_literal() {
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
                    propertyTuple("\$choice") {
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
                    propertyString("\$choice", "d")
                    propertyString("e", "e")
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test // S = a (b c | d e) f ;
    fun _7_concat_group_choice_concat_leaf_literal_2() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b c | d e) f ;
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
        tests.define("abcf") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyTuple("\$choice") {
                        propertyString("b", "b")
                        propertyString("c", "c")
                    }
                    propertyString("f", "f")
                }
            }
        }
        tests.define("adef") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyTuple("\$choice") {
                        propertyString("d", "d")
                        propertyString("e", "e")
                    }
                    propertyString("f", "f")
                }
            }
        }
        testAll(proc, tests)
    }

    @Test // S = a ( ('x' | 'y') b c | d e) f ;
    fun _7_concat_group_choice_concat_leaf_literal_3() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ( ('x'|'y') b c | d e) f ;
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
        tests.define("axbcf") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyTuple("\$choice") {
                        propertyString("b", "b")
                        propertyString("c", "c")
                    }
                    propertyString("f", "f")
                }
            }
        }
        tests.define("adef") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyTuple("\$choice") {
                        propertyString("d", "d")
                        propertyString("e", "e")
                    }
                    propertyString("f", "f")
                }
            }
        }
        testAll(proc, tests)
    }

    @Test // S = a (b c | d e)? f ;
    fun _7_concat_group_choice_concat_leaf_literal_4() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b c | d e)? f ;
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
        tests.define("af") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyNull("\$choice")
                    propertyString("f", "f")
                }
            }
        }
        tests.define(
            "abcf", """
            S {
              'a'
              §S§opt1 { §S§choice1 {
                'b'
                'c'
              } }
              'f'
            } 
        """
        ) {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyTuple("\$choice") {
                        propertyString("b", "b")
                        propertyString("c", "c")
                    }
                    propertyString("f", "f")
                }
            }
        }
        tests.define("adef") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyTuple("\$choice") {
                        propertyString("d", "d")
                        propertyString("e", "e")
                    }
                    propertyString("f", "f")
                }
            }
        }
        testAll(proc, tests)
    }

    @Test //  S = a (b? c) e ;
    fun _7_concat_group_concat_optional() {
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

    @Test // S = a ( (b | c) (d?) e ) f ;
    fun _7_concat_group_choice_group_concat_optional() {
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
                        propertyString("\$choice", "b")
                        propertyTuple("\$group") {
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
                        propertyString("\$choice", "c")
                        propertyTuple("\$group") {
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
                        propertyString("\$choice", "b")
                        propertyTuple("\$group") {
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
                        propertyString("\$choice", "c")
                        propertyTuple("\$group") {
                            propertyString("d", "d")
                        }
                        propertyString("e", "e")
                    }
                    propertyString("f", "f")
                }
            }
        }

        testAll(proc, tests)
    }

    @Test // S = (BC | d+) ;
    fun _7_rhs_group_choice_concat_nonTerm_list() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = (BC | d+) ;
                BC = b c ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkRuntimeGrammar(proc, runtimeRuleSet("test.Test") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("BC") }
                concatenation { ref("§S§multi1") }
            }
            concatenation("BC") { ref("b"); ref("c") }
            multi("§S§multi1", 1, -1, "d")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
        })

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            unnamedSuperTypeType("S") {
                elementRef("BC")
                listType(false) {
                    primitiveRef("String")
                }
            }

            dataType("BC", "BC") {
                propertyPrimitiveType("b", "String", false, 0)
                propertyPrimitiveType("c", "String", false, 1)
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("bc") {
            asmSimple {
                element("BC") {
                    propertyString("b", "b")
                    propertyString("c", "c")
                }
            }
        }
        tests.define("d") {
            asmSimple {
                listOfString("d")
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test // S = a (BC | d+) e ;
    fun _7_concat_group_choice_concat_nonTerm_list() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (BC | d+) e ;
                BC = b c ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkRuntimeGrammar(proc, runtimeRuleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§choice1"); ref("e") }
            choice("§S§choice1", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("BC") }
                concatenation { ref("§S§multi1") }
            }
            concatenation("BC") { ref("b"); ref("c") }
            multi("§S§multi1", 1, -1, "d")
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        })

        val tests = mutableListOf<TestData>()
        tests.define("abce") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyElementExplicitType("\$choice", "BC") {
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
                    propertyListOfString("\$choice", listOf("d"))
                    propertyString("e", "e")
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test // S = a (BC | d+)? e ;
    fun _7_concat_group_choice_concat_nonTerm_list_2() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (BC | d+)? e ;
                BC = b c ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkRuntimeGrammar(proc, runtimeRuleSet("test.Test") {
            concatenation("S") { ref("a"); ref("§S§opt2"); ref("e") }
            optional("§S§opt2", "§S§choice1")
            choice("§S§choice1", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("BC") }
                concatenation { ref("§S§multi1") }
            }
            concatenation("BC") { ref("b"); ref("c") }
            multi("§S§multi1", 1, -1, "d")
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        })

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyUnnamedSuperType("\$choice", true, 1) {
                    elementRef("BC")
                    listType(false) { primitiveRef("String") }
                }
                propertyPrimitiveType("e", "String", false, 2)
            }
            dataType("BC", "BC") {
                propertyPrimitiveType("b", "String", false, 0)
                propertyPrimitiveType("c", "String", false, 1)
            }
        })

        val tests = mutableListOf<TestData>()
        /*        tests.define("ae") {
                    asmSimple {
                        element("S") {
                            propertyString("a", "a")
                            propertyNull("\$choice")
                            propertyString("e", "e")
                        }
                    }
                }*/
        tests.define(
            "abce", """
            S {
              'a'
              §S§opt2 { §S§choice1 { BC {
                'b'
                'c'
              } } }
              'e'
            } 
        """.trimIndent()
        ) {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyElementExplicitType("\$choice", "BC") {
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
                    propertyListOfString("\$choice", listOf("d"))
                    propertyString("e", "e")
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test // S = a b | c d e ;
    fun _7_1a_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a b | c d e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkRuntimeGrammar(proc, runtimeRuleSet("test.Test") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("a"); ref("b") }
                concatenation { ref("c"); ref("d"); ref("e") }
            }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        })

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            unnamedSuperTypeType("S") {
                tupleType {
                    propertyPrimitiveType("a", "String", false, 0)
                    propertyPrimitiveType("b", "String", false, 1)
                }
                tupleType {
                    propertyPrimitiveType("c", "String", false, 0)
                    propertyPrimitiveType("d", "String", false, 1)
                    propertyPrimitiveType("e", "String", false, 2)
                }
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define(
            "ab", """
            S {
              'a'
              'b'
            } 
        """
        ) {
            asmSimple {
                tuple {
                    propertyString("a", "a")
                    propertyString("b", "b")
                }
            }
        }
        tests.define("cde") {
            asmSimple {
                tuple {
                    propertyString("c", "c")
                    propertyString("d", "d")
                    propertyString("e", "e")
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test // S = (a b) | (c d e) ;
    fun _7_1b_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = (a b) | (c d e) ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkRuntimeGrammar(proc, runtimeRuleSet("test.Test") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("§S§group1") }
                concatenation { ref("§S§group2") }
            }
            concatenation("§S§group1") { ref("a"); ref("b") }
            concatenation("§S§group2") { ref("c"); ref("d"); ref("e") }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        })

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            unnamedSuperTypeType("S") {
                tupleType {
                    propertyTupleType("\$group", false, 0) {
                        propertyPrimitiveType("a", "String", false, 0)
                        propertyPrimitiveType("b", "String", false, 1)
                    }
                }
                tupleType {
                    propertyTupleType("\$group", false, 0) {
                        propertyPrimitiveType("c", "String", false, 0)
                        propertyPrimitiveType("d", "String", false, 1)
                        propertyPrimitiveType("e", "String", false, 2)
                    }
                }
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define(
            "ab", """
            S { §S§group1 {
              'a'
              'b'
            } } 
        """
        ) {
            asmSimple {
                tuple {
                    propertyTuple("\$group") {
                        propertyString("a", "a")
                        propertyString("b", "b")
                    }
                }
            }
        }
        tests.define("cde") {
            asmSimple {
                tuple {
                    propertyTuple("\$group") {
                        propertyString("c", "c")
                        propertyString("d", "d")
                        propertyString("e", "e")
                    }
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test // S = (a b | c d e) ;
    fun _7_1c_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = (a b | c d e) ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkRuntimeGrammar(proc, runtimeRuleSet("test.Test") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("a"); ref("b") }
                concatenation { ref("c"); ref("d"); ref("e") }
            }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        })

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            unnamedSuperTypeType("S") {
                tupleType {
                    propertyPrimitiveType("a", "String", false, 0)
                    propertyPrimitiveType("b", "String", false, 1)
                }
                tupleType {
                    propertyPrimitiveType("c", "String", false, 0)
                    propertyPrimitiveType("d", "String", false, 1)
                    propertyPrimitiveType("e", "String", false, 2)
                }
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("ab") {
            asmSimple {
                tuple {
                    propertyString("a", "a")
                    propertyString("b", "b")
                }
            }
        }
        tests.define("cde") {
            asmSimple {
                tuple {
                    propertyString("c", "c")
                    propertyString("d", "d")
                    propertyString("e", "e")
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test // S = ((a b) | (c d e)) ;
    fun _7_2_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = (a b | c d e) ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkRuntimeGrammar(proc, runtimeRuleSet("test.Test") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("a"); ref("b") }
                concatenation { ref("c"); ref("d"); ref("e") }
            }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        })

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            unnamedSuperTypeType("S") {
                tupleType {
                    propertyPrimitiveType("a", "String", false, 0)
                    propertyPrimitiveType("b", "String", false, 1)
                }
                tupleType {
                    propertyPrimitiveType("c", "String", false, 0)
                    propertyPrimitiveType("d", "String", false, 1)
                    propertyPrimitiveType("e", "String", false, 2)
                }
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("ab") {
            asmSimple {
                tuple {
                    propertyString("a", "a")
                    propertyString("b", "b")
                }
            }
        }
        tests.define("cde") {
            asmSimple {
                tuple {
                    propertyString("c", "c")
                    propertyString("d", "d")
                    propertyString("e", "e")
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test // S = x (a b | c d e) y ;
    fun _7_3_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = x (a b | c d e) y ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
                leaf x = 'x' ;
                leaf y = 'y' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkRuntimeGrammar(proc, runtimeRuleSet("test.Test") {
            concatenation("S") { ref("x"); ref("§S§choice1"); ref("y") }
            choice("§S§choice1", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("a"); ref("b") }
                concatenation { ref("c"); ref("d"); ref("e") }
            }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
            literal("x", "x")
            literal("y", "y")
        })

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            stringTypeFor("x")
            stringTypeFor("y")
            dataType("S", "S") {
                propertyPrimitiveType("x", "String", false, 0)
                propertyUnnamedSuperType("\$choice", false, 1) {
                    tupleType {
                        propertyPrimitiveType("a", "String", false, 0)
                        propertyPrimitiveType("b", "String", false, 1)
                    }
                    tupleType {
                        propertyPrimitiveType("c", "String", false, 0)
                        propertyPrimitiveType("d", "String", false, 1)
                        propertyPrimitiveType("e", "String", false, 2)
                    }
                }
                propertyPrimitiveType("y", "String", false, 2)
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define(
            "xaby", """
            S {
              'x'
              §S§choice1 {
                'a'
                'b'
              }
              'y'
            } 
        """
        ) {
            asmSimple {
                element("S") {
                    propertyString("x", "x")
                    propertyTuple("\$choice") {
                        propertyString("a", "a")
                        propertyString("b", "b")
                    }
                    propertyString("y", "y")
                }
            }
        }
        tests.define("xcdey") {
            asmSimple {
                element("S") {
                    propertyString("x", "x")
                    propertyTuple("\$choice") {
                        propertyString("c", "c")
                        propertyString("d", "d")
                        propertyString("e", "e")
                    }
                    propertyString("y", "y")
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test // S = CH; CH = a b | c d e ;
    fun _7_4_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = CH ;
                CH = a b | c d e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkRuntimeGrammar(proc, runtimeRuleSet("test.Test") {
            concatenation("S") { ref("CH") }
            choice("CH", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("a"); ref("b") }
                concatenation { ref("c"); ref("d"); ref("e") }
            }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        })

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            dataType("S", "S") {
                propertyUnnamedSuperType("ch", false, 0) {
                    tupleType {
                        propertyPrimitiveType("a", "String", false, 0)
                        propertyPrimitiveType("b", "String", false, 1)
                    }
                    tupleType {
                        propertyPrimitiveType("c", "String", false, 0)
                        propertyPrimitiveType("d", "String", false, 1)
                        propertyPrimitiveType("e", "String", false, 2)
                    }
                }
            }
            unnamedSuperTypeType("CH") {
                tupleType {
                    propertyPrimitiveType("a", "String", false, 0)
                    propertyPrimitiveType("b", "String", false, 1)
                }
                tupleType {
                    propertyPrimitiveType("c", "String", false, 0)
                    propertyPrimitiveType("d", "String", false, 1)
                    propertyPrimitiveType("e", "String", false, 2)
                }
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("ab") {
            asmSimple {
                element("S") {
                    propertyTuple("ch") {
                        propertyString("a", "a")
                        propertyString("b", "b")
                    }
                }
            }
        }
        tests.define("cde") {
            asmSimple {
                element("S") {
                    propertyTuple("ch") {
                        propertyString("c", "c")
                        propertyString("d", "d")
                        propertyString("e", "e")
                    }
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test // S = CH; CH = (a b | c d e) ;
    fun _7_5_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = CH ;
                CH = (a b | c d e) ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkRuntimeGrammar(proc, runtimeRuleSet("test.Test") {
            concatenation("S") { ref("CH") }
            choice("CH", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("a"); ref("b") }
                concatenation { ref("c"); ref("d"); ref("e") }
            }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
        })

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            dataType("S", "S") {
                propertyUnnamedSuperType("ch", false, 0) {
                    tupleType {
                        propertyPrimitiveType("a", "String", false, 0)
                        propertyPrimitiveType("b", "String", false, 1)
                    }
                    tupleType {
                        propertyPrimitiveType("c", "String", false, 0)
                        propertyPrimitiveType("d", "String", false, 1)
                        propertyPrimitiveType("e", "String", false, 2)
                    }
                }
            }
            unnamedSuperTypeType("CH") {
                tupleType {
                    propertyPrimitiveType("a", "String", false, 0)
                    propertyPrimitiveType("b", "String", false, 1)
                }
                tupleType {
                    propertyPrimitiveType("c", "String", false, 0)
                    propertyPrimitiveType("d", "String", false, 1)
                    propertyPrimitiveType("e", "String", false, 2)
                }
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("ab") {
            asmSimple {
                element("S") {
                    propertyTuple("ch") {
                        propertyString("a", "a")
                        propertyString("b", "b")
                    }
                }
            }
        }
        tests.define("cde") {
            asmSimple {
                element("S") {
                    propertyTuple("ch") {
                        propertyString("c", "c")
                        propertyString("d", "d")
                        propertyString("e", "e")
                    }
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test // S = x CH y ; CH = a b | c d e
    fun _7_6_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = x CH y ;
                CH = a b | c d e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
                leaf x = 'x' ;
                leaf y = 'y' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkRuntimeGrammar(proc, runtimeRuleSet("test.Test") {
            concatenation("S") { ref("x"); ref("CH"); ref("y") }
            choice("CH", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("a"); ref("b") }
                concatenation { ref("c"); ref("d"); ref("e") }
            }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
            literal("x", "x")
            literal("y", "y")
        })

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            stringTypeFor("x")
            stringTypeFor("y")
            dataType("S", "S") {
                propertyPrimitiveType("x", "String", false, 0)
                propertyUnnamedSuperType("ch", false, 1) {
                    tupleType {
                        propertyPrimitiveType("a", "String", false, 0)
                        propertyPrimitiveType("b", "String", false, 1)
                    }
                    tupleType {
                        propertyPrimitiveType("c", "String", false, 0)
                        propertyPrimitiveType("d", "String", false, 1)
                        propertyPrimitiveType("e", "String", false, 2)
                    }
                }
                propertyPrimitiveType("y", "String", false, 2)
            }
            unnamedSuperTypeType("CH") {
                tupleType {
                    propertyPrimitiveType("a", "String", false, 0)
                    propertyPrimitiveType("b", "String", false, 1)
                }
                tupleType {
                    propertyPrimitiveType("c", "String", false, 0)
                    propertyPrimitiveType("d", "String", false, 1)
                    propertyPrimitiveType("e", "String", false, 2)
                }
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("xaby") {
            asmSimple {
                element("S") {
                    propertyString("x", "x")
                    propertyTuple("ch") {
                        propertyString("a", "a")
                        propertyString("b", "b")
                    }
                    propertyString("y", "y")
                }
            }
        }
        tests.define("xcdey") {
            asmSimple {
                element("S") {
                    propertyString("x", "x")
                    propertyTuple("ch") {
                        propertyString("c", "c")
                        propertyString("d", "d")
                        propertyString("e", "e")
                    }
                    propertyString("y", "y")
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    @Test // S = x C y ; C = (a b | c d e)
    fun _7_7_rhs_choice_concat_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = x CH y ;
                CH = (a b | c d e) ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
                leaf x = 'x' ;
                leaf y = 'y' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        checkRuntimeGrammar(proc, runtimeRuleSet("test.Test") {
            concatenation("S") { ref("x"); ref("CH"); ref("y") }
            choice("CH", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("a"); ref("b") }
                concatenation { ref("c"); ref("d"); ref("e") }
            }
            literal("a", "a")
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            literal("e", "e")
            literal("x", "x")
            literal("y", "y")
        })

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            stringTypeFor("x")
            stringTypeFor("y")
            dataType("S", "S") {
                propertyPrimitiveType("x", "String", false, 0)
                propertyUnnamedSuperType("ch", false, 1) {
                    tupleType {
                        propertyPrimitiveType("a", "String", false, 0)
                        propertyPrimitiveType("b", "String", false, 1)
                    }
                    tupleType {
                        propertyPrimitiveType("c", "String", false, 0)
                        propertyPrimitiveType("d", "String", false, 1)
                        propertyPrimitiveType("e", "String", false, 2)
                    }
                }
                propertyPrimitiveType("y", "String", false, 2)
            }
            unnamedSuperTypeType("CH") {
                tupleType {
                    propertyPrimitiveType("a", "String", false, 0)
                    propertyPrimitiveType("b", "String", false, 1)
                }
                tupleType {
                    propertyPrimitiveType("c", "String", false, 0)
                    propertyPrimitiveType("d", "String", false, 1)
                    propertyPrimitiveType("e", "String", false, 2)
                }
            }
        })

        val tests = mutableListOf<TestData>()
        tests.define("xaby") {
            asmSimple {
                element("S") {
                    propertyString("x", "x")
                    propertyTuple("ch") {
                        propertyString("a", "a")
                        propertyString("b", "b")
                    }
                    propertyString("y", "y")
                }
            }
        }
        tests.define("xcdey") {
            asmSimple {
                element("S") {
                    propertyString("x", "x")
                    propertyTuple("ch") {
                        propertyString("c", "c")
                        propertyString("d", "d")
                        propertyString("e", "e")
                    }
                    propertyString("y", "y")
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }


    @Test
    fun _7_where_root_is_UnnamedSuperType() {
        TODO()
    }

    @Test
    fun _7_group_where_tuple_property_is_UnnamedSuperType() {
        TODO()
    }

    @Test
    fun _7_UnnamedSuperType_of_UnnamedSuperType() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a X? e ;
                X = R | D ;
                R = ( ( b c ) | (c d) ) ;
                D = ( 'x' | 'y' );
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)

        val tests = mutableListOf<TestData>()
        tests.define("ae") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyNull("x")
                    propertyString("e", "e")
                }
            }
        }
        tests.define("abce") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyTuple("x") {
                        propertyTuple("\$group") {
                            propertyString("b", "b")
                            propertyString("c", "c")
                        }
                    }
                    propertyString("e", "e")
                }
            }
        }
        tests.define("axe") {
            asmSimple {
                element("S") {
                    propertyString("a", "a")
                    propertyString("x", "x")
                    propertyString("e", "e")
                }
            }
        }
        for (data in tests) {
            test(proc, data)
        }
    }

    // Embedded
    @Test
    fun _8_e() {
        //  S = <e> | S a
        // S = d | B S
        // B = b I::S b | c I::S c
        val grammarStr = """
            namespace test
            grammar I {
                S = a | S a ;
                leaf a = 'a' ;
            }
            grammar O {
                S = d | B S ;
                B = b I::S b | c I::S c ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
            }
        """.trimIndent()
        val proc = testProc(grammarStr)
        val Inner = runtimeRuleSet("test.I") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("a") }
                concatenation { ref("S"); ref("a") }
            }
            literal("a", "a")
        }
        checkRuntimeGrammar(proc, runtimeRuleSet("test.O") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("d") }
                concatenation { ref("B"); ref("S") }
            }
            choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("b"); ref("§I§S§embedded1"); ref("b"); }
                concatenation { ref("c"); ref("§I§S§embedded1"); ref("c") }
            }
            literal("b", "b")
            literal("c", "c")
            literal("d", "d")
            embedded("§I§S§embedded1", Inner, "S")
        })

        /*
        Can't define this as it is recursive on UnnamedSuperTypes
        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
            unnamedSuperTypeType("S") {
                primitiveRef("String")
                tupleType {
                    propertyUnnamedSuperType("b", false, 1) {
                        tupleType {
                            propertyPrimitiveType("b", "String", false, 0)
                            propertyPrimitiveType("b2", "String", false, 2)
                        }
                        tupleType {
                            propertyPrimitiveType("c", "String", false, 0)
                            propertyPrimitiveType("c2", "String", false, 2)
                        }
                    }
                    propertyElementTypeOf("s", "S", false, 1)
                }
            }
            unnamedSuperTypeType("B") {
                tupleType {
                    propertyPrimitiveType("b", "String", false, 0)
                    propertyUnnamedSuperType("s", false, 1) {
                        primitiveRef("String")
                        tupleType {
                            propertyUnnamedSuperType("s", false, 0) {
                                //recursive
                            }
                            propertyPrimitiveType("a","String",false, 1)
                        }
                    }
                    propertyPrimitiveType("b2", "String", false, 2)
                }
                tupleType {
                    propertyPrimitiveType("c", "String", false, 0)
                    propertyPrimitiveType("c2", "String", false, 2)
                }
            }
        })*/

        val tests = mutableListOf<TestData>()
        tests.define("d") {
            asmSimple {
                string("d")
            }
        }
        tests.define(
            "babd",
            """
            S {
              B {
                b : 'b'
                §I§S§embedded1 : <EMBED>::S { a : 'a' } 
                b : 'b'
              }
              S { d : 'd' }
            } 
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
        val Inner = runtimeRuleSet("test.I") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("A") }
                concatenation { ref("SA") }
            }
            concatenation("SA") { ref("S"); ref("A") }
            concatenation("A") { ref("a") }
            literal("a", "a")
        }
        checkRuntimeGrammar(proc, runtimeRuleSet("test.O") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("B") }
                concatenation { ref("SBC") }
            }
            concatenation("SBC") { ref("S"); ref("BC") }
            choice("BC", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("B") }
                concatenation { ref("C") }
            }
            concatenation("B") { literal("b"); ref("§I§S§embedded1"); literal("b"); }
            concatenation("C") { literal("c"); ref("§I§S§embedded1"); literal("c") }
            embedded("§I§S§embedded1", Inner, "S")
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
        val Inner = runtimeRuleSet("test.I") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { literal("a") }
                concatenation { ref("S"); literal("a") }
            }
        }
        checkRuntimeGrammar(proc, runtimeRuleSet("test.O") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("B") }
                concatenation { ref("S"); ref("B") }
            }
            choice("B", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { literal("b"); ref("§I§S§embedded1"); literal("b"); }
                concatenation { literal("c"); ref("§I§S§embedded1"); literal("c") }
            }
            embedded("§I§S§embedded1", Inner, "S")
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
        val Inner = runtimeRuleSet("test.I") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("A") }
                concatenation { ref("SA") }
            }
            concatenation("SA") { ref("S"); ref("A") }
            concatenation("A") { ref("a") }
            literal("a", "a")
        }
        checkRuntimeGrammar(proc, runtimeRuleSet("test.O") {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("B") }
                concatenation { ref("S"); ref("BC") }
            }
            choice("BC", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                concatenation { ref("B") }
                concatenation { ref("C") }
            }
            concatenation("B") { literal("b"); ref("§I§S§embedded1"); literal("b"); }
            concatenation("C") { literal("c"); ref("§I§S§embedded1"); literal("c") }
            embedded("§I§S§embedded1", Inner, "S")
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

        checkTypeModel(proc, grammarTypeModel("test.Test", "Test", "S") {
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