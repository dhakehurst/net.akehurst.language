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
import net.akehurst.language.agl.processor.contextFromGrammarRegistry
import net.akehurst.language.grammarTypemodel.builder.grammarTypeModel
import net.akehurst.language.grammarTypemodel.builder.grammarTypeNamespace
import net.akehurst.language.test.FixMethodOrder
import net.akehurst.language.test.MethodSorters
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.asmTransform.asm.AsmTransformDomainDefault
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.StdLibDefault
import net.akehurst.language.typemodel.asm.TypeModelSimple
import net.akehurst.language.typemodel.builder.typeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class test_deriveGrammarTypeNamespaceFromGrammar {

    private companion object {
        val grammarProc = Agl.registry.agl.grammar.processor ?: error("Internal error: AGL language processor not found")

        fun test(grammarStr: String, expectedTr: AsmTransformDomain, expectedTm: TypeModel) {
            val result = grammarProc.process(grammarStr, Agl.options {
                semanticAnalysis {
                    context(contextFromGrammarRegistry(Agl.registry))
                }
            })
            assertNotNull(result.asm)
            assertTrue(result.allIssues.isEmpty(), result.allIssues.toString())
            val grammarMdl = result.asm!!

            val grmrTypeModel = TypeModelSimple(grammarMdl.name)
            grmrTypeModel.addNamespace(StdLibDefault)
            val atfg = AsmTransformDomainDefault.fromGrammarModel(grammarMdl)
            //val actualTr = atfg.build()

            assertTrue(atfg.allIssues.isEmpty(), atfg.allIssues.toString())
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
        assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())

        val actual = AsmTransformDomainDefault.fromGrammarModel(result.asm!!).asm!!.typeModel!!
        val expected = typeModel("Test", true) {
            grammarTypeNamespace("test.Test") {
                stringTypeFor("ID")
                stringTypeFor("NAME")
                stringTypeFor("NUMBER")
                dataFor("S", "S") {
                    propertyPrimitiveType("id", "String", false, 0)
                    propertyListType("\$choiceList", false, 1) {
                        ref("S$1")
                    }
                }
                union("S$1") {
                    typeRef("A",false)
                    typeRef("B",false)
                }
                dataFor("A", "A") {
                    propertyPrimitiveType("name", "String", false, 0)
                    propertyPrimitiveType("number", "String", false, 1)
                }
                dataFor("B", "B") {
                    propertyPrimitiveType("name", "String", false, 0)
                    propertyPrimitiveType("name2", "String", false, 1)
                }
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
        assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())

        val actual = AsmTransformDomainDefault.fromGrammarModel(result.asm!!).asm!!.typeModel!!
        val tmI = grammarTypeModel("test.I", "Inner") {
            stringTypeFor("a")
            dataFor("S", "S") {
                subtypes("A", "SA")
            }
            dataFor("SA", "SA") {
                supertypes("S")
                propertyDataTypeOf("s", "S", false, 0)
                propertyDataTypeOf("a", "A", false, 1)
            }
            dataFor("A", "A") {
                supertypes("S")
                propertyPrimitiveType("a", "String", false, 0)
            }
        }
        val expected = grammarTypeModel(
            "test.O", "O", listOf(
                StdLibDefault,
                tmI.namespace[1]
            )
        ) {
            dataFor("S", "S") {
                subtypes("B", "SBC")
            }
            dataFor("SBC", "SBC") {
                supertypes("S")
                propertyDataTypeOf("s", "S", false, 0)
                propertyDataTypeOf("bc", "BC", false, 1)
            }
            dataFor("BC", "BC") {
                subtypes("B", "C")
            }
            dataFor("C", "C") {
                supertypes("BC")
                propertyDataTypeOf("s", "test.I.S", false, 1)
            }
            dataFor("B", "B") {
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
        assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())

        val actual = AsmTransformDomainDefault.fromGrammarModel(result.asm!!).asm!!.typeModel!!
        val expected = grammarTypeModel("test.O", "O") {
            unionFor("S","S") {
                typeRef("B")
                tupleType {
                    typeRef("s", "S", false)
                    typeRef("bc", "BC", false)
                }
            }
            dataFor("BC", "BC") {
                subtypes("B", "C")
            }
            dataFor("C", "C") {
                propertyPrimitiveType("s", "S", false, 1)
            }
            dataFor("B", "B") {
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
        assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())

        val actual = AsmTransformDomainDefault.fromGrammarModel(result.asm!!).asm!!.typeModel!!
        val expected = typeModel("FromGrammarParsedGrammarUnit", true) {
            grammarTypeNamespace("test.Test") {
                dataFor("S", "S") {
                    propertyDataTypeOf("exprList", "ExprList", false, 0)
                }
                dataFor("exprList", "ExprList") {
                    propertyDataTypeOf("expr", "Expr", false, 0)
                    propertyListOfTupleType(Grammar2TransformRuleSet.UNNAMED_LIST_PROPERTY_NAME.value, false, 1) {
                        typeRef("expr", "Expr", false)
                    }
                }
                dataFor("expr", "Expr") {
                    subtypes("Root", "Mul", "Add")
                }
                dataFor("root", "Root") {
                    subtypes("Var", "Literal")
                }
                dataFor("var", "Var") {
                    propertyPrimitiveType("name", "String", false, 0)
                }
                dataFor("literal", "Literal") {
                    propertyPrimitiveType("number", "String", false, 0)
                }
                dataFor("mul", "Mul") {
                    propertyDataTypeOf("expr", "Expr", false, 0)
                    propertyDataTypeOf("expr2", "Expr", false, 2)
                }
                dataFor("add", "Add") {
                    propertyDataTypeOf("expr", "Expr", false, 0)
                    propertyDataTypeOf("expr2", "Expr", false, 2)
                }
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
        assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())

        val actual = AsmTransformDomainDefault.fromGrammarModel(result.asm!!).asm!!.typeModel!!
        val expected = typeModel("FromGrammarParsedGrammarUnit", true) {
            grammarTypeNamespace("test.Test") {
                dataFor("S", "S") {
                    propertyDataTypeOf("exprList", "ExprList", false, 0)
                }
                dataFor("exprList", "ExprList") {
                    propertyDataTypeOf("expr", "Expr", false, 0)
                    propertyListOfTupleType(Grammar2TransformRuleSet.UNNAMED_LIST_PROPERTY_NAME.value, false, 1) {
                        typeRef("expr", "Expr", false)
                    }
                }
                dataFor("expr", "Expr") {
                    subtypes("Root", "Mul", "Add")
                }
                dataFor("root", "Root") {
                    subtypes("Var", "Literal")
                }
                dataFor("var", "Var") {
                    propertyPrimitiveType("name", "String", false, 0)
                }
                dataFor("literal", "Literal") {
                    propertyPrimitiveType("number", "String", false, 0)
                }
                dataFor("mul", "Mul") {
                    propertyListSeparatedTypeOf("expr", "Expr", "String", false, 0)
                }
                //listTypeOf("add", "Expr")
                dataFor("add", "Add") {
                    propertyListSeparatedTypeOf("expr", "Expr", "String", false, 0)
                }
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }
}