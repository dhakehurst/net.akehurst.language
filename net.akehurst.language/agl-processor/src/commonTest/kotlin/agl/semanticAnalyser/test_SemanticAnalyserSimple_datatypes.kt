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

import net.akehurst.language.agl.grammar.scopes.ScopeModelAgl
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.asm.asmSimple
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.typemodel.StringType
import net.akehurst.language.api.typemodel.TypeModelTest
import net.akehurst.language.api.typemodel.typeModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_SemanticAnalyserSimple_datatypes {

    private companion object {
        val grammarProc = Agl.registry.agl.grammar.processor ?: error("Internal error: AGL language processor not found")

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
        val grammar = Agl.registry.agl.grammar.processor!!.process(grammarStr).asm!!.first()
        val typeModel by lazy {
            val result = grammarProc.process(grammarStr)
            assertNotNull(result.asm)
            assertTrue(result.issues.none { it.kind == LanguageIssueKind.ERROR }, result.issues.joinToString(separator = "\n") { "$it" })
            TypeModelFromGrammar(result.asm!!.last())
        }
        val scopeModel = ScopeModelAgl.fromString(
            ContextFromTypeModel(TypeModelFromGrammar(grammar)),
            """
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
            """.trimIndent()
        ).asm!!
        val syntaxAnalyser = SyntaxAnalyserSimple(typeModel, scopeModel)
        val processor = Agl.processorFromString<AsmSimple, ContextSimple>(
            grammarStr,
            Agl.configuration {
                scopeModelResolver { ProcessResultDefault(scopeModel, IssueHolder(LanguageProcessorPhase.ALL)) }
                typeModelResolver { ProcessResultDefault(typeModel, IssueHolder(LanguageProcessorPhase.ALL)) }
                syntaxAnalyserResolver { ProcessResultDefault(syntaxAnalyser, IssueHolder(LanguageProcessorPhase.ALL)) }
            }
        ).processor!!
    }

    @Test
    fun datatype_with_no_properties() {
        val sentence = """
            datatype A { }
        """.trimIndent()

        val result = processor.process(sentence)
        assertTrue(result.issues.errors.isEmpty(),result.issues.joinToString(separator = "\n") { "$it" })
        assertNotNull(result.asm)

        val expected = asmSimple {
            element("Unit") {
                propertyListOfElement("declaration",) {
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

        val result = processor.process(sentence)
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty())

        val expected = asmSimple {
            element("Unit") {
                propertyListOfElement("declaration",) {
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
                propertyListOfElement("declaration",) {
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
                "Cannot find 'String' as reference for 'TypeReference.type'"
            )
        )

        assertEquals(expected.asString("  ", ""), result.asm!!.asString("  ", ""))
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
                    context(ContextSimple())
                }
            }
        )
        assertNotNull(result.asm)
        assertTrue(result.issues.errors.isEmpty(), result.issues.joinToString(separator = "\n") { "$it" })

        val expected = asmSimple(scopeModel, ContextSimple()) {
            element("Unit") {
                propertyListOfElement("declaration",) {
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