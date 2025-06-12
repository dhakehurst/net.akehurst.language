/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

package net.akehurst.language.grammar.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import kotlin.test.Test
import kotlin.test.assertEquals

class test_AglGrammar_expectedItemsAt {

    @Test
    fun empty() {

        val sentence = ""
        val result = Agl.registry.agl.grammar.processor!!.expectedItemsAt(sentence, 0)

        val expected = listOf<CompletionItem>(
            CompletionItem(CompletionItemKind.SEGMENT, "namespace", "<namespace>"),
            CompletionItem(CompletionItemKind.SEGMENT, "unit", "<option> <namespace>"),
            CompletionItem(CompletionItemKind.LITERAL, "'#'", "#"),
            CompletionItem(CompletionItemKind.LITERAL, "'namespace'", "namespace"),
        )

        assertEquals(expected.joinToString("\n"), result.items.joinToString("\n"))
    }

    @Test
    fun WS() {

        val sentence = " "
        val result = Agl.registry.agl.grammar.processor!!.expectedItemsAt(sentence, 0)

        val expected = listOf<CompletionItem>(
            CompletionItem(CompletionItemKind.SEGMENT, "namespace", "<namespace>"),
            CompletionItem(CompletionItemKind.SEGMENT, "unit", "<option> <namespace>"),
            CompletionItem(CompletionItemKind.LITERAL, "'#'", "#"),
            CompletionItem(CompletionItemKind.LITERAL, "'namespace'", "namespace"),
        )

        assertEquals(expected, result.items)
    }

    @Test
    fun namespace() {

        val sentence = "namespace"
        val result = Agl.registry.agl.grammar.processor!!.expectedItemsAt(sentence, sentence.length)

        val expected = listOf<CompletionItem>(
            CompletionItem(CompletionItemKind.LITERAL, "'namespace'", "namespace")
        )

        assertEquals(expected, result.items)
    }

    @Test
    fun namespace_WS() {

        val sentence = "namespace "
        val result = Agl.registry.agl.grammar.processor!!.expectedItemsAt(sentence, sentence.length)

        val expected = listOf<CompletionItem>(
            CompletionItem(CompletionItemKind.SEGMENT, "possiblyQualifiedName", "<possiblyQualifiedName>"),
            CompletionItem(CompletionItemKind.PATTERN, "[a-zA-Z_][a-zA-Z_0-9-]*", "<IDENTIFIER>")
        )

        assertEquals(expected, result.items)
    }

    @Test
    fun namespace_WS_n_WS() {

        val sentence = "namespace n "
        val result = Agl.registry.agl.grammar.processor!!.expectedItemsAt(sentence, sentence.length, options = Agl.options { completionProvider { depth(2) } })

        val expected = listOf<CompletionItem>(
            CompletionItem(CompletionItemKind.SEGMENT, "grammar", "grammar <IDENTIFIER> <extends>? { <option> <rule> }"),
            CompletionItem(CompletionItemKind.SEGMENT, "import", "import <possiblyQualifiedName>"),
            CompletionItem(CompletionItemKind.SEGMENT, "namespace", "namespace <possiblyQualifiedName> <option> <import> <grammar>"),
            CompletionItem(CompletionItemKind.SEGMENT, "option", "# <IDENTIFIER> : <IDENTIFIER>?"),
            CompletionItem(CompletionItemKind.LITERAL, "'#'", "#"),
            CompletionItem(CompletionItemKind.LITERAL, "'.'", "."),
            CompletionItem(CompletionItemKind.LITERAL, "'grammar'", "grammar"),
            CompletionItem(CompletionItemKind.LITERAL, "'import'", "import"),
            CompletionItem(CompletionItemKind.LITERAL, "'namespace'", "namespace"),
        )

        assertEquals(expected.size, result.items.size, result.items.joinToString("\n"))
        assertEquals(expected.joinToString("\n"), result.items.joinToString("\n"))
    }

    @Test
    fun namespace_WS_n_grammar_WS() {

        val sentence = "namespace n grammar "
        val result = Agl.registry.agl.grammar.processor!!.expectedItemsAt(sentence, sentence.length)

        val expected = listOf<CompletionItem>(
            CompletionItem(CompletionItemKind.PATTERN, "[a-zA-Z_][a-zA-Z_0-9-]*", "<IDENTIFIER>")
        )

        assertEquals(expected, result.items)
    }
}
