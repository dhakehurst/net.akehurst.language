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
import net.akehurst.language.completion.builder.completionDomain
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.LiteralExpression
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

        /** the value of a literal string expression, null for null */
        fun str(exp: Expression?): String? = exp?.let { (it as LiteralExpression).value as String }

        fun items(completionStr: String?, sentence: String, depth: Int = 0, path: List<Pair<Int, Int>> = emptyList()): List<CompletionItem> {
            val res = Agl.processorFromStringSimple(GrammarString(GRAMMAR), completionStr = completionStr?.let { CompletionString(it) })
            assertTrue(res.issues.errors.isEmpty(), res.issues.toString())
            val proc = res.processor!!
            val result = proc.expectedItemsAt(sentence, sentence.length, Agl.options { completionProvider { depth(depth); path(path) } })
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
        assertEquals("an A", str(addItem.label))
        assertEquals("a <ID>", str(addItem.text))
        assertEquals("the A", str(addItem.description))

        val replace = rules[1]
        assertEquals("S", replace.context)
        val replaceItems = (replace.action as CompletionActionReplace).items
        assertEquals(listOf(CompletionItemKind.LITERAL, CompletionItemKind.SEGMENT), replaceItems.map { it.kind })
        assertEquals(listOf("B", null), replaceItems.map { str(it.label) }) // no label: the same as the text

        assertTrue(rules[2].action is CompletionActionHide)
        val decorate = rules[3].action as CompletionActionDecorate
        assertEquals("bee", str(decorate.label))
        assertEquals(null, decorate.text)
        assertEquals("a B", str(decorate.description))
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
        assertTrue(messages[4].startsWith("Value of 'label' must be a String"), messages[4])
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

    @Test
    fun parse_literal_keys() {
        val res = parse(
            completion(
                """
                ';' -> decorate { label := 'end' }
                in S 'a' -> hide
                '\'' -> hide
                item -> hide
                """.trimIndent()
            )
        )
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        val rules = res.asm!!.allCompletionRules
        assertEquals(listOf(";", "a", "'", "item"), rules.map { it.forItem })
        assertEquals(listOf(true, true, true, false), rules.map { it.forLiteral })
        assertEquals("in S 'a' -> hide", rules[1].toString())
        assertEquals("'\\'' -> hide", rules[2].toString())
    }

    @Test
    fun literal_decorate() {
        val actual = items(completion("'a' -> decorate { label := 'letter a' description := 'starts an A' }"), "start ")
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.SEGMENT, "item", "<item>"),
                CompletionItem(CompletionItemKind.LITERAL, "'b'", "b"),
                CompletionItem(CompletionItemKind.LITERAL, "letter a", "a"),
            ),
            actual
        )
        assertEquals("starts an A", actual[2].description)
    }

    @Test
    fun literal_hide() {
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.SEGMENT, "item", "<item>"),
                CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
            ),
            items(completion("'b' -> hide"), "start ")
        )
    }

    @Test
    fun literal_add_snippet_where_only_a_literal_is_expected() {
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.SEGMENT, "end", "; start <item>;"),
                CompletionItem(CompletionItemKind.LITERAL, "';'", ";"),
            ),
            items(completion("';' -> add [ CompletionItem() { label := 'end' text := '; start <item>;' } ]"), "start a x ")
        )
    }

    @Test
    fun literal_replace() {
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.SEGMENT, "end", "; start <item>;"),
            ),
            items(completion("';' -> replace [ CompletionItem() { label := 'end' text := '; start <item>;' } ]"), "start a x ")
        )
    }

    @Test
    fun literal_context() {
        assertItems(
            emptyList(),
            items(completion("in S ';' -> hide"), "start a x ")
        )
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.LITERAL, "';'", ";"),
            ),
            items(completion("in B ';' -> hide"), "start a x ")
        )
    }

    @Test
    fun decorate_with_expressions() {
        val actual = items(completion("item -> decorate { label := 'the ' + \$self.label description := \$expected + ' in ' + \$rules.first }"), "start ")
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.SEGMENT, "the item", "<item>"),
                CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                CompletionItem(CompletionItemKind.LITERAL, "'b'", "b"),
            ),
            actual
        )
        assertEquals("item in S", actual[0].description)
    }

    @Test
    fun literal_decorate_with_expressions() {
        val actual = items(completion("';' -> decorate { label := 'end of ' + \$rules.first description := \$self.text }"), "start a x ")
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.LITERAL, "end of S", ";"),
            ),
            actual
        )
        assertEquals(";", actual[0].description)
    }

    @Test
    fun add_with_expressions() {
        val actual = items(completion("item -> add [ CompletionItem() { text := \$expected + ' here' description := \$rules.first } ]"), "start ")
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.SEGMENT, "item", "<item>"),
                CompletionItem(CompletionItemKind.SEGMENT, "item here", "item here"),
                CompletionItem(CompletionItemKind.LITERAL, "'a'", "a"),
                CompletionItem(CompletionItemKind.LITERAL, "'b'", "b"),
            ),
            actual
        )
        assertEquals("S", actual[1].description)
    }

    @Test
    fun add_with_expression_that_is_not_a_string() {
        // the text does not give a String, so the item is left out
        assertItems(
            items(null, "start "),
            items(completion("item -> add [ CompletionItem() { text := \$rules } ]"), "start ")
        )
    }

    @Test
    fun path_mode_without_rules() {
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.SEGMENT, "A", "<A>"),
                CompletionItem(CompletionItemKind.SEGMENT, "B", "<B>"),
            ),
            items(null, "start ", path = listOf(Pair(1, -1)))
        )
    }

    @Test
    fun path_mode_applies_rules() {
        val actual = items(
            completion(
                """
                B -> decorate { label := 'bee' description := 'a B' }
                A -> hide
                B -> add [ CompletionItem() { label := 'just b' text := 'b' } ]
                """.trimIndent()
            ),
            "start ", path = listOf(Pair(1, -1))
        )
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.SEGMENT, "bee", "<B>"),
                CompletionItem(CompletionItemKind.SEGMENT, "just b", "b"),
            ),
            actual
        )
        assertEquals("a B", actual[0].description)
    }

    @Test
    fun path_mode_applies_depth() {
        assertItems(
            listOf(
                CompletionItem(CompletionItemKind.SEGMENT, "A", "a <ID>"),
                CompletionItem(CompletionItemKind.SEGMENT, "B", "b"),
            ),
            items(completion("item -> depth 2"), "start ", path = listOf(Pair(1, -1)))
        )
    }

    @Test
    fun builder_same_as_parsed() {
        val built = completionDomain("Test") {
            namespace("test") {
                completion("Test") {
                    add("item") { item(text = "a <ID>", label = "an A", description = "the A") }
                    replace("item", context = "S") { item(text = "b", kind = CompletionItemKind.LITERAL) }
                    hide("ID")
                    decorate("B", label = "bee")
                    depth("item", 2)
                    decorate("b", label = "bee", forLiteral = true)
                    hide(";", context = "S", forLiteral = true)
                }
            }
        }
        val parsed = parse(
            completion(
                """
                item -> add [ CompletionItem() { label := 'an A' text := 'a <ID>' description := 'the A' } ]
                in S item -> replace [ CompletionItem() { kind := 'LITERAL' text := 'b' } ]
                ID -> hide
                B -> decorate { label := 'bee' }
                item -> depth 2
                'b' -> decorate { label := 'bee' }
                in S ';' -> hide
                """.trimIndent()
            )
        )
        assertTrue(parsed.allIssues.errors.isEmpty(), parsed.allIssues.toString())
        assertEquals(parsed.asm!!.allCompletionRules.map { it.toString() }, built.allCompletionRules.map { it.toString() })
    }

    @Test
    fun unknown_rule_names() {
        val res = Agl.processorFromStringSimple(
            GrammarString(GRAMMAR),
            completionStr = CompletionString(completion("in X item -> hide\nY -> hide\nin S ID -> hide"))
        )
        val proc = res.processor!!
        proc.completionDomain
        val messages = proc.allIssues.errors.map { it.message }
        assertEquals(
            listOf(
                "Grammar Rule 'X' not found for completion rule",
                "Grammar Rule 'Y' not found for completion rule",
            ),
            messages,
            proc.allIssues.toString()
        )
        assertTrue(proc.allIssues.errors.all { null != it.location }, proc.allIssues.toString())
    }

    @Test
    fun aglStyle_completionString_same_as_completionDomain() {
        val res = AglCompletionDomainDefault.fromString(
            net.akehurst.language.grammar.processor.contextFromGrammar(net.akehurst.language.style.processor.AglStyle.grammarDomain),
            CompletionString(net.akehurst.language.style.processor.AglStyle.completionString)
        )
        assertTrue(res.allIssues.errors.isEmpty(), res.allIssues.toString())
        assertEquals(
            net.akehurst.language.style.processor.AglStyle.completionDomain.allCompletionRules.map { it.toString() },
            res.asm!!.allCompletionRules.map { it.toString() }
        )
    }
}
