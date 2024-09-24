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
import net.akehurst.language.grammar.api.GrammarModel
import net.akehurst.language.grammar.processor.ContextFromGrammar
import net.akehurst.language.style.asm.AglStyleModelDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_CompletionProvider {

    private companion object {
        val aglProc = Agl.registry.agl.style.processor!!

        fun grammarFor(grammarStr: String): GrammarModel {
            return Agl.registry.agl.grammar.processor?.process(grammarStr)?.asm!!
        }

        fun test(grammarStr: String, sentence: String, position: Int, expected: List<CompletionItem>) {
            val testGrammar = grammarFor(grammarStr)
            val context = ContextFromGrammar.createContextFrom(testGrammar)
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
        val sentence = ""
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "'a'", "LITERAL"),
            CompletionItem(CompletionItemKind.LITERAL, "S", "GrammarRule"),
            CompletionItem(CompletionItemKind.LITERAL, AglStyleModelDefault.KEYWORD_STYLE_ID, "META_IDENTIFIER"),
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
        val sentence = "S"
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, ",", "selectorAndComposition"),
            CompletionItem(CompletionItemKind.LITERAL, "{", "rule"),
            CompletionItem(CompletionItemKind.SEGMENT, "{\n  <STYLE_ID>: <STYLE_VALUE>;\n}", "rule"),
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
        val sentence = "S,"
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "'a'", "LITERAL"),
            CompletionItem(CompletionItemKind.LITERAL, "S", "GrammarRule"),
            CompletionItem(CompletionItemKind.LITERAL, AglStyleModelDefault.KEYWORD_STYLE_ID, "META_IDENTIFIER"),
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
        val sentence = "S {"
        val expected = listOf(
            CompletionItem(CompletionItemKind.LITERAL, "foreground", "STYLE_ID"),
            CompletionItem(CompletionItemKind.LITERAL, "background", "STYLE_ID"),
            CompletionItem(CompletionItemKind.LITERAL, "font-style", "STYLE_ID"),
            CompletionItem(CompletionItemKind.LITERAL, "}", "rule"),
            CompletionItem(CompletionItemKind.SEGMENT, "<STYLE_ID>: <STYLE_VALUE>;", "style"),
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
            CompletionItem(CompletionItemKind.LITERAL, ":", "style"),
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
            CompletionItem(CompletionItemKind.LITERAL, "<colour>", "STYLE_VALUE"),
            CompletionItem(CompletionItemKind.LITERAL, "bold", "STYLE_VALUE"),
            CompletionItem(CompletionItemKind.LITERAL, "italic", "STYLE_VALUE"),
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
            CompletionItem(CompletionItemKind.LITERAL, "<colour>", "STYLE_VALUE"),
            CompletionItem(CompletionItemKind.LITERAL, "bold", "STYLE_VALUE"),
            CompletionItem(CompletionItemKind.LITERAL, "italic", "STYLE_VALUE"),
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
            CompletionItem(CompletionItemKind.LITERAL, ";", "style"),
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
            CompletionItem(CompletionItemKind.LITERAL, "foreground", "STYLE_ID"),
            CompletionItem(CompletionItemKind.LITERAL, "background", "STYLE_ID"),
            CompletionItem(CompletionItemKind.LITERAL, "font-style", "STYLE_ID"),
            CompletionItem(CompletionItemKind.LITERAL, "}", "rule"),
            CompletionItem(CompletionItemKind.SEGMENT, "<STYLE_ID>: <STYLE_VALUE>;", "style"),
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
            CompletionItem(CompletionItemKind.LITERAL, "'a'", "LITERAL"),
            CompletionItem(CompletionItemKind.LITERAL, "S", "GrammarRule"),
            CompletionItem(CompletionItemKind.LITERAL, AglStyleModelDefault.KEYWORD_STYLE_ID, "META_IDENTIFIER"),
        )
        test(grammarStr, sentence, sentence.length, expected)
    }
}