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
package net.akehurst.language.agl.language.scopes

import net.akehurst.language.agl.default.TypeModelFromGrammar
import net.akehurst.language.agl.grammarTypeModel.GrammarTypeModelTest
import net.akehurst.language.agl.grammarTypeModel.grammarTypeModel
import net.akehurst.language.agl.language.expressions.NavigationDefault
import net.akehurst.language.agl.language.reference.*
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_CrossReferences {

    private companion object {
        val aglProc = Agl.registry.agl.scopes.processor!!

        fun test(grammarStr: String, sentence: String, expected: CrossReferenceModelDefault, expIssues: Set<LanguageIssue>) {
            val grammar = Agl.registry.agl.grammar.processor!!.process(grammarStr).asm!![0]
            val result = aglProc.process(
                sentence = sentence,
                Agl.options {
                    semanticAnalysis { context(ContextFromTypeModel(TypeModelFromGrammar.create(grammar))) }
                }
            )

            assertEquals(expIssues, result.issues.all, result.issues.toString())
            val actual = result.asm!!
            assertEquals(expected.declarationsForNamespace, result.asm?.declarationsForNamespace)
            val expNs = expected.declarationsForNamespace["test.Test"]!!
            val actNs = actual.declarationsForNamespace["test.Test"]!!
            assertEquals(expNs.scopes, actNs.scopes)
            assertEquals(expNs.scopes.flatMap { it.value.identifiables }, actNs.scopes.flatMap { it.value.identifiables })
            assertEquals(expNs.references, actNs.references)
        }
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

        val expected = CrossReferenceModelDefault()

        assertEquals(expected.declarationsForNamespace, result.asm?.declarationsForNamespace)
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

        val expected = CrossReferenceModelDefault()

        assertEquals(expected.declarationsForNamespace, result.asm?.declarationsForNamespace)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
    }

    @Test
    fun one_empty_scope() {
        val grammarStr = """
                namespace test
                grammar Test {
                    rule1 = 'a' ;
                }
            """.trimIndent()

        val sentence = """
            namespace test.Test {
                scope Rule1 { }
            }
        """.trimIndent()

        val expected = CrossReferenceModelDefault().apply {
            declarationsForNamespace["test.Test"] = DeclarationsForNamespaceDefault("test.Test").apply {
                scopes["Rule1"] = (ScopeDefinitionDefault("Rule1"))
            }
        }

        test(grammarStr, sentence, expected, emptySet())
    }

    @Test
    fun one_empty_scope_wrong_scope_ruleName() {
        val grammarStr = """
                namespace test
                grammar Test {
                    rule1 = 'a' ;
                }
            """.trimIndent()

        val sentence = """
            namespace test.Test {
                scope RuleX { }
            }
        """.trimIndent()

        val expected = CrossReferenceModelDefault().apply {
            declarationsForNamespace["test.Test"] = DeclarationsForNamespaceDefault("test.Test").apply {
                scopes["RuleX"] = (ScopeDefinitionDefault("RuleX"))
            }
        }
        val expIssues = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(32, 11, 2, 5),
                "Type 'RuleX' not found"
            )
        )

        test(grammarStr, sentence, expected, expIssues)
    }

    @Test
    fun scope_one_identifiable() {
        val grammarStr = """
                namespace test
                grammar Test {
                    rule1 = 'X' rule2 'Y' ;
                    rule2 = 'a' rule3 ;
                    rule3 = "[a-z]" ;
                }
            """.trimIndent()

        val sentence = """
            namespace test.Test {
                scope Rule1 {
                    identify Rule2 by rule3
                }
            }
        """.trimIndent()

        val expected = CrossReferenceModelDefault().apply {
            declarationsForNamespace["test.Test"] = DeclarationsForNamespaceDefault("test.Test").apply {
                scopes["Rule1"] = (ScopeDefinitionDefault("Rule1").apply {
                    identifiables.add(IdentifiableDefault("Rule2", NavigationDefault("rule3")))
                })
            }
        }

        test(grammarStr, sentence, expected, emptySet())
    }

    @Test
    fun scope_one_identifiable_wrong_type_ruleName() {
        val grammarStr = """
                namespace test
                grammar Test {
                    rule1 = 'X' rule2 'Y' ;
                    rule2 = 'a' rule3 ;
                    rule3 = "[a-z]" ;
                }
            """.trimIndent()

        val sentence = """
            namespace test.Test {
                scope Rule1 {
                    identify RuleX by rule3
                }
            }
        """.trimIndent()

        val expected = CrossReferenceModelDefault().apply {
            declarationsForNamespace["test.Test"] = DeclarationsForNamespaceDefault("test.Test").apply {
                scopes["Rule1"] = (ScopeDefinitionDefault("Rule1").apply {
                    identifiables.add(IdentifiableDefault("RuleX", NavigationDefault("rule3")))
                })
            }
        }
        val expIssues = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(57, 18, 3, 5),
                "Type 'RuleX' not found"
            )
        )

        test(grammarStr, sentence, expected, expIssues)

    }

    @Test
    fun scope_one_identifiable_wrong_property_ruleName() {
        val grammarStr = """
                namespace test
                grammar Test {
                    rule1 = 'X' rule2 'Y' ;
                    rule2 = 'a' rule3 ;
                    rule3 = "[a-z]" ;
                }
            """.trimIndent()


        val sentence = """
            namespace test.Test {
                scope Rule1 {
                    identify Rule2 by ruleX
                }
            }
        """.trimIndent()

        val expected = CrossReferenceModelDefault().apply {
            declarationsForNamespace["test.Test"] = DeclarationsForNamespaceDefault("test.Test").apply {
                scopes["Rule1"] = (ScopeDefinitionDefault("Rule1").apply {
                    identifiables.add(IdentifiableDefault("Rule2", NavigationDefault("ruleX")))
                })
            }
        }
        val expIssues = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(36, 23, 2, 5),
                "In scope for 'Rule1', 'ruleX' not found for identifying property of 'Rule2'"
            )
        )

        test(grammarStr, sentence, expected, expIssues)

    }

    @Test
    fun one_reference() {
        val grammarStr = """
                namespace test
                grammar Test {
                    rule1 = 'X' rule2 'Y' ;
                    rule2 = 'a' rule3 ;
                    rule3 = "[a-z]" ;
                }
            """.trimIndent()

        val sentence = """
            namespace test.Test {
                references {
                    in Rule2 {
                        property rule3 refers-to Rule1
                    }
                }
            }
        """.trimIndent()

        val expected = CrossReferenceModelDefault().apply {
            declarationsForNamespace["test.Test"] = DeclarationsForNamespaceDefault("test.Test").apply {
                references.add(
                    ReferenceDefinitionDefault(
                        "Rule2", listOf(
                            PropertyReferenceExpressionDefault(NavigationDefault("rule3"), listOf("Rule1"), null)
                        )
                    )
                )
            }
        }
        val expIssues = setOf<LanguageIssue>()

        test(grammarStr, sentence, expected, expIssues)

    }

    @Test
    fun one_reference_unknown_rules() {
        val grammarStr = """
                namespace test
                grammar Test {
                    rule1 = 'X' rule2 'Y' ;
                    rule2 = 'a' rule3 ;
                    rule3 = "[a-z]" ;
                }
            """.trimIndent()

        val sentence = """
            namespace test.Test {
                references {
                    in RuleX {
                        property ruleY refers-to RuleZ|RuleW
                    }
                }
            }
        """.trimIndent()

        val expected = CrossReferenceModelDefault().apply {
            declarationsForNamespace["test.Test"] = DeclarationsForNamespaceDefault("test.Test").apply {
                references.add(ReferenceDefinitionDefault("RuleX", listOf(PropertyReferenceExpressionDefault(NavigationDefault("ruleY"), listOf("RuleZ", "RuleW"), null))))
            }
        }
        val expIssues = setOf(
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

        test(grammarStr, sentence, expected, expIssues)

    }

    @Test
    fun one_reference_to_three() {
        val grammarStr = """
                namespace test
                grammar Test {
                    rule1 = 'X' rule2 'Y' ;
                    rule2 = 'a' rule3 ;
                    rule3 = "[a-z]" ;
                }
            """.trimIndent()

        val sentence = """
            namespace test.Test {
                references {
                    in Type1 {
                        property prop refers-to Type2|Type3|Type4
                    }
                }
            }
        """.trimIndent()

        val expected = CrossReferenceModelDefault().apply {
            declarationsForNamespace["test.Test"] = DeclarationsForNamespaceDefault("test.Test").apply {
                references.add(ReferenceDefinitionDefault("Type1", listOf(PropertyReferenceExpressionDefault(NavigationDefault("prop"), listOf("Type2", "Type3", "Type4"), null))))

            }
        }
        val expIssues = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR,
                LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(36, 23, 2, 5),
                "In scope for 'Rule1', 'ruleX' not found for identifying property of 'Rule2'"
            )
        )

        test(grammarStr, sentence, expected, expIssues)

    }

    @Test
    fun reference_to_external_type_fails() {
        val grammarStr = """
                namespace test
                grammar Test {
                    rule1 = 'X' rule2 'Y' ;
                    rule2 = 'a' rule3 ;
                    rule3 = "[a-z]" ;
                }
            """.trimIndent()

        val sentence = """
            namespace test.Test {
                references {
                    in Rule2 {
                        property rule3 refers-to AnExternalType1 | AnExternalType2
                    }
                }
            }
        """.trimIndent()

        val expected = CrossReferenceModelDefault().apply {
            declarationsForNamespace["test.Test"] = DeclarationsForNamespaceDefault("test.Test").apply {
                references.add(
                    ReferenceDefinitionDefault(
                        "Rule2", listOf(
                            PropertyReferenceExpressionDefault(NavigationDefault("rule3"), listOf("Rule1"), null)
                        )
                    )
                )
            }
        }
        val expIssues = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(0, 0, 0, 0),
                "For references in 'Rule2', referred to type 'AnExternalType1' not found"
            ),
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(0, 0, 0, 0),
                "For references in 'Rule2', referred to type 'AnExternalType2' not found"
            )
        )

        test(grammarStr, sentence, expected, expIssues)

    }

    @Test
    fun reference_to_external_type() {
        val grammarStr = """
                namespace test
                grammar Test {
                    rule1 = 'X' rule2 'Y' ;
                    rule2 = 'a' rule3 ;
                    rule3 = "[a-z]" ;
                }
            """.trimIndent()

        val sentence = """
            namespace test.Test {
                references {
                    external-types AnExternalType1, AnExternalType2
                    in Rule2 {
                        property rule3 refers-to AnExternalType1 | AnExternalType2
                    }
                }
            }
        """.trimIndent()

        val expected = CrossReferenceModelDefault().apply {
            declarationsForNamespace["test.Test"] = DeclarationsForNamespaceDefault("test.Test").apply {
                references.add(
                    ReferenceDefinitionDefault(
                        "Rule2", listOf(
                            PropertyReferenceExpressionDefault(NavigationDefault("rule3"), listOf("AnExternalType1", "AnExternalType2"), null)
                        )
                    )
                )
            }
        }
        val expIssues = setOf<LanguageIssue>()

        test(grammarStr, sentence, expected, expIssues)

    }

    //TODO more checks + check rules (types/properties) exist in context of grammar


}