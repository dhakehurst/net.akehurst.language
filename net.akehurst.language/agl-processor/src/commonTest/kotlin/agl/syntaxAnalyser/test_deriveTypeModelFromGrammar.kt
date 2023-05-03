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
import net.akehurst.language.api.typemodel.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_deriveTypeModelFromGrammar {

    private companion object {
        val grammarProc = Agl.registry.agl.grammar.processor ?: error("Internal error: AGL language processor not found")
    }

    @Test // S =  ;
    fun rhs_empty() {
        val grammarStr = """
            namespace test
            grammar Test {
                S =  ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = 'a' ;
    fun rhs_terminal_nonleaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = a ; leaf a = 'a' ;
    fun rhs_terminal_leaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ;
                leaf a = 'a' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("a", false, 0)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test //  S = "[a-z]" ;
    fun rhs_terminal_nonLeaf_pattern() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = "[a-z]" ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = v ; leaf v = "[a-z]" ;
    fun rhs_terminal_leaf_pattern() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = v ;
                leaf v = "[a-z]" ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("v", false, 0)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = A B C ;
    fun rhs_concat_nonTerm_x3_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A B C ;
                A = 'a' ;
                B = 'b' ;
                C = 'c' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("a", false, 0)
                propertyStringType("b", false, 1)
                propertyStringType("c", false, 2)
            }
            elementType("A", "A") {}
            elementType("B", "B") {}
            elementType("C", "C") {}
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = a ',' b ',' c ;
    fun rhs_concat_nonTerm_x3_literal_with_separator() {
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
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("a", false, 0)
                propertyStringType("b", false, 2)
                propertyStringType("c", false, 4)
            }
            elementType("a", "A") {}
            elementType("b", "B") {}
            elementType("c", "C") {}
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = 'a' | 'b' | 'c' ;
    fun rhs_choice_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' | 'b' | 'c' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            stringTypeFor("S")
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = A | B | C ; A = a x; B = b x; C = c x;
    fun rhs_choice_nonTerm() {
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
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                subTypes("A", "B", "C")
            }
            elementType("A", "A") {
                propertyStringType("a", false, 0)
                propertyStringType("x", false, 1)
            }
            elementType("B", "B") {
                propertyStringType("b", false, 0)
                propertyStringType("x", false, 1)
            }
            elementType("C", "C") {
                propertyStringType("c", false, 0)
                propertyStringType("x", false, 1)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = L | M ; L = 'a' | 'b' | 'c' ; M = 'x' | 'y' ;
    fun rhs_choice_of_choice_all_literal() {
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
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            stringTypeFor("S")
            stringTypeFor("L")
            stringTypeFor("M")
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = L | M ; L = a | b | c ;  M = x | y ;
    fun rhs_choice_of_choice_all_leaf() {
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            stringTypeFor("S")
            stringTypeFor("L")
            stringTypeFor("M")
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = A | B | C ; A = a x ; B = C | D ; C = c x; D = d x ;
    fun rhs_choice_of_choice_all_concats() {
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        //TODO: there are ambiguities! assertTrue(result.issues.errors.isEmpty(),result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                subTypes("A", "B", "C")
            }
            elementType("A", "A") {
                propertyStringType("a", false, 0)
                propertyStringType("x", false, 1)
            }
            elementType("B", "B") {
                subTypes("C", "D")
            }
            elementType("C", "C") {
                propertyStringType("c", false, 0)
                propertyStringType("x", false, 1)
            }
            elementType("D", "D") {
                propertyStringType("d", false, 0)
                propertyStringType("x", false, 1)
            }
        }
        println(actual.asString())
        assertEquals(expected.asString(), actual.asString())
        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = A | B | C ; A = a x ; B = c | D ; C = c ; D = d ;
    fun rhs_choice_of_choice_mixed_literal_and_concats() {
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        //TODO: there are ambiguities! assertTrue(result.issues.errors.isEmpty(),result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            val B = unnamedSuperTypeTypeFor("B", listOf(StringType, "D"))
            unnamedSuperTypeTypeFor("S", listOf("A", B, "C"))
            elementType("A", "A") {
                propertyStringType("a", false, 0)
                propertyStringType("x", false, 1)
            }
            elementType("C", "C") {
                propertyStringType("c", false, 0)
            }
            elementType("D", "D") {
                propertyStringType("d", false, 0)
            }
        }
        println(actual.asString())
        assertEquals(expected.asString(), actual.asString())
        TypeModelTest.assertEquals(expected, actual)
    }

    @Test //  S = 'a'? ;
    fun rhs_optional_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a'? ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = a 'b'? c ;
    fun concat_optional_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a 'b'? c ;
                leaf a = 'a' ;
                leaf c = 'c' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("a", false, 0)
                propertyStringType("c", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = a? ;
    fun rhs_optional_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a? ;
                leaf a = 'a';
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("a", true, 0)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = 'a'* ;
    fun rhs_list_literal_nonLeaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a'* ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {

            }
//            listTypeFor("S", StringType)
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = a 'b'* c ;
    fun concat_list_literal_nonLeaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a 'b'* c ;
                leaf a = 'a' ;
                leaf c = 'c' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("a", false, 0)
                propertyStringType("c", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test //  S = a* ;
    fun rhs_list_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a* ;
                leaf a = 'a';
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyListType("a", false, 0) { stringType() }
            }
            //listTypeFor("S", StringType)
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = a b* c ;
    fun concat_list_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a b* c ;
                leaf a = 'a';
                leaf b = 'b';
                leaf c = 'c';
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("a", false, 0)
                propertyListType("b", false, 1) { stringType() }
                propertyStringType("c", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test //S = as ; as = a* ;
    fun rhs_nonTerm_multi_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = a* ;
                leaf a = 'a' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyListType("as", false, 0) { stringType() }
            }
            //listTypeFor("as", StringType)
            elementType("as", "As") {
                propertyListType("a", false, 0) { stringType() }
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = as ; as = ['a' / ',']* ;
    fun rhs_nonTerm_sepList_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = as ;
                as = ['a' / ',']* ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                //propertyListSeparatedType("as", StringType, StringType, false, 0)
            }
            elementType("as", "As") {

            }
            //listSeparatedTypeFor("as", StringType, StringType)
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = a bs c ; bs = ['b' / ',']* ;
    fun concat_nonTerm_sepList_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a bs c;
                bs = ['b' / ',']* ;
                leaf a = 'a' ;
                leaf c = 'c' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("a", false, 0)
                propertyStringType("c", false, 2)
            }
            elementType("bs", "Bs") {

            }
            //listSeparatedTypeFor("as", StringType, StringType)
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = as ; as = [a / ',']* ;
    fun rhs_nonTerm_sepList_literal_leaf() {
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
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyListTypeOf("as", "A", false, 0) // of String
            }
            //listSeparatedTypeOf("as", "A", StringType)
            elementType("as", "As") {
                propertyListTypeOf("a", "A", false, 0) // of String
            }
            elementType("a", "A") {
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
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyListSeparatedTypeOf("ass", "As", StringType, false, 0) // of String
            }
            elementType("ass", "Ass") {
                propertyListTypeOf("as", "A", false, 0)
            }
            elementType("as", "As") {
                propertyListTypeOf("a", "A", false, 0)
            }
            //val t_as = listTypeOf("as", "A")
            //listSeparatedTypeFor("ass", t_as, StringType.use)
            elementType("a", "A") {
            }
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
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                //propertyListType("as", StringType, false, 0) // of String
            }
            elementType("as", "As") {

            }
            //listTypeFor("as", StringType)
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    // --- Group ---
    @Test // S = a ('b' 'c' 'e') e ;
    fun concat_group_concat_nonLeaf_literal() {
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
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("a", false, 0)
                propertyStringType("e", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = a (b c d) e ;
    fun concat_group_concat_leaf_literal() {
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
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
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

    @Test // S = a (b c d) (b a c) e ;
    fun concat_group_concat_leaf_literal_2() {
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
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
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

    @Test // S = a (b) e ;
    fun concat_group_1_leaf_literal() {
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
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("a", false, 0)
                propertyTupleType("\$group", false, 1) {
                    propertyStringType("b", false, 0)
                }
                propertyStringType("e", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = a (b (c) d) e ;
    fun concat_group_concat_group_group_leaf_literal() {
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("a", false, 0)
                propertyTupleType("\$group", false, 1) {
                    propertyStringType("b", false, 0)
                    propertyTupleType("\$group", false, 1) {
                        propertyStringType("c", false, 0)
                    }
                    propertyStringType("d", false, 0)
                }
                propertyStringType("e", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = a (b | c | d) e ;
    fun concat_group_choice_leaf_literal() {
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
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("a", false, 0)
                propertyStringType("\$choice", false, 1)
                propertyStringType("e", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = a (b c | d) e ;
    fun concat_group_choice_concat_leaf_literal() {
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
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("a", false, 0)
                property(
                    "\$choice", TypeUsage.ofType(
                        UnnamedSuperTypeType(
                            listOf(
                                TupleType {
                                    PropertyDeclaration(this, "b", StringType.use, 0)
                                    PropertyDeclaration(this, "c", StringType.use, 1)
                                },
                                StringType,
                            ).map { TypeUsage.ofType(it) }, false
                        )
                    ), 1
                )
                propertyStringType("e", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = a (b c | d e) f ;
    fun concat_group_choice_concat_leaf_literal_2() {
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
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("a", false, 0)
                property(
                    "\$choice", TypeUsage.ofType(UnnamedSuperTypeType(listOf(
                        TupleType {
                            PropertyDeclaration(this, "b", StringType.use, 0)
                            PropertyDeclaration(this, "c", StringType.use, 1)
                        },
                        TupleType {
                            PropertyDeclaration(this, "d", StringType.use, 0)
                            PropertyDeclaration(this, "e", StringType.use, 1)
                        }
                    ).map { TypeUsage.ofType(it) }, false
                    )
                    ), 1
                )
                propertyStringType("f", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test //  S = a (b? c) e ;
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("a", false, 0)
                propertyTupleType("\$group", false, 1) {
                    propertyStringType("b", true, 0)
                    propertyStringType("c", false, 2)
                }
                propertyStringType("e", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = a ( (b | c) (d?) e ) f ;
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
        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("a", false, 0)
                propertyTupleType("\$group", false, 1) {
                    propertyStringType("\$choice", false, 0)
                    propertyTupleType("\$group", false, 1) {
                        propertyStringType("d", true, 0)
                    }
                    propertyStringType("e", false, 0)
                }
                propertyStringType("f", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = (BC | d*) ;
    fun rhs_group_choice_concat_nonTerm_list() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = (BC | d*) ;
                BC = b c ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            val BC = elementType("BC", "BC") {
                propertyStringType("b", false, 0)
                propertyStringType("c", false, 1)
            }
            unnamedSuperTypeTypeFor("S", listOf(BC))
        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = a (BC | d*) e ;
    fun concat_group_choice_concat_nonterm_list() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a (BC | d*) e ;
                BC = b c ;
                leaf a = 'a' ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf e = 'e' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            val BC = elementType("BC", "BC") {
                propertyStringType("b", false, 0)
                propertyStringType("c", false, 1)
            }
            elementType("S", "S") {
                propertyStringType("a", false, 0)
                property(
                    "\$choice", TypeUsage.ofType(
                        UnnamedSuperTypeType(
                            listOf(TypeUsage.ofType(BC), ListSimpleType.ofType(StringType.use)), false
                        )
                    ), 1
                )
                propertyStringType("e", false, 2)
            }

        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test // S = ID (A | B)* ;
    fun concat_list_group_choice() {
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
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyStringType("id", false, 0)
                propertyListType("\$choiceList", false, 1) {
                    unnamedSuperTypeOf("A", "B")
                }
            }
            elementType("A", "A") {
                propertyStringType("name", false, 0)
                propertyStringType("number", false, 1)
            }
            elementType("B", "B") {
                propertyStringType("name", false, 0)
                propertyStringType("name2", false, 1)
            }
        }

        assertEquals(expected.asString(), actual.asString())
        TypeModelTest.assertEquals(expected, actual)
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
                var = NAME ;
                literal = NUMBER ;
                mul = expr '*' expr ;
                add = expr '+' expr ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyElementTypeOf("exprList", "ExprList", false, 0)
            }
            elementType("exprList", "ExprList") {
                propertyElementTypeOf("expr", "Expr", false, 0)
                propertyListUnnamedOfTupleType(false, 1) {
                    propertyElementTypeOf("expr", "Expr", false, 1)
                }
            }
            elementType("expr", "Expr") {
                subTypes("Root", "Mul", "Add")
            }
            elementType("root", "Root") {
                subTypes("Var", "Literal")
            }
            elementType("var", "Var") {
                propertyStringType("name", false, 0)
            }
            elementType("literal", "Literal") {
                propertyStringType("number", false, 0)
            }
            elementType("mul", "Mul") {
                propertyElementTypeOf("expr", "Expr", false, 0)
                propertyElementTypeOf("expr2", "Expr", false, 2)
            }
            elementType("add", "Add") {
                propertyElementTypeOf("expr", "Expr", false, 0)
                propertyElementTypeOf("expr2", "Expr", false, 2)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar(result.asm!!)
        val expected = typeModel("test", "Test") {
            elementType("S", "S") {
                propertyElementTypeOf("exprList", "ExprList", false, 0)
            }
            elementType("exprList", "ExprList") {
                propertyElementTypeOf("expr", "Expr", false, 0)
                propertyListUnnamedOfTupleType(false, 1) {
                    propertyElementTypeOf("expr", "Expr", false, 1)
                }
            }
            elementType("expr", "Expr") {
                subTypes("Root", "Mul", "Add")
            }
            elementType("root", "Root") {
                subTypes("Var", "Literal")
            }
            elementType("var", "Var") {
                propertyStringType("name", false, 0)
            }
            elementType("literal", "Literal") {
                propertyStringType("number", false, 0)
            }
            elementType("mul", "Mul") {
                propertyListSeparatedTypeOf("expr", "Expr", StringType, false, 0)
            }
            //listTypeOf("add", "Expr")
            elementType("add", "Add") {
                propertyListSeparatedTypeOf("expr", "Expr", StringType, false, 0)
            }
        }

        TypeModelTest.assertEquals(expected, actual)
    }
}