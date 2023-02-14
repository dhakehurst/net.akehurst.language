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

import net.akehurst.language.agl.grammar.grammar.ContextFromGrammar
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageIssueKind
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.typeModel.TypeModel
import net.akehurst.language.api.typeModel.TypeModelTest
import net.akehurst.language.api.typeModel.typeModel
import kotlin.test.Test
import kotlin.test.assertEquals

class test_AglScopes {

    private companion object {
        val aglProc = Agl.registry.agl.scopes.processor!!
    }

    @Test
    fun scopes_typeModel() {
        val actual = aglProc.typeModel
        val expected = typeModel("net.akehurst.language.agl","AglScopes") {

        }

        TypeModelTest.assertEquals(expected, actual)
    }

    @Test
    fun single_line_comment() {

        val text = """
            // single line comment
        """.trimIndent()

        val result = aglProc.process(text)

        val expected = ScopeModel()

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertEquals(emptyList(), result.issues)
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

        val expected = ScopeModel()

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertEquals(emptyList(), result.issues)
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
            scope rule1 { }
        """.trimIndent()

        val result = aglProc.process(
            sentence = text,
            Agl.options {
                syntaxAnalysis { context(ContextFromGrammar(grammar)) }
            }
        )

        val expected = ScopeModel().apply {
            scopes["rule1"]=(ScopeDefinition("rule1"))
        }

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertEquals(emptyList(), result.issues)
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
            scope ruleX { }
        """.trimIndent()

        val result = aglProc.process(
            sentence = text,
            Agl.options {
                syntaxAnalysis { context(ContextFromGrammar(grammar)) }
            }
        )

        val expected = ScopeModel().apply {
            scopes["ruleX"]=(ScopeDefinition("ruleX"))
        }

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertEquals(
            listOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SYNTAX_ANALYSIS, InputLocation(6, 7, 1, 6), "GrammarRule 'ruleX' not found for scope")
            ), result.issues
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
            scope rule1 {
                identify rule2 by rule3
            }
        """.trimIndent()

        val result = aglProc.process(
            sentence = text,
            Agl.options {
                syntaxAnalysis { context(ContextFromGrammar(grammar)) }
            }
        )

        val expected = ScopeModel().apply {
            scopes["rule1"]=(ScopeDefinition("rule1").apply {
                identifiables.add(Identifiable("rule2", "rule3"))
            })
        }

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertEquals(emptyList(), result.issues)
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
            scope rule1 {
                identify ruleX by rule3
            }
        """.trimIndent()

        val result = aglProc.process(
            sentence = text,
            Agl.options {
                syntaxAnalysis { context(ContextFromGrammar(grammar)) }
            }
        )

        val expected = ScopeModel().apply {
            scopes["rule1"]=(ScopeDefinition("rule1").apply {
                identifiables.add(Identifiable("ruleX", "rule3"))
            })
        }

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertEquals(
            listOf(
                LanguageIssue(
                    LanguageIssueKind.ERROR,
                    LanguageProcessorPhase.SYNTAX_ANALYSIS,
                    InputLocation(27, 14, 2, 6),
                    "In scope for 'rule1' GrammarRule 'ruleX' not found as identifiable type"
                )
            ),
            result.issues
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
            scope rule1 {
                identify rule2 by ruleX
            }
        """.trimIndent()

        val result = aglProc.process(
            sentence = text,
            Agl.options {
                syntaxAnalysis { context(ContextFromGrammar(grammar)) }
            }
        )

        val expected = ScopeModel().apply {
            scopes["rule1"]=(ScopeDefinition("rule1").apply {
                identifiables.add(Identifiable("rule2", "ruleX"))
            })
        }

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertEquals(
            listOf(
                LanguageIssue(
                    LanguageIssueKind.ERROR,
                    LanguageProcessorPhase.SYNTAX_ANALYSIS,
                    InputLocation(36, 23, 2, 6),
                    "In scope for 'rule1' GrammarRule 'ruleX' not found for identifying property of 'rule2'"
                )
            ), result.issues
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
                in rule2 property rule3 refers-to rule1
            }
        """.trimIndent()

        val result = aglProc.process(
            sentence = text,
            Agl.options {
                syntaxAnalysis { context(ContextFromGrammar(grammar)) }
            }
        )

        val expected = ScopeModel().apply {
            references.add(ReferenceDefinition("rule2", "rule3", listOf("rule1")))
        }

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertEquals(emptyList(), result.issues)
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
                in ruleX property ruleY refers-to ruleZ|ruleW
            }
        """.trimIndent()

        val result = aglProc.process(
            sentence = text,
            Agl.options {
                syntaxAnalysis { context(ContextFromGrammar(grammar)) }
            }
        )

        val expected = ScopeModel().apply {
            references.add(ReferenceDefinition("ruleX", "ruleY", listOf("ruleZ", "ruleW")))
        }

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertEquals(
            listOf(
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SYNTAX_ANALYSIS, InputLocation(20, 8, 2, 6), "Referring type GrammarRule 'ruleX' not found"),
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SYNTAX_ANALYSIS, InputLocation(35, 23, 2, 6), "For reference in 'ruleX' referring property GrammarRule 'ruleY' not found"),
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SYNTAX_ANALYSIS, InputLocation(51, 39, 2, 5), "For reference in 'ruleX' referred to type GrammarRule 'ruleZ' not found"),
                LanguageIssue(LanguageIssueKind.ERROR, LanguageProcessorPhase.SYNTAX_ANALYSIS, InputLocation(57, 45, 2, 6), "For reference in 'ruleX' referred to type GrammarRule 'ruleW' not found")
            ), result.issues
        )
    }

    @Test
    fun one_reference_to_three() {
        val text = """
            references {
                in type1 property prop refers-to type2|type3|type4
            }
        """.trimIndent()

        val result = aglProc.process(text)

        val expected = ScopeModel().apply {
            references.add(ReferenceDefinition("type1", "prop", listOf("type2", "type3", "type4")))
        }

        assertEquals(expected.scopes, result.asm?.scopes)
        assertEquals(expected.scopes.flatMap { it.value.identifiables }, result.asm?.scopes?.flatMap { it.value.identifiables })
        assertEquals(expected.references, result.asm?.references)
        assertEquals(emptyList(), result.issues)
    }

    //TODO more checks + check rules (types/properties) exist in context of grammar
}