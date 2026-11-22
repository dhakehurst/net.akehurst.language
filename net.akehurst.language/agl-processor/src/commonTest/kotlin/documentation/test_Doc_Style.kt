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
import net.akehurst.language.agl.processor.tokensWithSemanticTags
import net.akehurst.language.agl.semanticAnalyser.contextFromTypesDomain
import net.akehurst.language.agl.simple.contextAsmSimple
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.documentation.test_Doc_GettingStarted.Companion.SHAPES_GRAMMAR
import net.akehurst.language.grammar.processor.contextFromGrammar
import net.akehurst.language.style.api.AglStyleTagRule
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

    private val shapesReferences = """
        namespace example.Shapes
            identify Circle by name
            identify Square by name
            references {
                in Connection {
                    property source refers-to Circle|Square
                    property target refers-to Circle|Square
                }
            }
    """.trimIndent()

    @Test
    fun semantic_selectors() {
        // style.md: "Semantic selectors"
        val style = """
            namespace example
            styles Shapes {
                identify Shape {
                    font-weight: bold;
                }
                refers-to Circle {
                    foreground: red;
                }
                type Connection {
                    font-style: italic;
                }
            }
        """.trimIndent()
        val definition = Agl.languageDefinitionFromStringSimple(
            LanguageIdentity("example.Shapes3"),
            GrammarString(SHAPES_GRAMMAR),
            referenceStr = CrossReferenceString(shapesReferences),
            styleStr = StyleString(style)
        )
        val styleSet = definition.styleDomain!!.allDefinitions.single()
        assertTrue(definition.issues.errors.isEmpty(), definition.issues.toString())
        assertEquals(
            listOf("identify example.Shapes.Shape", "refers-to example.Shapes.Circle", "type example.Shapes.Connection"),
            styleSet.tagRules.map { it.selector.single().matchTag }
        )

        val sentence = "circle c\nconnect c to c"
        val result = definition.processor!!.process(sentence, Agl.options<Asm, SentenceContext> { semanticAnalysis { sentenceContext(contextAsmSimple()) } })
        assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())
        val tokens = tokensWithSemanticTags(result.parse!!.sppt!!.tokensByLineAll(), result.semanticAnalysis!!.semanticTags).flatten()
        val names = tokens.filter { sentence.substring(it.position, it.position + it.length) == "c" }
        assertTrue(names[0].tagList.contains("identify example.Shapes.Shape"), names[0].tagList.toString())     // bold
        assertTrue(names[1].tagList.contains("refers-to example.Shapes.Circle"), names[1].tagList.toString())   // red
        val to = tokens.first { sentence.substring(it.position, it.position + it.length) == "to" }
        assertTrue(to.tagList.contains("type example.Shapes.Connection"), to.tagList.toString())                // italic
    }

    @Test
    fun semantic_selectors_import_types() {
        // style.md: "Semantic selectors" (import-types)
        val definition = Agl.languageDefinitionFromStringSimple(
            LanguageIdentity("example.Shapes4"),
            GrammarString(SHAPES_GRAMMAR),
            referenceStr = CrossReferenceString(shapesReferences)
        )
        val style = """
            namespace other
            styles X {
                import-types example.Shapes
                refers-to Circle { foreground: red; }
            }
        """.trimIndent()
        val res = Agl.registry.agl.style.processor!!.process(
            style,
            Agl.options { semanticAnalysis { sentenceContext(contextFromGrammar(definition.grammarDomain!!).union(contextFromTypesDomain(definition.typesDomain!!))) } }
        )
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val sel = (res.asm!!.allDefinitions.single().rules.single() as AglStyleTagRule).selector.single()
        assertEquals("refers-to example.Shapes.Circle", sel.matchTag)
    }
}
