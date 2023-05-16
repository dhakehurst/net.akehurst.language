/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.agl.grammar.scopes

import net.akehurst.language.agl.grammarTypeModel.GrammarTypeModelTest
import net.akehurst.language.agl.grammarTypeModel.grammarTypeModel
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.syntaxAnalyser.ContextFromTypeModel
import net.akehurst.language.agl.syntaxAnalyser.TypeModelFromGrammar
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_AglScopes {

    private companion object {
        val aglProc = Agl.registry.agl.scopes.processor!!
    }

    @Test
    fun typeModel() {
        val actual = aglProc.typeModel
        val expected = grammarTypeModel("net.akehurst.language.agl", "AglScopes") {
            // declarations = rootIdentifiables scopes references?
            elementType("declarations", "Declarations") {
                propertyListTypeOf("rootIdentifiables", "Identifiable", false, 0)
                propertyListTypeOf("scopes", "Scope", false, 1)
                propertyListTypeOf("references", "Reference", true, 2)
            }
            // rootIdentifiables = identifiable*
            elementType("rootIdentifiables", "RootIdentifiables") {
                propertyListTypeOf("identifiables", "Identifiable", false, 0)
            }
            // scopes = scope*
            elementType("scopes", "Scope") {
                propertyListTypeOf("scope", "Scope", false, 0)
            }
            // scope = 'scope' typeReference '{' identifiables '}
            elementType("scope", "Scope") {
                propertyElementTypeOf("typeReference", "TypeReference", false, 0)
                propertyListTypeOf("identifiables", "Identifiable", false, 1)
            }
            // identifiables = identifiable*
            elementType("identifiables", "Identifiable") {
                propertyListTypeOf("identifiable", "Identifiable", false, 0)
            }
            // identifiable = 'identify' typeReference 'by' propertyReferenceOrNothing
            elementType("identifiable", "Identifiable") {
                propertyElementTypeOf("typeReference", "TypeReference", false, 0)
                propertyPrimitiveType("propertyReferenceOrNothing", "String", false, 1)
            }
            // references = 'references' '{' referenceDefinitions '}'
            elementType("references", "References") {
                propertyListTypeOf("referenceDefinitions", "ReferenceDefinition", false, 1)
            }
            // referenceDefinitions = referenceDefinition*
            elementType("referenceDefinitions", "ReferenceDefinition") {
                propertyListTypeOf("referenceDefinition", "ReferenceDefinition", false, 0)
            }
            // referenceDefinition = 'in' typeReference 'property' propertyReference 'refers-to' typeReferences
            elementType("referenceDefinition", "ReferenceDefinition") {
                propertyElementTypeOf("typeReference", "TypeReference", false, 0)
                propertyElementTypeOf("propertyReference", "PropertyReference", false, 1)
                propertyListTypeOf("typeReferences", "TypeReference", false, 2)
            }
            // typeReferences = [typeReferences / '|']+
            elementType("typeReferences", "TypeReference") {
                propertyListSeparatedTypeOf("typeReference", "TypeReference", "String", false, 0)
            }
            // propertyReferenceOrNothing = '§nothing' | propertyReference
            elementType("propertyReferenceOrNothing", "PropertyReferenceOrNothing") {

            }
            // typeReference = IDENTIFIER     // same as grammar rule name
            elementType("typeReference", "TypeReference") {
                propertyPrimitiveType("identifier", "String", false, 0)
            }
            // propertyReference = IDENTIFIER // same as grammar rule name
            elementType("propertyReference", "PropertyReference") {
                propertyPrimitiveType("identifier", "String", false, 0)
            }
            // leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9-]*"

        }

        GrammarTypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun single_line_comment() {

        val text = """
            // single line comment
        """.trimIndent()

        val result = aglProc.process(text)

        val expected = ScopeModelAgl()

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun multi_line_comment() {

        val text = """
            /* multi
               line
               comment
            */
        """.trimIndent()

        val result = aglProc.process(text)

        val expected = ScopeModelAgl()

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun one_empty_scope() {
        val grammar = Agl.registry.agl.grammar.processor!!.process(
            sentence = """
                namespace test
                grammar Test {
                    rule1 = 'a' ;
                }
            """.trimIndent()
        ).asm!![0]

        val text = """
            scope Rule1 { }
        """.trimIndent()

        val result = aglProc.process(
            sentence = text,
            Agl.options {
                semanticAnalysis { context(ContextFromTypeModel(TypeModelFromGrammar(grammar))) }
            }
        )

        val expected = ScopeModelAgl().apply {
            scopes["Rule1"] = (ScopeDefinition("Rule1"))
        }

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun one_empty_scope_wrong_scope_ruleName() {
        val grammar = Agl.registry.agl.grammar.processor!!.process(
            sentence = """
                namespace test
                grammar Test {
                    rule1 = 'a' ;
                }
            """.trimIndent()
        ).asm!![0]

        val text = """
            scope RuleX { }
        """.trimIndent()

        val result = aglProc.process(
            sentence = text,
            Agl.options {
                semanticAnalysis { context(ContextFromTypeModel(TypeModelFromGrammar(grammar))) }
            }
        )

        val expected = ScopeModelAgl().apply {
            scopes["RuleX"] = (ScopeDefinition("RuleX"))
        }

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertEquals(
            setOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(6, 7, 1, 6), "Type 'RuleX' not found in scope")
            ), result.issues.all
        )
    }

    @Test
    fun scope_one_identifiable() {
        val grammar = Agl.registry.agl.grammar.processor!!.process(
            sentence = """
                namespace test
                grammar Test {
                    rule1 = 'X' rule2 'Y' ;
                    rule2 = 'a' rule3 ;
                    rule3 = "[a-z]" ;
                }
            """.trimIndent()
        ).asm!![0]

        val text = """
            scope Rule1 {
                identify Rule2 by rule3
            }
        """.trimIndent()

        val result = aglProc.process(
            sentence = text,
            Agl.options {
                semanticAnalysis { context(ContextFromTypeModel(TypeModelFromGrammar(grammar))) }
            }
        )

        val expected = ScopeModelAgl().apply {
            scopes["Rule1"] = (ScopeDefinition("Rule1").apply {
                identifiables.add(Identifiable("Rule2", "rule3"))
            })
        }

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun scope_one_identifiable_wrong_type_ruleName() {
        val grammar = Agl.registry.agl.grammar.processor!!.process(
            sentence = """
                namespace test
                grammar Test {
                    rule1 = 'X' rule2 'Y' ;
                    rule2 = 'a' rule3 ;
                    rule3 = "[a-z]" ;
                }
            """.trimIndent()
        ).asm!![0]

        val text = """
            scope Rule1 {
                identify RuleX by rule3
            }
        """.trimIndent()

        val result = aglProc.process(
            sentence = text,
            Agl.options {
                semanticAnalysis { context(ContextFromTypeModel(TypeModelFromGrammar(grammar))) }
            }
        )

        val expected = ScopeModelAgl().apply {
            scopes["Rule1"] = (ScopeDefinition("Rule1").apply {
                identifiables.add(Identifiable("RuleX", "rule3"))
            })
        }

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertEquals(
            listOf(
                LanguageIssue(
                    LanguageIssueKind.ERROR,
                    LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                    InputLocation(27, 14, 2, 6),
                    "Type 'RuleX' not found in scope"
                )
            ),
            result.issues.errors
        )
    }

    @Test
    fun scope_one_identifiable_wrong_property_ruleName() {
        val grammar = Agl.registry.agl.grammar.processor!!.process(
            sentence = """
                namespace test
                grammar Test {
                    rule1 = 'X' rule2 'Y' ;
                    rule2 = 'a' rule3 ;
                    rule3 = "[a-z]" ;
                }
            """.trimIndent()
        ).asm!![0]

        val text = """
            scope Rule1 {
                identify Rule2 by ruleX
            }
        """.trimIndent()

        val result = aglProc.process(
            sentence = text,
            Agl.options {
                semanticAnalysis { context(ContextFromTypeModel(TypeModelFromGrammar(grammar))) }
            }
        )

        val expected = ScopeModelAgl().apply {
            scopes["Rule1"] = (ScopeDefinition("Rule1").apply {
                identifiables.add(Identifiable("Rule2", "ruleX"))
            })
        }

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertEquals(
            listOf(
                LanguageIssue(
                    LanguageIssueKind.ERROR,
                    LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                    InputLocation(36, 23, 2, 6),
                    "In scope for 'Rule1', 'ruleX' not found for identifying property of 'Rule2'"
                )
            ), result.issues.errors
        )
    }

    @Test
    fun one_reference() {
        val grammar = Agl.registry.agl.grammar.processor!!.process(
            sentence = """
                namespace test
                grammar Test {
                    rule1 = 'X' rule2 'Y' ;
                    rule2 = 'a' rule3 ;
                    rule3 = "[a-z]" ;
                }
            """.trimIndent()
        ).asm!![0]

        val text = """
            references {
                in Rule2 property rule3 refers-to Rule1
            }
        """.trimIndent()

        val result = aglProc.process(
            sentence = text,
            Agl.options {
                semanticAnalysis { context(ContextFromTypeModel(TypeModelFromGrammar(grammar))) }
            }
        )

        val expected = ScopeModelAgl().apply {
            references.add(ReferenceDefinition("Rule2", "rule3", listOf("Rule1")))
        }

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun one_reference_unknown_rules() {
        val grammar = Agl.registry.agl.grammar.processor!!.process(
            sentence = """
                namespace test
                grammar Test {
                    rule1 = 'X' rule2 'Y' ;
                    rule2 = 'a' rule3 ;
                    rule3 = "[a-z]" ;
                }
            """.trimIndent()
        ).asm!![0]

        val text = """
            references {
                in RuleX property ruleY refers-to RuleZ|RuleW
            }
        """.trimIndent()

        val result = aglProc.process(
            sentence = text,
            Agl.options {
                semanticAnalysis { context(ContextFromTypeModel(TypeModelFromGrammar(grammar))) }
            }
        )

        val expected = ScopeModelAgl().apply {
            references.add(ReferenceDefinition("RuleX", "ruleY", listOf("RuleZ", "RuleW")))
        }

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertEquals(
            listOf(
                LanguageIssue(
                    LanguageIssueKind.ERROR,
                    LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                    InputLocation(20, 8, 2, 6),
                    "Referring type 'RuleX' not found in scope"
                ),
                LanguageIssue(
                    LanguageIssueKind.ERROR,
                    LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                    InputLocation(51, 39, 2, 5),
                    "For reference in 'RuleX' referred to type 'RuleZ' not found"
                ),
                LanguageIssue(
                    LanguageIssueKind.ERROR,
                    LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                    InputLocation(57, 45, 2, 6),
                    "For reference in 'RuleX' referred to type 'RuleW' not found"
                )
            ), result.issues.errors
        )
    }

    @Test
    fun one_reference_to_three() {
        val text = """
            references {
                in Type1 property prop refers-to Type2|Type3|Type4
            }
        """.trimIndent()

        val result = aglProc.process(text)

        val expected = ScopeModelAgl().apply {
            references.add(ReferenceDefinition("Type1", "prop", listOf("Type2", "Type3", "Type4")))
        }

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    //TODO more checks + check rules (types/properties) exist in context of grammar
}