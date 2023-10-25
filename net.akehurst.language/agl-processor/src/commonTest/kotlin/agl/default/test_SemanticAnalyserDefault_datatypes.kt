/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.agl.default

import net.akehurst.language.agl.language.scopes.ScopeModelAgl
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.agl.semanticAnalyser.ContextSimple
import net.akehurst.language.api.asm.asmSimple
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_SemanticAnalyserDefault_datatypes {

    private companion object {
        val grammarStr = """
            namespace test
            
            grammar Test {
                skip leaf WS = "\s+" ;
                skip leaf COMMENT = "//[^\n]*(\n)" ;
            
                unit = declaration* ;
                declaration = datatype | primitive | collection ;
                primitive = 'primitive' ID ;
                collection = 'collection' ID typeParameters? ;
                typeParameters = '<' typeParameterList '>' ;
                typeParameterList = [ID / ',']+ ;
                datatype = 'datatype' ID '{' property* '}' ;
                property = ID ':' typeReference ;
                typeReference = type typeArguments? ;
                typeArguments = '<' typeArgumentList '>' ;
                typeArgumentList = [typeReference / ',']+ ;
            
                leaf ID = "[A-Za-z_][A-Za-z0-9_]*" ;
                leaf type = ID;
            }
        """.trimIndent()
        val scopeModelStr = """
            namespace test.Test {
                identify Unit by §nothing
                scope Unit {
                    identify Primitive by id
                    identify Datatype by id
                    identify Collection by id
                }
                references {
                    in TypeReference {
                      property type refers-to Primitive|Datatype|Collection
                    }
                }
            }
        """.trimIndent()
        val scopeModel = ScopeModelAgl.fromString(null, scopeModelStr).let { it.asm ?: error(it.issues.toString()) }
        val processor = Agl.processorFromStringDefault(
            grammarStr,
            scopeModelStr
        ).processor!!
    }

    @Test
    fun check_scopeModel() {
        val context = ContextFromTypeModel(processor.grammar!!.qualifiedName, processor.typeModel)
        val res = ScopeModelAgl.fromString(context, scopeModelStr)
        assertTrue(res.issues.isEmpty(), res.issues.toString())
    }

    @Test
    fun datatype_with_no_properties() {
        val sentence = """
            datatype A { }
        """.trimIndent()

        val result = processor.process(sentence, Agl.options {
            semanticAnalysis {
                context(ContextSimple())
            }
        })
        assertTrue(result.issues.isEmpty(), result.issues.toString())
        assertNotNull(result.asm)

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

        assertEquals(expected.asString("  ", ""), result.asm!!.asString("  ", ""))
    }

    @Test
    fun two_datatypes_with_no_properties() {
        val sentence = """
            datatype A { }
            datatype B { }
        """.trimIndent()

        val result = processor.process(sentence, Agl.options {
            semanticAnalysis {
                context(ContextSimple())
            }
        })
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.toString())

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

        assertEquals(expected.asString("  ", ""), result.asm!!.asString("  ", ""))
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
                    context(ContextSimple())
                }
            }
        )
        assertNotNull(result.asm)

        val expected = asmSimple {
            element("Unit") {
                propertyListOfElement("declaration") {
                    element("Datatype") {
                        propertyString("id", "A")
                        propertyListOfElement("property") {
                            element("Property") {
                                propertyString("id", "a")
                                propertyElementExplicitType("typeReference", "TypeReference") {
                                    reference("type", "String")
                                    propertyString("typeArguments", null)
                                }
                            }
                        }
                    }
                }
            }
        }
        val expItems = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(21, 9, 2, 7),
                "No target of type(s) [Primitive, Datatype, Collection] found for referring value 'String' in scope of element ':TypeReference([/0/declaration/0/property/0/typeReference])'"
            )
        )

        assertEquals(expItems, result.issues.errors)
        assertEquals(expected.asString("  ", ""), result.asm!!.asString("  ", ""))
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
                    context(ContextSimple())
                }
            }
        )
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.toString())

        val expected = asmSimple(scopeModel, ContextSimple()) {
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
                                    reference("type", "String")
                                    propertyString("typeArguments", null)
                                }
                            }
                        }
                    }
                }
            }
        }

        assertEquals(expected.asString("  ", ""), result.asm!!.asString("  ", ""))

    }
}