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
import net.akehurst.language.api.processor.CompletionString
import net.akehurst.language.api.processor.GrammarString
import net.akehurst.language.completion.api.*
import net.akehurst.language.completion.asm.AglCompletionDomainDefault
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class test_AglCompletion {

    private companion object {
        val GRAMMAR = """
            namespace test
            grammar Test {
                skip leaf WS = "\s+" ;
                S = 'start' item ';' ;
                item = A | B ;
                A = 'a' ID ;
                B = 'b' ;
                leaf ID = "[a-z]+" ;
            }
        """.trimIndent()

        fun parse(completionStr: String) = AglCompletionDomainDefault.fromString(null, CompletionString(completionStr))

        fun items(completionStr: String?, sentence: String, depth: Int = 0): List<CompletionItem> {
            val res = Agl.processorFromStringSimple(GrammarString(GRAMMAR), completionStr = completionStr?.let { CompletionString(it) })
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            val proc = res.processor!!
            val result = proc.expectedItemsAt(sentence, sentence.length, Agl.options { completionProvider { depth(depth) } })
            assertTrue(result.issues.errors.isEmpty(), result.issues.toString())
            assertTrue(proc.allIssues.errors.isEmpty(), proc.allIssues.toString())
            return result.items
        }

        fun assertItems(expected: List<CompletionItem>, actual: List<CompletionItem>) {
            assertEquals(expected.joinToString("\n"), actual.joinToString("\n"))
        }

        fun completion(rules: String) = """
namespace test
completion Test {
$rules
}
        """.trimIndent()
    }

    @Test
    fun parse_all_actions() {
        val res = parse(
            completion(
                """
                item -> add [ CompletionItem() { label := 'an A' text := 'a <ID>' description := 'the A' } ]
                in S item -> replace [
                    CompletionItem() { kind := 'LITERAL' label := 'B' text := 'b' },
                    CompletionItem() { text := 'a x' }
                ]
                ID -> hide
                B -> decorate { label := 'bee' description := 'a B' }
                item -> depth 2
                """.trimIndent()
            )
        )
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val rules = res.asm!!.allCompletionRules
        assertEquals(5, rules.size)

        val add = rules[0]
        assertEquals(null, add.context)
        assertEquals("item", add.forItem)
        val addItem = (add.action as CompletionActionAdd).items.single()
        assertEquals(CompletionItemKind.SEGMENT, addItem.kind)
        assertEquals("an A", addItem.label)
        assertEquals("a <ID>", addItem.text)
        assertEquals("the A", addItem.description)

        val replace = rules[1]
        assertEquals("S", replace.context)
        val replaceItems = (replace.action as CompletionActionReplace).items
        assertEquals(listOf(CompletionItemKind.LITERAL, CompletionItemKind.SEGMENT), replaceItems.map { it.kind })
        assertEquals(listOf("B", "a x"), replaceItems.map { it.label }) // label defaults to the text

        assertTrue(rules[2].action is CompletionActionHide)
        val decorate = rules[3].action as CompletionActionDecorate
        assertEquals("bee", decorate.label)
        assertEquals(null, decorate.text)
        assertEquals("a B", decorate.description)
        assertEquals(2, (rules[4].action as CompletionActionDepth).depth)
    }

    @Test
    fun parse_errors() {
        val res = parse(
            completion(
                """
                item -> add [ CompletionItem() { colour := 'red' text := 'x' } ]
                item -> add [ CompletionItem() { kind := 'OTHER' text := 'x' } ]
                item -> add [ CompletionItem() { label := 'no text' } ]
                item -> add [ Other() { text := 'x' } ]
                item -> decorate { label := 1 }
                """.trimIndent()
            )
        )
        val messages = res.allIssues.errors.map { it.message }
        assertEquals(5, messages.size, res.allIssues.toString())
        assertTrue(messages[0].startsWith("Unknown property 'colour'"), messages[0])
        assertTrue(messages[1].startsWith("Unknown kind 'OTHER'"), messages[1])
        assertTrue(messages[2].startsWith("A CompletionItem needs a 'text'"), messages[2])
        assertTrue(messages[3].startsWith("Expected 'CompletionItem() { ... }'"), messages[3])
        assertTrue(messages[4].startsWith("Value of 'label' must be a literal string"), messages[4])
    }

    @Test
    fun no_completion_definitions() {
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.SEGMENT, "item", "<item>"),
                CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                CompletionItem(CompletionItemKind.LITERAL, "'b'", "b"),
            ),
            items(null, "start ")
        )
    }

    @Test
    fun hide() {
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                CompletionItem(CompletionItemKind.LITERAL, "'b'", "b"),
            ),
            items(completion("item -> hide"), "start ")
        )
    }

    @Test
    fun add() {
        val actual = items(completion("item -> add [ CompletionItem() { label := 'an A' text := 'a <ID>' description := 'the A' } ]"), "start ")
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.SEGMENT, "an A", "a <ID>"),
                CompletionItem(CompletionItemKind.SEGMENT, "item", "<item>"),
                CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                CompletionItem(CompletionItemKind.LITERAL, "'b'", "b"),
            ),
            actual
        )
        assertEquals("the A", actual[0].description)
    }

    @Test
    fun replace() {
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.SEGMENT, "an A", "a <ID>"),
                CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                CompletionItem(CompletionItemKind.LITERAL, "'b'", "b"),
            ),
            items(completion("item -> replace [ CompletionItem() { label := 'an A' text := 'a <ID>' } ]"), "start ")
        )
    }

    @Test
    fun add_not_expected_here() {
        // 'A' is not expected next after 'start ', so nothing is added
        assertItems(
            items(null, "start "),
            items(completion("A -> add [ CompletionItem() { text := 'a <ID>' } ]"), "start ")
        )
    }

    @Test
    fun decorate() {
        val actual = items(completion("item -> decorate { label := 'Item' description := 'an A or a B' }"), "start ")
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.SEGMENT, "Item", "<item>"),
                CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                CompletionItem(CompletionItemKind.LITERAL, "'b'", "b"),
            ),
            actual
        )
        assertEquals("an A or a B", actual[0].description)
    }

    @Test
    fun depth() {
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.SEGMENT, "A", "<A>"),
                CompletionItem(CompletionItemKind.SEGMENT, "B", "<B>"),
                CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                CompletionItem(CompletionItemKind.LITERAL, "'b'", "b"),
            ),
            items(completion("item -> depth 1"), "start ")
        )
    }

    @Test
    fun context_matches() {
        val actual = items(completion("in A ID -> decorate { description := 'name of the A' }"), "start a ")
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.PATTERN, "ID", "<ID>"),
            ),
            actual
        )
        assertEquals("name of the A", actual[0].description)
    }

    @Test
    fun context_does_not_match() {
        val actual = items(completion("in B ID -> decorate { description := 'name of the B' }"), "start a ")
        assertEquals("[a-z]+", actual.single().description)
    }
}
