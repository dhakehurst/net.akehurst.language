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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.grammarTypeModel.GrammarTypeModelTest
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.agl.simple.ContextAsmSimple
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.agl.simple.SyntaxAnalyserSimple
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.grammarTypemodel.builder.grammarTypeNamespace
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.transform.asm.TransformDomainDefault
import net.akehurst.language.typemodel.builder.typeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_SyntaxAnalyserSimple_datatypes {

    private companion object {
        val grammarProc = Agl.registry.agl.grammar.processor ?: error("Internal error: AGL language processor not found")

        val grammarStr = """
            namespace test
            
            grammar Test {
                skip WS = "\s+" ;
            
                unit = declaration* ;
                declaration = datatype | primitive ;
                primitive = 'primitive' ID ;
                datatype = 'datatype' ID '{' property* '}' ;
                property = ID ':' typeReference ;
                typeReference = type typeArguments? ;
                typeArguments = '<' [typeReference / ',']+ '>' ;
            
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
                leaf type = ID;
            
            }
        """.trimIndent()
        val grammar = grammarProc.process(grammarStr).asm!!
        val asmTransformModel by lazy {
            val result = grammarProc.process(grammarStr)
            assertNotNull(result.asm)
            assertTrue(result.issues.none { it.kind == LanguageIssueKind.ERROR }, result.issues.toString())
            val tr = TransformDomainDefault.fromGrammarModel(result.asm!!)
            assertNotNull(tr.asm)
            assertTrue(tr.issues.none { it.kind == LanguageIssueKind.ERROR }, result.issues.toString())
            tr.asm!!
        }
        val typeModel by lazy {
            asmTransformModel.typeModel!!
        }
        val scopeModel = CrossReferenceModelDefault(grammar.primary!!.name)
        val syntaxAnalyser = SyntaxAnalyserSimple(typeModel, asmTransformModel, grammar.primary!!.qualifiedName)
        val processor = Agl.processorFromString<Asm, ContextWithScope<Any, Any>>(
            grammarStr,
            Agl.configuration {
                crossReferenceResolver { ProcessResultDefault(scopeModel, IssueHolder(LanguageProcessorPhase.ALL)) }
                typesResolver { ProcessResultDefault(typeModel, IssueHolder(LanguageProcessorPhase.ALL)) }
                syntaxAnalyserResolver { ProcessResultDefault(syntaxAnalyser, IssueHolder(LanguageProcessorPhase.ALL)) }
            }
        ).processor!!
    }

    @Test
    fun checkTypeModel() {
        val actual = processor.typesModel
        val expected = typeModel("FromGrammarParsedGrammarUnit", true) {
            grammarTypeNamespace("test.Test") {
                //unit = declaration* ;
                dataFor("unit", "Unit") {
                    propertyListTypeOf("declaration", "Declaration", false, 0)
                }
                // primitive = 'primitive' ID ;
                dataFor("primitive", "Primitive") {
                    propertyPrimitiveType("id", "String", false, 1)
                }
                // datatype = 'datatype' ID '{' property* '}' ;
                dataFor("datatype", "Datatype") {
                    propertyPrimitiveType("id", "String", false, 1)
                    propertyListTypeOf("property", "Property", false, 3)
                }
                // declaration = datatype | primitive ;
                dataFor("declaration", "Declaration") {
                    subtypes("Datatype", "Primitive")
                }
                // property = ID ':' typeReference ;
                dataFor("property", "Property") {
                    propertyPrimitiveType("id", "String", false, 0)
                    propertyDataTypeOf("typeReference", "TypeReference", false, 2)
                }
                // typeReference = type typeArguments? ;
                dataFor("typeReference", "TypeReference") {
                    propertyPrimitiveType("type", "String", false, 0)
                    propertyDataTypeOf("typeArguments", "TypeArguments", true, 1)
                }
                // typeArguments = '<' [typeReference / ',']+ '>' ;
                dataFor("typeArguments", "TypeArguments") {
                    propertyListTypeOf("typeReference", "TypeReference", false, 1)
                }
                stringTypeFor("ID")
                stringTypeFor("type")
            }
        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
    }

    @Test
    fun datatype_with_no_properties() {
        val sentence = """
            datatype A { }
        """.trimIndent()

        val result = processor.process(sentence)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty())

        val expected = asmSimple {
            element("Unit") {
                propertyListOfElement("declaration") {
                    element("Datatype") {
                        propertyString("id", "A")
                        propertyListOfElement("property") {}
                    }
                }
            }
        }

        assertEquals(expected.asString("", "  "), result.asm!!.asString("", "  "))
    }

    @Test
    fun two_datatypes_with_no_properties() {
        val sentence = """
            datatype A { }
            datatype B { }
        """.trimIndent()

        val result = processor.process(sentence)
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty())

        val expected = asmSimple {
            element("Unit") {
                propertyListOfElement("declaration") {
                    element("Datatype") {
                        propertyString("id", "A")
                        propertyListOfElement("property") {}
                    }
                    element("Datatype") {
                        propertyString("id", "B")
                        propertyListOfElement("property") {}
                    }
                }
            }
        }

        assertEquals(expected.asString("", "  "), result.asm!!.asString("", "  "))
    }

    @Test
    fun datatype_with_property_whos_type_is_undefined() {
        val sentence = """
            datatype A {
                a : String
            }
        """.trimIndent()

        val result = processor.process(
            sentence = sentence,
            Agl.options {
                semanticAnalysis {
                    context(ContextAsmSimple())
                }
            }
        )
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty())

        val expected = asmSimple {
            element("Unit") {
                propertyListOfElement("declaration") {
                    element("Datatype") {
                        propertyString("id", "A")
                        propertyListOfElement("property") {
                            element("Property") {
                                propertyString("id", "a")
                                propertyElementExplicitType("typeReference", "TypeReference") {
                                    propertyString("type", "String") // no scope model - not a reference
                                    propertyString("typeArguments", null)
                                }
                            }
                        }
                    }
                }
            }
        }
        val expItems = emptyList<LanguageIssue>()

        assertEquals(expected.asString("", "  "), result.asm!!.asString("", "  "))
        assertEquals(expItems, result.issues.errors)
    }

    @Test
    fun datatype_with_property_whos_type_is_defined() {
        val sentence = """
            primitive String
            datatype A {
                a : String
            }
        """.trimIndent()

        val result = processor.process(
            sentence = sentence,
            Agl.options {
                semanticAnalysis {
                    context(ContextAsmSimple())
                }
            }
        )
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())

        val expected = asmSimple(crossReferenceModel = scopeModel, context = ContextAsmSimple()) {
            element("Unit") {
                propertyListOfElement("declaration") {
                    element("Primitive") {
                        propertyString("id", "String")
                    }
                    element("Datatype") {
                        propertyString("id", "A")
                        propertyListOfElement("property") {
                            element("Property") {
                                propertyString("id", "a")
                                propertyElementExplicitType("typeReference", "TypeReference") {
                                    propertyString("type", "String") // no scope model - not a reference
                                    propertyString("typeArguments", null)
                                }
                            }
                        }
                    }
                }
            }
        }

        assertEquals(expected.asString("", "  "), result.asm!!.asString("", "  "))

    }
}