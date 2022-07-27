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


import net.akehurst.language.agl.grammar.grammar.ContextFromGrammar
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.asm.asmSimple
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.*
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

        val typeModel by lazy {
            val (grammars, gramIssues) = grammarProc.process(grammarStr)
            assertNotNull(grammars)
            assertTrue(gramIssues.none { it.kind == LanguageIssueKind.ERROR },gramIssues.joinToString(separator = "\n") { "$it" })
            TypeModelFromGrammar(grammars.last()).derive()
        }
        val syntaxAnalyser = SyntaxAnalyserSimple(typeModel)
        val processor = Agl.processorFromString<AsmSimple, ContextSimple>(
            grammarStr,
            Agl.configuration { syntaxAnalyser(syntaxAnalyser) }
        ).also {
            syntaxAnalyser.configure(
                configurationContext = ContextFromGrammar(it.grammar),
                configuration = """
                    identify unit by §nothing
                    scope unit {
                        identify primitive by ID
                        identify datatype by ID
                    }
                    references {
                        in typeReference property type refers-to primitive|datatype
                    }
                """.trimIndent()
            )
        }
    }

    @Test
    fun datatype_with_no_properties() {
        val sentence = """
            datatype A { }
        """.trimIndent()

        val (actual, issues) = processor.process(sentence)
        assertNotNull(actual)
        assertEquals(emptyList(), issues)

        val expected = asmSimple {
            root("unit") {
                propertyListOfElement("declaration") {
                    element("datatype") {
                        propertyString("ID", "A")
                        propertyListOfElement("property") {}
                    }
                }
            }
        }

        assertEquals(expected.asString("  ", ""), actual.asString("  ", ""))
    }

    @Test
    fun two_datatypes_with_no_properties() {
        val sentence = """
            datatype A { }
            datatype B { }
        """.trimIndent()

        val (actual, issues) = processor.process(sentence)
        assertNotNull(actual)
        assertEquals(emptyList(), issues)

        val expected = asmSimple {
            root("unit") {
                propertyListOfElement("declaration") {
                    element("datatype") {
                        propertyString("ID", "A")
                        propertyListOfElement("property") {}
                    }
                    element("datatype") {
                        propertyString("ID", "B")
                        propertyListOfElement("property") {}
                    }
                }
            }
        }

        assertEquals(expected.asString("  ", ""), actual.asString("  ", ""))
    }

    @Test
    fun datatype_with_property_whos_type_is_undefined() {
        val sentence = """
            datatype A {
                a : String
            }
        """.trimIndent()

        val (actual, issues) = processor.process(
            sentence = sentence,
            processor.options {
                syntaxAnalysis {
                    context(ContextSimple())
                }
            }
        )
        assertNotNull(actual)

        val expected = asmSimple {
            root("unit") {
                propertyListOfElement("declaration") {
                    element("datatype") {
                        propertyString("ID", "A")
                        propertyListOfElement("property") {
                            element("property") {
                                propertyString("ID", "a")
                                propertyElement("typeReference") {
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
            LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SYNTAX_ANALYSIS, InputLocation(21, 9, 2, 7), "Cannot find 'String' as reference for 'typeReference.type'")
        )

        assertEquals(expected.asString("  ", ""), actual.asString("  ", ""))
        assertEquals(expItems, issues)
    }

    @Test
    fun datatype_with_property_whos_type_is_defined() {
        val sentence = """
            primitive String
            datatype A {
                a : String
            }
        """.trimIndent()

        val (actual, issues) = processor.process(
            sentence = sentence,
            processor.options {
                syntaxAnalysis {
                    context(ContextSimple())
                }
            }
        )
        assertNotNull(actual)
        assertEquals(emptyList(), issues)

        val expected = asmSimple(syntaxAnalyser.scopeModel, ContextSimple()) {
            root("unit") {
                propertyListOfElement("declaration") {
                    element("primitive") {
                        propertyString("ID", "String")
                    }
                    element("datatype") {
                        propertyString("ID", "A")
                        propertyListOfElement("property") {
                            element("property") {
                                propertyString("ID", "a")
                                propertyElement("typeReference") {
                                    reference("type", "String")
                                    propertyString("typeArguments", null)
                                }
                            }
                        }
                    }
                }
            }
        }

        assertEquals(expected.asString("  ", ""), actual.asString("  ", ""))

    }
}