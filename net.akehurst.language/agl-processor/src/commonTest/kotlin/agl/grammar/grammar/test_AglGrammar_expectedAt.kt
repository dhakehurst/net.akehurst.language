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

import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.api.processor.CompletionItem
import kotlin.test.Test
import kotlin.test.assertEquals

class test_AglGrammar_expectedAt {

    @Test
    fun empty() {

        val sentence = ""
        val actual = Agl.grammarProcessor.expectedAt(sentence, 0, 1)

        val expected = listOf<CompletionItem>(
                CompletionItem(Agl.grammarProcessor.grammar.findAllRule("namespace"), "namespace")
        )

        assertEquals(expected, actual)
    }

    @Test
    fun WS() {

        val sentence = " "
        val actual = Agl.grammarProcessor.expectedAt(sentence, 0, 1)

        val expected = listOf<CompletionItem>(
                CompletionItem(Agl.grammarProcessor.grammar.findAllRule("namespace"), "namespace")
        )

        assertEquals(expected, actual)
    }

    @Test
    fun namespace() {

        val sentence = "namespace"
        val actual = Agl.grammarProcessor.expectedAt(sentence, 9, 1)

        val expected = listOf<CompletionItem>(
                CompletionItem(Agl.grammarProcessor.grammar.findAllRule("IDENTIFIER"), "IDENTIFIER")
        )

        assertEquals(expected, actual)
    }

    @Test
    fun namespace_WS() {

        val sentence = "namespace "
        val actual = Agl.grammarProcessor.expectedAt(sentence, 10, 1)

        val expected = listOf<CompletionItem>(
                CompletionItem(Agl.grammarProcessor.grammar.findAllRule("IDENTIFIER"), "IDENTIFIER")
        )

        assertEquals(expected, actual)
    }

    @Test
    fun namespace_WS_n() {

        val sentence = "namespace n"
        val actual = Agl.grammarProcessor.expectedAt(sentence, 11, 1)

        val expected = listOf<CompletionItem>(
                CompletionItem(Agl.grammarProcessor.grammar.findAllRule("qualifiedName"), "."),
                CompletionItem(Agl.grammarProcessor.grammar.findAllRule("grammar"), "grammar")
        )

        assertEquals(expected, actual)
    }

    @Test
    fun namespace_WS_n_grammar() {

        val sentence = "namespace n grammar"
        val actual = Agl.grammarProcessor.expectedAt(sentence, sentence.length, 1)

        val expected = listOf<CompletionItem>(
                CompletionItem(Agl.grammarProcessor.grammar.findAllRule("IDENTIFIER"), "IDENTIFIER")
        )

        assertEquals(expected, actual)
    }
}
