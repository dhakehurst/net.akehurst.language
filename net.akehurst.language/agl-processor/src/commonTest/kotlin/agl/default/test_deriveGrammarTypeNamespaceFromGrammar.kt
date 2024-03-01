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

package net.akehurst.language.agl.syntaxAnalyser


import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.default.GrammarNamespaceAndAsmTransformBuilderFromGrammar
import net.akehurst.language.agl.default.GrammarTypeNamespaceFromGrammar
import net.akehurst.language.agl.default.TypeModelFromGrammar
import net.akehurst.language.agl.grammarTypeModel.GrammarTypeModelTest
import net.akehurst.language.agl.grammarTypeModel.grammarTypeModel
import net.akehurst.language.agl.language.asmTransform.asmTransform
import net.akehurst.language.agl.language.asmTransform.test.AsmTransformModelTest
import net.akehurst.language.agl.language.grammar.ContextFromGrammarRegistry
import net.akehurst.language.api.language.asmTransform.AsmTransformModel
import net.akehurst.language.test.FixMethodOrder
import net.akehurst.language.test.MethodSorters
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.simple.TypeModelSimple
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class test_deriveGrammarTypeNamespaceFromGrammar {

    private companion object {
        val grammarProc = Agl.registry.agl.grammar.processor ?: error("Internal error: AGL language processor not found")

        fun test(grammarStr: String, expectedTr: AsmTransformModel, expectedTm: TypeModel) {
            val result = grammarProc.process(grammarStr, Agl.options {
                semanticAnalysis {
                    context(ContextFromGrammarRegistry(Agl.registry))
                }
            })
            assertNotNull(result.asm)
            assertTrue(result.issues.isEmpty(), result.issues.toString())
            val grammar = result.asm!!.first()

            val grmrTypeModel = TypeModelSimple(grammar.name)
            grmrTypeModel.addNamespace(SimpleTypeModelStdLib)
            val atfg = GrammarNamespaceAndAsmTransformBuilderFromGrammar(grmrTypeModel, grammar)
            atfg.build()

            assertTrue(atfg.issues.isEmpty(), atfg.issues.toString())
            val actualTm = atfg.typeModel
            val actualTr = atfg.transformModel

            GrammarTypeModelTest.tmAssertEquals(expectedTm, actualTm)
            AsmTransformModelTest.trAssertEquals(expectedTr, actualTr)
        }
    }

    // --- Empty ---
    @Test // S =  ;
    fun _00_rhs_empty() {
        val grammarStr = """
            namespace test
            grammar Test {
                S =  ;
            }
        """.trimIndent()

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }

        test(grammarStr, expectedTr, expectedTm)
    }

    // --- Literal ---
    @Test // S = 'a' ;
    fun _11_rhs_terminal_nonleaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """.trimIndent()

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S")
        }
        test(grammarStr, expectedTr, expectedTm)
    }

    @Test // S = a ; leaf a = 'a' ;
    fun _12_rhs_terminal_leaf_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ;
                leaf a = 'a' ;
            }
        """.trimIndent()


        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            stringRule("a")
            createObject("S", "S") {
                assignment("a", "child[0]")
            }
        }
        test(grammarStr, expectedTr, expectedTm)
    }

    // --- Pattern ---
    @Test //  S = "[a-z]" ;
    fun _13_rhs_terminal_nonLeaf_pattern() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = "[a-z]" ;
            }
        """.trimIndent()

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
            }
        }
        test(grammarStr, expectedTr, expectedTm)
    }

    @Test // S = v ; leaf v = "[a-z]" ;
    fun _14_rhs_terminal_leaf_pattern() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = v ;
                leaf v = "[a-z]" ;
            }
        """.trimIndent()

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("v")
            dataType("S", "S") {
                propertyPrimitiveType("v", "String", false, 0)
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            stringRule("v")
            createObject("S", "S") {
                assignment("v", "child[0]")
            }
        }
        test(grammarStr, expectedTr, expectedTm)
    }

    // --- Concatenation ---
    @Test // S = A B C ;
    fun _21_rhs_concat_nonTerm_x3_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A B C ;
                A = 'a' ;
                B = 'b' ;
                C = 'c' ;
            }
        """.trimIndent()

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("b", "String", false, 1)
                propertyPrimitiveType("c", "String", false, 2)
            }
            dataType("A", "A") {}
            dataType("B", "B") {}
            dataType("C", "C") {}
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("b", "child[1]")
                assignment("c", "child[2]")
            }
            createObject("A", "A")
            createObject("B", "B")
            createObject("C", "C")
        }
        test(grammarStr, expectedTr, expectedTm)
    }

    @Test // S = a ',' b ',' c ;
    fun _22_rhs_concat_nonTerm_x3_literal_with_separator() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a ',' b ',' c ;
                a = 'a' ;
                b = 'b' ;
                c = 'c' ;
            }
        """.trimIndent()

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("b", "String", false, 2)
                propertyPrimitiveType("c", "String", false, 4)
            }
            dataType("a", "A") {}
            dataType("b", "B") {}
            dataType("c", "C") {}
        }

        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("b", "child[2]")
                assignment("c", "child[4]")
            }
            createObject("a", "A")
            createObject("b", "B")
            createObject("c", "C")
        }
        test(grammarStr, expectedTr, expectedTm)
    }

    // --- Choice ---
    @Test // S = 'a' | 'b' | 'c' ;
    fun _31_rhs_choice_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' | 'b' | 'c' ;
            }
        """.trimIndent()

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("S")
        }

        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            stringRule("S")
        }
        test(grammarStr, expectedTr, expectedTm)
    }

    @Test // S = A | B | C ; A = a x; B = b x; C = c x;
    fun _32_rhs_choice_nonTerm() {
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

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("x")
            dataType("S", "S") {
                subtypes("A", "B", "C")
            }
            dataType("A", "A") {
                supertypes("S")
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
            dataType("B", "B") {
                supertypes("S")
                propertyPrimitiveType("b", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
            dataType("C", "C") {
                supertypes("S")
                propertyPrimitiveType("c", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            stringRule("a")
            stringRule("b")
            stringRule("c")
            stringRule("x")
            subtypeRule("S", "S")
            createObject("A", "A") {
                assignment("a", "child[0]")
                assignment("x", "child[1]")
            }
            createObject("B", "B") {
                assignment("b", "child[0]")
                assignment("x", "child[1]")
            }
            createObject("C", "C") {
                assignment("c", "child[0]")
                assignment("x", "child[1]")
            }
        }
        test(grammarStr, expectedTr, expectedTm)
    }

    @Test // S = L | M ; L = 'a' | 'b' | 'c' ; M = 'x' | 'y' ;
    fun _33_rhs_choice_of_choice_all_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = L | M ;
                L = 'a' | 'b' | 'c' ;
                M = 'x' | 'y' ;
            }
        """.trimIndent()

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("S")
            stringTypeFor("L")
            stringTypeFor("M")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            stringRule("S")
            stringRule("L")
            stringRule("M")
        }
        test(grammarStr, expectedTr, expectedTm)
    }

    @Test // S = L | M ; L = a | b | c ;  M = x | y ;
    fun _34_rhs_choice_of_choice_all_leaf() {
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

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("S")
            stringTypeFor("L")
            stringTypeFor("M")
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("x")
            stringTypeFor("y")
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            stringRule("S")
            stringRule("L")
            stringRule("M")
            stringRule("a")
            stringRule("b")
            stringRule("c")
            stringRule("x")
            stringRule("y")
        }
        test(grammarStr, expectedTr, expectedTm)
    }

    @Test // S = A | B | C ; A = a x ; B = C | D ; C = c x; D = d x ;
    fun _35_rhs_choice_of_choice_all_concats() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A | B | C ;
                A = a x ;
                B = C | D ;
                C = c x ;
                D = d x ;
                leaf a = 'a' ;
                leaf c = 'c' ;
                leaf d = 'd' ;
                leaf x = 'x' ;
            }
        """.trimIndent()

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("x")
            dataType("S", "S") {
                subtypes("A", "B", "C")
            }
            dataType("A", "A") {
                supertypes("S")
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
            dataType("B", "B") {
                supertypes("S")
                subtypes("C", "D")
            }
            dataType("C", "C") {
                supertypes("S", "B")
                propertyPrimitiveType("c", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
            dataType("D", "D") {
                supertypes("B")
                propertyPrimitiveType("d", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            stringRule("a")
            stringRule("c")
            stringRule("d")
            stringRule("x")
            subtypeRule("S", "S")
            createObject("A", "A") {
                assignment("a", "child[0]")
                assignment("x", "child[1]")
            }
            subtypeRule("B", "B")
            createObject("C", "C") {
                assignment("c", "child[0]")
                assignment("x", "child[1]")
            }
            createObject("D", "D") {
                assignment("d", "child[0]")
                assignment("x", "child[1]")
            }
        }
        test(grammarStr, expectedTr, expectedTm)
    }

    @Test // S = A | B | C ; A = a x ; B = c | D ; C = c ; D = d ;
    fun _36_rhs_choice_of_choice_mixed_literal_and_concats() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = A | B | C ;
                A = a x ;
                B = c | D ;
                C = c ;
                D = d ;
                leaf a = 'a';
                leaf c = 'c';
                leaf d = 'd';
                leaf x = 'x';
            }
        """.trimIndent()

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("x")
            val B = unnamedSuperTypeTypeOf("B", listOf(StringType, "D"))
            unnamedSuperTypeTypeOf("S", listOf("A", B, "C"))
            dataType("A", "A") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("x", "String", false, 1)
            }
            dataType("C", "C") {
                propertyPrimitiveType("c", "String", false, 0)
            }
            dataType("D", "D") {
                propertyPrimitiveType("d", "String", false, 0)
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            stringRule("a")
            stringRule("c")
            stringRule("d")
            stringRule("x")
            unnamedSubtypeRule("S")
            createObject("A", "A") {
                assignment("a", "child[0]")
                assignment("x", "child[1]")
            }
            unnamedSubtypeRule("B")
            createObject("C", "C") {
                assignment("c", "child[0]")
            }
            createObject("D", "D") {
                assignment("d", "child[0]")
            }
        }
        test(grammarStr, expectedTr, expectedTm)
    }

    // --- Optional ---
    @Test //  S = 'a'? ;
    fun _41_rhs_optional_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a'? ;
            }
        """.trimIndent()

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
            }
        }
        test(grammarStr, expectedTr, expectedTm)
    }

    @Test // S = a 'b'? c ;
    fun _42_concat_optional_literal() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a 'b'? c ;
                leaf a = 'a' ;
                leaf c = 'c' ;
            }
        """.trimIndent()

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("c")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("c", "String", false, 2)
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            stringRule("a")
            stringRule("c")

            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("c", "child[2]")
            }
        }
        test(grammarStr, expectedTr, expectedTm)
    }

    @Test // S = a? ;
    fun _43_rhs_optional_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a? ;
                leaf a = 'a';
            }
        """.trimIndent()

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", true, 0)
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            stringRule("a")
            createObject("S", "S") {
                assignment("a", "child[0]")
            }
        }
        test(grammarStr, expectedTr, expectedTm)
    }

    // --- ListSimple ---
    @Test // S = 'a'* ;
    fun _5_list_literal_nonLeaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a'* ;
            }
        """.trimIndent()

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {

            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
            }
        }
        test(grammarStr, expectedTr, expectedTm)
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

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("c")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("c", "String", false, 2)
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            stringRule("a")
            stringRule("c")

            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("c", "child[2]")
            }
        }
        test(grammarStr, expectedTr, expectedTm)
    }

    @Test //  S = a* ;
    fun _5_list_literal_leaf() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = a* ;
                leaf a = 'a';
            }
        """.trimIndent()

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            dataType("S", "S") {
                propertyListType("a", false, 0) { primitiveRef("String") }
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            stringRule("a")
            createObject("S", "S") {
                assignment("a", "children")
            }
        }
        test(grammarStr, expectedTr, expectedTm)
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

        val expectedTm = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyListType("b", false, 1) { primitiveRef("String") }
                propertyPrimitiveType("c", "String", false, 2)
            }
        }
        val expectedTr = asmTransform("test.Test", typeModel = expectedTm, false) {
            createObject("S", "S") {
                assignment("a", "child[0]")
                assignment("b", "child[1]")
                assignment("c", "child[2]")
            }
        }

        test(grammarStr, expectedTr, expectedTm)
    }

    @Test //S = as ; as = a* ;
    fun _5_nonTerm_multi_literal_leaf() {
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

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            dataType("S", "S") {
                propertyListType("as", false, 0) { primitiveRef("String") }
            }
            //listTypeFor("as", StringType)
            dataType("as", "As") {
                propertyListType("a", false, 0) { primitiveRef("String") }
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test // S = as ; as = ['a' / ',']* ;
    fun _5_nonTerm_sepList_literal() {
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

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                //propertyListSeparatedType("as", StringType, StringType, false, 0)
            }
            dataType("as", "As") {

            }
            //listSeparatedTypeFor("as", StringType, StringType)
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    // --- ListSeparated ---
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("c")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("c", "String", false, 2)
            }
            dataType("bs", "Bs") {

            }
            //listSeparatedTypeFor("as", StringType, StringType)
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test // S = as ; as = [a / ',']* ;
    fun _6_nonTerm_sepList_literal_leaf() {
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

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyListTypeOf("as", "A", false, 0) // of String
            }
            //listSeparatedTypeOf("as", "A", StringType)
            dataType("as", "As") {
                propertyListTypeOf("a", "A", false, 0) // of String
            }
            dataType("a", "A") {
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test
    fun _6_sepList_multi_nonTerm() {
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

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyListTypeOf("ass", "As", false, 0) // of String
            }
            dataType("ass", "Ass") {
                propertyListTypeOf("as", "As", false, 0)
            }
            dataType("as", "As") {
                propertyListTypeOf("a", "A", false, 0)
            }
            //val t_as = listTypeOf("as", "A")
            //listSeparatedTypeFor("ass", t_as, StringType.use)
            dataType("a", "A") {
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test
    fun _6_nonTerm_multi_literal() {
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

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                //propertyListType("as", StringType, false, 0) // of String
            }
            dataType("as", "As") {

            }
            //listTypeFor("as", StringType)
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    // --- Group ---
    @Test // S = a ('b' 'c' 'e') e ;
    fun _7_concat_group_concat_nonLeaf_literal() {
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

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("e")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("e", "String", false, 2)
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test // S = a (b c d) e ;
    fun _7_concat_group_concat_leaf_literal() {
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

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyTupleType("\$group", false, 1) {
                    propertyPrimitiveType("b", "String", false, 0)
                    propertyPrimitiveType("c", "String", false, 1)
                    propertyPrimitiveType("d", "String", false, 2)
                }
                propertyPrimitiveType("e", "String", false, 2)
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyTupleType("\$group", false, 1) {
                    propertyPrimitiveType("b", "String", false, 0)
                    propertyPrimitiveType("c", "String", false, 1)
                    propertyPrimitiveType("d", "String", false, 2)
                }
                propertyTupleType("\$group2", false, 2) {
                    propertyPrimitiveType("b", "String", false, 0)
                    propertyPrimitiveType("a", "String", false, 1)
                    propertyPrimitiveType("c", "String", false, 2)
                }
                propertyPrimitiveType("e", "String", false, 3)
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("e")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyTupleType("\$group", false, 1) {
                    propertyPrimitiveType("b", "String", false, 0)
                }
                propertyPrimitiveType("e", "String", false, 2)
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test // S = a (b (c) d) e ;
    fun _7_concat_group_concat_group_group_leaf_literal() {
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

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyTupleType("\$group", false, 1) {
                    propertyPrimitiveType("b", "String", false, 0)
                    propertyTupleType("\$group", false, 1) {
                        propertyPrimitiveType("c", "String", false, 0)
                    }
                    propertyPrimitiveType("d", "String", false, 2)
                }
                propertyPrimitiveType("e", "String", false, 2)
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyPrimitiveType("\$choice", "String", false, 1)
                propertyPrimitiveType("e", "String", false, 2)
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            val gtb = this
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyUnnamedSuperType("\$choice", false, 1) {
                    tupleType {
                        propertyPrimitiveType("b", "String", false, 0)
                        propertyPrimitiveType("c", "String", false, 1)
                    }
                    primitiveRef("String")
                }
                propertyPrimitiveType("e", "String", false, 2)
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            val gtb = this
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            stringTypeFor("f")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyUnnamedSuperType("\$choice", false, 1) {
                    tupleType {
                        propertyPrimitiveType("b", "String", false, 0)
                        propertyPrimitiveType("c", "String", false, 1)
                    }
                    tupleType {
                        propertyPrimitiveType("d", "String", false, 0)
                        propertyPrimitiveType("e", "String", false, 1)
                    }
                }
                propertyPrimitiveType("f", "String", false, 2)
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test //  S = a (b? c) e ;
    fun _7_group_concat_optional() {
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

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyTupleType("\$group", false, 1) {
                    propertyPrimitiveType("b", "String", true, 0)
                    propertyPrimitiveType("c", "String", false, 1)
                }
                propertyPrimitiveType("e", "String", false, 2)
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test // S = a ( (b | c) (d?) e ) f ;
    fun _7_group_choice_group_concat_optional() {
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

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            stringTypeFor("f")
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyTupleType("\$group", false, 1) {
                    propertyPrimitiveType("\$choice", "String", false, 0)
                    propertyTupleType("\$group", false, 1) {
                        propertyPrimitiveType("d", "String", true, 0)
                    }
                    propertyPrimitiveType("e", "String", false, 2)
                }
                propertyPrimitiveType("f", "String", false, 2)
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test // S = (BC | d*) ;
    fun _7_rhs_group_choice_concat_term_list() {
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

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            unnamedSuperTypeType("S") {
                elementRef("BC")
                listType(false) { primitiveRef("String") }
            }
            dataType("BC", "BC") {
                propertyPrimitiveType("b", "String", false, 0)
                propertyPrimitiveType("c", "String", false, 1)
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test // S = (BC | d*) ;
    fun _7_rhs_group_choice_concat_nonTerm_list() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = (BC | D*) ;
                BC = b c ;
                leaf b = 'b' ;
                leaf c = 'c' ;
                D = d ;
                leaf d = 'd' ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            unnamedSuperTypeType("S") {
                elementRef("BC")
                listType(false) {
                    elementRef("D")
                }
            }
            dataType("BC", "BC") {
                propertyPrimitiveType("b", "String", false, 0)
                propertyPrimitiveType("c", "String", false, 1)
            }
            dataType("D", "D") {
                propertyPrimitiveType("d", "String", false, 0)
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
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

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            val gtb = this
            stringTypeFor("a")
            stringTypeFor("b")
            stringTypeFor("c")
            stringTypeFor("d")
            stringTypeFor("e")
            val BC = dataType("BC", "BC") {
                propertyPrimitiveType("b", "String", false, 0)
                propertyPrimitiveType("c", "String", false, 1)
            }
            dataType("S", "S") {
                propertyPrimitiveType("a", "String", false, 0)
                propertyUnnamedSuperType("\$choice", false, 1) { elementRef("BC"); listType(false) { primitiveRef("String") } }
                propertyPrimitiveType("e", "String", false, 2)
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
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

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            stringTypeFor("ID")
            stringTypeFor("NAME")
            stringTypeFor("NUMBER")
            dataType("S", "S") {
                propertyPrimitiveType("id", "String", false, 0)
                propertyListType("\$choiceList", false, 1) {
                    unnamedSuperTypeOf("A", "B")
                }
            }
            dataType("A", "A") {
                propertyPrimitiveType("name", "String", false, 0)
                propertyPrimitiveType("number", "String", false, 1)
            }
            dataType("B", "B") {
                propertyPrimitiveType("name", "String", false, 0)
                propertyPrimitiveType("name2", "String", false, 1)
            }
        }

        assertEquals(expected.asString(), actual.asString())
        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test
    fun embedded() {
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val tmI = grammarTypeModel("test.I", "Inner", "S") {
            stringTypeFor("a")
            dataType("S", "S") {
                subtypes("A", "SA")
            }
            dataType("SA", "SA") {
                supertypes("S")
                propertyDataTypeOf("s", "S", false, 0)
                propertyDataTypeOf("a", "A", false, 1)
            }
            dataType("A", "A") {
                supertypes("S")
                propertyPrimitiveType("a", "String", false, 0)
            }
        }
        val expected = grammarTypeModel(
            "test.O", "O", "S", listOf(
                SimpleTypeModelStdLib,
                tmI.allNamespace[1]
            )
        ) {
            dataType("S", "S") {
                subtypes("B", "SBC")
            }
            dataType("SBC", "SBC") {
                supertypes("S")
                propertyDataTypeOf("s", "S", false, 0)
                propertyDataTypeOf("bc", "BC", false, 1)
            }
            dataType("BC", "BC") {
                subtypes("B", "C")
            }
            dataType("C", "C") {
                supertypes("BC")
                propertyDataTypeOf("s", "test.I.S", false, 1)
            }
            dataType("B", "B") {
                supertypes("S", "BC")
                propertyDataTypeOf("s", "test.I.S", false, 1)
            }
        }

        assertEquals(expected.asString(), actual.asString())
        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test
    fun embedded2() {
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

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.O", "O", "S") {
            unnamedSuperTypeType("S") {
                elementRef("B")
                tupleType {
                    propertyDataTypeOf("s", "S", false, 0)
                    propertyDataTypeOf("bc", "BC", false, 1)
                }
            }
            dataType("BC", "BC") {
                subtypes("B", "C")
            }
            dataType("C", "C") {
                propertyPrimitiveType("s", "S", false, 1)
            }
            dataType("B", "B") {
                propertyPrimitiveType("s", "S", false, 1)
            }
        }

        assertEquals(expected.asString(), actual.asString())
        GrammarTypeModelTest.tmAssertEquals(expected, actual)
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

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyDataTypeOf("exprList", "ExprList", false, 0)
            }
            dataType("exprList", "ExprList") {
                propertyDataTypeOf("expr", "Expr", false, 0)
                propertyListOfTupleType(GrammarTypeNamespaceFromGrammar.UNNAMED_LIST_PROPERTY_NAME, false, 1) {
                    propertyDataTypeOf("expr", "Expr", false, 1)
                }
            }
            dataType("expr", "Expr") {
                subtypes("Root", "Mul", "Add")
            }
            dataType("root", "Root") {
                subtypes("Var", "Literal")
            }
            dataType("var", "Var") {
                propertyPrimitiveType("name", "String", false, 0)
            }
            dataType("literal", "Literal") {
                propertyPrimitiveType("number", "String", false, 0)
            }
            dataType("mul", "Mul") {
                propertyDataTypeOf("expr", "Expr", false, 0)
                propertyDataTypeOf("expr2", "Expr", false, 2)
            }
            dataType("add", "Add") {
                propertyDataTypeOf("expr", "Expr", false, 0)
                propertyDataTypeOf("expr2", "Expr", false, 2)
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
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

        val actual = TypeModelFromGrammar.create(result.asm!!.last())
        val expected = grammarTypeModel("test.Test", "Test", "S") {
            dataType("S", "S") {
                propertyDataTypeOf("exprList", "ExprList", false, 0)
            }
            dataType("exprList", "ExprList") {
                propertyDataTypeOf("expr", "Expr", false, 0)
                propertyListOfTupleType(GrammarTypeNamespaceFromGrammar.UNNAMED_LIST_PROPERTY_NAME, false, 1) {
                    propertyDataTypeOf("expr", "Expr", false, 1)
                }
            }
            dataType("expr", "Expr") {
                subtypes("Root", "Mul", "Add")
            }
            dataType("root", "Root") {
                subtypes("Var", "Literal")
            }
            dataType("var", "Var") {
                propertyPrimitiveType("name", "String", false, 0)
            }
            dataType("literal", "Literal") {
                propertyPrimitiveType("number", "String", false, 0)
            }
            dataType("mul", "Mul") {
                propertyListSeparatedTypeOf("expr", "Expr", "String", false, 0)
            }
            //listTypeOf("add", "Expr")
            dataType("add", "Add") {
                propertyListSeparatedTypeOf("expr", "Expr", "String", false, 0)
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }
}