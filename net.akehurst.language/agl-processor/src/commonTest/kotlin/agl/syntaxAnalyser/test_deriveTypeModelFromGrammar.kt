/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
import net.akehurst.language.api.typeModel.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_deriveTypeModelFromGrammar {

    private companion object {
        val grammarProc = Agl.registry.agl.grammar.processor ?: error("Internal error: AGL language processor not found")
    }

    @Test
    fun nonleaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty())

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun leaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ;
                leaf a = 'a' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty())

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyStringType("a", false, 0)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun nonLeaf_pattern() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = "[a-z]" ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty())

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun leaf_pattern() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = v ;
                leaf v = "[a-z]" ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty())

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyStringType("v", false, 0)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun empty() {
        val grammarStr = """
            namespace test
            grammar Test {
                S =  ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty())

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun choice_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' | 'b' | 'c' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty())

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            stringTypeFor("S")
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun choice_nonTerm() {
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty())

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                subTypes("A", "B", "C")
            }
            elementType("A") {
                propertyStringType("a", false, 0)
                propertyStringType("x", false, 1)
            }
            elementType("B") {
                propertyStringType("b", false, 0)
                propertyStringType("x", false, 1)
            }
            elementType("C") {
                propertyStringType("c", false, 0)
                propertyStringType("x", false, 1)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun choice_of_choice_all_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = L | M ;
                L = 'a' | 'b' | 'c' ;
                M = 'x' | 'y' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty())

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            stringTypeFor("S")
            stringTypeFor("L")
            stringTypeFor("M")
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun choice_of_choice_all_concats() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A | B | C ;
                A = a x;
                B = C | D;
                C = c x;
                D = d x;
                leaf a = 'a';
                leaf b = 'b';
                leaf c = 'c';
                leaf d = 'd';
                leaf x = 'x';
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        //TODO: there are ambiguities! assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                subTypes("A", "B", "C")
            }
            elementType("A") {
                propertyStringType("a", false, 0)
                propertyStringType("x", false, 1)
            }
            elementType("B") {
                subTypes("C", "D")
            }
            elementType("C") {
                propertyStringType("c", false, 0)
                propertyStringType("x", false, 1)
            }
            elementType("D") {
                propertyStringType("d", false, 0)
                propertyStringType("x", false, 1)
            }
        }
println(actual.asString())
        assertEquals(expected.asString(), actual.asString())
        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun choice_of_choice_mixed_literal_and_concats() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A | B | C ;
                A = a x;
                B = c | D;
                C = c;
                D = d;
                leaf a = 'a';
                leaf b = 'b';
                leaf c = 'c';
                leaf d = 'd';
                leaf x = 'x';
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        //TODO: there are ambiguities! assertTrue(result.issues.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            unnamedSuperTypeTypeFor("S", listOf("A", "B", "C"))

            elementType("A") {
                propertyStringType("a", false, 0)
                propertyStringType("x", false, 1)
            }

            unnamedSuperTypeTypeFor("B", listOf(StringType, "D"))

            elementType("C") {
                propertyStringType("c", false, 0)
                propertyStringType("x", false, 1)
            }
            elementType("D") {
                propertyStringType("d", false, 0)
            }
        }
        println(actual.asString())
        assertEquals(expected.asString(), actual.asString())
        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun optional_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a'? ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty())

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyStringTypeUnnamed(true, 0) // of String
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun list_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a'* ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyListTypeUnnamed(StringType, false, 0) // of String
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun list_of_group() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = ID (A | B)* ;
                A = NAME NUMBER ;
                B = NAME NAME ;
                leaf ID = "[a-z]" ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z][a-zA-Z0-9]+" ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyStringType("id", false, 0)
                propertyListTypeUnnamedOfUnnamedSuperTypeType(listOf("A","B"),1)
            }
            elementType("A") {
                propertyStringType("name", false, 0)
                propertyStringType("number", false, 1)
            }
            elementType("B") {
                propertyStringType("name", false, 0)
                propertyStringType("name2", false, 1)
            }
        }

        assertEquals(expected.asString(),actual.asString())
        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun sepList_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = ['a' / ',']* ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyListSeparatedType("as", StringType, StringType, false, 0)
            }
            elementType("As") {
                propertyUnnamedListSeparatedType(StringType, StringType, false, 0)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun sepList_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = [a / ',']* ;
                a = 'a' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyListSeparatedTypeOf("as", "A", StringType, false, 0) // of String
            }
            elementType("As") {
                propertyListSeparatedTypeOf("a", "A", StringType, false, 0)
            }
            elementType("A") {
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun sepList_multi_nonTerm() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = ass ;
                ass = [as / ',']* ;
                as = a* ;
                a = 'a' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyListSeparatedTypeOf("ass", "As", StringType, false, 0) // of String
            }
            elementType("Ass") {
                propertyListSeparatedTypeOf("as", "As", StringType, false, 0)
            }
            elementType("As") {
                propertyListTypeOf("a", "A", false, 0)
            }
            elementType("A") {
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun nonTerm_x3_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a b c ;
                a = 'a' ;
                b = 'b' ;
                c = 'c' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyStringType("a", false, 0)
                propertyStringType("b", false, 1)
                propertyStringType("c", false, 2)
            }
            elementType("A") {}
            elementType("B") {}
            elementType("C") {}
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun nonTerm_x3_literal_with_separator() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ',' b ',' c ;
                a = 'a' ;
                b = 'b' ;
                c = 'c' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty())

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyStringType("a", false, 0)
                propertyStringType("b", false, 2)
                propertyStringType("c", false, 4)
            }
            elementType("A") {}
            elementType("B") {}
            elementType("C") {}
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun nonTerm_multi_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = 'a'* ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyListType("as", StringType, false, 0) // of String
            }
            elementType("As") {
                propertyListTypeUnnamed(StringType, false, 0) // of String
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun group_concat_nonLeaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ('b' 'c' 'e') e ;
                leaf a = 'a' ;
                leaf e = 'e' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyStringType("a", false, 0)
                propertyStringType("e", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
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
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyStringType("a", false, 0)
                propertyTupleType("\$group", false, 1) {
                    propertyStringType("b", false, 0)
                    propertyStringType("c", false, 1)
                    propertyStringType("d", false, 2)
                }
                propertyStringType("e", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun group_concat_leaf_literal_2() {
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyStringType("a", false, 0)
                propertyTupleType("\$group", false, 1) {
                    propertyStringType("b", false, 0)
                    propertyStringType("c", false, 1)
                    propertyStringType("d", false, 2)
                }
                propertyTupleType("\$group2", false, 2) {
                    propertyStringType("b", false, 0)
                    propertyStringType("a", false, 1)
                    propertyStringType("c", false, 2)
                }
                propertyStringType("e", false, 3)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun group_concat_1_leaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (b) e ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf e = 'e' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty())

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyStringType("a", false, 0)
                propertyStringType("\$group", false, 1)
                propertyStringType("e", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
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
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyStringType("a", false, 0)
                propertyStringType("\$group", false, 1)
                propertyStringType("e", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
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
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyStringType("a", false, 0)
                property(
                    "\$group", UnnamedSuperTypeType(
                        listOf(
                            TupleType {
                                PropertyDeclaration(this, "b", StringType, false, 0)
                                PropertyDeclaration(this, "c", StringType, false, 1)
                            },
                            StringType,
                        )
                    ), false, 1
                )
                propertyStringType("e", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun group_choice_concat_leaf_literal_2() {
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyStringType("a", false, 0)
                property(
                    "\$group", UnnamedSuperTypeType(listOf(
                        TupleType {
                            PropertyDeclaration(this, "b", StringType, false, 0)
                            PropertyDeclaration(this, "c", StringType, false, 1)
                        },
                        TupleType {
                            PropertyDeclaration(this, "d", StringType, false, 0)
                            PropertyDeclaration(this, "e", StringType, false, 1)
                        }
                    )), false, 1
                )
                propertyStringType("f", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun list_expressions() {
        val grammarStr = """
            namespace test
            grammar Test {
                skip WS = "\s+" ;
                S = exprList ;
                exprList = expr (';' expr)* ;
                expr = root | mul | add ;
                root = var | literal ;
                var = NAME ;
                literal = NUMBER ;
                mul = [expr / '*']2+ ;
                add = [expr / '+']2+ ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })

        val actual = TypeModelFromGrammar(result.asm!!.last())
        val expected = typeModel("test", "Test") {
            elementType("S") {
                propertyElementTypeOf("exprList", "ExprList", false, 0)
            }
            elementType("ExprList") {
                propertyElementTypeOf("expr", "Expr", false, 0)
                propertyListUnnamedOfTupleType(false, 1){
                    propertyElementTypeOf("expr", "Expr", false, 1)
                }
            }
            elementType("Expr") {
                subTypes("Root","Mul","Add")
            }
            elementType("Root") {
                subTypes("Var","Literal")
            }
            elementType("Var") {
                propertyStringType("name",false,0)
            }
            elementType("Literal") {
                propertyStringType("number",false,0)
            }
            elementType("Mul") {
                propertyListSeparatedTypeOf("expr","Expr", StringType, false,0)
            }
            elementType("Add") {
                propertyListSeparatedTypeOf("expr","Expr", StringType, false,0)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }
}