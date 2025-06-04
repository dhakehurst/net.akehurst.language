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

package net.akehurst.language.agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.asm.builder.asmSimple
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.sentence.api.InputLocation
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class test_SemanticAnalyserSimple_datatypes {

    private companion object {
        val grammarStr = GrammarString(
            """
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
        )
        val crossReferenceModelStr = CrossReferenceString(
            """
            namespace test.Test
                identify Primitive by id
                identify Datatype by id
                identify Collection by id

                scope Datatype {
                    identify Property by id
                }
                scope Property {}

                references {
                    in TypeReference {
                      property type refers-to Primitive|Datatype|Collection
                    }
                }
        """.trimIndent()
        )
        val processor = Agl.processorFromStringSimple(
            grammarDefinitionStr = grammarStr,
            referenceStr = crossReferenceModelStr
        ).processor!!
        val typeModel = processor.typesModel
        val crossReferenceModel = processor.crossReferenceModel

    }

    @Test
    fun check_scopeModel() {
        val context = ContextFromTypeModel(processor.typesModel)
        val res = CrossReferenceModelDefault.fromString(context, crossReferenceModelStr)
        assertTrue(res.issues.isEmpty(), res.issues.toString())
    }

    @Test
    fun datatype_with_no_properties() {
        val sentence = """
            datatype A { }
        """.trimIndent()

        val result = processor.process(sentence, Agl.options {
            semanticAnalysis {
                context(contextAsmSimple())
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

        assertEquals(expected.asString("", "  "), result.asm!!.asString("", "  "))
    }

    @Test
    fun two_datatypes_with_no_properties() {
        val sentence = """
            datatype A { }
            datatype B { }
        """.trimIndent()

        val result = processor.process(sentence, Agl.options {
            semanticAnalysis {
                context(contextAsmSimple())
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
                    context(contextAsmSimpleWithAsmPath())
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
                InputLocation(21, 9, 2, 7, null),
                "Reference 'String' not resolved, to type(s) [Primitive, Datatype, Collection] in scope '/A/a'"
            )
        )
        println(result.asm!!.asString())
        assertEquals(expected.asString(), result.asm!!.asString())
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
                    context(contextAsmSimple())
                }
            }
        )
        assertNotNull(result.asm)
        assertTrue(result.issues.isEmpty(), result.issues.toString())

        val expected = asmSimple(typeModel = typeModel, crossReferenceModel = crossReferenceModel, context = contextAsmSimple()) {
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

        assertEquals(expected.asString("", "  "), result.asm!!.asString("", "  "))

    }

    @Test
    fun reprocess_with_same_context_same_def_same_sentence__pass() {
        val sentence = "primitive String"
        val context = contextAsmSimple(
            createScopedItem = {ref, item, loc -> Pair(item, loc?.sentenceIdentity) }
        )

        // process once to fill context
        val result1 = processor.process(sentence = sentence, Agl.options {
            parse { sentenceIdentity { 1 } }
            semanticAnalysis { context(context) }
        })

        assertTrue(result1.issues.isEmpty(), result1.issues.toString())
        assertNotNull(result1.asm)

        // process again, should not have semantic error because same definition is found, but in same location
        val result2 = processor.process(sentence = sentence, Agl.options {
            parse { sentenceIdentity { 1 } }
            semanticAnalysis { context(context) }
        })
        assertTrue(result2.issues.isEmpty(), result2.issues.toString())
        assertNotNull(result2.asm)

    }

    @Test
    fun reprocess_with_same_context_same_def_diff_sentence__fail() {
        val sentence = "primitive String"
        val context = contextAsmSimple()

        // process once to fill context
        val result1 = processor.process(sentence = sentence, Agl.options {
            parse { sentenceIdentity {
                1
            } }
            semanticAnalysis { context(context) }
        })

        assertTrue(result1.issues.isEmpty(), result1.issues.toString())
        assertNotNull(result1.asm)

        // process again, should have semantic error because same definition is found in different location
        val result2 = processor.process(sentence = sentence, Agl.options {
            parse { sentenceIdentity { 2 } }
            semanticAnalysis { context(context) }
        })

        val expected = setOf(
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(0, 1, 1, 16, 2),"'String' with type 'test.Test.Primitive' already exists in scope /"),
        )

        assertEquals(expected, result2.issues.all)

    }
}