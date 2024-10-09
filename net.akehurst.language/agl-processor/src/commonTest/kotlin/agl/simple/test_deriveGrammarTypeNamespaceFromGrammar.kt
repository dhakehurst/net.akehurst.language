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
import net.akehurst.language.agl.grammarTypeModel.GrammarTypeModelTest
import net.akehurst.language.grammarTypemodel.builder.grammarTypeModel
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.test.FixMethodOrder
import net.akehurst.language.test.MethodSorters
import net.akehurst.language.transform.api.TransformModel
import net.akehurst.language.transform.asm.TransformModelDefault
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.asm.TypeModelSimple
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class test_deriveGrammarTypeNamespaceFromGrammar {

    private companion object {
        val grammarProc = Agl.registry.agl.grammar.processor ?: error("Internal error: AGL language processor not found")

        fun test(grammarStr: String, expectedTr: TransformModel, expectedTm: TypeModel) {
            val result = grammarProc.process(grammarStr, Agl.options {
                semanticAnalysis {
                    context(ContextFromGrammarRegistry(Agl.registry))
                }
            })
            assertNotNull(result.asm)
            assertTrue(result.issues.isEmpty(), result.issues.toString())
            val grammar = result.asm!!.allDefinitions.first()

            val grmrTypeModel = TypeModelSimple(grammar.name)
            grmrTypeModel.addNamespace(SimpleTypeModelStdLib)
            val atfg = TransformModelDefault.fromGrammar(grammar)
            //val actualTr = atfg.build()

            assertTrue(atfg.issues.isEmpty(), atfg.issues.toString())
            // val actualTm = atfg.typeModel

            // GrammarTypeModelTest.tmAssertEquals(expectedTm, actualTm)
            //  AsmTransformModelTest.trAssertEquals(expectedTr, actualTr)
        }
    }

    // --- Group ---
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

        val actual = TransformModelDefault.fromGrammarModel(result.asm!!).asm!!.typeModel!!
        val expected = grammarTypeModel("test.Test", "Test") {
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

        val actual = TransformModelDefault.fromGrammarModel(result.asm!!).asm!!.typeModel!!
        val tmI = grammarTypeModel("test.I", "Inner") {
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
            "test.O", "O", listOf(
                SimpleTypeModelStdLib,
                tmI.namespace[1]
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

        val actual = TransformModelDefault.fromGrammarModel(result.asm!!).asm!!.typeModel!!
        val expected = grammarTypeModel("test.O", "O") {
            unnamedSuperTypeType("S") {
                typeRef("B")
                tupleType {
                    typeRef("s", "S", false)
                    typeRef("bc", "BC", false)
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

        val actual = TransformModelDefault.fromGrammarModel(result.asm!!).asm!!.typeModel!!
        val expected = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyDataTypeOf("exprList", "ExprList", false, 0)
            }
            dataType("exprList", "ExprList") {
                propertyDataTypeOf("expr", "Expr", false, 0)
                propertyListOfTupleType(Grammar2TransformRuleSet.UNNAMED_LIST_PROPERTY_NAME.value, false, 1) {
                    typeRef("expr", "Expr", false)
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

        val actual = TransformModelDefault.fromGrammarModel(result.asm!!).asm!!.typeModel!!
        val expected = grammarTypeModel("test.Test", "Test") {
            dataType("S", "S") {
                propertyDataTypeOf("exprList", "ExprList", false, 0)
            }
            dataType("exprList", "ExprList") {
                propertyDataTypeOf("expr", "Expr", false, 0)
                propertyListOfTupleType(Grammar2TransformRuleSet.UNNAMED_LIST_PROPERTY_NAME.value, false, 1) {
                    typeRef("expr", "Expr", false)
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