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

package net.akehurst.language.agl.grammar.grammar

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import kotlin.test.Test
import kotlin.test.assertEquals

class test_AglGrammar_expectedTerminalsAt {

    @Test
    fun empty() {

        val sentence = ""
        val result = Agl.registry.agl.grammar.processor!!.expectedTerminalsAt(sentence, 0, 1)

        val expected = listOf<CompletionItem>(
            CompletionItem(CompletionItemKind.LITERAL, "namespace", "'namespace'")
        )

        assertEquals(expected, result.items)
    }

    @Test
    fun WS() {

        val sentence = " "
        val result = Agl.registry.agl.grammar.processor!!.expectedTerminalsAt(sentence, 0, 1)

        val expected = listOf<CompletionItem>(
            CompletionItem(CompletionItemKind.LITERAL, "namespace", "'namespace'")
        )

        assertEquals(expected, result.items)
    }

    @Test
    fun namespace() {

        val sentence = "namespace"
        val result = Agl.registry.agl.grammar.processor!!.expectedTerminalsAt(sentence, 9, 1)

        val expected = listOf<CompletionItem>(
            CompletionItem(CompletionItemKind.PATTERN, "<IDENTIFIER>", "[a-zA-Z_][a-zA-Z_0-9-]*")
        )

        assertEquals(expected, result.items)
    }

    @Test
    fun namespace_WS() {

        val sentence = "namespace "
        val result = Agl.registry.agl.grammar.processor!!.expectedTerminalsAt(sentence, 10, 1)

        val expected = listOf<CompletionItem>(
            CompletionItem(CompletionItemKind.PATTERN, "<IDENTIFIER>", "[a-zA-Z_][a-zA-Z_0-9-]*")
        )

        assertEquals(expected, result.items)
    }

    @Test
    fun namespace_WS_n() {

        val sentence = "namespace n"
        val result = Agl.registry.agl.grammar.processor!!.expectedTerminalsAt(sentence, 11, 1)

        val expected = listOf<CompletionItem>(
            CompletionItem(CompletionItemKind.LITERAL, ".", "'.'"),
            CompletionItem(CompletionItemKind.LITERAL, "grammar", "'grammar'")
        )

        assertEquals(expected, result.items)
    }

    @Test
    fun namespace_WS_n_grammar() {

        val sentence = "namespace n grammar"
        val result = Agl.registry.agl.grammar.processor!!.expectedTerminalsAt(sentence, sentence.length, 1)

        val expected = listOf<CompletionItem>(
            CompletionItem(CompletionItemKind.PATTERN, "<IDENTIFIER>", "[a-zA-Z_][a-zA-Z_0-9-]*")
        )

        assertEquals(expected, result.items)
    }
}
