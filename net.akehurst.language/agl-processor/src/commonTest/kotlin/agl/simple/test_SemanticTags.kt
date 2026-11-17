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

package net.akehurst.language.agl.simple

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.tokensWithSemanticTags
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.api.processor.StyleString
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asm.api.Asm
import net.akehurst.language.style.api.AglStyleTagRule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * semantic tags from semantic analysis, and style rules with semantic selectors that match them
 */
class test_SemanticTags {

    private companion object {
        val GRAMMAR = """
            namespace example
            grammar Shapes {
                skip leaf WS = "\s+" ;
                unit = item* ;
                item = shape | connection ;
                shape = circle | square ;
                circle = 'circle' NAME ;
                square = 'square' NAME ;
                connection = 'connect' source 'to' target ;
                leaf source = NAME ;
                leaf target = NAME ;
                leaf NAME = "[a-z]+" ;
            }
        """.trimIndent()
        val REFERENCES = """
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
        val STYLE = """
            namespace example
            styles Shapes {
                identify Shape { font-weight: bold; }
                refers-to Circle { foreground: red; }
                refers-to example.Shapes.Square { foreground: blue; }
                type Connection { font-style: italic; }
            }
        """.trimIndent()
        const val SENTENCE = "circle c\nsquare s\nconnect c to s"

        val definition by lazy {
            Agl.languageDefinitionFromStringSimple(
                net.akehurst.language.api.processor.LanguageIdentity("example.Shapes"),
                GrammarString(GRAMMAR),
                referenceStr = CrossReferenceString(REFERENCES),
                styleStr = StyleString(STYLE)
            )
        }
        val options = Agl.options<Asm, SentenceContext> { semanticAnalysis { sentenceContext(contextAsmSimple()) } }

        /** token text -> its tags, for the tokens of the sentence after semantic analysis */
        fun tokenTags(): List<Pair<String, List<String>>> {
            val proc = definition.processor!!
            val result = proc.process(SENTENCE, options)
            assertTrue(result.allIssues.errors.isEmpty(), result.allIssues.toString())
            val tokens = tokensWithSemanticTags(result.parse!!.sppt!!.tokensByLineAll(), result.semanticAnalysis!!.semanticTags)
            return tokens.flatten().map { Pair(SENTENCE.substring(it.position, it.position + it.length), it.tagList) }
        }
    }

    @Test
    fun declaration_identifier() {
        val c = tokenTags().first { it.first == "c" }.second
        assertTrue(c.contains("identify example.Shapes.Circle"), c.toString())
        assertTrue(c.contains("identify example.Shapes.Shape"), c.toString()) // a type it conforms to
        assertTrue(c.none { it.startsWith("refers-to") }, c.toString())
    }

    @Test
    fun reference_identifier() {
        val tokens = tokenTags()
        val refC = tokens.filter { it.first == "c" }[1].second
        assertTrue(refC.contains("refers-to example.Shapes.Circle"), refC.toString())
        val refS = tokens.filter { it.first == "s" }[1].second
        assertTrue(refS.contains("refers-to example.Shapes.Square"), refS.toString())
    }

    @Test
    fun element_type_on_all_its_tokens() {
        val tokens = tokenTags()
        val connect = tokens.first { it.first == "connect" }.second
        val to = tokens.first { it.first == "to" }.second
        assertTrue(connect.contains("type example.Shapes.Connection"), connect.toString())
        assertTrue(to.contains("type example.Shapes.Connection"), to.toString())
        val circle = tokens.first { it.first == "circle" }.second
        assertTrue(circle.none { it == "type example.Shapes.Connection" }, circle.toString())
    }

    @Test
    fun style_selectors_resolve_to_the_tags() {
        val style = definition.styleDomain!!
        assertTrue(definition.issues.errors.isEmpty(), definition.issues.toString())
        val selectors = style.allDefinitions.single().tagRules.flatMap { it.selector }
        assertEquals(
            listOf("identify example.Shapes.Shape", "refers-to example.Shapes.Circle", "refers-to example.Shapes.Square", "type example.Shapes.Connection"),
            selectors.map { it.matchTag }
        )
        // as written
        assertEquals(listOf("identify Shape", "refers-to Circle", "refers-to example.Shapes.Square", "type Connection"), selectors.map { it.value })
    }

    @Test
    fun style_type_not_found() {
        val res = Agl.registry.agl.style.processor!!.process(
            "namespace example\nstyles Shapes {\n  refers-to Nothing { foreground: red; }\n}",
            Agl.options {
                semanticAnalysis {
                    sentenceContext(
                        net.akehurst.language.grammar.processor.contextFromGrammar(definition.grammarDomain!!)
                            .union(net.akehurst.language.agl.semanticAnalyser.contextFromTypesDomain(definition.typesDomain!!))
                    )
                }
            }
        )
        assertEquals(listOf("Type 'Nothing' not found for style rule"), res.allIssues.errors.map { it.message })
    }

    @Test
    fun style_import_types() {
        val res = Agl.registry.agl.style.processor!!.process(
            "namespace other\nstyles X {\n  import-types example.Shapes\n  refers-to Circle { foreground: red; }\n}",
            Agl.options {
                semanticAnalysis {
                    sentenceContext(
                        net.akehurst.language.grammar.processor.contextFromGrammar(definition.grammarDomain!!)
                            .union(net.akehurst.language.agl.semanticAnalyser.contextFromTypesDomain(definition.typesDomain!!))
                    )
                }
            }
        )
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val sel = (res.asm!!.allDefinitions.single().rules.single() as AglStyleTagRule).selector.single()
        assertEquals("refers-to example.Shapes.Circle", sel.matchTag)
        // round trip keeps the import and the name as written
        val again = Agl.registry.agl.style.processor!!.process(res.asm!!.asString())
        assertTrue(again.allIssues.errors.isEmpty(), again.allIssues.toString())
        assertEquals(res.asm!!.asString(), again.asm!!.asString())
        assertTrue(res.asm!!.asString().contains("import-types example.Shapes"), res.asm!!.asString())
    }
}
