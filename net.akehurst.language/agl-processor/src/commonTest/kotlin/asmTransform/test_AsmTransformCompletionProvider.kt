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

package net.akehurst.language.asmTransform.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.processor.contextFromGrammarAndTypesDomain
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.types.builder.typesDomain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_AsmTransformCompletionProvider {

    private companion object {
        val aglProc = Agl.registry.agl.asmTransform.processor!!

        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' B ;
                B = 'b' ;
            }
        """
        val grammar = Agl.registry.agl.grammar.processor!!.process(grammarStr).asm!!
        val types = typesDomain("Test", true) { }

        fun test(sentence: String, expected: List<CompletionItem>, withContext: Boolean = true) {
            val actual = aglProc.expectedItemsAt(sentence, sentence.length, Agl.options {
                completionProvider {
                    sentenceContext(if (withContext) contextFromGrammarAndTypesDomain(grammar, types) else null)
                }
            })
            assertTrue(actual.issues.errors.isEmpty(), actual.issues.toString())
            val refs = actual.items.filter { it.kind == CompletionItemKind.REFERRED }
            assertEquals(expected.joinToString("\n"), refs.joinToString("\n"), actual.items.joinToString("\n"))
        }
    }

    @Test
    fun grammarRuleName() {
        val sentence = "namespace test\n  asm-transform Test {\n    "
        test(sentence, listOf(
            CompletionItem(CompletionItemKind.REFERRED, "GrammarRule", "S"),
            CompletionItem(CompletionItemKind.REFERRED, "GrammarRule", "B"),
        ))
    }

    @Test
    fun no_context() {
        val sentence = "namespace test\n  asm-transform Test {\n    "
        test(sentence, listOf(), false)
    }
}
