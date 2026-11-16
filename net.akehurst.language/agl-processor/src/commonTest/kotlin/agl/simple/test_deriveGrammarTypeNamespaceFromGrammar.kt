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
import net.akehurst.language.agl.processor.contextFromRegistryGrammars
import net.akehurst.language.asmTransform.api.AsmTransformDomain
import net.akehurst.language.asmTransform.asm.AsmTransformDomainDefault
import net.akehurst.language.grammarTypemodel.builder.grammarTypeModel
import net.akehurst.language.grammarTypemodel.builder.grammarTypeNamespace
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.asm.TypesDomainSimple
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.types.api.DataType
import net.akehurst.language.types.api.UnionType
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_deriveGrammarTypeNamespaceFromGrammar {

    private companion object {
        val grammarProc = Agl.registry.agl.grammar.processor ?: error("Internal error: AGL language processor not found")

        fun test(grammarStr: String, expectedTr: AsmTransformDomain, expectedTm: TypesDomain) {
            val result = grammarProc.process(grammarStr, Agl.options {
                semanticAnalysis {
                    sentenceContext(contextFromRegistryGrammars(Agl.registry))
                }
            })
            assertNotNull(result.asm)
            assertTrue(result.allIssues.isEmpty(), result.allIssues.toString())
            val grammarMdl = result.asm!!

            val grmrTypeModel = TypesDomainSimple(grammarMdl.name)
            grmrTypeModel.addNamespace(StdLibDefault)
            val atfg = AsmTransformDomainDefault.fromGrammarDomain(grammarMdl)
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

        val actual = AsmTransformDomainDefault.fromGrammarDomain(result.asm!!).asm!!.typesDomain!!
        val expected = typesDomain("Test", true) {
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

        val actual = AsmTransformDomainDefault.fromGrammarDomain(result.asm!!).asm!!.typesDomain!!
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

        val actual = AsmTransformDomainDefault.fromGrammarDomain(result.asm!!).asm!!.typesDomain!!
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

        val actual = AsmTransformDomainDefault.fromGrammarDomain(result.asm!!).asm!!.typesDomain!!
        val expected = typesDomain("FromGrammarParsedGrammarUnit", true) {
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
                    supertypes("Expr")
                    subtypes("Var", "Literal")
                }
                dataFor("var", "Var") {
                    supertypes("Root")
                    propertyPrimitiveType("name", "String", false, 0)
                }
                dataFor("literal", "Literal") {
                    supertypes("Root")
                    propertyPrimitiveType("number", "String", false, 0)
                }
                dataFor("mul", "Mul") {
                    supertypes("Expr")
                    propertyDataTypeOf("expr", "Expr", false, 0)
                    propertyDataTypeOf("expr2", "Expr", false, 2)
                }
                dataFor("add", "Add") {
                    supertypes("Expr")
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
                mul = [expr / MUL_OP]2+ ;
                leaf MUL_OP = '*';
                add = [expr / ADD_OP]2+ ;
                leaf ADD_OP = '+';
                var = NAME ;
                literal = NUMBER ;
                leaf NUMBER = "[0-9]+" ;
                leaf NAME = "[a-zA-Z]+" ;
            }
        """.trimIndent()

        val result = grammarProc.process(grammarStr)
        assertNotNull(result.asm)
        assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())

        val actual = AsmTransformDomainDefault.fromGrammarDomain(result.asm!!).asm!!.typesDomain!!
        val expected = typesDomain("FromGrammarParsedGrammarUnit", true) {
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
                    supertypes("Expr")
                    subtypes("Var", "Literal")
                }
                dataFor("var", "Var") {
                    supertypes("Root")
                    propertyPrimitiveType("name", "String", false, 0)
                }
                dataFor("literal", "Literal") {
                    supertypes("Root")
                    propertyPrimitiveType("number", "String", false, 0)
                }
                dataFor("mul", "Mul") {
                    supertypes("Expr")
                    propertyOf(setOf(CMP),"expr", "ListSeparated") {
                        typeArgument("Expr")
                        typeArgument("String")
                    }
                }
                //listTypeOf("add", "Expr")
                dataFor("add", "Add") {
                    supertypes("Expr")
                    propertyOf(setOf(CMP),"expr", "ListSeparated") {
                        typeArgument("Expr")
                        typeArgument("String")
                    }
                }
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test
    fun override_choice__inherited_rules_keep_base_types() {
        val grammarStr = """
            namespace test
            grammar Base {
                S = P ;
                P = A | B ;
                A = 'a' ;
                B = 'b' C ;
                C = 'c' ;
            }
            grammar Derived : Base {
                override P = A | B | D ;
                D = 'd' ;
            }
        """.trimIndent()

        val result = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = result.processor!!.typesDomain
        val baseNs = actual.findNamespaceOrNull(QualifiedName("test.Base"))!!
        val derivedNs = actual.findNamespaceOrNull(QualifiedName("test.Derived"))!!

        // inherited rules (A, B, C) are not overridden in Derived, so their types are only in Base
        assertEquals(setOf("P", "D"), derivedNs.ownedTypes.map { it.name.value }.toSet())
        assertEquals(setOf("S", "P", "A", "B", "C"), baseNs.ownedTypes.map { it.name.value }.toSet())
        val b = baseNs.findOwnedTypeNamed(SimpleName("B")) as DataType
        assertEquals(listOf("c"), b.property.map { it.name.value })

        // Derived.P is a union, because A and B are owned by Base; base types get no supertype from Derived
        val baseP = baseNs.findOwnedTypeNamed(SimpleName("P")) as DataType
        val derivedP = derivedNs.findOwnedTypeNamed(SimpleName("P")) as UnionType
        assertEquals(listOf("A", "B", "D"), derivedP.alternatives.map { it.resolvedDefinition.name.value })
        assertEquals(listOf(baseP), b.supertypes.map { it.resolvedDefinition })
        // the alternative owned by Derived is a subtype of the overridden type, so the union conforms to it
        val d = derivedNs.findOwnedTypeNamed(SimpleName("D")) as DataType
        assertEquals(listOf(baseP), d.supertypes.map { it.resolvedDefinition })
        assertTrue(derivedP.conformsTo(baseP))
        assertTrue(b.conformsTo(derivedP))
    }

    @Test
    fun override_choice__all_own_alternatives__subtype_of_overridden() {
        val grammarStr = """
            namespace test
            grammar Base {
                S = P ;
                P = A | B ;
                A = 'a' ;
                B = 'b' ;
            }
            grammar Derived : Base {
                override P = D | E ;
                D = 'd' ;
                E = 'e' ;
            }
        """.trimIndent()

        val result = Agl.processorFromStringSimple(GrammarString(grammarStr))
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val actual = result.processor!!.typesDomain
        val baseNs = actual.findNamespaceOrNull(QualifiedName("test.Base"))!!
        val derivedNs = actual.findNamespaceOrNull(QualifiedName("test.Derived"))!!
        val baseP = baseNs.findOwnedTypeNamed(SimpleName("P")) as DataType
        val derivedP = derivedNs.findOwnedTypeNamed(SimpleName("P")) as DataType
        assertEquals(listOf(baseP), derivedP.supertypes.map { it.resolvedDefinition })
        val d = derivedNs.findOwnedTypeNamed(SimpleName("D")) as DataType
        assertEquals(listOf(derivedP), d.supertypes.map { it.resolvedDefinition })
        assertTrue(d.conformsTo(baseP))
    }
}