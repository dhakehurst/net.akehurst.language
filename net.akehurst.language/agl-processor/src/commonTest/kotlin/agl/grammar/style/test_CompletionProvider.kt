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

import net.akehurst.language.agl.grammar.grammar.ContextFromGrammar
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_CompletionProvider {

    private companion object {
        val aglProc = Agl.registry.agl.style.processor!!

        fun grammarFor(grammarStr: String): Grammar {
            return Agl.registry.agl.grammar.processor?.process(grammarStr)?.asm?.first()!!
        }

        fun test(grammarStr: String, sentence: String, position: Int, expected: List<CompletionItem>) {
            val testGrammar = grammarFor(grammarStr)
            val context = ContextFromGrammar(testGrammar)
            val actual = aglProc.expectedItemsAt(sentence, position, 0, Agl.options {
                completionProvider {
                    context(context)
                }
            })
            assertTrue(actual.issues.errors.isEmpty(), actual.issues.toString())
            assertEquals(expected.size, actual.items.size)
            assertEquals(expected.toSet(), actual.items.toSet())
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
        val sentence = """
        """.trimIndent()
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "LITERAL", "'a'"),
            CompletionItem(CompletionItemKind.LITERAL, "GrammarRule", "S"),
            CompletionItem(CompletionItemKind.LITERAL, "META_IDENTIFIER", "\$keyword"),
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
        val sentence = """
            S
        """.trimIndent()
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "selectorAndComposition", ","),
            CompletionItem(CompletionItemKind.LITERAL, "rule", "{"),
            CompletionItem(CompletionItemKind.SEGMENT, "rule", "{\n  <STYLE_ID>: <STYLE_VALUE>;\n}"),
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
        val sentence = """
            S,
        """.trimIndent()
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "LITERAL", "'a'"),
            CompletionItem(CompletionItemKind.LITERAL, "GrammarRule", "S"),
            CompletionItem(CompletionItemKind.LITERAL, "META_IDENTIFIER", "\$keyword"),
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
        val sentence = """
            S {
        """.trimIndent()
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
        val sentence = """
            S {
              foreground
        """.trimIndent()
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
        val sentence = """
            S {
              foreground:
        """.trimIndent()
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
        val sentence = """
            S {
              foreground: blue
        """.trimIndent()
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
        val sentence = """
            S {
              foreground: blue;
        """.trimIndent()
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
        val sentence = """
            S {
              foreground: blue;
            }
        """.trimIndent()
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "LITERAL", "'a'"),
            CompletionItem(CompletionItemKind.LITERAL, "GrammarRule", "S"),
            CompletionItem(CompletionItemKind.LITERAL, "META_IDENTIFIER", "\$keyword"),
        )
        test(grammarStr, sentence, sentence.length, expected)
    }
}