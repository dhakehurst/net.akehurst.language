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

import net.akehurst.language.agl.agl.grammar.scopes.Identifiable
import net.akehurst.language.agl.agl.grammar.scopes.ReferenceDefinition
import net.akehurst.language.agl.agl.grammar.scopes.Scope
import net.akehurst.language.agl.agl.grammar.scopes.ScopeModel
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.asm.AsmElementSimple
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.asm.asmSimple
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageProcessorPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_SyntaxAnalyserSimple_datatypes {

    companion object {
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
        val scopeModel = ScopeModel().also {
            it.scopes.add(Scope("unit").also {
                it.identifiables.add(Identifiable("primitive", "ID"))
                it.identifiables.add(Identifiable("datatype", "ID"))
            })
            it.references.add(ReferenceDefinition("typeReference","type",listOf("primitive","datatype")))
        }
        val processor = Agl.processorFromString(
            grammarStr,
            "unit",
            syntaxAnalyser = SyntaxAnalyserSimple(scopeModel)
        )
    }


    @Test
    fun datatype_with_no_properties() {
        val sentence = """
            datatype A { }
        """.trimIndent()

        val (actual, issues) = processor.process<AsmSimple, Any>(sentence)
        assertNotNull(actual)
        assertEquals(emptyList(),issues)

        val expected = asmSimple {
            root("unit") {
                property("declaration", listOf(
                    element("datatype") {
                        property("ID", "A")
                        property("property", emptyList<AsmElementSimple>())
                    }
                ))
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

        val (actual, issues) = processor.process<AsmSimple, Any>(sentence)
        assertNotNull(actual)
        assertEquals(emptyList(),issues)

        val expected = asmSimple {
            root("unit") {
                property("declaration", listOf(
                    element("datatype") {
                        property("ID", "A")
                        property("property", emptyList<AsmElementSimple>())
                    },
                    element("datatype") {
                        property("ID", "B")
                        property("property", emptyList<AsmElementSimple>())
                    }
                ))
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

        val (actual, issues) = processor.process<AsmSimple, Any>(
            sentence = sentence,
            context = ContextSimple(null, "unit")
        )
        assertNotNull(actual)

        val expected = asmSimple {
            root("unit") {
                property("declaration", listOf(
                    element("datatype") {
                        property("ID", "A")
                        property("property", listOf(
                            element("property") {
                                property("ID", "a")
                                property("typeReference") {
                                    reference("type", "String")
                                    property("typeArguments", null)
                                }
                            }
                        ))
                    }
                ))
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

        val (actual, issues) = processor.process<AsmSimple, Any>(
            sentence = sentence,
            context = ContextSimple(null, "unit")
        )
        assertNotNull(actual)
        assertEquals(emptyList(),issues)

        val expected = asmSimple(scopeModel, ContextSimple(null,"unit")) {
            root("unit") {
                property("declaration", listOf(
                    element("primitive") {
                        property("ID", "String")
                    },
                    element("datatype") {
                        property("ID", "A")
                        property("property", listOf(
                            element("property") {
                                property("ID", "a")
                                property("typeReference") {
                                    reference("type", "String")
                                    property("typeArguments", null)
                                }
                            }
                        ))
                    }
                ))
            }
        }

        assertEquals(expected.asString("  ", ""), actual.asString("  ", ""))

    }
}