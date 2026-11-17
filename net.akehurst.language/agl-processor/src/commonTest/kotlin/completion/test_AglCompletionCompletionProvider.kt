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

package net.akehurst.language.completion.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.agl.semanticAnalyser.contextFromTypesDomain
import net.akehurst.language.api.processor.CrossReferenceString
import net.akehurst.language.grammar.processor.contextFromGrammar
import net.akehurst.language.reference.asm.CrossReferenceDomainDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_AglCompletionCompletionProvider {

    private companion object {
        val aglProc = Agl.registry.agl.completion.processor!!

        val grammarStr = """
            namespace test
            grammar Test {
                S = 'a' B ;
                B = 'b' ;
            }
        """

        fun grammarFor(grammarStr: String): GrammarDomain {
            return Agl.registry.agl.grammar.processor?.process(grammarStr)?.asm!!
        }

        fun test(sentence: String, position: Int, expected: List<CompletionItem>, withContext: Boolean = true) {
            val context = if (withContext) contextFromGrammar(grammarFor(grammarStr)) else null
            val actual = aglProc.expectedItemsAt(sentence, position, Agl.options {
                completionProvider {
                    sentenceContext(context)
                }
            })
            assertTrue(actual.issues.errors.isEmpty(), actual.issues.toString())
            assertEquals(expected.joinToString("\n"), actual.items.joinToString("\n"))
        }
    }

    @Test
    fun forItem_rule_names() {
        val sentence = "namespace test completion Test { "
        val expected = listOf(
            CompletionItem(CompletionItemKind.REFERRED, "GrammarRule", "S"),
            CompletionItem(CompletionItemKind.REFERRED, "GrammarRule", "B"),
            CompletionItem(CompletionItemKind.SEGMENT, "completionRule", "<completionRule>"),
            CompletionItem(CompletionItemKind.SEGMENT, "forItem", "<forItem>"),
            CompletionItem(CompletionItemKind.SEGMENT, "option", "<option>"),
            CompletionItem(CompletionItemKind.LITERAL, "'#'", "#"),
            CompletionItem(CompletionItemKind.LITERAL, "'in'", "in"),
            CompletionItem(CompletionItemKind.LITERAL, "'}'", "}"),
            CompletionItem(CompletionItemKind.PATTERN, "STRING", "<STRING>"),
        )
        test(sentence, sentence.length, expected)
    }

    @Test
    fun context_rule_names() {
        val sentence = "namespace test completion Test { in "
        // 'in' could also be the name of the rule the completion rule is for, then '->' is next
        val expected = listOf(
            CompletionItem(CompletionItemKind.REFERRED, "GrammarRule", "S"),
            CompletionItem(CompletionItemKind.REFERRED, "GrammarRule", "B"),
            CompletionItem(CompletionItemKind.LITERAL, "'->'", "->"),
        )
        test(sentence, sentence.length, expected)
    }

    @Test
    fun forItem_after_context_rule_names() {
        val sentence = "namespace test completion Test { in S "
        val expected = listOf(
            CompletionItem(CompletionItemKind.REFERRED, "GrammarRule", "S"),
            CompletionItem(CompletionItemKind.REFERRED, "GrammarRule", "B"),
            CompletionItem(CompletionItemKind.SEGMENT, "forItem", "<forItem>"),
            CompletionItem(CompletionItemKind.PATTERN, "STRING", "<STRING>"),
        )
        test(sentence, sentence.length, expected)
    }

    @Test
    fun forItem_no_context() {
        val sentence = "namespace test completion Test { "
        val expected = listOf(
            CompletionItem(CompletionItemKind.SEGMENT, "completionRule", "<completionRule>"),
            CompletionItem(CompletionItemKind.SEGMENT, "forItem", "<forItem>"),
            CompletionItem(CompletionItemKind.SEGMENT, "option", "<option>"),
            CompletionItem(CompletionItemKind.LITERAL, "'#'", "#"),
            CompletionItem(CompletionItemKind.LITERAL, "'in'", "in"),
            CompletionItem(CompletionItemKind.LITERAL, "'}'", "}"),
            CompletionItem(CompletionItemKind.PATTERN, "IDENTIFIER", "<IDENTIFIER>"),
            CompletionItem(CompletionItemKind.PATTERN, "STRING", "<STRING>"),
        )
        test(sentence, sentence.length, expected, false)
    }

    @Test
    fun crossReferenceString_same_as_crossReferenceDomain() {
        val res = CrossReferenceDomainDefault.fromString(contextFromTypesDomain(AglCompletion.typesDomain), CrossReferenceString(AglCompletion.crossReferenceString))
        // GrammarRule is not a type of the AglCompletion types domain (it is the type of items in a context from a grammar), so that is the only error
        assertEquals(
            listOf("For references in 'CompletionRule', referred to type 'GrammarRule' not found"),
            res.allIssues.errors.map { it.message }.distinct(),
            res.allIssues.toString()
        )
        assertEquals(AglCompletion.crossReferenceDomain.asString(), res.asm!!.asString())
    }
}
