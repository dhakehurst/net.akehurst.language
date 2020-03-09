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

import net.akehurst.language.api.analyser.AsmElementSimple
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.processor.Agl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_AglGrammar_expectedAt {

    @Test
    fun empty() {

        val sentence = ""
        val actual = Agl.grammarProcessor.expectedAt(sentence, 0, 1)

        val expected = listOf<CompletionItem>(
                CompletionItem(Agl.grammarProcessor.grammar.findAllRule("grammarDefinition"), "namespace"),
                CompletionItem(Agl.grammarProcessor.grammar.findAllRule("grammarDefinition"), "WHITE_SPACE"),
                CompletionItem(Agl.grammarProcessor.grammar.findAllRule("grammarDefinition"), "COMMENT"),
                CompletionItem(Agl.grammarProcessor.grammar.findAllRule("grammarDefinition"), "COMMENT")
        )

        assertEquals(expected, actual)
    }


}
