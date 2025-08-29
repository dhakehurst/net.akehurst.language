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

package net.akehurst.language.style.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.processor.contextFromGrammar
import net.akehurst.language.style.asm.AglStyleDomainDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_CompletionProvider {

    private companion object {
        val aglProc = Agl.registry.agl.style.processor!!

        fun grammarFor(grammarStr: String): GrammarDomain {
            return Agl.registry.agl.grammar.processor?.process(grammarStr)?.asm!!
        }

        fun test(grammarStr: String, sentence: String, position: Int, expected: List<CompletionItem>) {
            val testGrammar = grammarFor(grammarStr)
            val context = contextFromGrammar(testGrammar)
            val actual = aglProc.expectedItemsAt(sentence, position, Agl.options {
                completionProvider {
                    context(context)
                }
            })
            assertTrue(actual.issues.errors.isEmpty(), actual.issues.toString())
            assertEquals(expected.joinToString("\n"), actual.items.joinToString("\n"))
        }
    }

    @Test
    fun atStart() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = ""
        val expected = listOf(
            CompletionItem(kind = CompletionItemKind.SEGMENT, label = "unit", text = "<namespace> <styleSet>"),
            CompletionItem(kind = CompletionItemKind.LITERAL, label = "'namespace'", text = "namespace")
        )
        test(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun before_Selector() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = "namespace test styles StyleSet { "
        val expected = listOf(
            CompletionItem(kind=CompletionItemKind.REFERRED, label="GrammarRule", text="S"),
            CompletionItem(kind=CompletionItemKind.REFERRED, label="LITERAL", text="'a'"),
            CompletionItem(kind=CompletionItemKind.SEGMENT, label="rule", text="<rule>"),
            CompletionItem(kind=CompletionItemKind.LITERAL, label="'$$'", text="$$"),
            CompletionItem(kind=CompletionItemKind.LITERAL, label="'}'", text="}"),
            CompletionItem(kind=CompletionItemKind.PATTERN, label= $$"[\\$][a-zA-Z_][a-zA-Z_0-9-]*", text="<SPECIAL_IDENTIFIER>"),
        )
        test(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun after_Selector() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = "namespace test styles StyleSet { S "
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "','", ","),
            CompletionItem(CompletionItemKind.LITERAL, "'{'", "{"),
        )
        test(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun after_SelectorAndComposition() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = "namespace test styles StyleSet { S, "
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "LITERAL", "'a'"),
            CompletionItem(CompletionItemKind.LITERAL, "GrammarRule", "S"),
            CompletionItem(CompletionItemKind.LITERAL, "META_IDENTIFIER", AglStyleDomainDefault.KEYWORD_STYLE_ID.value),
        )
        test(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun after_RuleStart() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = "namespace test styles StyleSet { S {"
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_ID", "foreground"),
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_ID", "background"),
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_ID", "font-style"),
            CompletionItem(CompletionItemKind.LITERAL, "rule", "}"),
            CompletionItem(CompletionItemKind.SEGMENT, "style", "<STYLE_ID>: <STYLE_VALUE>;"),
        )
        test(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun after_STYLE_ID() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = "S { foreground"
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "style", ":"),
        )
        test(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun after_style_colon() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = "S { foreground:"
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_VALUE", "<colour>"),
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_VALUE", "bold"),
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_VALUE", "italic"),
        )
        test(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun after_style_colon_WS() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = "S { foreground: "
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_VALUE", "<colour>"),
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_VALUE", "bold"),
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_VALUE", "italic"),
        )
        test(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun after_STYLE_VALUE() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = "S { foreground: blue"
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "style", ";"),
        )
        test(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun after_style_end() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = "S { foreground: blue;"
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_ID", "foreground"),
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_ID", "background"),
            CompletionItem(CompletionItemKind.LITERAL, "STYLE_ID", "font-style"),
            CompletionItem(CompletionItemKind.LITERAL, "rule", "}"),
            CompletionItem(CompletionItemKind.SEGMENT, "style", "<STYLE_ID>: <STYLE_VALUE>;"),
        )
        test(grammarStr, sentence, sentence.length, expected)
    }

    @Test
    fun after_rule_end() {
        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' ;
            }
        """
        val sentence = "S { foreground: blue; }"
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "LITERAL", "'a'"),
            CompletionItem(CompletionItemKind.LITERAL, "GrammarRule", "S"),
            CompletionItem(CompletionItemKind.LITERAL, "META_IDENTIFIER", AglStyleDomainDefault.KEYWORD_STYLE_ID.value),
        )
        test(grammarStr, sentence, sentence.length, expected)
    }
}