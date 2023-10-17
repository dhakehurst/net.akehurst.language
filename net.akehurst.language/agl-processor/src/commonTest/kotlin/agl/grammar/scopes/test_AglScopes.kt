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

import net.akehurst.language.agl.default.TypeModelFromGrammar
import net.akehurst.language.agl.grammarTypeModel.GrammarTypeModelTest
import net.akehurst.language.agl.grammarTypeModel.grammarTypeModel
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
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
        val expected = grammarTypeModel("net.akehurst.language.agl", "AglScopes", "Declarations") {
            // declarations = rootIdentifiables scopes references?
            dataType("declarations", "Declarations") {
                propertyListTypeOf("rootIdentifiables", "Identifiable", false, 0)
                propertyListTypeOf("scopes", "Scope", false, 1)
                propertyListTypeOf("references", "ReferenceDefinition", true, 2)
            }
            // rootIdentifiables = identifiable*
            dataType("rootIdentifiables", "RootIdentifiables") {
                propertyListTypeOf("identifiables", "Identifiable", false, 0)
            }
            // scopes = scope*
            dataType("scopes", "Scopes") {
                propertyListTypeOf("scope", "Scope", false, 0)
            }
            // scope = 'scope' typeReference '{' identifiables '}
            dataType("scope", "Scope") {
                propertyDataTypeOf("typeReference", "TypeReference", false, 0)
                propertyListTypeOf("identifiables", "Identifiable", false, 1)
            }
            // identifiables = identifiable*
            dataType("identifiables", "Identifiables") {
                propertyListTypeOf("identifiable", "Identifiable", false, 0)
            }
            // identifiable = 'identify' typeReference 'by' propertyReferenceOrNothing
            dataType("identifiable", "Identifiable") {
                propertyDataTypeOf("typeReference", "TypeReference", false, 0)
                propertyPrimitiveType("propertyReferenceOrNothing", "String", false, 1)
            }
            // references = 'references' '{' referenceDefinitions '}'
            dataType("references", "References") {
                propertyListTypeOf("referenceDefinitions", "ReferenceDefinition", false, 1)
            }
            // referenceDefinitions = referenceDefinition*
            dataType("referenceDefinitions", "ReferenceDefinitions") {
                propertyListTypeOf("referenceDefinition", "ReferenceDefinition", false, 0)
            }
            // referenceDefinition = 'in' typeReference 'property' propertyReference 'refers-to' typeReferences
            dataType("referenceDefinition", "ReferenceDefinition") {
                propertyDataTypeOf("typeReference", "TypeReference", false, 0)
                propertyDataTypeOf("propertyReference", "PropertyReference", false, 1)
                propertyListTypeOf("typeReferences", "TypeReference", false, 2)
            }
            // typeReferences = [typeReferences / '|']+
            dataType("typeReferences", "TypeReferences") {
                propertyListSeparatedTypeOf("typeReference", "TypeReference", "String", false, 0)
            }
            // propertyReferenceOrNothing = '§nothing' | propertyReference
            dataType("propertyReferenceOrNothing", "PropertyReferenceOrNothing") {

            }
            // typeReference = IDENTIFIER     // same as grammar rule name
            dataType("typeReference", "TypeReference") {
                propertyPrimitiveType("identifier", "String", false, 0)
            }
            // propertyReference = IDENTIFIER // same as grammar rule name
            dataType("propertyReference", "PropertyReference") {
                propertyPrimitiveType("identifier", "String", false, 0)
            }
            // leaf IDENTIFIER = "[a-zA-Z_][a-zA-Z_0-9-]*"

        }

        GrammarTypeModelTest.tmAssertEquals(expected, actual)
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
                semanticAnalysis { context(ContextFromTypeModel(grammar.qualifiedName, TypeModelFromGrammar.create(grammar))) }
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
                semanticAnalysis { context(ContextFromTypeModel(grammar.qualifiedName, TypeModelFromGrammar.create(grammar))) }
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
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS, InputLocation(6, 7, 1, 5), "Type 'RuleX' not found in scope")
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
                semanticAnalysis { context(ContextFromTypeModel(grammar.qualifiedName, TypeModelFromGrammar.create(grammar))) }
            }
        )

        val expected = ScopeModelAgl().apply {
            scopes["Rule1"] = (ScopeDefinition("Rule1").apply {
                identifiables.add(Identifiable("Rule2", Navigation("rule3")))
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
                semanticAnalysis { context(ContextFromTypeModel(grammar.qualifiedName, TypeModelFromGrammar.create(grammar))) }
            }
        )

        val expected = ScopeModelAgl().apply {
            scopes["Rule1"] = (ScopeDefinition("Rule1").apply {
                identifiables.add(Identifiable("RuleX", Navigation("rule3")))
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
                    InputLocation(27, 14, 2, 5),
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
                semanticAnalysis { context(ContextFromTypeModel(grammar.qualifiedName, TypeModelFromGrammar.create(grammar))) }
            }
        )

        val expected = ScopeModelAgl().apply {
            scopes["Rule1"] = (ScopeDefinition("Rule1").apply {
                identifiables.add(Identifiable("Rule2", Navigation("ruleX")))
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
                    InputLocation(36, 23, 2, 5),
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
                in Rule2 {
                    property rule3 refers-to Rule1
                }
            }
        """.trimIndent()

        val result = aglProc.process(
            sentence = text,
            Agl.options {
                semanticAnalysis { context(ContextFromTypeModel(grammar.qualifiedName, TypeModelFromGrammar.create(grammar))) }
            }
        )

        val expected = ScopeModelAgl().apply {
            references.add(
                ReferenceDefinition(
                    "Rule2", listOf(
                        PropertyReferenceExpression("rule3", listOf("Rule1"), null)
                    )
                )
            )
        }

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)

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
                in RuleX {
                    property ruleY refers-to RuleZ|RuleW
                }
            }
        """.trimIndent()

        val result = aglProc.process(
            sentence = text,
            Agl.options {
                semanticAnalysis { context(ContextFromTypeModel(grammar.qualifiedName, TypeModelFromGrammar.create(grammar))) }
            }
        )

        val expected = ScopeModelAgl().apply {
            references.add(ReferenceDefinition("RuleX", listOf(PropertyReferenceExpression("ruleY", listOf("RuleZ", "RuleW"), null))))
        }
        val expectedIssues = listOf(
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(20, 8, 2, 5),
                "Referring type 'RuleX' not found in scope"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(51, 39, 2, 11),
                "For reference in 'RuleX' referred to type 'RuleZ' not found"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(51, 39, 2, 11),
                "For reference in 'RuleX' referred to type 'RuleW' not found"
            )
        )

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertEquals(expectedIssues.size, result.issues.errors.size)
        for (i in expectedIssues.indices) {
            assertEquals(expectedIssues[i], result.issues.errors[i])
        }
    }

    @Test
    fun one_reference_to_three() {
        val text = """
            references {
                in Type1 {
                    property prop refers-to Type2|Type3|Type4
                }
            }
        """.trimIndent()

        val result = aglProc.process(text)

        val expected = ScopeModelAgl().apply {
            references.add(ReferenceDefinition("Type1", listOf(PropertyReferenceExpression("prop", listOf("Type2", "Type3", "Type4"), null))))
        }

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)

    }

    //TODO more checks + check rules (types/properties) exist in context of grammar
}