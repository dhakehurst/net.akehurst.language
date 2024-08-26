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

package net.akehurst.language.agl.language.reference

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.grammarTypeModel.GrammarTypeModelTest
import net.akehurst.language.agl.grammarTypeModel.grammarTypeModel
import net.akehurst.language.agl.language.asmTransform.AsmTransformModelSimple
import net.akehurst.language.agl.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.agl.language.reference.asm.builder.crossReferenceModel
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.api.language.reference.CrossReferenceModel
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.api.typeModel
import net.akehurst.language.typemodel.simple.SimpleTypeModelStdLib
import net.akehurst.language.typemodel.simple.TypeModelSimple
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_CrossReferences {

    private companion object {
        val aglProc = Agl.registry.agl.crossReference.processor!!

        fun test(grammarStr: String, sentence: String, expected: CrossReferenceModel, typemodel: TypeModel? = null, expIssues: Set<LanguageIssue> = emptySet()) {
            val grammar = Agl.registry.agl.grammar.processor!!.process(grammarStr).asm!![0]
            val grmrTypeModel = TypeModelSimple(grammar.name)
            grmrTypeModel.addNamespace(SimpleTypeModelStdLib)
            AsmTransformModelSimple.fromGrammar(grammar, grmrTypeModel)
            val tm = grmrTypeModel
            typemodel?.let { tm.addAllNamespace(it.namespace.values) }
            val ctx = ContextFromTypeModel(tm)
            val result = aglProc.process(
                sentence = sentence,
                Agl.options {
                    semanticAnalysis { context(ctx) }
                }
            )

            assertEquals(expIssues, result.issues.all, result.issues.toString())
            val actual = result.asm!!
            assertEquals(expected.declarationsForNamespace, result.asm?.declarationsForNamespace?.toMap())
            val expNs = expected.declarationsForNamespace["test.Test"]!!
            val actNs = actual.declarationsForNamespace["test.Test"]!!
            assertEquals(expNs.scopeDefinition, actNs.scopeDefinition)
            assertEquals(expNs.scopeDefinition.flatMap { it.value.identifiables }, actNs.scopeDefinition.flatMap { it.value.identifiables })
            assertEquals(expNs.references, actNs.references)
        }
    }

    @Test
    fun check_typeModel() {
        val actual = aglProc.typeModel
        val expected = grammarTypeModel("net.akehurst.language.agl.language", "References") {
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

        val expected = crossReferenceModel {
            declarationsFor("test.Test") {
                scope("Rule1") {
                }
            }
        }

        test(grammarStr, sentence, expected)
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

        val expected = crossReferenceModel {
            declarationsFor("test.Test") {
                scope("RuleX") { }
            }
        }
        val expIssues = setOf(
            LanguageIssue(
                LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                InputLocation(32, 11, 2, 5),
                "Type 'RuleX' not found"
            )
        )

        test(grammarStr, sentence, expected, null, expIssues)
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

        val expected = crossReferenceModel {
            declarationsFor("test.Test") {
                scope("Rule1") {
                    identify("Rule2", "rule3")
                }
            }
        }

        test(grammarStr, sentence, expected)
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

        val expected = crossReferenceModel {
            declarationsFor("test.Test") {
                scope("Rule1") {
                    identify("RuleX", "rule3")
                }
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

        test(grammarStr, sentence, expected, null, expIssues)

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

        val expected = crossReferenceModel {
            declarationsFor("test.Test") {
                scope("Rule1") {
                    identify("Rule2", "ruleX")
                }
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

        test(grammarStr, sentence, expected, null, expIssues)

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

        val expected = crossReferenceModel {
            declarationsFor("test.Test") {
                reference("Rule2") {
                    property("rule3", listOf("Rule1"), null)
                }
            }
        }

        test(grammarStr, sentence, expected)

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

        val expected = crossReferenceModel {
            declarationsFor("test.Test") {
                reference("RuleX") {
                    property("ruleY", listOf("RuleZ", "RuleW"), null)
                }
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

        test(grammarStr, sentence, expected, null, expIssues)

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
                    in Rule1 {
                        property rule2 refers-to Rule1|Rule2|Rule3
                    }
                }
            }
        """.trimIndent()

        val expected = crossReferenceModel {
            declarationsFor("test.Test") {
                reference("Rule1") {
                    property("rule2", listOf("Rule1", "Rule2", "Rule3"), null)
                }
            }
        }

        test(grammarStr, sentence, expected)

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

        val expected = crossReferenceModel {
            declarationsFor("test.Test") {
                reference("Rule2") {
                    property("rule3", listOf("AnExternalType1", "AnExternalType2"), null)
                }
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

        test(grammarStr, sentence, expected, null, expIssues)

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

        val additionalTypeModel = typeModel("externals", true) {
            namespace("external") {
                dataType("AnExternalType1")
                dataType("AnExternalType2")
            }
        }

        val sentence = """
            namespace test.Test {
                import external
                references {
                    in Rule2 {
                        property rule3 refers-to AnExternalType1 | AnExternalType2
                    }
                }
            }
        """.trimIndent()

        val expected = crossReferenceModel {
            declarationsFor("test.Test") {
                import("external")
                reference("Rule2") {
                    property("rule3", listOf("AnExternalType1", "AnExternalType2"), null)
                }
            }
        }

        test(grammarStr, sentence, expected, additionalTypeModel)

    }

    //TODO more checks + check rules (types/properties) exist in context of grammar


}