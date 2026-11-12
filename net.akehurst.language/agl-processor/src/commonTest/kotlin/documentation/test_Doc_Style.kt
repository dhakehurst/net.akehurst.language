/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.documentation

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.LanguageIdentity
import net.akehurst.language.api.processor.StyleString
import net.akehurst.language.documentation.test_Doc_GettingStarted.Companion.SHAPES_GRAMMAR
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The examples in 5_Documentation/dsl/style.md.
 */
class test_Doc_Style {

    @Test
    fun style() {
        // style.md: "Example"
        val style = $$"""
            namespace example
            styles Shapes {
                $nostyle {
                    foreground: black;
                }
                'circle', 'square', 'connect' {
                    foreground: blue;
                    font-weight: bold;
                }
                NAME, NUMBER {
                    foreground: darkred;
                }
                COMMENT {
                    foreground: gray;
                    font-style: italic;
                }
            }
        """.trimIndent()
        val definition = Agl.languageDefinitionFromStringSimple(
            LanguageIdentity("example.Shapes"),
            GrammarString(SHAPES_GRAMMAR),
            styleStr = StyleString(style)
        )
        val styleDomain = definition.styleDomain!!
        assertTrue(definition.issues.errors.isEmpty(), definition.issues.toString())
        val rules = styleDomain.allDefinitions.single().tagRules
        assertEquals(listOf(listOf("\$nostyle"), listOf("'circle'", "'square'", "'connect'"), listOf("NAME", "NUMBER"), listOf("COMMENT")), rules.map { r -> r.selector.map { it.value } })
        assertEquals("bold", rules[1].declaration["font-weight"]?.value)
    }

    @Test
    fun keyword_and_meta_rule() {
        // style.md: "Syntax" ($keyword and $$ meta rule)
        val style = $$"""
            namespace example
            styles Shapes {
                $keyword {
                    font-weight: bold;
                }
                $$ "'[^a-zA-Z]+'" {
                    foreground: gray;
                }
            }
        """.trimIndent()
        val definition = Agl.languageDefinitionFromStringSimple(
            LanguageIdentity("example.Shapes2"),
            GrammarString(SHAPES_GRAMMAR),
            styleStr = StyleString(style)
        )
        val styleSet = definition.styleDomain!!.allDefinitions.single()
        assertTrue(definition.issues.errors.isEmpty(), definition.issues.toString())
        assertEquals(listOf("\$keyword"), styleSet.tagRules.single().selector.map { it.value })
        assertEquals(1, styleSet.metaRules.size)
    }
}
