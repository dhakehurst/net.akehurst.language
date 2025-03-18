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

package net.akehurst.language.agl.grammar.style

import net.akehurst.language.agl.Agl
import net.akehurst.language.grammar.processor.ContextFromGrammar
import net.akehurst.language.issues.api.LanguageIssue
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.style.api.AglStyleMetaRule
import net.akehurst.language.style.api.AglStyleSelectorKind
import net.akehurst.language.style.api.AglStyleTagRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


class test_AglStyle {

    private companion object {
        val aglProc = Agl.registry.agl.style.processor!!
        val testGrammar = Agl.registry.agl.grammar.processor?.process(
            """
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
            
                // not marked as leaf so can use to test patterns
                COMMENT = "\"(\\?.)*\"" ;
                INT = "[0-9]+" ;
            }
        """.trimIndent()
        )?.asm!!

        fun process(sentence: String) = aglProc.process(
            sentence,
            Agl.options {
                semanticAnalysis { context(ContextFromGrammar.createContextFrom(testGrammar)) }
            })
    }

    @Test
    fun single_line_comment() {

        val text = """
            namespace test
            // single line comment
            styles Test {}
        """.trimIndent()

        val result = process(text)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.asm)
        assertEquals(1, result.asm?.allDefinitions?.size) // 1 default nostyle rule

        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }

    @Test
    fun multi_line_comment() {

        val text = """
            /* multi
               line
               comment
            */
            namespace test
            styles Test {}
        """.trimIndent()

        val result = process(text)
        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.asm)
        assertEquals(1, result.asm?.allDefinitions?.size)
        assertEquals(0, result.issues.size, result.issues.joinToString("\n") { "$it" })
    }

    @Test
    fun emptyRule() {
        val text = """
            namespace test
            styles Test {
              declaration { }
            }
        """.trimIndent()

        val result = process(text)

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.asm)
        assertEquals(1, result.asm!!.allDefinitions.size)
        assertEquals(1, result.asm!!.allDefinitions[0].rules.size)
        val actual_0_0 = result.asm!!.allDefinitions[0].rules[0] as AglStyleTagRule
        assertEquals(0, actual_0_0.declaration.size)
    }

    @Test
    fun oneLeafRule() {

        val text = """
            namespace test
            styles Test {
                ID {
                    -fx-fill: green;
                    -fx-font-weight: bold;
                }
            }
        """.trimIndent()

        val result = process(text)

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.asm)
        assertEquals(1, result.asm!!.allDefinitions.size)
        assertEquals(1, result.asm!!.allDefinitions[0].rules.size)
        val actual_0_0 = result.asm!!.allDefinitions[0].rules[0] as AglStyleTagRule
        assertEquals("ID", actual_0_0.selector.first().value)
        assertEquals(2, actual_0_0.declaration.entries.size)
    }

    @Test
    fun multipleLeafRules() {
        val text = """
            namespace test
            styles Test {
                ID {
                    -fx-fill: green;
                    -fx-font-weight: bold;
                }
                "[0-9]+" {
                    -fx-fill: green;
                    -fx-font-weight: bold;
                }
            }
        """.trimIndent()

        val result = process(text)

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.asm)
        assertEquals(1, result.asm!!.allDefinitions.size)
        assertEquals(2, result.asm!!.allDefinitions[0].rules.size)
    }

    @Test
    fun regexWithQuotes() {
        val text = """
            namespace test
            styles Test {
                "\"(\\?.)*\"" {
                  font-family: 'Courier New';
                  color: darkblue;
                }
            }
        """.trimIndent()

        val result = process(text)

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.asm)
        assertEquals(1, result.asm!!.allDefinitions.size)
        assertEquals(1, result.asm!!.allDefinitions[0].rules.size)
        val actual_0_0 = result.asm!!.allDefinitions[0].rules[0] as AglStyleTagRule
        assertEquals(AglStyleSelectorKind.PATTERN, actual_0_0.selector[0].kind)
    }

    @Test
    fun selectorAndComposition() {

        val text = """
            namespace test
            styles Test {
              property,typeReference,typeArguments { }
            }
        """.trimIndent()

        val result = process(text)

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.asm)
        assertEquals(1, result.asm!!.allDefinitions.size)
        assertEquals(1, result.asm!!.allDefinitions[0].rules.size)
        val actual_0_0 = result.asm!!.allDefinitions[0].rules[0] as AglStyleTagRule
        assertEquals(3, actual_0_0.selector.size)
    }
    //TODO more tests

    @Test
    fun selector_notFound() {

        val text = """
            namespace test
            styles Test {
              xxx { }
            }
        """.trimIndent()

        val result = process(text)

        assertNotNull(result.asm)
        assertEquals(1, result.asm!!.allDefinitions.size)
        assertEquals(1, result.asm!!.allDefinitions[0].rules.size)
        assertEquals(
            setOf(
                LanguageIssue(
                    LanguageIssueKind.ERROR, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                    InputLocation(31, 3, 3, 3, null),
                    "Grammar Rule 'xxx' not found for style rule",
                    null
                )
            ), result.issues.all
        )
    }

    @Test
    fun oneSpecialRule() {

        val text = """
            namespace test
            styles Test {
                ${"$"}nostyle {
                    -fx-fill: green;
                    -fx-font-weight: bold;
                }
            }
        """.trimIndent()

        val result = process(text)

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.asm)
        assertEquals(1, result.asm!!.allDefinitions.size)
        assertEquals(1, result.asm!!.allDefinitions[0].rules.size)
        val actual_0_0 = result.asm!!.allDefinitions[0].rules[0] as AglStyleTagRule
    }

    @Test
    fun oneMetaRule() {

        val text = """
            namespace test
            styles Test {
                ${"$$"} "'([^']+)'" {
                    -fx-fill: green;
                    -fx-font-weight: bold;
                }
            }
        """.trimIndent()

        val result = process(text)

        assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
        assertNotNull(result.asm)
        assertEquals(1, result.asm!!.allDefinitions.size)
        assertEquals(1, result.asm!!.allDefinitions[0].rules.size)
        val actual_0_0 = result.asm!!.allDefinitions[0].rules[0] as AglStyleMetaRule
    }

}
